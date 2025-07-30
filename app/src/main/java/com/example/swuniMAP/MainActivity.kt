package com.example.swuniMAP

import android.graphics.Color
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.content.Intent
import android.app.Activity
import android.location.LocationManager
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

// 런타임 권한 요청을 위한 import 추가
import android.Manifest
import android.content.SharedPreferences
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraUpdate
import com.naver.maps.map.MapFragment
import com.naver.maps.map.NaverMap
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage


class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var naverMap : NaverMap
    private lateinit var quizProgressTextView: TextView
    private lateinit var sharedPreferences: SharedPreferences

    private var completedQuizzes = 0
    private val totalQuizzes = 6

    private var minZoomLevel: Double? = null
    private val quizCompletionStatus = mutableMapOf<String, Boolean>()
    private var buildingMarkers : MutableList<Marker> = mutableListOf()

    private val quizSizeMap = mapOf(
        "library_quiz"        to 2,
        "nuri_hall_quiz"      to 2,
        "anniversary_quiz"    to 1,
        "christian_ed_quiz"   to 1
    )

    private var userLocationMarker : Marker? = null

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
                completedQuizzes ++
                updateQuizProgress()
                Toast.makeText(
                    this,
                    "$quizId 미션 성공!",
                    Toast.LENGTH_SHORT
                ).show()
                saveQuizCompletionStatus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        quizProgressTextView = findViewById(R.id.quizProgressTextView)
        sharedPreferences = getSharedPreferences("QuizPrefs", MODE_PRIVATE)

        val quizIds = listOf(
            "anniversary_quiz_1",
            "nuri_hall_quiz_1", "nuri_hall_quiz_2",
            "library_quiz_1", "library_quiz_2",
            "christian_ed_quiz_1"
        )
        quizIds.forEach{quizCompletionStatus[it] = false}

        loadQuizCompletionStatus()
        updateQuizProgress()

        // 지도 초기화
        var mapFragment = supportFragmentManager.findFragmentById(R.id.map_container) as MapFragment?
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance()
            supportFragmentManager.beginTransaction()
                .add(R.id.map_container, mapFragment)
                .commit()
        }
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: NaverMap) {
        naverMap = map

        // 1. 캠퍼스 전체를 보기 위한 좌표
        val campusBounds = LatLng(37.6258, 127.0881) to LatLng(37.6294, 127.0936)
        val cameraUpdate = CameraUpdate.fitBounds(com.naver.maps.geometry.LatLngBounds(campusBounds.first, campusBounds.second))
        map.moveCamera(cameraUpdate)

        // 초기 줌 레벨 저장 -> 지도의 최소 축소 배율 -> 더 축소하면 자동으로 배율 복원
        minZoomLevel = map.cameraPosition.zoom

        addBuildingMarkers()
        showCurrentLocationMarker()

        naverMap.addOnCameraIdleListener {
            val zoom = naverMap.cameraPosition.zoom
            minZoomLevel?.let{minZoom->
                if (zoom < minZoom) {
                    naverMap.moveCamera(CameraUpdate.zoomTo(minZoom))
                }
            }
        }

        // 지도가 준비된 후 권한 확인 및 현재 위치 마커 표시를 시작합니다.
        checkLocationPermission()
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
                    val currentLatLng = LatLng(it.latitude, it.longitude)

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
        userLocationMarker?.map = null

        userLocationMarker = Marker().apply {
            position = latLng
            map = naverMap
            icon = OverlayImage.fromResource(R.drawable.marker_current_location)
            captionText = "현재 위치"
        }
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
        buildingMarkers.forEach { it.map = null }
        buildingMarkers.clear()

        addBuildingMarkers()
    }

    private fun addBuildingMarkers() {
        buildingMarkers.forEach { it.map = null }
        buildingMarkers.clear()

        val buildings = listOf(
            Triple("liberal_arts_info", com.naver.maps.geometry.LatLng(37.62834, 127.0926), R.drawable.marker_info),
            Triple("first_science_info", com.naver.maps.geometry.LatLng(37.62926, 127.0896), R.drawable.marker_info),
            Triple("second_science_info", com.naver.maps.geometry.LatLng(37.62942, 127.0905), R.drawable.marker_info),
            Triple("art_hall_info", com.naver.maps.geometry.LatLng(37.62913, 127.0916), R.drawable.marker_info),
            Triple("anniversary_quiz", com.naver.maps.geometry.LatLng(37.62640, 127.0930), R.drawable.marker_quiz),
            Triple("nuri_hall_quiz", com.naver.maps.geometry.LatLng(37.62873, 127.0906), R.drawable.marker_quiz),
            Triple("library_quiz", com.naver.maps.geometry.LatLng(37.62857, 127.0913), R.drawable.marker_quiz),
            Triple("christian_ed_quiz", com.naver.maps.geometry.LatLng(37.62709, 127.0925), R.drawable.marker_quiz)
        )
        buildings.forEach { (tag, position, iconRes) ->
            val marker = Marker().apply {
                this.position = position
                this.map = naverMap
                this.icon = OverlayImage.fromResource(iconRes)
                this.tag = tag
                // 필요하면 캡션(텍스트)도 설정 가능
                // this.captionText = tag
                setOnClickListener {
                    val clickedTag = it.tag as? String
                    clickedTag?.let { id ->
                        if (id.contains("quiz")) {
                            if (quizCompletionStatus[id] == true) {
                                Toast.makeText(this@MainActivity, "완료된 미션입니다.", Toast.LENGTH_SHORT).show()
                            } else {
                                val intent = Intent(this@MainActivity, QuizActivity::class.java)
                                intent.putExtra("QUIZ_ID", id)
                                quizResultLauncher.launch(intent)
                            }
                        } else if (id.contains("info")) {
                            val buildingId = id.removeSuffix("_info") // "liberal_arts_info" → "liberal_arts"
                            val intent = Intent(this@MainActivity, BuildingDetailActivity::class.java)
                            intent.putExtra("BUILDING_ID", buildingId)
                            startActivity(intent)
                        } else {
                            Toast.makeText(this@MainActivity, "알 수 없는 마커입니다.", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
            }
            buildingMarkers.add(marker)
        }
    }

    private fun updateQuizProgress() {
        val percentage = if (totalQuizzes > 0) (completedQuizzes * 100) / totalQuizzes else 0
        quizProgressTextView.text = "퀴즈 진행률: ${completedQuizzes}/${totalQuizzes} (${percentage}%)"
    }

    private fun saveQuizCompletionStatus() {
        val prefs = getSharedPreferences("quiz_prefs", MODE_PRIVATE)
        val editor = prefs.edit()

        for ((quizId, isCompleted) in quizCompletionStatus) {
            editor.putBoolean(quizId, isCompleted)
        }
        editor.apply()  // 비동기 저장
    }

    private fun loadQuizCompletionStatus() {
        val prefs = getSharedPreferences("quiz_prefs", MODE_PRIVATE)

        for (quizId in quizCompletionStatus.keys) {
            val isCompleted = prefs.getBoolean(quizId, false)
            quizCompletionStatus[quizId] = isCompleted
        }

        completedQuizzes = quizCompletionStatus.count { it.value }
        updateQuizProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}