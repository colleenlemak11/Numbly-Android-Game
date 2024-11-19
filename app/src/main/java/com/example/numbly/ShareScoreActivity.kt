package com.example.numbly

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class ShareScoreActivity : AppCompatActivity() {

    private lateinit var guessesGrid: GridLayout
    private lateinit var shareButton: Button
    private lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_share_score)

        // Get the guessHistory and randomNumber from the Intent
        val guessHistory = intent.getStringArrayListExtra("guessHistory") ?: arrayListOf()
        val randomNumber = intent.getStringExtra("randomNumber") ?: "00000"

        // Find the views in the layout
        guessesGrid = findViewById(R.id.guessGrid)
        shareButton = findViewById(R.id.shareButton)
        backButton = findViewById(R.id.backButton)

        // Create the grid of guesses based on guessHistory and colors
        populateGrid(guessHistory, randomNumber)

        // Back button to return to GameActivity
        backButton.setOnClickListener {
            finish()  // Finish ShareScoreActivity and return to GameActivity
        }

        // Share button to share the score and guesses via text
        shareButton.setOnClickListener {
            val shareText = buildString {
                append("I played Numbly and here's my result!\n")

                // Loop through each guess
                for (guess in guessHistory) {
                    for (i in guess.indices) {
                        val guessChar = guess[i]
                        val cellColor = getCellColor(guessChar, randomNumber, i)
                        append(
                            when (cellColor) {
                                Color.GREEN -> "\uD83D\uDFE9" // Correct position
                                Color.YELLOW -> "\uD83D\uDFE8" // Correct digit, wrong position
                                else -> "⬛" // Incorrect digit
                            }
                        )
                    }
                    append("\n")
                }
            }

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivity(Intent.createChooser(shareIntent, "Share via"))
        }
    }

    private fun populateGrid(guessHistory: List<String>, randomNumber: String) {
        guessesGrid.removeAllViews() // Clear any previous views in the grid

        // Loop through each guess in the guessHistory
        for (guess in guessHistory) {
            // Create a new row for each guess
            val guessRow = GridLayout(this)
            guessRow.rowCount = 1
            guessRow.columnCount = 5 // Fixed column count for 5 digits

            // Loop through each character in the guess and add it as a TextView to the row
            for (i in guess.indices) {
                val guessChar = guess[i]
                val cell = TextView(this).apply {
                    text = guessChar.toString()
                    textSize = 24f
                    setPadding(16, 16, 16, 16) // Add padding to the text for better spacing

                    // Set background color based on comparison with the random number
                    setBackgroundColor(getCellColor(guessChar, randomNumber, i))
                    setTextColor(getCellTextColor(guessChar, randomNumber[i]))
                    gravity = android.view.Gravity.CENTER // Center align the text inside the cell
                }

                // Add the cell to the row
                guessRow.addView(cell)
            }

            // Add the completed row to the GridLayout
            guessesGrid.addView(guessRow)
        }
    }

    private fun getCellColor(guessChar: Char, randomNumber: String, index: Int): Int {
        return when (guessChar) {
            randomNumber[index] -> Color.GREEN // Correct position
            in randomNumber -> Color.YELLOW // Correct digit, wrong position
            else -> Color.BLACK // Incorrect digit (use black background for contrast)
        }
    }

    private fun getCellTextColor(guessChar: Char, randomChar: Char): Int {
        return when {
            guessChar == randomChar -> Color.BLACK // Correct position (white text on green background)
            randomChar in guessChar.toString() -> Color.BLACK // Correct digit, wrong position (white text on yellow background)
            else -> Color.WHITE // Incorrect digit (black text on black background)
        }
    }
}
