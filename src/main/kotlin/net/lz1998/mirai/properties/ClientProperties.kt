package net.lz1998.mirai.properties

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component


@Component
@ConfigurationProperties("bot.client")
object ClientProperties {
    @Value("\${bot.client.wsUrl}")
    var wsUrl = "ws://127.0.0.1/ws/cq/"
}