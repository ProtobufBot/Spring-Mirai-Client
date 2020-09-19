package net.lz1998.mirai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SpringMiraiClientApplication

fun main(args: Array<String>) {
    runApplication<SpringMiraiClientApplication>(*args)
}
