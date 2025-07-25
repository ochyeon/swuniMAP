package com.example.swuniMAP

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class UserDBHelper(context: Context) :
    SQLiteOpenHelper(context, "UserDB", null, 2){

    override fun onCreate(db: SQLiteDatabase){
        val createTable = """
            CREATE TABLE users(
            id TEXT PRIMARY KEY,
            pw TEXT
            )""".trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldversion: Int, newVersion: Int){
        // 새 버전으로 바뀔 때 실행됨
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun registerUser(id:String, pw:String):Boolean{
        val db = writableDatabase

        // 중복 체크
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE id = ?",
            arrayOf(id))
        val exists = cursor.count > 0
        cursor.close()
        
        if(exists) return false // 이미 존재하는 회원

        val values = ContentValues().apply{
            put("id", id)
            put("pw", pw)
        }

        return try{
            db.insertOrThrow("users", null, values)
            true
        }catch(e:Exception){
            false // 등록 실패
        }
    }

    fun loginUser(id:String, pw:String):Boolean{
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE id = ? AND pw = ?",
            arrayOf(id, pw)
        )
        val loginSuccess = cursor.count > 0
        cursor.close()
        return loginSuccess
    }

    // 회원가입 시 ID 중복 여부 확인에 사용됨
    fun isUserExists(id: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM users WHERE id = ?", arrayOf(id))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    // 개발 중 테스트 용 계정 삭제 위한 함수 -> 로그인 화면에 버튼으로 구현 -> 실제 배포 시 삭제
    fun deleteAllUsers() {
        val db = writableDatabase
        db.delete("users", null, null)
    }

}