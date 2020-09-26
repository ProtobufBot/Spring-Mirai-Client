package net.lz1998.mirai

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.converter.protobuf.ProtobufHttpMessageConverter

@SpringBootApplication
class SpringMiraiClientApplication

fun main(args: Array<String>) {
    runApplication<SpringMiraiClientApplication>(*args)
}
