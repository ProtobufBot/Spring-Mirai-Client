package net.lz1998.mirai

import net.lz1998.mirai.properties.ClientProperties
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity

@SpringBootApplication
@EnableConfigurationProperties(ClientProperties::class)
class SpringMiraiClientApplication

fun main(args: Array<String>) {
    runApplication<SpringMiraiClientApplication>(*args)
}
