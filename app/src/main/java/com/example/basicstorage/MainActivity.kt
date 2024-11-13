package com.example.basicstorage

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import android.content.Intent
import android.widget.Toast

class MainActivity : ComponentActivity() {
    private lateinit var textView: TextView
    private lateinit var changeColorButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize the views
        textView = findViewById(R.id.appName)
        changeColorButton = findViewById(R.id.changeColorButton)

        // Load and apply the saved text color
        val savedColor = loadTextColorPreference()
        textView.setTextColor(Color.parseColor(savedColor))

        // Set up button click listener to open color picker
        changeColorButton.setOnClickListener {
            showColorPickerDialog()
        }

        // Add continue to gameplay button
        val continueButton: Button = findViewById(R.id.continueButton)
        continueButton.setOnClickListener {
            val intent = Intent(this, GameActivity::class.java)
            startActivity(intent)
        }
    }

    private fun showColorPickerDialog() {
        val colors = arrayOf("#FF0000", "#00FF00", "#0000FF", "#FFFF00", "#FF00FF", "#00FFFF")
        val colorNames = arrayOf("Red", "Green", "Blue", "Yellow", "Magenta", "Cyan")

        // Use 'this' to refer to the activity context
        AlertDialog.Builder(this)
            .setTitle("Choose Text Color")
            .setItems(colorNames) { _, which ->
                val selectedColor = colors[which]
                changeTextColor(selectedColor)
            }
            .show()
    }

    private fun saveTextColorPreference(color: String) {
        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("text_color", color)
        editor.apply()
    }

    private fun loadTextColorPreference(): String {
        val sharedPreferences = getSharedPreferences("MyPreferences", MODE_PRIVATE)
        return sharedPreferences.getString("text_color", "#000000") ?: "#000000"
    }

    private fun changeTextColor(newColor: String) {
        saveTextColorPreference(newColor)
        textView.setTextColor(Color.parseColor(newColor))
    }
}
