package com.example.swuniMAP

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

// QuizActivity.kt
class QuizActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quiz)

        val quizId = intent.getStringExtra("QUIZ_ID")
            ?: throw IllegalArgumentException("QUIZ_ID가 필요합니다")

        val fragment = QuizFragment.newInstance(quizId)
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}
