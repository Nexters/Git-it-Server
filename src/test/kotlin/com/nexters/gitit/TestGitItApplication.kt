package com.nexters.gitit

import org.springframework.boot.fromApplication
import org.springframework.boot.with

fun main(args: Array<String>) {
    fromApplication<GitItApplication>().with(TestcontainersConfiguration::class).run(*args)
}
