package net.lz1998.mirai.service

import net.lz1998.mirai.entity.BotStatus
import net.lz1998.mirai.entity.MiraiBot
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotOfflineEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.subscribeAlways
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import net.lz1998.mirai.utils.*

@Service
class BotService {
    val botMap = mutableMapOf<Long, MiraiBot>()

    @Autowired
    lateinit var loginService: LoginService

    @Autowired
    lateinit var eventService: EventService

    suspend fun createBot(botId: Long, password: String) {
        val bot = Bot(botId, password) {
            loginSolver = loginService
            fileBasedDeviceInfo("device.json")
            noNetworkLog()
        }
        initBot(bot)
    }

    suspend fun initBot(bot: Bot): MiraiBot {
        val miraiBot = MiraiBot(botId = bot.id, bot = bot, status = BotStatus.OFFLINE)
        botMap[bot.id] = MiraiBot(botId = bot.id, bot = bot, status = BotStatus.OFFLINE)

        // TODO websocket/http 发送给远程消息处理器
        bot.subscribeAlways<BotOnlineEvent> {
            botMap[bot.id]?.status = BotStatus.ONLINE
        }
        bot.subscribeAlways<BotOfflineEvent> {
            botMap[bot.id]?.status = BotStatus.OFFLINE
        }
        bot.subscribeAlways<BotEvent> {
            val protoMessage = it.toProtoMessage()
            eventService.handleEvent(bot.id, protoMessage)
        }
        miraiBot.status = BotStatus.LOGIN
        miraiBot.bot.login()
        return miraiBot
    }

}

