package com.nexters.gitit.application

import com.nexters.gitit.domain.auth.JwtProvider
import com.nexters.gitit.domain.auth.JwtToken
import com.nexters.gitit.domain.auth.OauthAuthenticator
import com.nexters.gitit.domain.auth.OauthCredential
import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.MemberRepository

class Login(
    private val memberRepository: MemberRepository,
    private val jwtProvider: JwtProvider,
    private val oauthAuthenticator: OauthAuthenticator,
) {
    operator fun invoke(command: Command): Result {
        val socialAccount = oauthAuthenticator.authenticate(command.credential)

        // 소셜 로그인만 지원하므로 별도 회원가입 없이 첫 로그인이 곧 가입
        val member =
            memberRepository.findBySocialIdentity(socialAccount.socialIdentity)
                ?: memberRepository.save(
                    Member(
                        socialIdentity = socialAccount.socialIdentity,
                        email = socialAccount.email,
                    ),
                )

        // TBD: 알림 기능 확정 시 알림 대상인 DeviceToken을 저장하는 로직 필요

        val jwt = jwtProvider.generateToken(member.id)

        return Result(
            memberId = member.id,
            jwtToken = jwt,
        )
    }

    data class Command(
        val credential: OauthCredential,
    )

    data class Result(
        val memberId: String,
        val jwtToken: JwtToken,
    )
}
