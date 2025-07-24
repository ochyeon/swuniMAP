package com.example.swuniMAP

import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {
    private lateinit var dbHelper : UserDBHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        dbHelper = UserDBHelper(this)

        val idEditText = findViewById<EditText>(R.id.idEditText)
        val pwEditText = findViewById<EditText>(R.id.pwEditText)
        val collegeSpinner = findViewById<Spinner>(R.id.collegeSpinner)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val signupButton = findViewById<Button>(R.id.signupButton)

        // 단과대 목록 (학교 홈페이지 내 대학 정보 기준으로 작성함)
        val colleges = arrayOf("교양대학", "인문대학", "사회과학대학", "과학기술융합대학", "미래산업융합대학", "아트앤디자인스쿨", "자유전공학부", "글로벌통상학부", "연계융합전공", "마이크로전공")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, colleges)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        collegeSpinner.adapter = adapter

        // 로그인 버튼 이벤트 리스너
        loginButton.setOnClickListener{
            val id = idEditText.text.toString()
            val pw = pwEditText.text.toString()
            val college = collegeSpinner.selectedItem.toString()

            if(dbHelper.loginUser(id, pw, college)){
                val userCollege = dbHelper.getUserCollegeById(id)

                val intent = Intent(this, MainActivity::class.java)
                intent.putExtra("userId", id)
                intent.putExtra("college", userCollege)
                startActivity(intent)

                Toast.makeText(this, "로그인 성공", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
            }else{
                Toast.makeText(this, "로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
        
        // 회원가입 버튼 이벤트 리스너
        signupButton.setOnClickListener{
            val id = idEditText.text.toString()
            val pw = pwEditText.text.toString()
            val college = collegeSpinner.selectedItem.toString()

            if(dbHelper.registerUser(id, pw, college)){
                Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
            }else{
                Toast.makeText(this, "회원가입 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}