package com.example.swuniMAP

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog

class LoginActivity : AppCompatActivity() {
    private lateinit var dbHelper : UserDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = UserDBHelper(this)

        val idEditText = findViewById<EditText>(R.id.idEditText)
        val pwEditText = findViewById<EditText>(R.id.pwEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupButton = findViewById<Button>(R.id.signupButton)
        // 테스트용 계정 삭제 버튼 -> 마지막에 제거 필요
        val deleteTestButton = findViewById<Button>(R.id.DeleteTestID)

        // 로그인 버튼 이벤트 리스너
        loginButton.setOnClickListener{
            val id = idEditText.text.toString()
            val pw = pwEditText.text.toString()

            if(dbHelper.loginUser(id, pw)){
                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()

                //로그인 id를 prefs에 저장
                val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
                prefs.edit().putString("CURRENT_USER_ID", id).apply()

                startActivity(Intent(this, IntroActivity::class.java))
            }else{
                Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 회원가입 버튼 이벤트 리스너
        signupButton.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.dialog_signup, null)
            val dialogIdEditText = dialogView.findViewById<EditText>(R.id.dialogIdEditText)
            val dialogPwEditText = dialogView.findViewById<EditText>(R.id.dialogPwEditText)

            val dialog = AlertDialog.Builder(this)
                .setTitle("회원가입")
                .setView(dialogView)
                .setPositiveButton("가입", null) // null 처리 후 show() 이후 직접 클릭 리스너 설정
                .setNegativeButton("취소", null)
                .create()

            dialog.show()

            // 가입 버튼 커스텀 클릭 리스너 설정
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val id = dialogIdEditText.text.toString().trim()
                val pw = dialogPwEditText.text.toString().trim()

                if (id.isEmpty() || pw.isEmpty()) {
                    Toast.makeText(this, "아이디와 비밀번호를 입력해주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                if (dbHelper.isUserExists(id)) {
                    Toast.makeText(this, "이미 존재하는 ID입니다.", Toast.LENGTH_SHORT).show()
                } else {
                    val result = dbHelper.registerUser(id, pw)
                    if (result) {
                        Toast.makeText(this, "회원가입 성공! 로그인해주세요.", Toast.LENGTH_SHORT).show()

                        // 로그인 화면의 EditText에 자동 입력
                        findViewById<EditText>(R.id.idEditText).setText(id)
                        findViewById<EditText>(R.id.pwEditText).setText(pw)

                        dialog.dismiss()
                    } else {
                        Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // 테스트용 계정 삭제 버튼 -> 마지막에 제거 필요
        deleteTestButton.setOnClickListener {
            dbHelper.deleteAllUsers()
            Toast.makeText(this, "모든 사용자 삭제 완료", Toast.LENGTH_SHORT).show()
        }

    }
}