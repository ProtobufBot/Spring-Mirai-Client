package net.lz1998.mirai.entity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lz1998.mirai.alias.BFrame
import net.lz1998.mirai.alias.BFrameType
import net.lz1998.mirai.ext.*
import net.lz1998.mirai.service.MyLoginSolver
import net.lz1998.mirai.utils.*
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.events.BotInvitedJoinGroupRequestEvent
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.MessageEvent
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit

class WebsocketBotClient(override var botId: Long, override var password: String, wsUrl: String) : RemoteBot {
    override lateinit var bot: Bot


//    private var lastWsConnectTime: Long = 0

    private var wsClient: WebSocket? = null
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
                if (!ok!!) {
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
            t.printStackTrace()
            wsConnect()
            super.onFailure(webSocket, t, response)
        }
    }


    @Synchronized
    fun wsConnect() {
        if (wsClient == null) {
            println("ws try connect")
            wsClient = httpClient.newWebSocket(wsRequest, wsListener)
        }
//        val now = System.currentTimeMillis()
//        if (now - lastWsConnectTime > 5000L) {

//            lastWsConnectTime = now
//        } else {
//            println("wait ws reconnect interval 5s")
//        }
    }

    override suspend fun initBot() {
        wsClient = httpClient.newWebSocket(wsRequest, wsListener)
        bot = Bot(botId, password) {
            fileStrBasedDeviceInfo("device/${botId}.json")
            loginSolver = MyLoginSolver
//            noNetworkLog()
        }.alsoLogin()
        bot.subscribeAlways<BotEvent> {
            onBotEvent(this)
        }
        bot.subscribeAlways<MessageEvent> {
            val messageSource = this.source // 撤回消息用
            bot.messageSourceLru.put(messageSource.id, messageSource)
        }
        bot.subscribeAlways<MemberJoinRequestEvent> {
            bot.groupRequestLru.put(it.eventId, it)
        }
        bot.subscribeAlways<BotInvitedJoinGroupRequestEvent> {
            bot.botInvitedGroupRequestLru.put(it.eventId, it)
        }
        bot.subscribeAlways<NewFriendRequestEvent> {
            bot.friendRequestLru.put(it.eventId, it)
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
            BFrameType.TSendPrivateMsgReq -> {respBuilder.frameType = BFrameType.TSendPrivateMsgResp; respBuilder.sendPrivateMsgResp = handleSendPrivateMsg(bot, req.sendPrivateMsgReq)}
            BFrameType.TSendGroupMsgReq -> {respBuilder.frameType = BFrameType.TSendGroupMsgResp; respBuilder.sendGroupMsgResp = handleSendGroupMsg(bot, req.sendGroupMsgReq)}
            BFrameType.TSendMsgReq -> {respBuilder.frameType = BFrameType.TSendMsgResp; respBuilder.sendMsgResp = handleSendMsgReq(bot, req.sendMsgReq)}
            BFrameType.TDeleteMsgReq -> {respBuilder.frameType = BFrameType.TDeleteMsgResp; respBuilder.deleteMsgResp = handleDeleteMsg(bot, req.deleteMsgReq)}
            BFrameType.TGetMsgReq -> {respBuilder.frameType = BFrameType.TGetMsgResp; respBuilder.getMsgResp = handleGetMsg(bot, req.getMsgReq)}
            BFrameType.TSetGroupKickReq -> {respBuilder.frameType = BFrameType.TSetGroupKickResp; respBuilder.setGroupKickResp = handleSetGroupKick(bot, req.setGroupKickReq)}
            BFrameType.TSetGroupBanReq -> {respBuilder.frameType = BFrameType.TSetGroupBanResp; respBuilder.setGroupBanResp = handleSetGroupBan(bot, req.setGroupBanReq)}
            BFrameType.TSetGroupWholeBanReq -> {respBuilder.frameType = BFrameType.TSetGroupWholeBanResp; respBuilder.setGroupWholeBanResp = handleSetGroupWholeBan(bot, req.setGroupWholeBanReq)}
            BFrameType.TSetGroupCardReq -> {respBuilder.frameType = BFrameType.TSetGroupCardResp; respBuilder.setGroupCardResp = handleSetGroupCard(bot, req.setGroupCardReq)}
            BFrameType.TSetGroupNameReq -> {respBuilder.frameType = BFrameType.TSetGroupNameResp; respBuilder.setGroupNameResp = handleSetGroupName(bot, req.setGroupNameReq)}
            BFrameType.TSetGroupLeaveReq -> {respBuilder.frameType = BFrameType.TSetGroupLeaveResp; respBuilder.setGroupLeaveResp = handleSetGroupLeave(bot, req.setGroupLeaveReq)}
            BFrameType.TSetGroupSpecialTitleReq -> {respBuilder.frameType = BFrameType.TSetGroupSpecialTitleResp; respBuilder.setGroupSpecialTitleResp = handleSetGroupSpecialTitle(bot, req.setGroupSpecialTitleReq)}
            BFrameType.TSetFriendAddRequestReq -> {respBuilder.frameType = BFrameType.TSetFriendAddRequestResp; respBuilder.setFriendAddRequestResp = handleSetFriendAddRequest(bot, req.setFriendAddRequestReq)}
            BFrameType.TSetGroupAddRequestReq -> {respBuilder.frameType = BFrameType.TSetGroupAddRequestResp; respBuilder.setGroupAddRequestResp = handleSetGroupAddRequest(bot, req.setGroupAddRequestReq)}
            BFrameType.TGetLoginInfoReq -> {respBuilder.frameType = BFrameType.TGetLoginInfoResp; respBuilder.getLoginInfoResp = handleGetLoginInfo(bot, req.getLoginInfoReq)}
            BFrameType.TGetFriendListReq -> {respBuilder.frameType = BFrameType.TGetFriendListResp; respBuilder.getFriendListResp = handleGetFriendList(bot, req.getFriendListReq)}
            BFrameType.TGetGroupInfoReq -> {respBuilder.frameType = BFrameType.TGetGroupInfoResp; respBuilder.getGroupInfoResp = handleGetGroupInfo(bot, req.getGroupInfoReq)}
            BFrameType.TGetGroupListReq -> {respBuilder.frameType = BFrameType.TGetGroupListResp; respBuilder.getGroupListResp = handleGetGroupList(bot, req.getGroupListReq)}
            BFrameType.TGetGroupMemberInfoReq -> {respBuilder.frameType = BFrameType.TGetGroupMemberInfoResp; respBuilder.getGroupMemberInfoResp = handleGetGroupMemberInfo(bot, req.getGroupMemberInfoReq)}
            BFrameType.TGetGroupMemberListReq -> {respBuilder.frameType = BFrameType.TGetGroupMemberListResp; respBuilder.getGroupMemberListResp = handleGetGroupMemberList(bot, req.getGroupMemberListReq)}
            else -> respBuilder.ok = false
        }
        return respBuilder.build()
    }

    override suspend fun onBotEvent(botEvent: BotEvent) {
        val eventFrame = botEvent.toFrame() ?: return
        // TODO 写二进制还是json？配置
        val ok = wsClient?.send(eventFrame.toByteArray().toByteString())
        if (!ok!!) {
            wsConnect()
        }
    }

}