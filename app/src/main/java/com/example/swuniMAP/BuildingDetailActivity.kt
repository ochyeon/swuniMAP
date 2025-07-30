package com.example.swuniMAP

import android.os.Bundle
import android.view.View // View.VISIBLE, View.GONE을 사용하기 위해 import
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BuildingDetailActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_building_detail)

        val buildingId = intent.getStringExtra("BUILDING_ID")

        val buildingNameTextView: TextView = findViewById(R.id.buildingName)
        val buildingDescriptionTextView: TextView = findViewById(R.id.buildingDescription)
        val buildingImageView: ImageView = findViewById(R.id.buildingImage)
        val buildingImage2View: ImageView = findViewById(R.id.buildingImage2) // 새로 추가된 ImageView
        val buildingImage3View: ImageView = findViewById(R.id.buildingImage3) // 새로 추가된 ImageView

        when (buildingId) {
            "liberal_arts" -> {
                buildingNameTextView.text = "인문사회관"
                buildingDescriptionTextView.text = "1층에 GS25 편의점과 휴게 공간, 2층에 러닝커먼스 휴게 공간, 3층에 프린트존이 있습니다."
                // 기존 건물 외관 이미지
                buildingImageView.setImageResource(R.drawable.humanities_social_science_building_image)

                // 인문사회관 추가 이미지 1: 편의점
                buildingImage2View.setImageResource(R.drawable.humanities_social_science_convenience_store_image)
                buildingImage2View.visibility = View.VISIBLE // 이미지 표시

                // 인문사회관 추가 이미지 2: 휴게 공간
                buildingImage3View.setImageResource(R.drawable.humanities_social_science_lounge_image)
                buildingImage3View.visibility = View.VISIBLE // 이미지 표시
            }
            "first_science" -> {
                buildingNameTextView.text = "제1과학관"
                buildingDescriptionTextView.text = "1층 로비에 휴게 공간이 있습니다."
                buildingImageView.setImageResource(R.drawable.first_science_building_image)
                // 추가 이미지가 없으므로 숨김
                buildingImage2View.visibility = View.GONE
                buildingImage3View.visibility = View.GONE
            }
            "second_science" -> {
                buildingNameTextView.text = "제2과학관"
                buildingDescriptionTextView.text = "지하 1층 102호와 1층에 러닝커먼스 휴게 공간이 있습니다. 지하 1층에는 빈백도 있습니다."
                // 기존 건물 외관 이미지
                buildingImageView.setImageResource(R.drawable.second_science_building_image)

                // 제2과학관 추가 이미지 1: 휴게 공간
                buildingImage2View.setImageResource(R.drawable.second_science_lounge_image)
                buildingImage2View.visibility = View.VISIBLE // 이미지 표시

                // 제2과학관은 추가 이미지가 하나뿐이므로 세 번째 이미지는 숨김
                buildingImage3View.visibility = View.GONE
            }
            "art_hall" -> {
                buildingNameTextView.text = "조형예술관"
                buildingDescriptionTextView.text = "지하 1층 103호와 1층에 러닝커먼스 휴게 공간이 있습니다."
                buildingImageView.setImageResource(R.drawable.art_hall_building_image)
                // 추가 이미지가 없으므로 숨김
                buildingImage2View.visibility = View.GONE
                buildingImage3View.visibility = View.GONE
            }
            else -> {
                // 알 수 없는 buildingId의 경우
                buildingNameTextView.text = "건물 정보 없음"
                buildingDescriptionTextView.text = "죄송합니다. 요청하신 건물의 상세 정보를 찾을 수 없습니다."
                buildingImageView.setImageResource(R.drawable.placeholder_building_image)
                // 추가 이미지도 숨김
                buildingImage2View.visibility = View.GONE
                buildingImage3View.visibility = View.GONE
            }
        }
    }
}