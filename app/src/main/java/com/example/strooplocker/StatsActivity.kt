package com.example.strooplocker

import android.content.Context
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class StatsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)

        val statsTextView: TextView = findViewById(R.id.statsTextView)
        val prefs = getSharedPreferences("StroopLockerPrefs", Context.MODE_PRIVATE)
        val successful = prefs.getInt("stats_successful", 0)
        val unsuccessful = prefs.getInt("stats_unsuccessful", 0)

        val statsMessage = """
            Successful Attempts: $successful
            Unsuccessful Attempts: $unsuccessful
        """.trimIndent()

        statsTextView.text = statsMessage
    }
}
