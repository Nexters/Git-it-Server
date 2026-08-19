package com.nexters.gitit.scheduler

import com.nexters.gitit.application.quizrepo.GenerateQuiz
import com.nexters.gitit.application.quizrepo.GetReadyQuizRepos
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.TimeUnit

/**
 * 문제 생성의 소비자. 시계가 부르는 입구입니다.
 *
 * 등록·재시도는 저장소를 대기 상태로 저장만 하고 끝냅니다. 실행 도중 프로세스가 죽어도 도큐먼트는
 * 대기줄에 그대로 남아 재기동 뒤 다시 집힙니다.
 */
@Configuration
// 테스트에서는 뜨지 않습니다. 켜 두면 테스트가 만든 READY 저장소를 폴링이 집어 GitHub·Gemini를 실제로 부릅니다.
@Profile("!test")
class QuizGenerationScheduler(
    private val getReadyQuizRepos: GetReadyQuizRepos,
    private val generateQuiz: GenerateQuiz,
) {
    /**
     * 대기 중인 저장소를 오래 기다린 순서로 하나씩 돌립니다.
     *
     * `fixedDelay`라 이번 회차가 다 끝나야 다음 조회가 시작합니다 — 생성이 동시에 두 개 돌지 않습니다.
     * Gemini 쿼터가 분당이라 겹쳐 쏘면 뒤쪽이 전부 백오프에 걸려 더 느려집니다.
     *
     * 10초는 대기줄이 비었을 때의 폴링 간격입니다. 작업 하나가 몇 분이라 등록 직후 시작까지의
     * 체감 지연만 정합니다.
     *
     * 한 건이 예외로 끝나면 남은 건은 다음 회차가 이어 받습니다. 그 저장소는 이미 FAILED로 대기줄에서
     * 빠져 있어 여기서 따로 잡지 않습니다.
     */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS, scheduler = TASK_SCHEDULER)
    fun run() {
        getReadyQuizRepos().items.forEach {
            generateQuiz(GenerateQuiz.Command(it.quizRepoId))
        }
    }

    /**
     * [run] 전용 스케줄러. [run]이 몇십 분씩 스레드를 붙잡는데 부트 기본 `taskScheduler`는 풀 크기가 1이라,
     * 같이 쓰면 다른 `@Scheduled`가 그동안 조용히 밀립니다.
     */
    @Bean(TASK_SCHEDULER)
    fun quizGenerationTaskScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("quiz-gen-")
        }

    companion object {
        private const val TASK_SCHEDULER = "quizGenerationTaskScheduler"
    }
}
