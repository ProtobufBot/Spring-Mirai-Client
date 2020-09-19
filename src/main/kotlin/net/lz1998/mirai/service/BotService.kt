package net.lz1998.mirai.service

import net.lz1998.mirai.entity.BotStatus
import net.lz1998.mirai.entity.MiraiBot
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotOfflineEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.id
import onebot.OnebotEvent
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BotService {
    val botMap = mutableMapOf<Long, MiraiBot>()

    @Autowired
    lateinit var loginService: LoginService

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
        bot.subscribeAlways<GroupMessageEvent> { event ->
            bot.logger.info("群 ${event.group.id} 消息 ${event.message}")
            val onebotEvent = OnebotEvent.GroupMessageEvent.newBuilder()
                    .setGroupId(event.group.id)
                    .setUserId(event.sender.id)
                    .setMessageId(message.id)
                    .build()
        }
        bot.subscribeAlways<FriendMessageEvent> {
            bot.logger.info("好友 ${it.friend.id} 消息 ${it.message}")
        }
        miraiBot.status = BotStatus.LOGIN
        miraiBot.bot.login()
        return miraiBot
    }

}

