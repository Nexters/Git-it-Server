package com.nexters.gitit.application

import com.nexters.gitit.TestcontainersConfiguration
import com.nexters.gitit.domain.member.DeviceInfo
import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.SocialIdentity
import com.nexters.gitit.domain.member.SocialType
import com.nexters.gitit.infrastructure.mongo.SpringDataMemberRepository
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class RegisterDeviceInfoTest(
    @Autowired private val registerDeviceInfo: RegisterDeviceInfo,
    @Autowired private val memberRepository: SpringDataMemberRepository,
) {
    @BeforeEach
    fun clear() {
        // 컨테이너는 테스트 클래스 간 공유되므로 도큐먼트만 비운다. 인덱스는 유지된다.
        memberRepository.deleteAll()
    }

    @Test
    fun `기기 정보를 저장한다`() {
        val member = memberRepository.save(memberOf())

        registerDeviceInfo(RegisterDeviceInfo.Command(member.id, DEVICE_INFO))

        val saved = memberRepository.findById(member.id).orElse(null).shouldNotBeNull()
        saved.deviceInfo shouldBe DEVICE_INFO
    }

    @Test
    fun `다시 등록하면 기존 기기 정보를 덮어쓴다`() {
        val member = memberRepository.save(memberOf())
        registerDeviceInfo(RegisterDeviceInfo.Command(member.id, DEVICE_INFO))

        val newDeviceInfo = DEVICE_INFO.copy(deviceId = "device-2", appVersion = "1.1.0", deviceToken = null)
        registerDeviceInfo(RegisterDeviceInfo.Command(member.id, newDeviceInfo))

        val saved = memberRepository.findById(member.id).orElse(null).shouldNotBeNull()
        saved.deviceInfo shouldBe newDeviceInfo
    }

    private fun memberOf() =
        Member(
            socialIdentity = SocialIdentity("device-owner", SocialType.GOOGLE),
            email = "gitit@nexters.com",
            name = "겁없는 SegFault",
        )

    companion object {
        private val DEVICE_INFO =
            DeviceInfo(
                deviceId = "device-1",
                deviceType = "ios",
                appVersion = "1.0.0",
                osVersion = "18.2",
                deviceToken = "device-token",
            )
    }
}
