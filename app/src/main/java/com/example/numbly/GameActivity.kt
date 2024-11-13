package com.example.numbly

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlin.random.Random

class GameActivity : AppCompatActivity() {
    private lateinit var guessInput: EditText
    private lateinit var submitGuessButton: Button
    private lateinit var guessGrid: GridLayout
    private lateinit var hintButton : Button

    private val maxGuesses = 5
    private var currentGuessRow = 0
    private lateinit var randomNumber: String
    private val guessHistory = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_game)

        guessInput = findViewById(R.id.guessInput)
        submitGuessButton = findViewById(R.id.submitGuessButton)
        guessGrid = findViewById(R.id.guessGrid)
        hintButton = findViewById(R.id.hintButton)

        // Generate or restore a random 5-digit number
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        randomNumber = prefs.getString("randomNumber", null) ?: generateRandomNumber()
        currentGuessRow = prefs.getInt("currentGuessRow", 0)
        guessHistory.addAll(prefs.getStringSet("guessHistory", emptySet())!!.toList())

        // Set up or restore the grid if available
        setupGuessGrid()
        restoreGuessGrid()

        submitGuessButton.setOnClickListener {
            submitGuess()
        }

        hintButton.setOnClickListener {
            provideHint()
        }
    }

    override fun onPause() {
        super.onPause()
        saveGameState()
    }

    override fun onStop() {
        super.onStop()
        saveGameState()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE)

        // Reset only if there's no saved game state or if game is completed
        if (currentGuessRow >= maxGuesses || guessHistory.contains(randomNumber)) {
            Toast.makeText(this, "Starting new game", Toast.LENGTH_LONG).show()
            resetGameBoard()
            clearSavedGameState()
        }
    }

    // Save game state method to reuse in multiple lifecycle methods
    private fun saveGameState() {
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
        prefs.putString("randomNumber", randomNumber)
        prefs.putInt("currentGuessRow", currentGuessRow)
        prefs.putStringSet("guessHistory", guessHistory.toSet())
        prefs.apply()
    }

    // Save instance state to preserve data
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("randomNumber", randomNumber)
        outState.putInt("currentGuessRow", currentGuessRow)
        outState.putStringArrayList("guessHistory", ArrayList(guessHistory))
    }

    // Restore instance state when the app resumes
    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        randomNumber = savedInstanceState.getString("randomNumber") ?: generateRandomNumber()
        currentGuessRow = savedInstanceState.getInt("currentGuessRow")
        guessHistory.clear()
        guessHistory.addAll(savedInstanceState.getStringArrayList("guessHistory") ?: emptyList())
        restoreGuessGrid() // Restore the grid display
    }

    private fun generateRandomNumber(): String {
        return Random.nextInt(10000, 99999).toString()
    }

    private fun setupGuessGrid() {
        // Create 5x5 TextViews dynamically
        for (i in 0 until 25) {
            val textView = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(4, 4, 4, 4)
                }
                textSize = 40f
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(android.R.color.white)
                text = " " // Initialize with empty text
            }
            guessGrid.addView(textView)
        }
    }

    private fun restoreGuessGrid() {
        // Restoring grid in reverse guessHistory order to ensure user's sequential guesses
        for (i in guessHistory.indices) {
            val guess = guessHistory[i]
            for (j in guess.indices) {
                val index = (guessHistory.size - 1 - i) * 5 + j
                val textView = guessGrid.getChildAt(index) as TextView
                textView.text = guess[j].toString()
                textView.setBackgroundColor(getColorForDigit(guess[j], j))
            }
        }
    }

    // Helper method to check for saved game state
    private fun hasSavedGameState(): Boolean {
        val sharedPreferences = getSharedPreferences("GamePrefs", MODE_PRIVATE)
        return sharedPreferences.contains("currentGuessRow") // Or any key that holds your game state
    }

    private fun submitGuess() {
        val guess = guessInput.text.toString()

        // Check if guess is valid (5 digits)
        if (guess.length == 5 && guess.all { it.isDigit() }) {
            if (currentGuessRow < maxGuesses) {

                Log.d("GameActivity", "Submitting guess: $guess at row $currentGuessRow")

                // Update the grid with the guess and change colors
                for (i in 0 until 5) {
                    val index = currentGuessRow * 5 + i
                    val textView = guessGrid.getChildAt(index) as TextView
                    textView.text = guess[i].toString()

                    // Change color based on the guess
                    textView.gravity = Gravity.CENTER
                    textView.setBackgroundColor(getColorForDigit(guess[i], i))
                    textView.invalidate() // Forces the TextView to redraw

                    Log.d("GameActivity", "Updating cell index $index with digit ${guess[i]}")
                }
                guessHistory.add(guess)

                // Check if the guess is correct
                if (guess == randomNumber) {
                    Toast.makeText(this, "Congratulations! You guessed the number!", Toast.LENGTH_SHORT).show()
                    //clearSavedGameState()
                    //resetGameBoard()
                } else if (currentGuessRow == maxGuesses - 1) {
                    Toast.makeText(this, "Maximum guesses reached! The correct number was $randomNumber.", Toast.LENGTH_LONG).show()
                    //clearSavedGameState()
                    //resetGameBoard()
                }

                currentGuessRow++
                guessInput.text.clear() // Clear the input after submission
                guessGrid.invalidate()
                guessGrid.requestLayout()

            } else {
                Toast.makeText(this, "Maximum guesses reached", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter a valid 5-digit number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun provideHint() {
        val duplicates = findDuplicates(randomNumber)
        if (duplicates.isNotEmpty()) {
            Toast.makeText(this, "Duplicate digits found: ${duplicates.joinToString(", ")}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "No duplicate digits.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun findDuplicates(guess: String): List<Char> {
        val digitCount = mutableMapOf<Char, Int>()
        guess.forEach { digit ->
            digitCount[digit] = digitCount.getOrDefault(digit, 0) + 1
        }
        return digitCount.filter { it.value > 1 }.keys.toList() // Return duplicates
    }

    private fun clearSavedGameState() {
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()
    }

    private fun resetGameBoard() {
        // Clear the guess grid and reset game variables
        for (i in 0 until guessGrid.childCount) {
            (guessGrid.getChildAt(i) as TextView).text = " "
            guessGrid.getChildAt(i).setBackgroundColor(Color.WHITE)
        }
        currentGuessRow = 0
        randomNumber = generateRandomNumber() // Generate a new number
        guessHistory.clear()
        guessInput.text.clear()
    }

    private fun getColorForDigit(digit: Char, index: Int): Int {
        return when {
            digit == randomNumber[index] -> Color.GREEN // Correct digit in the correct place
            randomNumber.contains(digit) -> Color.YELLOW // Correct digit but in the wrong place
            else -> Color.WHITE // Incorrect digit
        }
    }
}
