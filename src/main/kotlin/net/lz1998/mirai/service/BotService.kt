package net.lz1998.mirai.service

import dto.HttpDto
import net.lz1998.mirai.entity.WebsocketBotClient
import net.lz1998.mirai.properties.ClientProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class BotService {
    val botMap = mutableMapOf<Long, WebsocketBotClient>()

    @Autowired
    lateinit var clientProperties: ClientProperties

    @Synchronized
    suspend fun createBot(botId: Long, password: String) {
        var bot = botMap[botId]
        // 如果有旧的，关掉旧的
        bot?.bot?.close()
        bot?.wsClient?.close(1001, "")

        // 开新的
        bot = WebsocketBotClient(botId, password, wsUrl = clientProperties.wsUrl)
        botMap[botId] = bot
        bot.initBot()
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

