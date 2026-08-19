package com.nexters.gitit.application.project

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Answer
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.Anchor
import com.nexters.gitit.domain.quizrepo.Depth
import com.nexters.gitit.domain.quizrepo.LearningSet
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.stereotype.Service

@Service
class GetLearningSet(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
) {
    /**
     * 세트 하나를 회원이 고른 난이도에 맞춰 엽니다. 그 레벨의 문제가 저장된 순서 그대로 전부 나오고,
     * 이미 푼 문제에는 그때 낸 답이 붙습니다.
     *
     * 푼 문제를 걸러내지 않는 이유는 복습 때문입니다 — 걸러내면 다 푼 세트에 들어갔을 때 볼 것이 없습니다.
     * 순서를 손대지 않는 것도 같은 이유입니다. 풀이 여부로 다시 정렬하면 화면에 붙는 문제 번호가
     * 재방문마다 달라집니다.
     */
    operator fun invoke(command: Command): Result {
        val project = projectRepository.findById(command.projectId) ?: throw BaseException(ErrorCode.PROJECT_NOT_FOUND)
        project.requireOwnedBy(command.memberId)

        // 프로젝트가 가리키는 저장소가 없는 것은 잘못된 요청이 아니라 데이터가 깨진 것이라, 404로 덮으면 원인이 묻힌다.
        val quizRepo: QuizRepo =
            quizRepoRepository.findById(project.quizRepoId)
                ?: error("프로젝트가 가리키는 저장소가 없습니다: quizRepoId=${project.quizRepoId}")
        // 문제 생성이 아직 안 끝난 저장소는 세트가 비어 있어 여기로 떨어진다. 상태를 따로 검사하지 않는 이유다.
        val learningSet = quizRepo.findLearningSet(command.setId) ?: throw BaseException(ErrorCode.LEARNING_SET_NOT_FOUND)

        // 문제마다 답 목록을 훑으면 문제 수 × 답 수가 된다.
        val answers = project.answers.associateBy(Answer::questionId)

        return Result(
            setId = learningSet.id,
            title = learningSet.title,
            description = learningSet.description,
            orientation = learningSet.orientation,
            level = project.quizLevel,
            questions =
                learningSet.questionsOf(project.quizLevel.toDepth()).map { question ->
                    Result.SolvableQuestion(
                        question = question,
                        sources = question.anchors.map { sourceOf(quizRepo, learningSet, it) },
                        answer = answers[question.id],
                    )
                },
        )
    }

    /** 앵커 자체에는 설명이 없어, 화면에 띄울 문장은 같은 세트의 노트에서 가져옵니다. */
    private fun sourceOf(
        quizRepo: QuizRepo,
        learningSet: LearningSet,
        anchor: Anchor,
    ) = Result.Source(
        anchor = anchor,
        summary = learningSet.summaryOf(anchor),
        url = quizRepo.sourceUrlOf(anchor),
    )

    // 이름이 같아도 enum이 둘이라 valueOf로 잇지 않는다. 한쪽에 레벨이 늘면 컴파일이 깨져야 옮겨 적는 것을 잊지 않는다.
    private fun QuizLevel.toDepth() =
        when (this) {
            QuizLevel.L1 -> Depth.L1
            QuizLevel.L2 -> Depth.L2
            QuizLevel.L3 -> Depth.L3
        }

    data class Command(
        val memberId: String,
        val projectId: String,
        val setId: String,
    )

    /**
     * 정답·해설·채점 기준을 지우지 않고 [Question]을 통째로 싣습니다. 무엇을 내보낼지는 응답 스키마를 쥔
     * 쪽이 정하는 편이 낫습니다 — 여기서 미리 골라 담으면 같은 필드 목록이 두 벌이 됩니다.
     */
    data class Result(
        val setId: String,
        val title: String,
        val description: String,
        val orientation: String,
        val level: QuizLevel,
        val questions: List<SolvableQuestion>,
    ) {
        /** 문제 하나와 그것을 푸는 자리에 함께 필요한 것들. [answer]가 있으면 이미 푼 문제입니다. */
        data class SolvableQuestion(
            val question: Question,
            val sources: List<Source>,
            val answer: Answer?,
        )

        data class Source(
            val anchor: Anchor,
            val summary: String?,
            val url: String,
        )
    }
}
