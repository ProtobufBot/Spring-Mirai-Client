package net.lz1998.mirai.entity

import net.mamoe.mirai.Bot

data class MiraiBot(
        var botId: Long,
        var status: BotStatus,
        var bot: Bot
)

enum class BotStatus(status: String) {
    CREATED("created"), // 创建成功

    LOGIN("login"), // 等验证码处理

    ONLINE("online"), // 在线

    OFFLINE("offline"), // 离线
}