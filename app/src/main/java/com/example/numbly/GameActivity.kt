package com.example.numbly

import android.content.Intent
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
        Log.d("GameActivity", "Restored currentGuessRow: $currentGuessRow")
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
            currentGuessRow = 0
            Log.d("GameActivity", "Reset currentGuessRow to 0 in onResume()")
        }
        else {
            currentGuessRow = prefs.getInt("currentGuessRow", 0)
            Log.d("GameActivity", "else stmt currentGuessRow: $currentGuessRow")
        }
    }

    private fun saveGameState() {
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
        prefs.putString("randomNumber", randomNumber)
        prefs.putInt("currentGuessRow", currentGuessRow)
        Log.d("GameActivity", "saveGameState $currentGuessRow")
        prefs.putStringSet("guessHistory", guessHistory.toSet())
        prefs.apply()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("randomNumber", randomNumber)
        outState.putInt("currentGuessRow", currentGuessRow)
        outState.putStringArrayList("guessHistory", ArrayList(guessHistory))
    }

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
        for (i in 0 until 25) {
            val textView = TextView(this).apply {
                layoutParams = GridLayout.LayoutParams().apply {
                    width = GridLayout.LayoutParams.WRAP_CONTENT
                    height = GridLayout.LayoutParams.WRAP_CONTENT
                    setMargins(4, 4, 4, 4)
                }
                textSize = 40f
                gravity = Gravity.CENTER
                setBackgroundResource(android.R.color.white)
                text = " "
            }
            guessGrid.addView(textView)
        }
    }

    private fun restoreGuessGrid() {
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

    private fun submitGuess() {
        Log.d("GameActivity", "Current guess row: $currentGuessRow")
        val guess = guessInput.text.toString()

        if (guess.length == 5 && guess.all { it.isDigit() }) {
            if (currentGuessRow < maxGuesses) {
                for (i in 0 until 5) {
                    val index = currentGuessRow * 5 + i
                    val textView = guessGrid.getChildAt(index) as TextView
                    textView.text = guess[i].toString()
                    textView.setBackgroundColor(getColorForDigit(guess[i], i))
                    textView.invalidate()
                }
                guessHistory.add(guess)

                if (guess == randomNumber) {
                    Toast.makeText(this, "Congratulations! You guessed the number!", Toast.LENGTH_SHORT).show()
                    clearGameStateAndLaunchShareScore()
                } else if (currentGuessRow == maxGuesses - 1) {
                    Toast.makeText(this, "Maximum guesses reached! The correct number was $randomNumber.", Toast.LENGTH_LONG).show()
                    clearGameStateAndLaunchShareScore()
                }
                else {
                    currentGuessRow++
                }
                guessInput.text.clear()
                guessGrid.invalidate()
                guessGrid.requestLayout()
            } else {
                Toast.makeText(this, "Maximum guesses reached", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Please enter a valid 5-digit number", Toast.LENGTH_SHORT).show()
        }
    }

    private fun clearGameStateAndLaunchShareScore() {
        // Launch the ShareScoreActivity and pass the guess history and random number
        Toast.makeText(this, "Launching ShareScoreActivity", Toast.LENGTH_LONG).show()

        val intent = Intent(this, ShareScoreActivity::class.java)
        intent.putStringArrayListExtra("guessHistory", ArrayList(guessHistory))
        intent.putExtra("randomNumber", randomNumber)
        currentGuessRow = 0
        startActivity(intent)

        // Clear game state
        resetGameBoard()
        clearSavedGameState()
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
        return digitCount.filter { it.value > 1 }.keys.toList()
    }

    private fun clearSavedGameState() {
        val prefs = getSharedPreferences("GamePrefs", MODE_PRIVATE).edit()
        prefs.clear()
        prefs.apply()
        Log.d("GameActivity", "clearsavedgamestate $currentGuessRow")
    }

    private fun resetGameBoard() {
        for (i in 0 until guessGrid.childCount) {
            (guessGrid.getChildAt(i) as TextView).text = " "
            guessGrid.getChildAt(i).setBackgroundColor(Color.WHITE)
        }
        currentGuessRow = 0
        randomNumber = generateRandomNumber()
        guessHistory.clear()
        guessInput.text.clear()
        Log.d("GameActivity", "Game board reset, currentGuessRow set to 0")
    }

    private fun getColorForDigit(digit: Char, index: Int): Int {
        return when {
            digit == randomNumber[index] -> Color.GREEN
            randomNumber.contains(digit) -> Color.YELLOW
            else -> Color.WHITE
        }
    }
}
