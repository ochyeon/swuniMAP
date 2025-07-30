package com.example.swuniMAP

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import com.example.swuniMAP.R

class QuizFragment : Fragment() {

    // 1. Question 모델 정의
    data class Question(
        val text: String,
        @DrawableRes val imageRes: Int?,
        val options: List<String>?,   // 객관식 보기 (null이면 주관식)
        val hint: String,
        val answer: String
    )

    private lateinit var quizId: String
    private lateinit var questions: List<Question>
    private var currentIndex = 0
    private var correctCount = 0

    // 뷰 바인딩
    private lateinit var questionTv: TextView
    private lateinit var hintTv: TextView
    private lateinit var photoIv: ImageView
    private lateinit var optionsRg: RadioGroup
    private lateinit var answerEt: EditText
    private lateinit var nextBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        quizId = requireArguments().getString("QUIZ_ID")
            ?: throw IllegalArgumentException("QUIZ_ID 필요")

        // 2. quizId에 따라 Question 리스트 구성
        questions = when (quizId) {
            // 도서관
            "library_quiz" -> listOf(
                Question(
                    text = "다음 사진 속 빈칸을 채우세요.",
                    imageRes = R.drawable.library_quiz_image_2,
                    options = null,
                    answer = "중앙간접등",
                    hint = "도서관 1층 멀티플렉스존 내부의 전등 스위치를 찾아보세요."
                ),
                Question(
                    text = "다음 중 도서관에서 노트북 사용이 불가능한 공간은?",
                    imageRes = R.drawable.library_quiz_image_1,
                    options = listOf("1층 세미나실", "4층 일반열람실", "4층 자유열람실"),
                    answer = "4층 일반열람실",
                    hint = "도서관에서 사진 속 안내문을 찾아보세요."
                )
            )
            // 누리관
            "nuri_hall_quiz" -> listOf(
                Question(
                    text = "음악감상실 왼쪽 첫 번째 줄의 의자 개수는?",
                    imageRes = null,
                    options = listOf("2개", "3개", "4개", "5개"),
                    answer = "3개",
                    hint = "2층 음악감상실에 방문해 보세요"
                ),
                Question(
                    text = "구시아 앞에 있는 키오스크 개수는?",
                    imageRes = null,
                    options = listOf("2개", "3개", "4개", "5개"),
                    answer = "4개",
                    hint = "지하 1층 구시아에 방문해 보세요"
                )
            )
            // 50주년기념관
            "anniversary_quiz" -> listOf(
                Question(
                    text = "학생 및 외부인은 교직원 식당을 이용할 수 없다.",
                    imageRes = null,
                    options = listOf("O", "X"),
                    answer = "X",
                    hint = "1층 교직원 식당 입구의 안내문을 확인해 보세요"
                )
            )
            // 기독교 교육관
            "christian_ed_quiz" -> listOf(
                Question(
                    text = "커뮤니티 라운지 이용 시간은\n평일 __시부터 __시까지이다.",
                    imageRes = null,
                    options = listOf("8, 17", "8, 18", "9, 17", "9, 18"),
                    answer = "8, 18",
                    hint = "1층 커뮤니티 라운지 입구의 안내문을 확인해 보세요"
                )
            )
            // 기본: 빈 리스트
            else -> emptyList()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_quiz, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. 뷰 바인딩
        questionTv = view.findViewById(R.id.textViewQuestion)
        hintTv = view.findViewById(R.id.textViewHint)
        photoIv    = view.findViewById(R.id.imageViewPhoto)
        optionsRg  = view.findViewById(R.id.radioGroupOptions)
        answerEt   = view.findViewById(R.id.editTextAnswer)
        nextBtn    = view.findViewById(R.id.buttonNext)

        if (questions.isEmpty()) {
            // 예외 처리 or placeholder 텍스트
            questionTv.text = "유효하지 않은 퀴즈입니다."
            nextBtn.isEnabled = false
            return
        }

        // 4. 첫 문제 표시
        showQuestion(currentIndex)

        nextBtn.setOnClickListener {
            val current = questions[currentIndex]
            // 답 체크
            val userAnswer = if (current.options != null) {
                // 객관식: 선택된 라디오 버튼 텍스트
                val rbId = optionsRg.checkedRadioButtonId
                if (rbId == -1) "" else view.findViewById<RadioButton>(rbId).text.toString()
            } else {
                // 주관식
                answerEt.text.toString().trim()
            }

            if (userAnswer == current.answer) {
                // 정답일 경우
                correctCount++
            } else {
                // 오답 토스트
                Toast.makeText(requireContext(), "오답입니다. 다시 시도해 주세요.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentIndex < questions.lastIndex) {
                // 다음 문제로
                currentIndex++
                showQuestion(currentIndex)
            } else {
                // 마지막 문제 -> 한 번에 결과 전달
                val resultIntent = Intent().apply {
                    putExtra("QUIZ_ID", quizId)
                    putExtra("CORRECT_COUNT", correctCount)
                }
                requireActivity().setResult(Activity.RESULT_OK, resultIntent)
                requireActivity().finish()
            }
        }
    }

    // 문제 인덱스에 맞춰 뷰 업데이트
    private fun showQuestion(index: Int) {
        val q = questions[index]
        questionTv.text = q.text
        hintTv.text = q.hint

        // 이미지 유무
        if (q.imageRes != null) {
            photoIv.visibility = View.VISIBLE
            photoIv.setImageResource(q.imageRes)
        } else {
            photoIv.visibility = View.GONE
        }

        // 객관식 vs 주관식
        if (q.options != null) {
            optionsRg.visibility = View.VISIBLE
            answerEt.visibility = View.GONE
            optionsRg.removeAllViews()
            optionsRg.clearCheck()
            q.options.forEach { opt ->
                val rb = RadioButton(requireContext()).apply {
                    text = opt
                    layoutParams = RadioGroup.LayoutParams(
                        0,
                        RadioGroup.LayoutParams.WRAP_CONTENT,
                        1f
                    )
                    gravity = Gravity.CENTER
                }
                optionsRg.addView(rb)
            }
        } else {
            optionsRg.visibility = View.GONE
            answerEt.visibility = View.VISIBLE
            answerEt.text?.clear()
        }

        // 버튼 텍스트: 마지막 문제면 “제출” 아니면 “다음”
        nextBtn.text = if (index < questions.lastIndex) "다음" else "제출"
    }

    companion object {
        fun newInstance(quizId: String): QuizFragment {
            val frag = QuizFragment()
            frag.arguments = Bundle().apply {
                putString("QUIZ_ID", quizId)
            }
            return frag
        }
    }
}
