package com.example.tg2_23102_22891

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var audioManager: AudioManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Inicializa TTS antes de usar
        textToSpeech = TextToSpeech(this, this)

        // Serviços do sistema
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        // Referências dos botões
        val btnHome = findViewById<Button>(R.id.btnHome)
        val btnSchool = findViewById<Button>(R.id.btnSchool)
        val btnPark = findViewById<Button>(R.id.btnPark)
        val btnHelp = findViewById<Button>(R.id.btnHelp)
        val btnConfigHelp = findViewById<Button>(R.id.btnConfigHelp)

        // Dois cliques para todos os botões de destino
        setupDoubleClickButton(
            btnHome,
            getString(R.string.home_button),
            textToSpeech
        ) {
            selectDestination(getString(R.string.home_button))
        }

        setupDoubleClickButton(
            btnSchool,
            getString(R.string.school_button),
            textToSpeech
        ) {
            selectDestination(getString(R.string.school_button))
        }

        setupDoubleClickButton(
            btnPark,
            getString(R.string.park_button),
            textToSpeech
        ) {
            selectDestination(getString(R.string.park_button))
        }

        // Botão de ajuda: faz TTS, depois liga para o número guardado
        setupDoubleClickButton(
            btnHelp,
            getString(R.string.help_button),
            textToSpeech
        ) {
            val (helpName, helpPhone) = getHelpContact()
            if (helpPhone.isEmpty()) {
                Toast.makeText(this, "Configura primeiro o contacto de ajuda!", Toast.LENGTH_SHORT).show()
                return@setupDoubleClickButton
            }
            val emergencyMsg = getString(R.string.calling_emergency) + " $helpName"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(emergencyMsg, TextToSpeech.QUEUE_FLUSH, null, "EMERGENCY_CALL")
                textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "EMERGENCY_CALL") {
                            runOnUiThread {
                                callEmergencyNumber(helpPhone)
                            }
                        }
                    }
                })
            } else {
                @Suppress("DEPRECATION")
                textToSpeech.speak(emergencyMsg, TextToSpeech.QUEUE_FLUSH, null)
                Handler(Looper.getMainLooper()).postDelayed({
                    callEmergencyNumber(helpPhone)
                }, 2000)
            }
        }

        // Botão para configurar contacto de ajuda
        setupDoubleClickButton(
            btnConfigHelp,
            getString(R.string.config_help_button),
            textToSpeech
        ) {
            startActivity(Intent(this, ConfigHelpActivity::class.java))
        }

    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("pt", "PT")
            speak(getString(R.string.welcome_message))
        }
    }

    private fun selectDestination(destination: String) {
        vibrate(100)
        speak(getString(R.string.destination_selected, destination))
        startActivity(Intent(this, NavigationActivity::class.java).apply {
            putExtra("DESTINATION", destination)
        })
    }

    private fun speak(text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            @Suppress("DEPRECATION")
            textToSpeech.speak(text, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    private fun vibrate(milliseconds: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(milliseconds)
        }
    }

    // Função utilitária para dois cliques para confirmar
    private fun setupDoubleClickButton(
        button: Button,
        buttonLabel: String,
        tts: TextToSpeech,
        onConfirmed: () -> Unit
    ) {
        var clickedOnce = false
        button.setOnClickListener {
            if (!clickedOnce) {
                tts.speak(buttonLabel, TextToSpeech.QUEUE_FLUSH, null, null)
                clickedOnce = true
                button.postDelayed({ clickedOnce = false }, 2000)
            } else {
                clickedOnce = false
                onConfirmed()
            }
        }
    }

    // Lê o contacto de ajuda guardado
    private fun getHelpContact(): Pair<String, String> {
        val prefs = getSharedPreferences("help_contact", Context.MODE_PRIVATE)
        val name = prefs.getString("help_name", "") ?: ""
        val phone = prefs.getString("help_phone", "") ?: ""
        return Pair(name, phone)
    }

    // Função para chamada de emergência
    private fun callEmergencyNumber(phoneNumber: String) {
        val callIntent = Intent(Intent.ACTION_CALL)
        callIntent.data = Uri.parse("tel:$phoneNumber")
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), 1)
        } else {
            startActivity(callIntent)
        }
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
