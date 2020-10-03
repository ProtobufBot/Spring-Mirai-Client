package net.lz1998.mirai.entity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lz1998.mirai.alias.BFrame
import net.lz1998.mirai.alias.BFrameType
import net.lz1998.mirai.ext.friendRequestLru
import net.lz1998.mirai.ext.groupRequestLru
import net.lz1998.mirai.ext.messageSourceLru
import net.lz1998.mirai.service.MyLoginSolver
import net.lz1998.mirai.utils.*
import net.lz1998.mirai.utils.toFrame
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.BotEvent
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


    private var lastWsConnectTime: Long = 0
    private lateinit var wsClient: WebSocket
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
                val ok = wsClient.send(resp.toByteArray().toByteString())
                if (!ok) {
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
            super.onClosed(webSocket, code, reason)
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            println("websocket正在关闭 $reason")
            super.onClosing(webSocket, code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("websocket失败${t.message}")
            t.printStackTrace()
            wsConnect()
            super.onFailure(webSocket, t, response)
        }
    }


    @Synchronized
    fun wsConnect() {
        val now = System.currentTimeMillis()
        if (now - lastWsConnectTime > 5000L) {
            println("ws try connect")
            wsClient = httpClient.newWebSocket(wsRequest, wsListener)
            lastWsConnectTime = now
        } else {
            println("wait ws reconnect interval 5s")
        }
    }


    override suspend fun initBot() {
        wsClient = httpClient.newWebSocket(wsRequest, wsListener)
        bot = Bot(botId, password) {
            fileBasedDeviceInfo("device.json")
            loginSolver = MyLoginSolver
            noNetworkLog()
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
        when (req.frameType) {
            BFrameType.SendPrivateMsgReq -> respBuilder.sendPrivateMsgResp = handleSendPrivateMsg(bot, req.sendPrivateMsgReq)
            BFrameType.SendGroupMsgReq -> respBuilder.sendGroupMsgResp = handleSendGroupMsg(bot, req.sendGroupMsgReq)
            BFrameType.SendMsgReq -> respBuilder.sendMsgResp = handleSendMsgReq(bot, req.sendMsgReq)
            BFrameType.DeleteMsgReq -> respBuilder.deleteMsgResp = handleDeleteMsg(bot, req.deleteMsgReq)
            BFrameType.SetGroupKickReq -> respBuilder.setGroupKickResp = handleSetGroupKick(bot, req.setGroupKickReq)
            BFrameType.SetGroupBanReq -> respBuilder.setGroupBanResp = handleSetGroupBan(bot, req.setGroupBanReq)
            BFrameType.SetGroupWholeBanReq -> respBuilder.setGroupWholeBanResp = handleSetGroupWholeBan(bot, req.setGroupWholeBanReq)
            BFrameType.SetGroupCardReq -> respBuilder.setGroupCardResp = handleSetGroupCard(bot, req.setGroupCardReq)
            BFrameType.SetGroupNameReq -> respBuilder.setGroupNameResp = handleSetGroupName(bot, req.setGroupNameReq)
            BFrameType.SetGroupLeaveReq -> respBuilder.setGroupLeaveResp = handleSetGroupLeave(bot, req.setGroupLeaveReq)
            BFrameType.SetGroupSpecialTitleReq -> respBuilder.setGroupSpecialTitleResp = handleSetGroupSpecialTitle(bot, req.setGroupSpecialTitleReq)
            BFrameType.SetFriendAddRequestReq -> respBuilder.setFriendAddRequestResp = handleSetFriendAddRequest(bot, req.setFriendAddRequestReq)
            BFrameType.SetGroupAddRequestReq -> respBuilder.setGroupAddRequestResp = handleSetGroupAddRequest(bot, req.setGroupAddRequestReq)
            BFrameType.GetLoginInfoReq -> respBuilder.getLoginInfoResp = handleGetLoginInfo(bot, req.getLoginInfoReq)
            BFrameType.GetFriendListReq -> respBuilder.getFriendListResp = handleGetFriendList(bot, req.getFriendListReq)
            BFrameType.GetGroupInfoReq -> respBuilder.getGroupInfoResp = handleGetGroupInfo(bot, req.getGroupInfoReq)
            BFrameType.GetGroupListReq -> respBuilder.getGroupListResp = handleGetGroupList(bot, req.getGroupListReq)
            BFrameType.GetGroupMemberInfoReq -> respBuilder.getGroupMemberInfoResp = handleGetGroupMemberInfo(bot, req.getGroupMemberInfoReq)
            BFrameType.GetGroupMemberListReq -> respBuilder.getGroupMemberListResp = handleGetGroupMemberList(bot, req.getGroupMemberListReq)
        }
        return respBuilder.build()
    }

    override fun onBotEvent(botEvent: BotEvent) {
        val eventFrame = botEvent.toFrame() ?: return
        // TODO 写二进制还是json？配置
        val ok = wsClient.send(eventFrame.toByteArray().toByteString())
        if (!ok) {
            wsConnect()
        }
    }

}