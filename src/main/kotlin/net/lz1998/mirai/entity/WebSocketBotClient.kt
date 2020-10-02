package net.lz1998.mirai.entity

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.lz1998.mirai.ext.messageSourceLru
import net.lz1998.mirai.service.MyLoginSolver
import net.lz1998.mirai.utils.*
import net.lz1998.mirai.utils.toFrame
import net.mamoe.mirai.Bot
import net.mamoe.mirai.alsoLogin
import net.mamoe.mirai.event.events.BotEvent
import net.mamoe.mirai.event.subscribeAlways
import net.mamoe.mirai.message.MessageEvent
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import onebot.OnebotFrame
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
                val req = withContext(Dispatchers.IO) { OnebotFrame.Frame.parseFrom(bytes.toByteArray()) }
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
    }

    override suspend fun login() {
        bot.login()
    }

    override suspend fun onRemoteApi(req: OnebotFrame.Frame): OnebotFrame.Frame {
        val respBuilder = OnebotFrame.Frame.newBuilder()
        respBuilder.echo = req.echo
        respBuilder.botId = botId
        when (req.messageType) {
            OnebotFrame.Frame.MessageType.SendPrivateMsgReq -> respBuilder.sendPrivateMsgResp = handleSendPrivateMsg(bot, req.sendPrivateMsgReq)
            OnebotFrame.Frame.MessageType.SendGroupMsgReq -> respBuilder.sendGroupMsgResp = handleSendGroupMsg(bot, req.sendGroupMsgReq)
            OnebotFrame.Frame.MessageType.SendMsgReq -> respBuilder.sendMsgResp = handleSendMsgReq(bot, req.sendMsgReq)
            OnebotFrame.Frame.MessageType.DeleteMsgReq -> respBuilder.deleteMsgResp = handleDeleteMsg(bot, req.deleteMsgReq)
            OnebotFrame.Frame.MessageType.SetGroupKickReq -> respBuilder.setGroupKickResp = handleSetGroupKick(bot, req.setGroupKickReq)
            OnebotFrame.Frame.MessageType.SetGroupBanReq -> respBuilder.setGroupBanResp = handleSetGroupBan(bot, req.setGroupBanReq)
            OnebotFrame.Frame.MessageType.SetGroupWholeBanReq -> respBuilder.setGroupWholeBanResp = handleSetGroupWholeBan(bot, req.setGroupWholeBanReq)
            OnebotFrame.Frame.MessageType.SetGroupCardReq -> respBuilder.setGroupCardResp = handleSetGroupCard(bot, req.setGroupCardReq)
            OnebotFrame.Frame.MessageType.SetGroupNameReq -> respBuilder.setGroupNameResp = handleSetGroupName(bot, req.setGroupNameReq)
            OnebotFrame.Frame.MessageType.SetGroupLeaveReq -> respBuilder.setGroupLeaveResp = handleSetGroupLeave(bot, req.setGroupLeaveReq)
            OnebotFrame.Frame.MessageType.SetGroupSpecialTitleReq -> respBuilder.setGroupSpecialTitleResp = handleSetGroupSpecialTitle(bot, req.setGroupSpecialTitleReq)
            OnebotFrame.Frame.MessageType.GetLoginInfoReq -> respBuilder.getLoginInfoResp = handleGetLoginInfo(bot, req.getLoginInfoReq)
            OnebotFrame.Frame.MessageType.GetFriendListReq -> respBuilder.getFriendListResp = handleGetFriendList(bot, req.getFriendListReq)
            OnebotFrame.Frame.MessageType.GetGroupInfoReq -> respBuilder.getGroupInfoResp = handleGetGroupInfo(bot, req.getGroupInfoReq)
            OnebotFrame.Frame.MessageType.GetGroupListReq -> respBuilder.getGroupListResp = handleGetGroupList(bot, req.getGroupListReq)
            OnebotFrame.Frame.MessageType.GetGroupMemberInfoReq -> respBuilder.getGroupMemberInfoResp = handleGetGroupMemberInfo(bot, req.getGroupMemberInfoReq)
            OnebotFrame.Frame.MessageType.GetGroupMemberListReq -> respBuilder.getGroupMemberListResp = handleGetGroupMemberList(bot, req.getGroupMemberListReq)
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