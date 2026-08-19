package com.nexters.gitit.ui.project.dto

import com.nexters.gitit.application.project.GetLearningSet
import com.nexters.gitit.domain.project.Answer
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.QuestionFormat
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

data class LearningSetResponse(
    @field:Schema(description = "학습 세트 id")
    val setId: String,
    @field:Schema(description = "세트 제목", example = "라우팅 흐름 따라가기")
    val title: String,
    @field:Schema(description = "세트 설명")
    val description: String,
    @field:Schema(description = "문제를 풀기 전에 읽는 안내. 문제로 낼 가치가 없는 사실(폴더 구조·진입점)이 여기 담깁니다")
    val orientation: String,
    @field:Schema(description = "이 프로젝트에 걸린 난이도. 아래 문제는 전부 이 레벨입니다")
    val level: Depth,
    @field:Schema(description = "이 레벨의 문제 전부. 이미 푼 문제도 걸러내지 않고 만들어진 순서 그대로 나갑니다")
    val questions: List<QuestionResponse>,
) {
    companion object {
        fun from(result: GetLearningSet.Result) =
            LearningSetResponse(
                setId = result.setId,
                title = result.title,
                description = result.description,
                orientation = result.orientation,
                level = result.level,
                questions = result.questions.map(QuestionResponse::from),
            )
    }
}

/**
 * 정답([com.nexters.gitit.domain.quizrepo.Question.answerIndex])·해설·채점 기준은 싣지 않습니다.
 * 화면에서 "확인"을 누르는 순간이 곧 제출이고, 제출 응답이 그것들을 돌려줍니다 —
 * 여기 실으면 풀기도 전에 정답이 클라이언트에 가 있게 됩니다.
 *
 * 풀었는지는 [myAnswer]가 있는지로 판단합니다. 별도 플래그를 두면 둘이 어긋날 수 있습니다.
 */
data class QuestionResponse(
    @field:Schema(description = "문제 id. 답을 제출할 때 이 값을 씁니다")
    val questionId: String,
    @field:Schema(description = "문제 형식. 어느 제출 API를 부를지와 choices가 차 있는지를 결정합니다")
    val format: QuestionFormat,
    @field:Schema(description = "문제 본문")
    val text: String,
    @field:Schema(description = "선택지. 서술형이면 빈 배열입니다")
    val choices: List<String>,
    @field:Schema(description = "이 문제가 인용한 코드 위치")
    val sources: List<SourceResponse>,
    @field:Schema(description = "이미 푼 문제라면 그때 낸 답, 아니면 null")
    val myAnswer: MyAnswerResponse?,
) {
    companion object {
        fun from(solvable: GetLearningSet.Result.SolvableQuestion) =
            QuestionResponse(
                questionId = solvable.question.id,
                format = solvable.question.format,
                text = solvable.question.text,
                choices = solvable.question.choices,
                sources = solvable.sources.map(SourceResponse::from),
                myAnswer = solvable.answer?.let(MyAnswerResponse::from),
            )
    }
}

data class SourceResponse(
    @field:Schema(description = "레포 루트 기준 상대 경로", example = "src/flask/sansio/blueprints.py")
    val file: String,
    @field:Schema(description = "인용한 첫 줄", example = "1")
    val startLine: Int,
    @field:Schema(description = "인용한 마지막 줄", example = "40")
    val endLine: Int,
    @field:Schema(description = "그 범위에 적혀 있던 식별자", example = "Blueprint")
    val symbol: String,
    @field:Schema(description = "이 자리가 무엇을 하는 곳인지. 짝지어 둔 설명이 없으면 null입니다")
    val summary: String?,
    @field:Schema(description = "GitHub에서 이 코드를 여는 주소. 커밋으로 고정돼 있어 레포가 갱신돼도 같은 코드를 가리킵니다")
    val url: String,
) {
    companion object {
        fun from(source: GetLearningSet.Result.Source) =
            SourceResponse(
                file = source.anchor.file,
                startLine = source.anchor.startLine,
                endLine = source.anchor.endLine,
                symbol = source.anchor.symbol,
                summary = source.summary,
                url = source.url,
            )
    }
}

/**
 * 채워지는 필드는 문제 형식을 따라갑니다 — 4지선다면 [selectedIndex]·[correct], 서술형이면 [text]입니다.
 * 제출 API처럼 형식별로 나눌 수 없는 자리라(한 목록에 두 형식이 섞입니다) 평평하게 두고
 * 어느 쪽이 차 있는지는 [QuestionResponse.format]이 말해줍니다.
 */
data class MyAnswerResponse(
    @field:Schema(description = "고른 선택지 번호(0부터). 서술형이면 null")
    val selectedIndex: Int?,
    @field:Schema(description = "제출한 답안. 4지선다면 null")
    val text: String?,
    @field:Schema(description = "정답 여부. 서술형은 학습자가 스스로 채점하므로 항상 null")
    val correct: Boolean?,
    @field:Schema(description = "제출 시각")
    val answeredAt: Instant,
) {
    companion object {
        fun from(answer: Answer) =
            when (answer) {
                is Answer.Choice -> {
                    MyAnswerResponse(
                        selectedIndex = answer.selectedIndex,
                        text = null,
                        correct = answer.correct,
                        answeredAt = answer.answeredAt,
                    )
                }

                is Answer.Essay -> {
                    MyAnswerResponse(
                        selectedIndex = null,
                        text = answer.text,
                        correct = null,
                        answeredAt = answer.answeredAt,
                    )
                }
            }
    }
}
