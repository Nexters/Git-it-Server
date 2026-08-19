package com.nexters.gitit.application.project

import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import com.nexters.gitit.domain.project.Answer
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.ProjectRepository
import com.nexters.gitit.domain.quizrepo.Question
import com.nexters.gitit.domain.quizrepo.QuestionFormat
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import com.nexters.gitit.domain.quizrepo.Rubric
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.Instant

@Service
class SubmitAnswer(
    private val projectRepository: ProjectRepository,
    private val quizRepoRepository: QuizRepoRepository,
    private val clock: Clock,
) {
    operator fun invoke(command: Command.Choice): Result.Choice {
        val (project, question) = load(command)
        val correct = question.grade(command.selectedIndex)

        project.submit(Answer.Choice(question.id, Instant.now(clock), command.selectedIndex, correct))
        projectRepository.save(project)

        return Result.Choice(
            questionId = question.id,
            explanation = question.explanation,
            correct = correct,
            // 4지선다인데 정답이 비어 있으면 게이트를 지나면 안 됐을 문제가 저장된 것이라, 오답으로 흘려보내지 않고 터뜨린다.
            answerIndex = question.answerIndex ?: error("4지선다인데 정답이 없습니다: questionId=${question.id}"),
        )
    }

    operator fun invoke(command: Command.Essay): Result.Essay {
        val (project, question) = load(command)
        question.requireFormat(QuestionFormat.ESSAY)

        // 공백만 낸 답도 받되 ""로 굳혀 둡니다. 안 그러면 "빈 답"이 여러 형태로 저장돼 읽는 쪽마다 다시 판별해야 합니다.
        project.submit(Answer.Essay(question.id, Instant.now(clock), command.text.trim()))
        projectRepository.save(project)

        return Result.Essay(
            questionId = question.id,
            explanation = question.explanation,
            rubric = question.rubric ?: error("서술형인데 채점 기준이 없습니다: questionId=${question.id}"),
        )
    }

    private fun load(command: Command): Pair<Project, Question> {
        val project = projectRepository.findById(command.projectId) ?: throw BaseException(ErrorCode.PROJECT_NOT_FOUND)
        project.requireOwnedBy(command.memberId)

        // 프로젝트가 가리키는 저장소가 없는 것은 잘못된 요청이 아니라 데이터가 깨진 것이라, 404로 덮으면 원인이 묻힌다.
        val quizRepo: QuizRepo =
            quizRepoRepository.findById(project.quizRepoId)
                ?: error("프로젝트가 가리키는 저장소가 없습니다: quizRepoId=${project.quizRepoId}")
        val question = quizRepo.findQuestion(command.questionId) ?: throw BaseException(ErrorCode.QUESTION_NOT_FOUND)

        return project to question
    }

    /**
     * 객관식과 서술형을 나눠 받는 이유는 한 타입에 선택지 번호와 서술 답안을 nullable로 함께 두면
     * 둘 다 비었거나 둘 다 찬 요청이 타입에서 걸러지지 않기 때문입니다.
     */
    sealed class Command {
        abstract val memberId: String
        abstract val projectId: String
        abstract val questionId: String

        data class Choice(
            override val memberId: String,
            override val projectId: String,
            override val questionId: String,
            val selectedIndex: Int,
        ) : Command()

        data class Essay(
            override val memberId: String,
            override val projectId: String,
            override val questionId: String,
            val text: String,
        ) : Command()
    }

    sealed class Result {
        abstract val questionId: String
        abstract val explanation: String

        data class Choice(
            override val questionId: String,
            override val explanation: String,
            val correct: Boolean,
            val answerIndex: Int,
        ) : Result()

        /** 서버가 채점하지 않으므로 정답 자리에 [rubric]이 나갑니다 — 학습자가 스스로 대조할 재료입니다. */
        data class Essay(
            override val questionId: String,
            override val explanation: String,
            val rubric: Rubric,
        ) : Result()
    }
}
