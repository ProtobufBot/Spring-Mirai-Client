package net.lz1998.mirai.service

import dto.HttpDto
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import net.lz1998.mirai.entity.WebsocketBotClient
import net.lz1998.mirai.entity.json
import net.lz1998.mirai.properties.ClientProperties
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.File

@Service
class BotService {
    init {
        autoCreate("bots.json")
    }

    val botMap = mutableMapOf<Long, WebsocketBotClient>()

    @Autowired
    lateinit var clientProperties: ClientProperties

    @Synchronized
    suspend fun createBot(botId: Long, password: String, wsUrl: String = clientProperties.wsUrl) {
        var bot = botMap[botId]
        // 如果有旧的，关掉旧的
        bot?.bot?.close()
        bot?.wsClient?.close(1001, "")

        // 开新的
        bot = WebsocketBotClient(botId, password, wsUrl = wsUrl)
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

    // 自动使用配置文件创建
    private final fun autoCreate(filepath: String) {
        File(filepath).loadAsBotInfo(json).accounts.forEach {
            GlobalScope.launch {
                println("auto create ${it.uin}")
                createBot(it.uin, it.password, it.wsUrl)
            }
        }
    }
}


fun File.loadAsBotInfo(json: Json): BotInfo {
    if (!this.exists() || this.length() == 0L) {
        return BotInfo(listOf())
    }
    return json.decodeFromString(BotInfo.serializer(), this.readText())
}

@Serializable
class BotInfo(
        val accounts: List<BotAccount>
)

@Serializable
class BotAccount(
        val uin: Long,
        val password: String,
        val wsUrl: String,
)
