package net.lz1998.mirai.entity

import net.lz1998.mirai.ext.messageSourceLru
import net.lz1998.mirai.service.myLoginSolver
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.MessageEvent
import onebot.OnebotFrame

interface RemoteBot {
    var bot: Bot
    var botId: Long
    var password: String

    suspend fun initBot() {
        bot = Bot(botId, password) {
            fileBasedDeviceInfo("device.json")
            loginSolver = myLoginSolver
            noNetworkLog()
        }.alsoLogin()
        bot.subscribeAlways<BotEvent> {
            onBotEvent(this)
        }
        bot.subscribeAlways<MessageEvent> {
            val messageSource = this.source // 撤回消息用
            bot.messageSourceLru.put(messageSource.id, messageSource)
        }
    }

    // 执行并返回结果
    suspend fun onRemoteApi(req: OnebotFrame.Frame): OnebotFrame.Frame

    // 收到机器人事件
    fun onBotEvent(botEvent: BotEvent)
}