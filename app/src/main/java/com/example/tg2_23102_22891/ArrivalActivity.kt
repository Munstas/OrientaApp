package com.example.tg2_23102_22891

import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ArrivalActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var textToSpeech: TextToSpeech
    private lateinit var audioManager: AudioManager
    private lateinit var destination: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_arrival)

        destination = intent.getStringExtra("DESTINATION") ?: getString(R.string.default_destination)
        audioManager = getSystemService(AudioManager::class.java)

        val btnHome = findViewById<Button>(R.id.btnHome)
        val tvArrivedAt = findViewById<TextView>(R.id.tvArrivedAt)
        tvArrivedAt?.text = getString(R.string.arrived_at, destination)

        textToSpeech = TextToSpeech(this, this)

        setupDoubleClickButton(
            btnHome,
            getString(R.string.returning_home),
            textToSpeech
        ) {
            val returnHomeText = getString(R.string.returning_home)
            // Fala a mensagem e fecha imediatamente a Activity
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                textToSpeech.speak(returnHomeText, TextToSpeech.QUEUE_FLUSH, null, null)
            } else {
                @Suppress("DEPRECATION")
                textToSpeech.speak(returnHomeText, TextToSpeech.QUEUE_FLUSH, null)
            }
            finish()
        }

    }



    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("pt", "PT")
            speak(getString(R.string.arrived_at, destination))
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
                button.postDelayed({ clickedOnce = false }, 3000)
            } else {
                clickedOnce = false
                onConfirmed()
            }
        }
    }


    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
