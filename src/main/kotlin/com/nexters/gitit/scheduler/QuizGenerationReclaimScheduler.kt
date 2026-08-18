package com.nexters.gitit.scheduler

import com.nexters.gitit.application.ReclaimQuizGeneration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.TaskScheduler
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler
import java.util.concurrent.TimeUnit

/** 회수의 입구. `ui`·`event`와 나란한 또 하나의 입구입니다 — 시계가 부릅니다. */
@Configuration
// 테스트에서는 뜨지 않습니다. 켜 두면 테스트가 만든 점유를 폴링이 되돌려 상태 단언이 흔들립니다.
@Profile("!test")
class QuizGenerationReclaimScheduler(
    private val reclaimQuizGeneration: ReclaimQuizGeneration,
) {
    /**
     * 1분은 시효(`GenerateQuiz.TIMEOUT`)에 비하면 순간이라, 회수 간격이 되돌아오는 시점을 좌우하지 않습니다.
     */
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES, scheduler = TASK_SCHEDULER)
    fun run() {
        reclaimQuizGeneration()
    }

    /**
     * 생성 폴링과 스레드를 나눠 갖습니다. 생성 한 회차는 대기줄이 길면 몇 시간씩 스레드를 붙잡아,
     * 같은 스케줄러에 얹으면 회수가 그만큼 밀립니다.
     */
    @Bean(TASK_SCHEDULER)
    fun quizReclaimTaskScheduler(): TaskScheduler =
        ThreadPoolTaskScheduler().apply {
            poolSize = 1
            setThreadNamePrefix("quiz-reclaim-")
        }

    companion object {
        private const val TASK_SCHEDULER = "quizReclaimTaskScheduler"
    }
}
