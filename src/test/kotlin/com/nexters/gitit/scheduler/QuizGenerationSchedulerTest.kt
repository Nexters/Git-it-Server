package com.nexters.gitit.scheduler

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.application.quizrepo.GenerateQuiz
import com.nexters.gitit.application.quizrepo.ReclaimQuizGeneration
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import org.junit.jupiter.api.Test
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant

/**
 * 폴링이 실제로 도는지를 봅니다.
 *
 * `@EnableScheduling`이 빠지거나 `scheduler` 빈 이름이 어긋나면 **예외 없이 조용히 안 돕니다.** 컴파일도
 * 기동도 통과하고 대기줄만 영영 쌓이므로, 배선이 살아 있다는 사실을 여기서 못 박습니다.
 *
 * 스케줄러는 `@Profile("!test")`라 기본 테스트 컨텍스트에는 뜨지 않습니다 — 여기서만 프로필을 바꿔 켭니다.
 */
@SpringBootTest(properties = ["spring.profiles.active=scheduler-test"])
@Import(TestcontainersConfiguration::class)
class QuizGenerationSchedulerTest(
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
) {
    // 진짜로 돌리면 GitHub와 Gemini를 부른다. 여기서 볼 것은 "집어 갔는가"까지다.
    @MockitoBean
    private lateinit var generateQuiz: GenerateQuiz

    // 진짜 회수는 QuizGenerationReclaimerTest가 본다. 여기서 볼 것은 폴링이 부르는가까지다.
    @MockitoBean
    private lateinit var reclaimQuizGeneration: ReclaimQuizGeneration

    @Test
    fun `대기줄에 세워 두면 폴링이 집어 가고, 회수도 함께 돈다`() {
        val quizRepo =
            quizRepoRepository.save(
                QuizRepo(
                    githubRepoId = "7517918",
                    githubRepoUrl = "https://github.com/spring-projects/spring-petclinic",
                    name = "spring-petclinic",
                    ownerImageUrl = "https://avatars.githubusercontent.com/u/317776?v=4",
                    starCount = 7800,
                    techStacks = listOf("java"),
                    registeredAt = Instant.EPOCH,
                ),
            )

        // fixedDelay는 초기 지연이 없어 첫 회차가 곧바로 돈다. 여유는 컨텍스트 기동이 겹칠 때를 위한 것이다.
        verify(generateQuiz, timeout(15_000)).invoke(GenerateQuiz.Command(quizRepo.id))
        // 회수를 따로 떼지 않는 것은 목이 테스트마다 초기화되기 때문이다. 다음 회수는 1분 뒤라 기다릴 수 없다.
        verify(reclaimQuizGeneration, timeout(15_000)).invoke()
    }
}
