package com.nexters.gitit.domain.member

import com.nexters.gitit.domain.common.BaseEntity
import org.springframework.data.mongodb.core.index.CompoundIndex
import org.springframework.data.mongodb.core.mapping.Document

@Document(collection = "members")
@CompoundIndex(
    name = "uk_social_identity",
    def = "{'socialIdentity.socialId': 1, 'socialIdentity.socialType': 1}",
    unique = true,
    partialFilter = "{'deletedAt': null}",
)
class Member(
    val socialIdentity: SocialIdentity,
    email: String?,
    position: Position? = null,
    careerLevel: CareerLevel? = null,
) : BaseEntity() {
    var email: String? = email
        private set

    // TBD 큐레이션 플로우가 정해지지 않았기에 아래 값들은 미정
    var position: Position? = position
        private set

    var careerLevel: CareerLevel? = careerLevel
        private set

    // 가입 시점에는 알 수 없고 앱이 별도 요청으로 올려주므로 생성자에서 받지 않는다.
    var deviceInfo: DeviceInfo? = null
        private set

    fun isCurated(): Boolean = position != null && careerLevel != null

    /**
     * 기기 정보를 통째로 교체합니다.
     *
     * 회원당 기기 하나만 들고 있으므로 부분 갱신을 지원하지 않습니다. 앱 버전·OS 버전은 함께 바뀌는 값이라
     * 필드별로 갱신하면 서로 다른 시점의 값이 섞일 수 있습니다.
     */
    fun updateDeviceInfo(deviceInfo: DeviceInfo) {
        this.deviceInfo = deviceInfo
    }
}
