package com.nexters.gitit.ui.auth

import com.nexters.gitit.application.auth.Login
import com.nexters.gitit.application.auth.VerifyMember
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

// 헤더 형식별 분기는 LoginMemberArgumentResolverTest가 덮으므로, 여기서는 이 엔드포인트의 계약인 200과 401만 본다.
@WebMvcTest(controllers = [AuthController::class])
class AuthControllerTest(
    @Autowired private val mockMvc: MockMvc,
) {
    @MockitoBean
    private lateinit var login: Login

    @MockitoBean
    private lateinit var verifyMember: VerifyMember

    @Test
    fun `유효한 액세스 토큰이면 200으로 응답한다`() {
        given(verifyMember(ACCESS_TOKEN)).willReturn(MEMBER_ID)

        mockMvc
            .get(TOKEN_PATH) { header(HttpHeaders.AUTHORIZATION, "Bearer $ACCESS_TOKEN") }
            .andExpect {
                status { isOk() }
                jsonPath("$.success") { value(true) }
                jsonPath("$.data") { doesNotExist() }
            }
    }

    @Test
    fun `만료된 액세스 토큰이면 401로 응답한다`() {
        given(verifyMember(ACCESS_TOKEN)).willThrow(BaseException(ErrorCode.UNAUTHORIZED, "액세스 토큰 검증에 실패했습니다"))

        mockMvc
            .get(TOKEN_PATH) { header(HttpHeaders.AUTHORIZATION, "Bearer $ACCESS_TOKEN") }
            .andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value(ErrorCode.UNAUTHORIZED.code) }
            }
    }

    companion object {
        private const val TOKEN_PATH = "/api/v1/auth/token"
        private const val ACCESS_TOKEN = "access-token"
        private const val MEMBER_ID = "member-1"
    }
}
