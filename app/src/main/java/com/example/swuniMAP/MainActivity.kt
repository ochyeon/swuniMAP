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
import com.kakao.vectormap.label.LabelStyle
import com.kakao.vectormap.label.LabelStyles
import com.kakao.vectormap.label.LabelTextBuilder // LabelTextBuilder 다시 임포트
import android.content.Intent
import android.app.Activity
import android.location.LocationManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.camera.CameraPosition


class MainActivity : AppCompatActivity() {
    private lateinit var mapView : MapView
    private lateinit var quizProgressTextView: TextView

    private var completedQuizzes = 0
    private val totalQuizzes = 6

    private var kakaoMap:KakaoMap? = null
    private var minZoomLevel: Float? = null

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
            override fun onMapReady(map: KakaoMap) {
                kakaoMap = map

                // 1. 캠퍼스 영역 좌표들 (정문과 반대편)
                val campusPoints = arrayOf(
                    LatLng.from(37.6258, 127.0881),  // 남서쪽
                    LatLng.from(37.6294, 127.0936)   // 북동쪽
                )

                // 2. 카메라 이동 (padding: 50px 정도 여유)
                val cameraUpdate = CameraUpdateFactory.fitMapPoints(campusPoints, 50)
                map.moveCamera(cameraUpdate)

                // 초기 줌 레벨 저장 -> 지도의 최소 축소 배율
                minZoomLevel = map.cameraPosition?.zoomLevel?.toFloat()

                Log.d("Map", "카메라 캠퍼스 전체로 이동 완료")


                mapView.post {
                    // 초기 아이콘 크기로 마커 표시
                    addBuildingMarkers(map, calculateIconSize(map.cameraPosition?.zoomLevel?.toInt()?: 10))
                }

                // 초기 배율보다 작게 축소하려고 할 경우
                map.setOnCameraMoveEndListener { map, cameraPosition, _ ->
                    val zoom = cameraPosition?.zoomLevel ?: return@setOnCameraMoveEndListener

                    minZoomLevel?.let { minZoom ->
                        if (zoom < minZoom) {
                            cameraPosition?.let { safeCameraPosition ->
                                val limitedPosition = CameraPosition.from(
                                    safeCameraPosition.position.latitude,
                                    safeCameraPosition.position.longitude,
                                    minZoom.toInt(),
                                    safeCameraPosition.tiltAngle,
                                    safeCameraPosition.rotationAngle,
                                    0.0  // 마지막 인자 API 문서 확인 필요
                                )

                                map.moveCamera(CameraUpdateFactory.newCameraPosition(limitedPosition))
                            }
                            return@setOnCameraMoveEndListener
                        }
                    }
                    val iconSize = calculateIconSize(zoom.toInt())
                    updateMarkers(iconSize)
                }

                map.setOnLabelClickListener { _, label, _ ->
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

                // 현재 위치 마커 표시
                showCurrentLocationMarker()
            }
        })
    }

    // 현 위치 가져오기
    private fun showCurrentLocationMarker(){
        val locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val provider = when{
            isGpsEnabled -> LocationManager.GPS_PROVIDER
            isNetworkEnabled -> LocationManager.NETWORK_PROVIDER
            else -> null
        }

        provider?.let{
            try{
                val location = locationManager.getLastKnownLocation(it)
                location?.let{
                    val currentLatLng = LatLng.from(it.latitude, it.longitude)

                    if(isInCampus(currentLatLng)){
                        showCurrentLocationMarkerOnMap(currentLatLng)
                    }else{
                        // 교외지역일 때 메세지 -> startScene으로 전환
                        Toast.makeText(this, "교외 지역입니다. 캠퍼스 내에서 실행해주세요.", Toast.LENGTH_LONG).show()

                        val intent = Intent(this, IntroActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                        finish()
                    }
                }?: run{
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }catch(e:SecurityException){
                e.printStackTrace()
            }
        }
    }

    private fun showCurrentLocationMarkerOnMap(latLng: LatLng) {
        kakaoMap?.labelManager?.layer?.addLabel(
            LabelOptions.from(latLng)
                .setTag("user_location")
                .setStyles(
                    LabelStyles.from(
                        LabelStyle.from(R.drawable.marker_current_location)
                    )
                )
                .setTexts(LabelTextBuilder().addTextLine("현재 위치", 0))
        )
    }

    private fun isInCampus(latLng: LatLng): Boolean {
        val minLat = 37.6258
        val maxLat = 37.6294
        val minLng = 127.0881
        val maxLng = 127.0936

        return (latLng.latitude in minLat..maxLat) && (latLng.longitude in minLng..maxLng)
    }



    private fun calculateIconSize(zoom: Int):Int{
        return when{
            zoom < 10f -> 15
            zoom < 14f -> 25
            else -> 35
        }
    }

    private fun updateMarkers(iconSize: Int) {
        val map = kakaoMap ?: return
        map.labelManager?.layer?.removeAll()
        addBuildingMarkers(map, iconSize)
    }

    private fun addBuildingMarkers(kakaoMap: KakaoMap, iconSize: Int) {
        val labelManager = kakaoMap.labelManager

        val infoMarkerStyle = LabelStyles.from(
            LabelStyle.from(R.drawable.marker_info)
                .setTextStyles(iconSize, Color.BLACK)
        )

        val quizMarkerStyle = LabelStyles.from(
            LabelStyle.from(R.drawable.marker_quiz)
                .setTextStyles(iconSize, Color.BLACK)
        )

        // 기존 마커 추가 코드에서 TextSize 대신 iconSize 사용
        val liberalArtsLocation = LatLng.from(37.62834, 127.0926)
        labelManager?.layer?.addLabel(
            LabelOptions.from(liberalArtsLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("인문사회관", 0))
                .setTag("liberal_arts_info")
        )

        val firstScienceLocation = LatLng.from(37.62926, 127.0896)
        labelManager?.layer?.addLabel(
            LabelOptions.from(firstScienceLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("제1 과학관", 0))
                .setTag("first_science_info")
        )

        val secondScienceLocation = LatLng.from(37.62942, 127.0905)
        labelManager?.layer?.addLabel(
            LabelOptions.from(secondScienceLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("제2 과학관", 0))
                .setTag("second_science_info")
        )

        val artHallLocation = LatLng.from(37.62913, 127.0916)
        labelManager?.layer?.addLabel(
            LabelOptions.from(artHallLocation)
                .setStyles(infoMarkerStyle)
                .setTexts(LabelTextBuilder().addTextLine("조형예술관", 0))
                .setTag("art_hall_info")
        )

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

    private fun updateQuizProgress() {
        val percentage = if (totalQuizzes > 0) (completedQuizzes * 100) / totalQuizzes else 0
        quizProgressTextView.text = "퀴즈 진행률: ${completedQuizzes}/${totalQuizzes} (${percentage}%)"
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}