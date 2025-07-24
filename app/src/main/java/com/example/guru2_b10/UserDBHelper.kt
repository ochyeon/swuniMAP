package com.example.guru2_b10

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
            pw TEXT,
            college TEXT
            )""".trimIndent()
        db.execSQL(createTable)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldversion: Int, newVersion: Int){
        // 새 버전으로 바뀔 때 실행됨
        db.execSQL("DROP TABLE IF EXISTS users")
        onCreate(db)
    }

    fun registerUser(id:String, pw:String, college:String):Boolean{
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
            put("college", college)
        }

        return try{
            db.insertOrThrow("users", null, values)
            true
        }catch(e:Exception){
            false // 등록 실패
        }
    }

    fun loginUser(id:String, pw:String, college:String):Boolean{
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT * FROM users WHERE id = ? AND pw = ? AND college = ?",
            arrayOf(id, pw, college)
        )
        val loginSuccess = cursor.count > 0
        cursor.close()
        return loginSuccess
    }

    // 로그인 한 user의 id 기반으로 단과대 정보 가져오기 -> 추후 필요할 수도 있을 듯 해서 구현해둠
    fun getUserCollegeById(id:String): String?{
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT college FROM users WHERE id = ?",
            arrayOf(id))

        return if(cursor.moveToFirst()){
            val college = cursor.getString(0)
            cursor.close()
            college
        }else{
            cursor.close()
            null
        }
    }
}