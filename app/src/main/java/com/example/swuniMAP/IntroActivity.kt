package com.example.swuniMAP

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class IntroActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_startscene)

        val startButton = findViewById<Button>(R.id.startButton)

        startButton.setOnClickListener {
            Toast.makeText(this, "탐험 시작!", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, MainActivity::class.java))
            finish() // Let's Go! 버튼 클릭 후 다시 로그인 화면으로 돌아오는 문제 방지
        }
    }
}