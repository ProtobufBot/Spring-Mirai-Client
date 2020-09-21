package net.lz1998.mirai.entity

import com.google.protobuf.util.JsonFormat
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.lz1998.mirai.utils.handleApiCall
import net.lz1998.mirai.utils.toFrame
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.BotEvent
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import onebot.OnebotApi
import onebot.OnebotFrame
import java.util.concurrent.TimeUnit

class WebsocketBotClient(override var botId: Long, override var password: String, wsUrl: String) : RemoteBot {
    override lateinit var bot: Bot

    val jsonFormatParser: JsonFormat.Parser = JsonFormat.parser().ignoringUnknownFields()
    val jsonFormatPrinter: JsonFormat.Printer = JsonFormat.printer().preservingProtoFieldNames()

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
        override fun onMessage(webSocket: WebSocket, text: String) {
            GlobalScope.launch {
                val reqBuilder = OnebotFrame.Frame.newBuilder()
                jsonFormatParser.merge(text, reqBuilder)
                val req = reqBuilder.build()
                val resp = onRemoteApi(req)
                val respStr = jsonFormatPrinter.print(resp)
                val ok = wsClient.send(respStr)
                if (!ok) {
                    wsConnect()
                }

            }
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            GlobalScope.launch {
                val req = OnebotFrame.Frame.parseFrom(bytes.toByteArray())
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
        super.initBot()
    }

    override suspend fun onRemoteApi(req: OnebotFrame.Frame): OnebotFrame.Frame {
        val apiReq = when (req.messageType) {
            OnebotFrame.Frame.MessageType.PrivateMessageEvent -> req.privateMessageEvent
            OnebotFrame.Frame.MessageType.GroupMessageEvent -> req.groupMessageEvent
            OnebotFrame.Frame.MessageType.GroupUploadNoticeEvent -> req.groupUploadNoticeEvent
            OnebotFrame.Frame.MessageType.GroupAdminNoticeEvent -> req.groupAdminNoticeEvent
            OnebotFrame.Frame.MessageType.GroupDecreaseNoticeEvent -> req.groupDecreaseNoticeEvent
            OnebotFrame.Frame.MessageType.GroupIncreaseNoticeEvent -> req.groupIncreaseNoticeEvent
            OnebotFrame.Frame.MessageType.GroupBanNoticeEvent -> req.groupBanNoticeEvent
            OnebotFrame.Frame.MessageType.FriendAddNoticeEvent -> req.friendAddNoticeEvent
            OnebotFrame.Frame.MessageType.GroupRecallNoticeEvent -> req.groupRecallNoticeEvent
            OnebotFrame.Frame.MessageType.FriendRecallNoticeEvent -> req.friendRecallNoticeEvent
            OnebotFrame.Frame.MessageType.FriendRequestEvent -> req.friendRequestEvent
            OnebotFrame.Frame.MessageType.GroupRequestEvent -> req.groupRequestEvent
            OnebotFrame.Frame.MessageType.SendPrivateMsgReq -> req.sendPrivateMsgReq
            OnebotFrame.Frame.MessageType.SendGroupMsgReq -> req.sendGroupMsgReq
            OnebotFrame.Frame.MessageType.SendMsgReq -> req.sendMsgReq
            OnebotFrame.Frame.MessageType.DeleteMsgReq -> req.deleteMsgReq
            OnebotFrame.Frame.MessageType.GetMsgReq -> req.getMsgReq
            OnebotFrame.Frame.MessageType.GetForwardMsgReq -> req.getForwardMsgReq
            OnebotFrame.Frame.MessageType.SendLikeReq -> req.sendLikeReq
            OnebotFrame.Frame.MessageType.SetGroupKickReq -> req.setGroupKickReq
            OnebotFrame.Frame.MessageType.SetGroupBanReq -> req.setGroupBanReq
            OnebotFrame.Frame.MessageType.SetGroupAnonymousReq -> req.setGroupAnonymousReq
            OnebotFrame.Frame.MessageType.SetGroupWholeBanReq -> req.setGroupWholeBanReq
            OnebotFrame.Frame.MessageType.SetGroupAdminReq -> req.setGroupAdminReq
            OnebotFrame.Frame.MessageType.SetGroupAnonymousBanReq -> req.setGroupAnonymousBanReq
            OnebotFrame.Frame.MessageType.SetGroupCardReq -> req.setGroupCardReq
            OnebotFrame.Frame.MessageType.SetGroupNameReq -> req.setGroupNameReq
            OnebotFrame.Frame.MessageType.SetGroupLeaveReq -> req.setGroupLeaveReq
            OnebotFrame.Frame.MessageType.SetGroupSpecialTitleReq -> req.setGroupSpecialTitleReq
            OnebotFrame.Frame.MessageType.SetFriendAddRequestReq -> req.setFriendAddRequestReq
            OnebotFrame.Frame.MessageType.SetGroupAddRequestReq -> req.setGroupAddRequestReq
            OnebotFrame.Frame.MessageType.GetLoginInfoReq -> req.getLoginInfoReq
            OnebotFrame.Frame.MessageType.GetStrangerInfoReq -> req.getStrangerInfoReq
            OnebotFrame.Frame.MessageType.GetFriendListReq -> req.getFriendListReq
            OnebotFrame.Frame.MessageType.GetGroupInfoReq -> req.getGroupInfoReq
            OnebotFrame.Frame.MessageType.GetGroupListReq -> req.getGroupListReq
            OnebotFrame.Frame.MessageType.GetGroupMemberInfoReq -> req.getGroupMemberInfoReq
            OnebotFrame.Frame.MessageType.GetGroupMemberListReq -> req.getGroupMemberListReq
            OnebotFrame.Frame.MessageType.GetGroupHonorInfoReq -> req.getGroupHonorInfoReq
            OnebotFrame.Frame.MessageType.GetCookiesReq -> req.getCookiesReq
            OnebotFrame.Frame.MessageType.GetCsrfTokenReq -> req.getCsrfTokenReq
            OnebotFrame.Frame.MessageType.GetCredentialsReq -> req.getCredentialsReq
            OnebotFrame.Frame.MessageType.GetRecordReq -> req.getRecordReq
            OnebotFrame.Frame.MessageType.GetImageReq -> req.getImageReq
            OnebotFrame.Frame.MessageType.CanSendImageReq -> req.canSendImageReq
            OnebotFrame.Frame.MessageType.CanSendRecordReq -> req.canSendRecordReq
            OnebotFrame.Frame.MessageType.GetStatusReq -> req.getStatusReq
            OnebotFrame.Frame.MessageType.GetVersionInfoReq -> req.getVersionInfoReq
            OnebotFrame.Frame.MessageType.SetRestartReq -> req.setRestartReq
            OnebotFrame.Frame.MessageType.CleanCacheReq -> req.cleanCacheReq
            OnebotFrame.Frame.MessageType.SendPrivateMsgResp -> req.sendPrivateMsgResp
            OnebotFrame.Frame.MessageType.SendGroupMsgResp -> req.sendGroupMsgResp
            OnebotFrame.Frame.MessageType.SendMsgResp -> req.sendMsgResp
            OnebotFrame.Frame.MessageType.DeleteMsgResp -> req.deleteMsgResp
            OnebotFrame.Frame.MessageType.GetMsgResp -> req.getMsgResp
            OnebotFrame.Frame.MessageType.GetForwardMsgResp -> req.getForwardMsgResp
            OnebotFrame.Frame.MessageType.SendLikeResp -> req.sendLikeResp
            OnebotFrame.Frame.MessageType.SetGroupKickResp -> req.setGroupKickResp
            OnebotFrame.Frame.MessageType.SetGroupBanResp -> req.setGroupBanResp
            OnebotFrame.Frame.MessageType.SetGroupAnonymousResp -> req.setGroupAnonymousResp
            OnebotFrame.Frame.MessageType.SetGroupWholeBanResp -> req.setGroupWholeBanResp
            OnebotFrame.Frame.MessageType.SetGroupAdminResp -> req.setGroupAdminResp
            OnebotFrame.Frame.MessageType.SetGroupAnonymousBanResp -> req.setGroupAnonymousBanResp
            OnebotFrame.Frame.MessageType.SetGroupCardResp -> req.setGroupCardResp
            OnebotFrame.Frame.MessageType.SetGroupNameResp -> req.setGroupNameResp
            OnebotFrame.Frame.MessageType.SetGroupLeaveResp -> req.setGroupLeaveResp
            OnebotFrame.Frame.MessageType.SetGroupSpecialTitleResp -> req.setGroupSpecialTitleResp
            OnebotFrame.Frame.MessageType.SetFriendAddRequestResp -> req.setFriendAddRequestResp
            OnebotFrame.Frame.MessageType.SetGroupAddRequestResp -> req.setGroupAddRequestResp
            OnebotFrame.Frame.MessageType.GetLoginInfoResp -> req.getLoginInfoResp
            OnebotFrame.Frame.MessageType.GetStrangerInfoResp -> req.getStrangerInfoResp
            OnebotFrame.Frame.MessageType.GetFriendListResp -> req.getFriendListResp
            OnebotFrame.Frame.MessageType.GetGroupInfoResp -> req.getGroupInfoResp
            OnebotFrame.Frame.MessageType.GetGroupListResp -> req.getGroupListResp
            OnebotFrame.Frame.MessageType.GetGroupMemberInfoResp -> req.getGroupMemberInfoResp
            OnebotFrame.Frame.MessageType.GetGroupMemberListResp -> req.getGroupMemberListResp
            OnebotFrame.Frame.MessageType.GetGroupHonorInfoResp -> req.getGroupHonorInfoResp
            OnebotFrame.Frame.MessageType.GetCookiesResp -> req.getCookiesResp
            OnebotFrame.Frame.MessageType.GetCsrfTokenResp -> req.getCsrfTokenResp
            OnebotFrame.Frame.MessageType.GetCredentialsResp -> req.getCredentialsResp
            OnebotFrame.Frame.MessageType.GetRecordResp -> req.getRecordResp
            OnebotFrame.Frame.MessageType.GetImageResp -> req.getImageResp
            OnebotFrame.Frame.MessageType.CanSendImageResp -> req.canSendImageResp
            OnebotFrame.Frame.MessageType.CanSendRecordResp -> req.canSendRecordResp
            OnebotFrame.Frame.MessageType.GetStatusResp -> req.getStatusResp
            OnebotFrame.Frame.MessageType.GetVersionInfoResp -> req.getVersionInfoResp
            OnebotFrame.Frame.MessageType.SetRestartResp -> req.setRestartResp
            OnebotFrame.Frame.MessageType.CleanCacheResp -> req.cleanCacheResp
            else -> null
        }
        val apiResp = apiReq?.let { handleApiCall(bot, it) }
        val respFrameBuilder = OnebotFrame.Frame.newBuilder()
        respFrameBuilder.echo = req.echo
        respFrameBuilder.messageType = req.messageType
        when (apiResp) {
            is OnebotApi.SendPrivateMsgResp -> respFrameBuilder.sendPrivateMsgResp = apiResp
            is OnebotApi.SendGroupMsgResp -> respFrameBuilder.sendGroupMsgResp = apiResp
            is OnebotApi.SendMsgResp -> respFrameBuilder.sendMsgResp = apiResp
            is OnebotApi.DeleteMsgResp -> respFrameBuilder.deleteMsgResp = apiResp
            is OnebotApi.GetMsgResp -> respFrameBuilder.getMsgResp = apiResp
            is OnebotApi.GetForwardMsgResp -> respFrameBuilder.getForwardMsgResp = apiResp
            is OnebotApi.SendLikeResp -> respFrameBuilder.sendLikeResp = apiResp
            is OnebotApi.SetGroupKickResp -> respFrameBuilder.setGroupKickResp = apiResp
            is OnebotApi.SetGroupBanResp -> respFrameBuilder.setGroupBanResp = apiResp
            is OnebotApi.SetGroupAnonymousBanResp -> respFrameBuilder.setGroupAnonymousBanResp = apiResp
            is OnebotApi.SetGroupWholeBanResp -> respFrameBuilder.setGroupWholeBanResp = apiResp
            is OnebotApi.SetGroupAdminResp -> respFrameBuilder.setGroupAdminResp = apiResp
            is OnebotApi.SetGroupAnonymousResp -> respFrameBuilder.setGroupAnonymousResp = apiResp
            is OnebotApi.SetGroupCardResp -> respFrameBuilder.setGroupCardResp = apiResp
            is OnebotApi.SetGroupNameResp -> respFrameBuilder.setGroupNameResp = apiResp
            is OnebotApi.SetGroupLeaveResp -> respFrameBuilder.setGroupLeaveResp = apiResp
            is OnebotApi.SetGroupSpecialTitleResp -> respFrameBuilder.setGroupSpecialTitleResp = apiResp
            is OnebotApi.SetFriendAddRequestResp -> respFrameBuilder.setFriendAddRequestResp = apiResp
            is OnebotApi.SetGroupAddRequestResp -> respFrameBuilder.setGroupAddRequestResp = apiResp
            is OnebotApi.GetLoginInfoResp -> respFrameBuilder.getLoginInfoResp = apiResp
            is OnebotApi.GetStrangerInfoResp -> respFrameBuilder.getStrangerInfoResp = apiResp
            is OnebotApi.GetFriendListResp -> respFrameBuilder.getFriendListResp = apiResp
            is OnebotApi.GetGroupInfoResp -> respFrameBuilder.getGroupInfoResp = apiResp
            is OnebotApi.GetGroupListResp -> respFrameBuilder.getGroupListResp = apiResp
            is OnebotApi.GetGroupMemberInfoResp -> respFrameBuilder.getGroupMemberInfoResp = apiResp
            is OnebotApi.GetGroupMemberListResp -> respFrameBuilder.getGroupMemberListResp = apiResp
            is OnebotApi.GetGroupHonorInfoResp -> respFrameBuilder.getGroupHonorInfoResp = apiResp
            is OnebotApi.GetCookiesResp -> respFrameBuilder.getCookiesResp = apiResp
            is OnebotApi.GetCsrfTokenResp -> respFrameBuilder.getCsrfTokenResp = apiResp
            is OnebotApi.GetCredentialsResp -> respFrameBuilder.getCredentialsResp = apiResp
            is OnebotApi.GetRecordResp -> respFrameBuilder.getRecordResp = apiResp
            is OnebotApi.GetImageResp -> respFrameBuilder.getImageResp = apiResp
            is OnebotApi.CanSendImageResp -> respFrameBuilder.canSendImageResp = apiResp
            is OnebotApi.CanSendRecordResp -> respFrameBuilder.canSendRecordResp = apiResp
            is OnebotApi.GetStatusResp -> respFrameBuilder.getStatusResp = apiResp
            is OnebotApi.GetVersionInfoResp -> respFrameBuilder.getVersionInfoResp = apiResp
            is OnebotApi.SetRestartResp -> respFrameBuilder.setRestartResp = apiResp
            is OnebotApi.CleanCacheResp -> respFrameBuilder.cleanCacheResp = apiResp
        }
        return respFrameBuilder.build()
    }

    override fun onBotEvent(botEvent: BotEvent) {
        val eventFrame = botEvent.toFrame()
        // TODO 写二进制还是json？配置
        val eventStr = jsonFormatPrinter.print(eventFrame)
        val ok = wsClient.send(eventStr)
        if (!ok) {
            wsConnect()
        }
    }

}