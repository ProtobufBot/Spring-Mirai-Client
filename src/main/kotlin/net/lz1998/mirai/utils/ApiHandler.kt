package net.lz1998.mirai.utils

import net.lz1998.mirai.ext.friendRequestLru
import net.lz1998.mirai.ext.groupRequestLru
import net.lz1998.mirai.ext.messageSourceLru
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.getFriendOrNull
import net.mamoe.mirai.getGroupOrNull
import net.mamoe.mirai.message.data.asMessageChain
import onebot.OnebotApi


suspend fun handleSendPrivateMsg(bot: Bot, req: OnebotApi.SendPrivateMsgReq): OnebotApi.SendPrivateMsgResp? {
    val contact = bot.getFriendOrNull(req.userId) ?: return null
    val messageChain = req.messageList.map { it.toMiraiMessage(bot, contact) }.asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    bot.messageSourceLru.put(messageSource.id, messageSource)
    return OnebotApi.SendPrivateMsgResp.newBuilder().setMessageId(messageSource.id).build()
}

suspend fun handleSendGroupMsg(bot: Bot, req: OnebotApi.SendGroupMsgReq): OnebotApi.SendGroupMsgResp? {
    val contact = bot.getGroupOrNull(req.groupId) ?: return null
    val messageChain = req.messageList.map { it.toMiraiMessage(bot, contact) }.asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    bot.messageSourceLru.put(messageSource.id, messageSource)
    return OnebotApi.SendGroupMsgResp.newBuilder().setMessageId(messageSource.id).build()
}

suspend fun handleSendMsgReq(bot: Bot, req: OnebotApi.SendMsgReq): OnebotApi.SendMsgResp? {
    val contact = when (req.messageType) {
        "group" -> {
            bot.getGroupOrNull(req.groupId)
        }
        "private" -> {
            bot.getFriendOrNull(req.userId)
        }
        else -> {
            bot.getGroupOrNull(req.groupId) ?: bot.getFriendOrNull(req.userId)
        }
    } ?: return null
    val messageChain = req.messageList.map { it.toMiraiMessage(bot, contact) }.asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    bot.messageSourceLru.put(messageSource.id, messageSource)
    return OnebotApi.SendMsgResp.newBuilder().setMessageId(messageSource.id).build()
}

suspend fun handleDeleteMsg(bot: Bot, req: OnebotApi.DeleteMsgReq): OnebotApi.DeleteMsgResp? {
    val messageSource = bot.messageSourceLru[req.messageId] ?: return null
    bot.recall(messageSource)
    return OnebotApi.DeleteMsgResp.newBuilder().build()
}

suspend fun handleSetGroupKick(bot: Bot, req: OnebotApi.SetGroupKickReq): OnebotApi.SetGroupKickResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    member.kick()
    return OnebotApi.SetGroupKickResp.newBuilder().build()
}

suspend fun handleSetGroupBan(bot: Bot, req: OnebotApi.SetGroupBanReq): OnebotApi.SetGroupBanResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    if (req.duration == 0) {
        member.unmute()
    } else {
        member.mute(req.duration)
    }
    return OnebotApi.SetGroupBanResp.newBuilder().build()
}

suspend fun handleSetGroupWholeBan(bot: Bot, req: OnebotApi.SetGroupWholeBanReq): OnebotApi.SetGroupWholeBanResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    group.settings.isMuteAll = req.enable
    return OnebotApi.SetGroupWholeBanResp.newBuilder().build()
}

suspend fun handleSetGroupCard(bot: Bot, req: OnebotApi.SetGroupCardReq): OnebotApi.SetGroupCardResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    member.nameCard = req.card
    return OnebotApi.SetGroupCardResp.newBuilder().build()
}

suspend fun handleSetGroupName(bot: Bot, req: OnebotApi.SetGroupNameReq): OnebotApi.SetGroupNameResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    group.name = req.groupName
    return OnebotApi.SetGroupNameResp.newBuilder().build()
}

suspend fun handleSetGroupLeave(bot: Bot, req: OnebotApi.SetGroupLeaveReq): OnebotApi.SetGroupLeaveResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    group.quit()
    return OnebotApi.SetGroupLeaveResp.newBuilder().build()
}

