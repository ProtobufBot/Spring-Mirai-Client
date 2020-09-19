package net.lz1998.mirai.service

import com.google.protobuf.util.JsonFormat
import net.lz1998.mirai.entity.BotStatus
import net.lz1998.mirai.entity.MiraiBot
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotOfflineEvent
import net.mamoe.mirai.event.events.BotOnlineEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.FriendMessageEvent
import net.mamoe.mirai.message.GroupMessageEvent
import net.mamoe.mirai.message.data.*
import onebot.OnebotBase
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

            // 构造sender
            val sender = OnebotEvent.GroupMessageEvent.Sender.newBuilder()
                    .setUserId(event.sender.id)
                    .setNickname(event.sender.nick)
                    .setCard(event.sender.nameCard)
                    .setRole(event.sender.permission.name)
                    .setTitle(event.sender.specialTitle)

            // 构造消息链
            val onebotMessage = event.message.toOnebotMessage()

            // 构造rawMessage
            var rawMessage = event.message.toRawMessage()

            // 构造GroupMessageEvent
            val groupMessageEvent = OnebotEvent.GroupMessageEvent.newBuilder()
                    .setTime(System.currentTimeMillis())
                    .setSelfId(bot.id)
                    .setPostType("message")
                    .setMessageType("group")
                    .setSubType("normal")
                    .setMessageId(message.id)
                    .setGroupId(event.group.id)
                    .setUserId(event.sender.id)
                    .addAllMessage(onebotMessage)
                    .setRawMessage(rawMessage)
                    .setSender(sender)
                    .build()
            val eventStr = JsonFormat.printer().print(groupMessageEvent)
            println(eventStr)
        }
        bot.subscribeAlways<FriendMessageEvent> {
            bot.logger.info("好友 ${it.friend.id} 消息 ${it.message}")
        }
        miraiBot.status = BotStatus.LOGIN
        miraiBot.bot.login()
        return miraiBot
    }

}

fun MessageChain.toRawMessage(): String {
    var rawMessage = ""
    this.forEachContent { rawMessage += it.contentToString() }
    return rawMessage
}

fun MessageChain.toOnebotMessage(): List<OnebotBase.Message> {
    val messageChain = mutableListOf<OnebotBase.Message>()
    this.forEachContent { content ->
        val message = when (content) {
            is At -> OnebotBase.Message.newBuilder().setType("at").putAllData(mapOf("qq" to content.target.toString())).build()
            is PlainText -> OnebotBase.Message.newBuilder().setType("text").putAllData(mapOf("text" to content.content)).build()
            else -> OnebotBase.Message.newBuilder().setType("unknown").build()
        }
        messageChain.add(message)
    }
    return messageChain
}
