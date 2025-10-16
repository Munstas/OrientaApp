package com.example.tg2_23102_22891

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class NavigationActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var vibrator: Vibrator
    private lateinit var destination: String
    private lateinit var audioManager: AudioManager
    private lateinit var tvNavigatingTo: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var handler: Handler
    private val navigationRunnables = mutableListOf<Runnable>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_navigation)

        // Inicializa TTS antes de usar
        textToSpeech = TextToSpeech(this, this)

        destination = intent.getStringExtra("DESTINATION") ?: getString(R.string.default_destination)

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VibratorManager::class.java)
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }

        audioManager = getSystemService(AudioManager::class.java)

        tvNavigatingTo = findViewById(R.id.tvNavigatingTo)
        progressBar = findViewById(R.id.progressBarNavigation)
        progressBar.progress = 0

        // Usa getString com placeholder para mostrar o destino corretamente
        tvNavigatingTo.text = getString(R.string.navigating_to, destination)

        val btnStop = findViewById<Button>(R.id.btnStop)
        setupDoubleClickButton(
            btnStop,
            getString(R.string.navigation_canceled),
            textToSpeech
        ) {
            vibrate(100)
            navigationRunnables.forEach { handler.removeCallbacks(it) }
            finish()
        }

        val btnHelp = findViewById<Button>(R.id.btnHelp)
        setupDoubleClickButton(
            btnHelp,
            getString(R.string.help_button),
            textToSpeech
        ) {
            val emergencyMsg = getString(R.string.calling_emergency) // "Ligando ao número de emergência"
            val phoneNumber = "964254324" // Substitui pelo número desejado

            // 1. Fala a mensagem de emergência
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(emergencyMsg, TextToSpeech.QUEUE_FLUSH, null, "EMERGENCY_CALL")
                textToSpeech.setOnUtteranceProgressListener(object : android.speech.tts.UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {}
                    override fun onError(utteranceId: String?) {}
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "EMERGENCY_CALL") {
                            runOnUiThread {
                                // 2. Volta ao ecrã inicial
                                val intent = Intent(this@NavigationActivity, MainActivity::class.java)
                                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                startActivity(intent)
                                finish()
                                // 3. Liga ao número de emergência
                                callEmergencyNumber(phoneNumber)
                            }
                        }
                    }
                })
            } else {
                @Suppress("DEPRECATION")
                textToSpeech.speak(emergencyMsg, TextToSpeech.QUEUE_FLUSH, null)
                Handler(Looper.getMainLooper()).postDelayed({
                    val intent = Intent(this, MainActivity::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                    startActivity(intent)
                    finish()
                    callEmergencyNumber(phoneNumber)
                }, 2000) // Ajusta o tempo conforme a duração da mensagem
            }
            // Cancela navegação imediatamente para evitar instruções futuras
            navigationRunnables.forEach { handler.removeCallbacks(it) }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("pt", "PT")
            speak(getString(R.string.navigating_to, destination))
            simulateNavigation()
        }
    }

    private fun simulateNavigation() {
        val instructions = listOf(
            getString(R.string.turn_left, 20),
            getString(R.string.go_straight, 50),
            getString(R.string.turn_right, 10),
            getString(R.string.arrived_at, destination)
        )

        handler = Handler(Looper.getMainLooper())
        navigationRunnables.clear()

        val totalSteps = instructions.size
        instructions.forEachIndexed { index, instruction ->
            val runnable = Runnable {
                vibrate(50)
                speak(instruction)
                val progress = ((index + 1) * 100) / totalSteps
                progressBar.progress = progress

                if (index == instructions.size - 1) {
                    startActivity(Intent(this, ArrivalActivity::class.java).apply {
                        putExtra("DESTINATION", destination)
                    })
                    finish()
                }
            }
            navigationRunnables.add(runnable)
            handler.postDelayed(runnable, (index * 5000L) + 2000L)
        }
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
        if (::handler.isInitialized) {
            navigationRunnables.forEach { handler.removeCallbacks(it) }
        }
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
