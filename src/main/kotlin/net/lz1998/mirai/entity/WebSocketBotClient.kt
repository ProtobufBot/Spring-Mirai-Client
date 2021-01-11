package net.lz1998.mirai.entity

import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import net.lz1998.mirai.alias.BFrame
import net.lz1998.mirai.alias.BFrameType
import net.lz1998.mirai.ext.*
import net.lz1998.mirai.service.MyLoginSolver
import net.lz1998.mirai.utils.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.BotFactory
import net.mamoe.mirai.event.events.*
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.io.File
import java.util.concurrent.TimeUnit

var json: Json = runCatching {
    Json {
        isLenient = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }
}.getOrElse { Json {} }

class WebsocketBotClient(override var botId: Long, override var password: String, wsUrl: String) : RemoteBot {
    override lateinit var bot: Bot


    private var lastWsConnectTime: Long = 0
    var connecting: Boolean = false

    var wsClient: WebSocket? = null
    private var httpClient: OkHttpClient = OkHttpClient.Builder()
            .callTimeout(20, TimeUnit.SECONDS)
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .pingInterval(3, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    private var wsRequest: Request = Request.Builder()
            .header("x-self-id", botId.toString())
            .url(wsUrl)
            .build()
    private var wsListener: WebSocketListener = object : WebSocketListener() {

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            GlobalScope.launch {
                val req = withContext(Dispatchers.IO) { BFrame.parseFrom(bytes.toByteArray()) }
                val resp = onRemoteApi(req)
                val ok = wsClient?.send(resp.toByteArray().toByteString())
                if (ok == null || !ok) {
                    wsConnect()
                }
            }
            super.onMessage(webSocket, bytes)
        }

        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("websocket 已连接")
            super.onOpen(webSocket, response)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("websocket 已关闭")
            wsClient = null
            super.onClosed(webSocket, code, reason)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            println("websocket正在关闭 $reason")
            wsClient = null
            super.onClosing(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("websocket失败${t.message}")
            wsClient = null
//            t.printStackTrace()
            GlobalScope.launch {
                println("10秒后重连")
                delay(10000)
                wsConnect()
            }
            super.onFailure(webSocket, t, response)
        }
    }


    //    @Synchronized
    fun wsConnect() {
        if (connecting || wsClient != null) {
            return
        }
        val now = System.currentTimeMillis()
        synchronized(connecting) {
            if (connecting || now - lastWsConnectTime < 5000L) {
                return
            }
            connecting = true
            lastWsConnectTime = now
        }
        wsClient?.close(1001, "")
        wsClient = null
        println("websocket正在尝试连接")
        wsClient = httpClient.newWebSocket(wsRequest, wsListener)
        synchronized(connecting) {
            connecting = false
        }
    }

