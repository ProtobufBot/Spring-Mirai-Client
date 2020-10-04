package net.lz1998.mirai.entity

import net.lz1998.mirai.alias.BFrame
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotEvent

interface RemoteBot {
    var bot: Bot
    var botId: Long
    var password: String

    suspend fun initBot()

    suspend fun login()

    // 执行并返回结果
    suspend fun onRemoteApi(req: BFrame): BFrame

    // 收到机器人事件
    suspend fun onBotEvent(botEvent: BotEvent)
}