suspend fun handleSetGroupSpecialTitle(bot: Bot, req: OnebotApi.SetGroupSpecialTitleReq): OnebotApi.SetGroupSpecialTitleResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    member.specialTitle = req.specialTitle

    return OnebotApi.SetGroupSpecialTitleResp.newBuilder().build()
}

suspend fun handleSetFriendAddRequest(bot: Bot, req: OnebotApi.SetFriendAddRequestReq): OnebotApi.SetFriendAddRequestResp? {
    val approve = req.approve
    val flag = req.flag
    val request = bot.friendRequestLru[flag.toLongOrNull()] ?: return null
    if (approve) request.accept() else request.reject()
    return OnebotApi.SetFriendAddRequestResp.newBuilder().build()
}

suspend fun handleSetGroupAddRequest(bot: Bot, req: OnebotApi.SetGroupAddRequestReq): OnebotApi.SetGroupAddRequestResp? {
    val approve = req.approve
    val flag = req.flag
    val request = bot.groupRequestLru[flag.toLongOrNull()] ?: return null
    if (approve) request.accept() else request.reject()
    return OnebotApi.SetGroupAddRequestResp.newBuilder().build()
}

suspend fun handleGetLoginInfo(bot: Bot, req: OnebotApi.GetLoginInfoReq): OnebotApi.GetLoginInfoResp {
    return OnebotApi.GetLoginInfoResp.newBuilder().setUserId(bot.id).setNickname(bot.nick).build()
}

suspend fun handleGetFriendList(bot: Bot, req: OnebotApi.GetFriendListReq): OnebotApi.GetFriendListResp {
    val friendList = bot.friends.map { friend ->
        OnebotApi.GetFriendListResp.Friend.newBuilder()
                .setUserId(friend.id)
                .setNickname(friend.nick)
                .build()
    }
    return OnebotApi.GetFriendListResp.newBuilder().addAllFriendList(friendList).build()
}

suspend fun handleGetGroupInfo(bot: Bot, req: OnebotApi.GetGroupInfoReq): OnebotApi.GetGroupInfoResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    return OnebotApi.GetGroupInfoResp.newBuilder().setGroupId(group.id).setGroupName(group.name).setMemberCount(group.members.size + 1).build()
}

suspend fun handleGetGroupList(bot: Bot, req: OnebotApi.GetGroupListReq): OnebotApi.GetGroupListResp {
    val groupList = bot.groups.map { group ->
        OnebotApi.GetGroupListResp.Group.newBuilder()
                .setGroupId(group.id)
                .setGroupName(group.name)
                .setMemberCount(group.members.size + 1)
                .build()
    }
    return OnebotApi.GetGroupListResp.newBuilder().addAllGroupList(groupList).build()
}

suspend fun handleGetGroupMemberInfo(bot: Bot, req: OnebotApi.GetGroupMemberInfoReq): OnebotApi.GetGroupMemberInfoResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val member = group.getOrNull(req.userId) ?: return null
    return OnebotApi.GetGroupMemberInfoResp.newBuilder()
            .setGroupId(group.id)
            .setUserId(member.id)
            .setNickname(member.nick)
            .setCard(member.nameCard)
            .setRole(member.permission.name)
            .setTitle(member.specialTitle)
            .setCardChangeable(group.botPermission.level > MemberPermission.MEMBER.level)
            .build()
}

suspend fun handleGetGroupMemberList(bot: Bot, req: OnebotApi.GetGroupMemberListReq): OnebotApi.GetGroupMemberListResp? {
    val group = bot.getGroupOrNull(req.groupId) ?: return null
    val groupMemberList = group.members.map { member ->
        OnebotApi.GetGroupMemberListResp.GroupMember.newBuilder()
                .setGroupId(group.id)
                .setUserId(member.id)
                .setNickname(member.nick)
                .setCard(member.nameCard)
                .setRole(member.permission.name)
                .setTitle(member.specialTitle)
                .setCardChangeable(group.botPermission.level > MemberPermission.MEMBER.level)
                .build()
    }
    return OnebotApi.GetGroupMemberListResp.newBuilder().addAllGroupMemberList(groupMemberList).build()
}