    override suspend fun initBot() {
        val myDeviceInfo = File("device/bot-${botId}.json").loadAsMyDeviceInfo(json)
        bot = BotFactory.newBot(botId, password) {
            protocol = myDeviceInfo.protocol
            deviceInfo = { myDeviceInfo.generateDeviceInfoData() }
            loginSolver = MyLoginSolver
        }
        bot.logger.info("DeviceInfo: ${json.encodeToString(MyDeviceInfo.serializer(), myDeviceInfo)}")

        bot.eventChannel.subscribeAlways<BotEvent> {
            onBotEvent(this)
        }
        bot.eventChannel.subscribeAlways<net.mamoe.mirai.event.events.MessageEvent> {
            val messageSource = this.source // 撤回消息用
            val messageId = if (messageSource.ids.isNotEmpty()) messageSource.ids[0] else 0
            bot.messageSourceLru.put(messageId, messageSource)
        }
        bot.eventChannel.subscribeAlways<MemberJoinRequestEvent> {
            bot.groupRequestLru.put(it.eventId, it)
        }
        bot.eventChannel.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            bot.botInvitedGroupRequestLru.put(it.eventId, it)
        }
        bot.eventChannel.subscribeAlways<NewFriendRequestEvent> {
            bot.friendRequestLru.put(it.eventId, it)
        }
        bot.eventChannel.subscribeAlways<BotOnlineEvent> {
            if (wsClient == null) {
                wsClient = httpClient.newWebSocket(wsRequest, wsListener)
            }
        }
        GlobalScope.launch {
            bot.login()
        }
    }

    override suspend fun login() {
        bot.login()
    }

    override suspend fun onRemoteApi(req: BFrame): BFrame {
        val respBuilder = BFrame.newBuilder()
        respBuilder.echo = req.echo
        respBuilder.botId = botId
        respBuilder.ok = true
        when (req.frameType) {
            BFrameType.TSendPrivateMsgReq -> {
                respBuilder.frameType = BFrameType.TSendPrivateMsgResp; respBuilder.sendPrivateMsgResp = handleSendPrivateMsg(bot, req.sendPrivateMsgReq)
            }
            BFrameType.TSendGroupMsgReq -> {
                respBuilder.frameType = BFrameType.TSendGroupMsgResp; respBuilder.sendGroupMsgResp = handleSendGroupMsg(bot, req.sendGroupMsgReq)
            }
            BFrameType.TSendMsgReq -> {
                respBuilder.frameType = BFrameType.TSendMsgResp; respBuilder.sendMsgResp = handleSendMsgReq(bot, req.sendMsgReq)
            }
            BFrameType.TDeleteMsgReq -> {
                respBuilder.frameType = BFrameType.TDeleteMsgResp; respBuilder.deleteMsgResp = handleDeleteMsg(bot, req.deleteMsgReq)
            }
            BFrameType.TGetMsgReq -> {
                respBuilder.frameType = BFrameType.TGetMsgResp; respBuilder.getMsgResp = handleGetMsg(bot, req.getMsgReq)
            }
            BFrameType.TSetGroupKickReq -> {
                respBuilder.frameType = BFrameType.TSetGroupKickResp; respBuilder.setGroupKickResp = handleSetGroupKick(bot, req.setGroupKickReq)
            }
            BFrameType.TSetGroupBanReq -> {
                respBuilder.frameType = BFrameType.TSetGroupBanResp; respBuilder.setGroupBanResp = handleSetGroupBan(bot, req.setGroupBanReq)
            }
            BFrameType.TSetGroupWholeBanReq -> {
                respBuilder.frameType = BFrameType.TSetGroupWholeBanResp; respBuilder.setGroupWholeBanResp = handleSetGroupWholeBan(bot, req.setGroupWholeBanReq)
            }
            BFrameType.TSetGroupCardReq -> {
                respBuilder.frameType = BFrameType.TSetGroupCardResp; respBuilder.setGroupCardResp = handleSetGroupCard(bot, req.setGroupCardReq)
            }
            BFrameType.TSetGroupNameReq -> {
                respBuilder.frameType = BFrameType.TSetGroupNameResp; respBuilder.setGroupNameResp = handleSetGroupName(bot, req.setGroupNameReq)
            }
            BFrameType.TSetGroupLeaveReq -> {
                respBuilder.frameType = BFrameType.TSetGroupLeaveResp; respBuilder.setGroupLeaveResp = handleSetGroupLeave(bot, req.setGroupLeaveReq)
            }
            BFrameType.TSetGroupSpecialTitleReq -> {
                respBuilder.frameType = BFrameType.TSetGroupSpecialTitleResp; respBuilder.setGroupSpecialTitleResp = handleSetGroupSpecialTitle(bot, req.setGroupSpecialTitleReq)
            }
            BFrameType.TSetFriendAddRequestReq -> {
                respBuilder.frameType = BFrameType.TSetFriendAddRequestResp; respBuilder.setFriendAddRequestResp = handleSetFriendAddRequest(bot, req.setFriendAddRequestReq)
            }
            BFrameType.TSetGroupAddRequestReq -> {
                respBuilder.frameType = BFrameType.TSetGroupAddRequestResp; respBuilder.setGroupAddRequestResp = handleSetGroupAddRequest(bot, req.setGroupAddRequestReq)
            }
            BFrameType.TGetLoginInfoReq -> {
                respBuilder.frameType = BFrameType.TGetLoginInfoResp; respBuilder.getLoginInfoResp = handleGetLoginInfo(bot, req.getLoginInfoReq)
            }
            BFrameType.TGetFriendListReq -> {
                respBuilder.frameType = BFrameType.TGetFriendListResp; respBuilder.getFriendListResp = handleGetFriendList(bot, req.getFriendListReq)
            }
            BFrameType.TGetGroupInfoReq -> {
                respBuilder.frameType = BFrameType.TGetGroupInfoResp; respBuilder.getGroupInfoResp = handleGetGroupInfo(bot, req.getGroupInfoReq)
            }
            BFrameType.TGetGroupListReq -> {
                respBuilder.frameType = BFrameType.TGetGroupListResp; respBuilder.getGroupListResp = handleGetGroupList(bot, req.getGroupListReq)
            }
            BFrameType.TGetGroupMemberInfoReq -> {
                respBuilder.frameType = BFrameType.TGetGroupMemberInfoResp; respBuilder.getGroupMemberInfoResp = handleGetGroupMemberInfo(bot, req.getGroupMemberInfoReq)
            }
            BFrameType.TGetGroupMemberListReq -> {
                respBuilder.frameType = BFrameType.TGetGroupMemberListResp; respBuilder.getGroupMemberListResp = handleGetGroupMemberList(bot, req.getGroupMemberListReq)
            }
            else -> respBuilder.ok = false
        }
        return respBuilder.build()
    }

    override suspend fun onBotEvent(botEvent: BotEvent) {
        val eventFrame = botEvent.toFrame() ?: return
        // TODO 写二进制还是json？配置
        val ok = wsClient?.send(eventFrame.toByteArray().toByteString())
        if (ok == null || !ok) {
            wsConnect()
        }
    }

}
