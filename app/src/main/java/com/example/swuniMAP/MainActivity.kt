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
import com.kakao.vectormap.label.LabelTextBuilder
import android.content.Intent
import android.app.Activity
import android.location.LocationManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import com.kakao.vectormap.KakaoMapReadyCallback
import com.kakao.vectormap.KakaoMapSdk
import com.kakao.vectormap.camera.CameraUpdateFactory
import com.kakao.vectormap.camera.CameraPosition

// 런타임 권한 요청을 위한 import 추가
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {
    private lateinit var mapView : MapView
    private lateinit var quizProgressTextView: TextView

    private var completedQuizzes = 0
    private val totalQuizzes = 6

    private var kakaoMap:KakaoMap? = null
    private var minZoomLevel: Float? = null

    private val quizCompletionStatus = mutableMapOf<String, Boolean>()

    private val quizSizeMap = mapOf(
        "library_quiz"        to 2,
        "nuri_hall_quiz"      to 2,
        "anniversary_quiz"    to 1,
        "christian_ed_quiz"   to 1
    )

    // 위치 권한 요청을 위한 고유 코드 정의
    private val LOCATION_PERMISSION_REQUEST_CODE = 1001

    private val quizResultLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult

            val data = result.data ?: return@registerForActivityResult
            val quizId = data.getStringExtra("QUIZ_ID") ?: return@registerForActivityResult
            val correctCount = data.getIntExtra("CORRECT_COUNT", 0)
            val totalCount = quizSizeMap[quizId] ?: return@registerForActivityResult

            // 모두 맞히면 완료 처리
            if (correctCount == totalCount && quizCompletionStatus[quizId] == false) {
                quizCompletionStatus[quizId] = true
                completedQuizzes += correctCount
                updateQuizProgress()
                Toast.makeText(
                    this,
                    "$quizId 미션 성공!",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        KakaoMapSdk.init(this, BuildConfig.KAKAO_MAP_KEY)

        setContentView(R.layout.activity_main)

        mapView = findViewById(R.id.map_view)
        quizProgressTextView = findViewById(R.id.quizProgressTextView)

        quizCompletionStatus["anniversary_quiz"] = false
        quizCompletionStatus["nuri_hall_quiz"] = false
        quizCompletionStatus["library_quiz"] = false
        quizCompletionStatus["christian_ed_quiz"] = false

        updateQuizProgress()

        mapView.start(object : MapLifeCycleCallback() {

            override fun onMapDestroy() {
                // 이 메서드는 비워두어도 됩니다.
            }

            override fun onMapError(error: Exception) {
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

                // 초기 줌 레벨 저장 -> 지도의 최소 축소 배율 -> 더 축소하면 자동으로 배율 복원
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

                map.setOnLabelClickListener { _,_,label ->
                    val tag = label.tag as? String
                    Log.d("MapClick", "Label clicked. Tag: $tag") // 디버그 로그 추가
                    tag?.let {
                        if (it.endsWith("_info")) {
                            Log.d("MapClick", "Opening BuildingDetailActivity for: $it") // 디버그 로그 추가
                            val buildingId = it.substringBefore("_info")
                            val intent = Intent(this@MainActivity, BuildingDetailActivity::class.java)
                            intent.putExtra("BUILDING_ID", buildingId)
                            startActivity(intent)
                        } else if (it.endsWith("_quiz")) {
                            Log.d("MapClick", "Opening QuizActivity for: $it") // 디버그 로그 추가
                            val quizId = it

                            //완료된 퀴즈인지 체크
                            if(quizCompletionStatus[quizId] == true){
                                Toast.makeText(this@MainActivity, "완료된 미션입니다.", Toast.LENGTH_SHORT).show()
                                return@setOnLabelClickListener true
                            }

                            val intent = Intent(this@MainActivity, QuizActivity::class.java)
                            intent.putExtra("QUIZ_ID", quizId)

                            quizResultLauncher.launch(intent)
                        }
                    }
                    true
                }

                // 지도가 준비된 후 권한 확인 및 현재 위치 마커 표시를 시작합니다.
                checkLocationPermission()
            }
        })
    }

    // --- 런타임 권한 요청 및 처리 로직 추가 시작 ---
    private fun checkLocationPermission() {
        // ACCESS_FINE_LOCATION 권한이 있는지 확인
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없는 경우 사용자에게 권한 요청
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
        } else {
            // 권한이 이미 있는 경우 위치 업데이트 시작
            showCurrentLocationMarker()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 사용자가 위치 권한을 허용한 경우
                showCurrentLocationMarker()
            } else {
                // 사용자가 위치 권한을 거부한 경우
                Toast.makeText(this, "위치 권한이 거부되어 현재 위치를 표시할 수 없습니다. 캠퍼스 내에서 앱을 사용해주세요.", Toast.LENGTH_LONG).show()

                // 교외 지역 처리 로직과 동일하게 IntroActivity로 전환
                val intent = Intent(this, IntroActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                startActivity(intent)
                finish()
            }
        }
    }
    // --- 런타임 권한 요청 및 처리 로직 추가 끝 ---


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
                    // 위치를 가져올 수 없지만, 이는 권한 문제가 아닐 가능성이 높음 (기기가 위치 정보를 제공하지 못할 때)
                    Toast.makeText(this, "현재 위치를 가져올 수 없습니다. 위치 설정을 확인해주세요.", Toast.LENGTH_SHORT).show()
                }
            }catch(e:SecurityException){
                // 권한이 없어서 발생할 수 있는 예외 (Logcat에서 확인 가능)
                Log.e("Location", "위치 권한 오류: ${e.message}")
                Toast.makeText(this, "위치 권한이 없어 현재 위치를 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            // GPS나 네트워크 제공자가 없을 경우
            Toast.makeText(this, "위치 서비스를 활성화해주세요.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showCurrentLocationMarkerOnMap(latLng: LatLng) {
        // 기존에 user_location 마커가 있다면 제거
        kakaoMap?.labelManager?.layer?.getLabel("user_location")?.run {
            kakaoMap?.labelManager?.layer?.remove(this)
        }

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
        map.labelManager?.layer?.removeAll() // 기존 마커 전체 제거
        addBuildingMarkers(map, iconSize) // 새 아이콘 크기로 마커 다시 추가
        showCurrentLocationMarkerOnMap(map.cameraPosition?.position ?: LatLng.from(0.0, 0.0)) // 현재 위치 마커 다시 추가 (임시 LatLng, 실제 위치로 업데이트 필요)
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
//                .setTexts(LabelTextBuilder().addTextLine("인문사회관", 0))
                .setTag("liberal_arts_info")
        )

        val firstScienceLocation = LatLng.from(37.62926, 127.0896)
        labelManager?.layer?.addLabel(
            LabelOptions.from(firstScienceLocation)
                .setStyles(infoMarkerStyle)
//                .setTexts(LabelTextBuilder().addTextLine("제1 과학관", 0))
                .setTag("first_science_info")
        )

        val secondScienceLocation = LatLng.from(37.62942, 127.0905)
        labelManager?.layer?.addLabel(
            LabelOptions.from(secondScienceLocation)
                .setStyles(infoMarkerStyle)
//                .setTexts(LabelTextBuilder().addTextLine("제2 과학관", 0))
                .setTag("second_science_info")
        )

        val artHallLocation = LatLng.from(37.62913, 127.0916)
        labelManager?.layer?.addLabel(
            LabelOptions.from(artHallLocation)
                .setStyles(infoMarkerStyle)
//                .setTexts(LabelTextBuilder().addTextLine("조형예술관", 0))
                .setTag("art_hall_info")
        )

        val anniversaryHallQuizLocation1 = LatLng.from(37.62640, 127.0930)
        labelManager?.layer?.addLabel(
            LabelOptions.from(anniversaryHallQuizLocation1)
                .setStyles(quizMarkerStyle)
//                .setTexts(LabelTextBuilder().addTextLine("50주년 퀴즈 1", 0))
                .setTag("anniversary_quiz")
        )

        val nuriHallQuizLocation1 = LatLng.from(37.62873, 127.0906)
        labelManager?.layer?.addLabel(
            LabelOptions.from(nuriHallQuizLocation1)
                .setStyles(quizMarkerStyle)
//                .setTexts(LabelTextBuilder().addTextLine("누리관 퀴즈 1", 0))
                .setTag("nuri_hall_quiz")
        )

        val libraryQuizLocation1 = LatLng.from(37.62857, 127.0913)
        labelManager?.layer?.addLabel(
            LabelOptions.from(libraryQuizLocation1)
                .setStyles(quizMarkerStyle)
//                .setTexts(LabelTextBuilder().addTextLine("도서관 퀴즈 1", 0))
                .setTag("library_quiz")
        )

        val christianEdHallQuizLocation1 = LatLng.from(37.62709, 127.0925)
        labelManager?.layer?.addLabel(
            LabelOptions.from(christianEdHallQuizLocation1)
                .setStyles(quizMarkerStyle)
//                .setTexts(LabelTextBuilder().addTextLine("기독교 교육관 퀴즈 1", 0))
                .setTag("christian_ed_quiz")
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