package com.example.swuniMAP

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.kakao.vectormap.KakaoMap
import com.kakao.vectormap.MapLifeCycleCallback
import com.kakao.vectormap.MapView
import com.kakao.vectormap.LatLng
import com.kakao.vectormap.label.LabelOptions
import com.kakao.vectormap.label.LabelLayer
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder // LabelTextBuilder 다시 임포트
import android.content.Intent
import com.kakao.vectormap.label.Label
import android.app.Activity
import androidx.activity.result.contract.ActivityResultContracts
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk


class MainActivity : AppCompatActivity() {
    private lateinit var mapView : MapView
    private lateinit var quizProgressTextView: TextView

    private var completedQuizzes = 0
    private val totalQuizzes = 6

    private val quizCompletionStatus = mutableMapOf<String, Boolean>()

    private val quizResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val quizId = result.data?.getStringExtra("QUIZ_ID")
            val isQuizCompleted = result.data?.getBooleanExtra("IS_QUIZ_COMPLETED", false) ?: false

            if (quizId != null && isQuizCompleted && quizCompletionStatus[quizId] == false) {
                quizCompletionStatus[quizId] = true
                completedQuizzes++
                updateQuizProgress()
                Toast.makeText(this, "$quizId 퀴즈 완료! (${completedQuizzes}/${totalQuizzes})", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KakaoMapSdk.init(this, BuildConfig.KAKAO_MAP_KEY)

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map_view)
        quizProgressTextView = findViewById(R.id.quizProgressTextView)

        quizCompletionStatus["anniversary_quiz_1"] = false
        quizCompletionStatus["nuri_hall_quiz_1"] = false
        quizCompletionStatus["nuri_hall_quiz_2"] = false
        quizCompletionStatus["library_quiz_1"] = false
        quizCompletionStatus["library_quiz_2"] = false
        quizCompletionStatus["christian_ed_quiz_1"] = false

        updateQuizProgress()

        // MapLifeCycleCallback 사용, onMapError 다시 포함
        mapView.start(object : MapLifeCycleCallback() {

            override fun onMapDestroy() {
                // 이 메서드는 비워두어도 됩니다.
            }

            override fun onMapError(error: Exception) { // onMapError 다시 추가 (필수)
                Toast.makeText(this@MainActivity, "지도 로딩 오류: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }, object: KakaoMapReadyCallback(){
            override fun onMapReady(kakaoMap: KakaoMap) {
                addBuildingMarkers(kakaoMap)

                kakaoMap.setOnLabelClickListener { _, label, _ ->
                    val tag = label.tag as? String
                    tag?.let {
                        if (it.endsWith("_info")) {
                            val buildingId = it.substringBefore("_info")
                            val intent = Intent(this@MainActivity, BuildingDetailActivity::class.java)
                            intent.putExtra("BUILDING_ID", buildingId)
                            startActivity(intent)
                        } else if (it.endsWith("_quiz_1") || it.endsWith("_quiz_2")) {
                            val buildingId = it.substringBefore("_quiz_")
                            val quizNumber = it.substringAfterLast("_").toIntOrNull()
                            val quizId = it

                            val intent = Intent(this@MainActivity, QuizActivity::class.java)
                            intent.putExtra("BUILDING_ID", buildingId)
                            intent.putExtra("QUIZ_NUMBER", quizNumber)
                            intent.putExtra("QUIZ_ID", quizId)

                            quizResultLauncher.launch(intent)
                        }
                    }
                    true
                }
            }
        })

        // searchIcon 관련 코드는 activity_main.xml에 없으므로 이 부분은 반드시 삭제하거나 주석 처리해야 합니다.
        // findViewById(R.id.searchIcon)
    }

    private fun updateQuizProgress() {
        val percentage = if (totalQuizzes > 0) (completedQuizzes * 100) / totalQuizzes else 0
        quizProgressTextView.text = "퀴즈 수행률: ${completedQuizzes}/${totalQuizzes} (${percentage}%)"
    }

    private fun addBuildingMarkers(kakaoMap: KakaoMap) {
        val labelManager = kakaoMap.labelManager

        val infoMarkerStyle = LabelStyles.from(
            LabelStyle.from(R.drawable.ic_marker_info)
                .setTextStyles(30, Color.BLACK)
        )

        val quizMarkerStyle = LabelStyles.from(
            LabelStyle.from(R.drawable.ic_marker_quiz)
                .setTextStyles(30, Color.BLACK)
        )

        // --- LabelOptions 생성 방식 LabelOptions.from(위치) 유지, .setTexts()는 LabelTextBuilder.from("문자열")로 변경 ---
        // 인문사회관
        val liberalArtsLocation = LatLng.from(37.62834, 127.0926)
        labelManager?.layer?.addLabel(
            LabelOptions.from(liberalArtsLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("인문사회관",0)) // LabelTextBuilder.from() 사용
                .setTag("liberal_arts_info")
        )

        // 제1과학관
        val firstScienceLocation = LatLng.from(37.62926, 127.0896)
        labelManager?.layer?.addLabel(
            LabelOptions.from(firstScienceLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("제1과학관", 0)) // LabelTextBuilder.from() 사용
                .setTag("first_science_info")
        )

        // 제2과학관
        val secondScienceLocation = LatLng.from(37.62942, 127.0905)
        labelManager?.layer?.addLabel(
            LabelOptions.from(secondScienceLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("제2과학관", 0)) // LabelTextBuilder.from() 사용
                .setTag("second_science_info")
        )

        // 조형예술관
        val artHallLocation = LatLng.from(37.62913, 127.0916)
        labelManager?.layer?.addLabel(
            LabelOptions.from(artHallLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("조형예술관", 0)) // LabelTextBuilder.from() 사용
                .setTag("art_hall_info")
        )

        // --- 퀴즈 아이콘 (ic_marker_quiz.xml)을 표시할 건물 ---
        val anniversaryHallQuizLocation1 = LatLng.from(37.62640, 127.0930)
        labelManager?.layer?.addLabel(
            LabelOptions.from(anniversaryHallQuizLocation1)
                .setStyles(quizMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("50주년 퀴즈 1", 0)) // LabelTextBuilder.from() 사용
                .setTag("anniversary_quiz_1")
        )

        val nuriHallQuizLocation1 = LatLng.from(37.62873, 127.0906)
        labelManager?.layer?.addLabel(
            LabelOptions.from(nuriHallQuizLocation1)
                .setStyles(quizMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("누리관 퀴즈 1", 0)) // LabelTextBuilder.from() 사용
                .setTag("nuri_hall_quiz_1")
        )
        val nuriHallQuizLocation2 = LatLng.from(37.6291, 127.0891)
        labelManager?.layer?.addLabel(
            LabelOptions.from(nuriHallQuizLocation2)
                .setStyles(quizMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("누리관 퀴즈 2", 0)) // LabelTextBuilder.from() 사용
                .setTag("nuri_hall_quiz_2")
        )

        val libraryQuizLocation1 = LatLng.from(37.62857, 127.0913)
        labelManager?.layer?.addLabel(
            LabelOptions.from(libraryQuizLocation1)
                .setStyles(quizMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("도서관 퀴즈 1", 0)) // LabelTextBuilder.from() 사용
                .setTag("library_quiz_1")
        )
        val libraryQuizLocation2 = LatLng.from(37.6296, 127.0886)
        labelManager?.layer?.addLabel(
            LabelOptions.from(libraryQuizLocation2)
                .setStyles(quizMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("도서관 퀴즈 2", 0)) // LabelTextBuilder.from() 사용
                .setTag("library_quiz_2")
        )

        val christianEdHallQuizLocation1 = LatLng.from(37.62709, 127.0925)
        labelManager?.layer?.addLabel(
            LabelOptions.from(christianEdHallQuizLocation1)
                .setStyles(quizMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("기독교 교육관 퀴즈 1", 0)) // LabelTextBuilder.from() 사용
                .setTag("christian_ed_quiz_1")
        )
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}