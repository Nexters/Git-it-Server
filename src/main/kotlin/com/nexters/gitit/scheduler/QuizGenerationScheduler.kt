package com.nexters.gitit.scheduler

import com.nexters.gitit.application.GenerateQuiz
import com.nexters.gitit.domain.quizrepo.QuizRepoRepository
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.TimeUnit

/**
 * 문제 생성의 소비자. `ui`·`event`와 나란한 또 하나의 입구입니다 — 스프링 이벤트가 아니라 시계가 부릅니다.
 *
 * 생산자(등록·재시도)는 저장소를 대기 상태로 저장만 하고 끝냅니다. 실행이 저장과 분리돼 있어,
 * 실행 도중 프로세스가 죽어도 도큐먼트는 대기줄에 그대로 남아 재기동 뒤 다시 집힙니다.
 */
@Configuration
// 테스트에서는 뜨지 않습니다. 켜 두면 테스트가 만든 READY 저장소를 폴링이 집어 GitHub·Gemini를 실제로 부릅니다.
@Profile("!test")
class QuizGenerationScheduler(
    private val quizRepoRepository: QuizRepoRepository,
    private val generateQuiz: GenerateQuiz,
) {
    /**
     * 대기 중인 저장소를 오래 기다린 순서로 하나씩 돌립니다.
     *
     * `fixedRate`가 아니라 `fixedDelay`인 것이 이 구조의 핵심입니다 — 이번 회차가 다 끝나야 다음 조회가
     * 시작하므로 생성이 동시에 두 개 도는 일이 생기지 않습니다. Gemini 쿼터가 분당이라, 겹쳐 쏘면
     * 뒤쪽이 전부 백오프에 걸려 더 느려집니다.
     *
     * 10초는 대기줄이 비었을 때의 폴링 간격이기도 합니다. 작업 하나가 몇 분이라 이 값이 쿼터에 주는
     * 영향은 없고, 등록 직후 시작까지의 체감 지연만 정합니다.
     *
     * 한 건이 예외로 끝나면 남은 건은 이번 회차에서 밀리지만, 그 저장소는 이미 FAILED가 되어 대기줄에서
     * 빠졌으므로 다음 회차가 나머지를 이어 받습니다. 그래서 여기서 따로 잡지 않습니다.
     *
     * 점유한 저장소는 STARTED가 되어 대기줄에서 빠지므로, SIGKILL·OOM으로 죽으면 아무도 다시
     * 집어 가지 않습니다 (예외로 끝나는 경로는 FAILED가 되어 재시도로 풀립니다).
     * 시효(`timeoutAt`)가 지난 STARTED를 되돌리는 회수는 다음 태스크에서 넣습니다.
     */
    @Scheduled(fixedDelay = 10, timeUnit = TimeUnit.SECONDS, scheduler = TASK_SCHEDULER)
    fun run() {
        quizRepoRepository.findAllPending().forEach {
            generateQuiz(GenerateQuiz.Command(it.id))
        }
    }

    /**
     * 전용 스케줄러를 두는 것은 이 작업이 몇십 분씩 스레드를 붙잡기 때문입니다. 부트 기본 `taskScheduler`는
     * 풀 크기가 1이라, 여기에 얹으면 나중에 추가되는 `@Scheduled`가 그동안 조용히 밀립니다.
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
