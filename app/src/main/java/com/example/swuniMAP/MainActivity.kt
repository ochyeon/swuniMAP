package com.example.swuniMAP

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.kakao.vectormap.MapView

class MainActivity : AppCompatActivity() {
    private lateinit var mapView : MapView

    // activity_main에서 활용할 수 있는 기본 틀만 작성 -> 필요에 따라 수정해서 사용해주세요!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map_view)
        mapView.start() // 지도 로딩 시작
    }

    override fun onDestroy() {
        mapView.stop() // 지도 리소스 해제
        super.onDestroy()
    }
}