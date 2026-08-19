package com.nexters.gitit.ui.common

import com.nexters.gitit.application.auth.VerifyMember
import com.nexters.gitit.domain.exception.BaseException
import com.nexters.gitit.domain.exception.ErrorCode
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

// 리졸버를 직접 호출하는 단위 테스트는 WebConfig 등록 누락을 못 잡으므로 슬라이스로 띄운다.
@WebMvcTest(controllers = [LoginMemberArgumentResolverTest.TestController::class])
@Import(LoginMemberArgumentResolverTest.TestController::class)
class LoginMemberArgumentResolverTest(
    @Autowired private val mockMvc: MockMvc,
) {
    // 토큰 검증 자체는 NimbusJwtProviderTest와 VerifyMemberTest가 덮으므로 여기서는 헤더 처리만 본다.
    @MockitoBean
    private lateinit var verifyMember: VerifyMember

    @Test
    fun `Bearer 토큰을 보내면 memberId가 주입된다`() {
        given(verifyMember(ACCESS_TOKEN)).willReturn(MEMBER_ID)

        mockMvc
            .get(TEST_PATH) { header(HttpHeaders.AUTHORIZATION, "Bearer $ACCESS_TOKEN") }
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { value(MEMBER_ID) }
            }
    }

    @Test
    fun `인증 스킴은 대소문자를 구분하지 않는다`() {
        given(verifyMember(ACCESS_TOKEN)).willReturn(MEMBER_ID)

        mockMvc
            .get(TEST_PATH) { header(HttpHeaders.AUTHORIZATION, "bearer $ACCESS_TOKEN") }
            .andExpect {
                status { isOk() }
                jsonPath("$.data") { value(MEMBER_ID) }
            }
    }

    @Test
    fun `Authorization 헤더가 없으면 401로 응답한다`() {
        mockMvc.get(TEST_PATH).andExpectUnauthorized()
    }

    @Test
    fun `Bearer 접두사가 없으면 401로 응답한다`() {
        mockMvc
            .get(TEST_PATH) { header(HttpHeaders.AUTHORIZATION, ACCESS_TOKEN) }
            .andExpectUnauthorized()
    }

    @Test
    fun `Bearer 뒤에 토큰이 없으면 401로 응답한다`() {
        mockMvc
            .get(TEST_PATH) { header(HttpHeaders.AUTHORIZATION, "Bearer ") }
            .andExpectUnauthorized()
    }

    @Test
    fun `토큰 검증에 실패하면 401로 응답한다`() {
        given(verifyMember(ACCESS_TOKEN)).willThrow(BaseException(ErrorCode.UNAUTHORIZED, "액세스 토큰 검증에 실패했습니다"))

        mockMvc
            .get(TEST_PATH) { header(HttpHeaders.AUTHORIZATION, "Bearer $ACCESS_TOKEN") }
            .andExpectUnauthorized()
    }

    /**
     * 상태 코드와 함께 응답 본문의 에러 코드까지 확인합니다.
     * 상태 코드만 보면 GlobalExceptionHandler를 거치지 않고 나온 401도 통과해서,
     * 리졸버가 의도한 예외를 던졌는지 구분하지 못합니다.
     */
    private fun ResultActionsDsl.andExpectUnauthorized() =
        andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value(ErrorCode.UNAUTHORIZED.code) }
        }

    /**
     * [LoginMember]를 실제 요청 경로에 태워 보기 위한 테스트 전용 컨트롤러입니다.
     * 주입된 memberId를 응답 본문에 그대로 담아, 테스트가 주입 결과를 확인할 수 있게 합니다.
     *
     * 중첩 클래스는 컴포넌트 스캔에 잡히지 않으므로 `@Import`로 직접 등록해야 합니다.
     */
    @RestController
    class TestController {
        @GetMapping(TEST_PATH)
        fun me(
            @LoginMember memberId: String,
        ): ApiResponse<String> = ApiResponse.success(memberId)
    }

    companion object {
        const val TEST_PATH = "/test/member-id"
        private const val ACCESS_TOKEN = "access-token"
        private const val MEMBER_ID = "member-1"
    }
}
