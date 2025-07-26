package com.example.swuniMAP

import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BuildingDetailActivity : AppCompatActivity() { // 반드시 AppCompatActivity를 상속받아야 합니다!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_building_detail) // 위에서 만든 XML 레이아웃과 연결

        // MainActivity에서 'BUILDING_ID'라는 이름으로 넘어온 데이터를 받아옵니다.
        val buildingId = intent.getStringExtra("BUILDING_ID")

        // XML 레이아웃에 있는 뷰들을 코드로 찾아옵니다. (ID를 사용해요)
        val buildingNameTextView: TextView = findViewById(R.id.buildingName)
        val buildingDescriptionTextView: TextView = findViewById(R.id.buildingDescription)
        val buildingImageView: ImageView = findViewById(R.id.buildingImage)

        // 받아온 buildingId 값에 따라 다른 정보와 이미지를 표시합니다.
        when (buildingId) {
            "liberal_arts" -> { // MainActivity의 "liberal_arts_info" 태그에서 "liberal_arts"를 가져옵니다.
                buildingNameTextView.text = "인문사회관"
                buildingDescriptionTextView.text = "1층에 GS25 편의점과 휴게 공간, 2층에 러닝커먼스 휴게 공간, 3층에 프린트존이 있습니다."
                buildingImageView.setImageResource(R.drawable.humanities_social_science_building_image)
            }
            "first_science" -> { // "first_science_info" 태그에 해당
                buildingNameTextView.text = "제1과학관"
                buildingDescriptionTextView.text = "1층 로비에 휴게 공간이 있습니다."
                buildingImageView.setImageResource(R.drawable.first_science_building_image)
            }
            "second_science" -> { // "second_science_info" 태그에 해당
                buildingNameTextView.text = "제2과학관"
                buildingDescriptionTextView.text = "지하 1층 102호와 1층에 러닝커먼스 휴게 공간이 있습니다. 지하 1층에는 빈백도 있습니다."
                buildingImageView.setImageResource(R.drawable.second_science_building_image)
            }
            "art_hall" -> { // "art_hall_info" 태그에 해당
                buildingNameTextView.text = "조형예술관"
                buildingDescriptionTextView.text = "지하 1층 103호와 1층에 러닝커먼스 휴게 공간이 있습니다."
                buildingImageView.setImageResource(R.drawable.art_hall_building_image)
            }
            else -> {
                // (만약 알 수 없는 buildingId가 넘어왔을 경우)
                buildingNameTextView.text = "건물 정보 없음"
                buildingDescriptionTextView.text = "죄송합니다. 요청하신 건물의 상세 정보를 찾을 수 없습니다."
                buildingImageView.setImageResource(R.drawable.placeholder_building_image) // 에러 발생 시 보여줄 기본 이미지
            }
        }
    }
}