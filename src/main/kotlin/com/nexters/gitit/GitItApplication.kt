package com.nexters.gitit

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class GitItApplication

fun main(args: Array<String>) {
    runApplication<GitItApplication>(*args)
}
