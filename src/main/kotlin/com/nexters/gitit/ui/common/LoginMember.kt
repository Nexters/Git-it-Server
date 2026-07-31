package com.nexters.gitit.ui.common

/**
 * Authorization 헤더의 accessToken을 검증해 memberId를 주입합니다.
 * 붙일 수 있는 파라미터 타입은 String뿐입니다.
 */
@Target(AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.RUNTIME)
annotation class LoginMember
