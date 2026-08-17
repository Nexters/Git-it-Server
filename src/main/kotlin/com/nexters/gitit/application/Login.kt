package com.nexters.gitit.application

import com.nexters.gitit.domain.auth.JwtProvider
import com.nexters.gitit.domain.auth.JwtToken
import com.nexters.gitit.domain.auth.OauthAuthenticator
import com.nexters.gitit.domain.auth.OauthCredential
import com.nexters.gitit.domain.member.Member
import com.nexters.gitit.domain.member.MemberRepository
import com.nexters.gitit.domain.member.NicknameGenerator
import org.springframework.stereotype.Service

@Service
class Login(
    private val memberRepository: MemberRepository,
    private val jwtProvider: JwtProvider,
    private val oauthAuthenticator: OauthAuthenticator,
    private val nicknameGenerator: NicknameGenerator,
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
                        name = nicknameGenerator.generate(),
                    ),
                )

        val jwt = jwtProvider.generateToken(member.id)

        return Result(
            memberId = member.id,
            jwtToken = jwt,
            needsCuration = !member.isCurated(),
        )
    }

    data class Command(
        val credential: OauthCredential,
    )

    data class Result(
        val memberId: String,
        val jwtToken: JwtToken,
        val needsCuration: Boolean,
    )
}
