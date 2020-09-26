package net.lz1998.mirai.service

import dto.HttpDto
import net.lz1998.mirai.entity.RemoteBot
import net.lz1998.mirai.entity.WebsocketBotClient
import net.lz1998.mirai.properties.ClientProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BotService {
    val botMap = mutableMapOf<Long, RemoteBot>()

    @Autowired
    lateinit var clientProperties: ClientProperties

    @Synchronized
    suspend fun createBot(botId: Long, password: String) {
        var bot = botMap[botId]
        if (bot == null) {
            bot = WebsocketBotClient(botId, password, wsUrl = clientProperties.wsUrl)
            botMap[botId] = bot
            bot.initBot()
        }
    }

    fun listBot(): Collection<HttpDto.Bot> {
        return botMap.values.map { remoteBot ->
            HttpDto.Bot.newBuilder().setBotId(remoteBot.botId).setIsOnline(
                    try {
                        remoteBot.bot.isOnline
                    } catch (e: Exception) {
                        false
                    }
            ).build()
        }
    }

    suspend fun botLogin(botId: Long) {
        val bot = botMap[botId]
        bot?.bot?.login()
    }
}

