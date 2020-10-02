package net.lz1998.mirai.entity

import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotEvent
import onebot.OnebotFrame

interface RemoteBot {
    var bot: Bot
    var botId: Long
    var password: String

    suspend fun initBot()

    suspend fun login()

    // 执行并返回结果
    suspend fun onRemoteApi(req: OnebotFrame.Frame): OnebotFrame.Frame

    // 收到机器人事件
    fun onBotEvent(botEvent: BotEvent)
}