// FinishActivity.kt
package com.example.swuniMAP

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class FinishActivity : AppCompatActivity() {
    private lateinit var dbHelper: UserDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_finish)

        dbHelper = UserDBHelper(this)

        val yesBtn = findViewById<Button>(R.id.buttonYes)
        val noBtn  = findViewById<Button>(R.id.buttonNo)

        // SharedPreferences 에 저장된 현재 로그인 ID 읽기
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val currentId = prefs.getString("CURRENT_USER_ID", null)

        yesBtn.setOnClickListener {
            if (currentId != null && dbHelper.deleteUser(currentId)) {
                Toast.makeText(this, "회원 탈퇴 처리되었습니다.", Toast.LENGTH_SHORT).show()
                // prefs 에 남아있는 ID 도 제거
                prefs.edit().remove("CURRENT_USER_ID").apply()
                // 로그인 화면으로 돌아가기 (task 클리어)
                val intent = Intent(this, LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                }
                startActivity(intent)
                finish()
            } else {
                Toast.makeText(this, "회원 탈퇴에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        noBtn.setOnClickListener {
            // 메인으로 복귀
            val intent = Intent(this, MainActivity::class.java)
            // 기존 스택을 그대로 두고 돌아가도 된다면 그냥 finish() 해도 됨
            startActivity(intent)
            finish()
        }
    }
}
