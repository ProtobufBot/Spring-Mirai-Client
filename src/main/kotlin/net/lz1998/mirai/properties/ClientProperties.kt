package net.lz1998.mirai.properties

import org.springframework.boot.context.properties.ConfigurationProperties


@ConfigurationProperties("bot.client")
class ClientProperties {
    var wsUrl = "ws://127.0.0.1/ws/cq/"
}