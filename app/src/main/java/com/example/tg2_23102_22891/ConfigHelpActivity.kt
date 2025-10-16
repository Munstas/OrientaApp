package com.example.tg2_23102_22891

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

class ConfigHelpActivity : AppCompatActivity() {
    private lateinit var textToSpeech: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_help)

        // Inicializa TTS antes de qualquer uso
        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale("pt", "PT")
            }
        }

        val etName = findViewById<EditText>(R.id.etHelpName)
        val etPhone = findViewById<EditText>(R.id.etHelpPhone)
        val btnSave = findViewById<Button>(R.id.btnSaveHelp)

        // Carrega valores guardados, se existirem
        val prefs = getSharedPreferences("help_contact", Context.MODE_PRIVATE)
        etName.setText(prefs.getString("help_name", ""))
        etPhone.setText(prefs.getString("help_phone", ""))

        // Lógica de dois cliques para o botão Guardar
        setupDoubleClickButton(
            btnSave,
            getString(R.string.save),
            textToSpeech
        ) {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Preenche ambos os campos!", Toast.LENGTH_SHORT).show()
                return@setupDoubleClickButton
            }

            val editor = prefs.edit()
            editor.putString("help_name", name)
            editor.putString("help_phone", phone)
            editor.apply()

            Toast.makeText(this, getString(R.string.saved_success), Toast.LENGTH_SHORT).show()
            finish()
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

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
