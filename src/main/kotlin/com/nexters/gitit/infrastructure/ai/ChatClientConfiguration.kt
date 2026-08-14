package com.nexters.gitit.infrastructure.ai

import org.springframework.ai.chat.client.ChatClientBuilderCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class ChatClientConfiguration {
    /**
     * 커스터마이저를 거치는 이유는 [org.springframework.ai.chat.client.advisor.api.Advisor] 빈이
     * 자동으로 적용되지 않기 때문입니다. 자동 설정은 [ChatClientBuilderCustomizer] 빈만 훑습니다.
     */
    @Bean
    fun chatCallLoggingCustomizer() = ChatClientBuilderCustomizer { it.defaultAdvisors(ChatCallLoggingAdvisor()) }
}
