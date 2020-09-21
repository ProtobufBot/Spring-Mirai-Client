package net.lz1998.mirai.service

import net.lz1998.mirai.entity.RemoteBot
import net.lz1998.mirai.entity.WebsocketBotClient
import org.springframework.stereotype.Service

@Service
class BotService {
    val botMap = mutableMapOf<Long, RemoteBot>()

    suspend fun createBot(botId: Long, password: String) {
        val bot = WebsocketBotClient(botId, password, "ws://127.0.0.1/ws/cq/")
        bot.initBot()
        botMap[botId] = bot
    }
}

