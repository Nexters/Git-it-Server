package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.member.DeviceInfo
import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.SocialIdentity
import com.nexters.gitit.domain.member.SocialType
import com.nexters.gitit.domain.notification.NotificationMessage
import com.nexters.gitit.domain.notification.NotificationSender
import com.nexters.gitit.domain.project.Project
import com.nexters.gitit.domain.project.QuizLevel
import com.nexters.gitit.domain.quizrepo.QuizRepo
import com.nexters.gitit.domain.quizrepo.completed
import com.nexters.gitit.infrastructure.mongo.SpringDataMemberRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataProjectRepository
import com.nexters.gitit.infrastructure.mongo.SpringDataQuizRepoRepository
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant

/**
 * 저장소 → 프로젝트 → 회원으로 가는 조회가 실제 Mongo에서 도는지가 이 테스트의 목적입니다.
 * 파생 쿼리는 메서드 **이름**이 조건이라, 목으로 막으면 이름이 틀려도 통과합니다.
 *
 * 발송만 목입니다 — FCM은 진짜로 부를 수 없고, 실제 발송은 `FcmNotificationSenderTest`가 봅니다.
 */
@SpringBootTest
@Import(TestcontainersConfiguration::class)
class NotifyQuizResultTest(
    @Autowired private val notifyQuizResult: NotifyQuizResult,
    @Autowired private val quizRepoRepository: SpringDataQuizRepoRepository,
    @Autowired private val projectRepository: SpringDataProjectRepository,
    @Autowired private val memberRepository: SpringDataMemberRepository,
) {
    @MockitoBean
    private lateinit var notificationSender: NotificationSender

    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        quizRepoRepository.deleteAll()
        projectRepository.deleteAll()
        memberRepository.deleteAll()
    }

    /**
     * 회원 둘이 같은 저장소를 학습 중이고 한 명은 푸시를 거부한 상황입니다. 여기서 확인하는 것은
     * 토큰 없는 회원이 건너뛰어지는 것과, 실린 id가 **프로젝트** id라는 것입니다 — 회원 id나 저장소 id를
     * 실으면 앱이 알림을 눌렀을 때 엉뚱한 화면을 엽니다.
     */
    @Test
    fun `토큰이 있는 회원에게만 그 회원의 프로젝트 id를 실어 보낸다`() {
        val quizRepo = quizRepoRepository.save(quizRepoOf().completed("sha", emptyList()))
        val pushEnabled = memberRepository.save(memberOf("push-enabled", DEVICE_TOKEN))
        val pushDenied = memberRepository.save(memberOf("push-denied", null))
        val project = projectRepository.save(Project(pushEnabled.id, quizRepo.id, QuizLevel.L1))
        projectRepository.save(Project(pushDenied.id, quizRepo.id, QuizLevel.L1))

        notifyQuizResult(NotifyQuizResult.Command(quizRepo.id))

        val tokens = argumentCaptor<List<String>>()
        val message = argumentCaptor<NotificationMessage>()
        // 목의 기본 검증 횟수가 1회라, 토큰 없는 회원 몫이 나가지 않은 것까지 여기서 걸린다.
        verify(notificationSender).send(tokens.capture(), message.capture())
        tokens.firstValue shouldBe listOf(DEVICE_TOKEN)
        message.firstValue.data shouldBe mapOf("type" to "QUIZ_READY", "projectId" to project.id)
    }

    // 표시용 필드는 알림 대상 선정과 무관해 아무 값이나 채운다.
    private fun quizRepoOf() =
        QuizRepo(
            githubRepoId = "1",
            githubRepoUrl = "https://github.com/nexters/git-it",
            name = "git-it",
            ownerImageUrl = "https://avatars.githubusercontent.com/u/4995702?v=4",
            starCount = 0,
            techStacks = emptyList(),
            registeredAt = Instant.EPOCH,
        )

    private fun memberOf(
        socialId: String,
        deviceToken: String?,
    ) = Member(
        socialIdentity = SocialIdentity(socialId, SocialType.GOOGLE),
        email = "$socialId@nexters.com",
        name = "겁없는 SegFault",
    ).apply {
        updateDeviceInfo(
            DeviceInfo(
                deviceId = socialId,
                deviceType = "ios",
                appVersion = "1.0.0",
                osVersion = "18.2",
                deviceToken = deviceToken,
            ),
        )
    }

    companion object {
        private const val DEVICE_TOKEN = "device-token"
    }
}
