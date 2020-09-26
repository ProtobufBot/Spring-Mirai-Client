package net.lz1998.mirai.service

import dto.HttpDto
import net.lz1998.mirai.entity.RemoteBot
import net.lz1998.mirai.entity.WebsocketBotClient
import org.springframework.stereotype.Service

@Service
class BotService {
    val botMap = mutableMapOf<Long, RemoteBot>()

    @Synchronized
    fun createBot(botId: Long, password: String) {
        var bot = botMap[botId]
        if (bot == null) {
            bot = WebsocketBotClient(botId, password, "ws://127.0.0.1:8081/ws/cq/")
            bot.initBot()
            botMap[botId] = bot
        }
    }

    fun listBot(): Collection<HttpDto.Bot> {
        return botMap.values.map { remoteBot ->
            HttpDto.Bot.newBuilder().setBotId(remoteBot.botId).setIsOnline(remoteBot.bot.isOnline).build()
        }
    }

    suspend fun botLogin(botId: Long) {
        val bot = botMap[botId]
        bot?.bot?.login()
    }
}

