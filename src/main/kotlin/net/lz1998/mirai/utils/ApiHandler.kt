package net.lz1998.mirai.utils

import net.lz1998.mirai.alias.*
import net.lz1998.mirai.ext.botInvitedGroupRequestLru
import net.lz1998.mirai.ext.friendRequestLru
import net.lz1998.mirai.ext.groupRequestLru
import net.lz1998.mirai.ext.messageSourceLru
import net.mamoe.mirai.Bot
import net.mamoe.mirai.contact.Friend
import net.mamoe.mirai.contact.Member
import net.mamoe.mirai.contact.MemberPermission
import net.mamoe.mirai.message.data.MessageSource.Key.recall
import net.mamoe.mirai.message.data.asMessageChain


suspend fun handleSendPrivateMsg(bot: Bot, req: BSendPrivateMsgReq): BSendPrivateMsgResp? {
    val contact = bot.getFriend(req.userId) ?: return null
    val messageChain = protoMessageToMiraiMessage(req.messageList, bot, contact, req.autoEscape).asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    val messageId = if (messageSource.ids.isNotEmpty()) messageSource.ids[0] else 0
    bot.messageSourceLru.put(messageId, messageSource)
    return BSendPrivateMsgResp.newBuilder().setMessageId(messageId).build()
}

suspend fun handleSendGroupMsg(bot: Bot, req: BSendGroupMsgReq): BSendGroupMsgResp? {
    val contact = bot.getGroup(req.groupId) ?: return null
    val messageChain = protoMessageToMiraiMessage(req.messageList, bot, contact, req.autoEscape).asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    val messageId = if (messageSource.ids.isNotEmpty()) messageSource.ids[0] else 0
    bot.messageSourceLru.put(messageId, messageSource)
    return BSendGroupMsgResp.newBuilder().setMessageId(messageId).build()
}

suspend fun handleSendMsgReq(bot: Bot, req: BSendMsgReq): BSendMsgResp? {
    val contact = when (req.messageType) {
        "group" -> {
            bot.getGroup(req.groupId)
        }
        "private" -> {
            bot.getFriend(req.userId)
        }
        else -> {
            bot.getGroup(req.groupId) ?: bot.getFriend(req.userId)
        }
    } ?: return null
    val messageChain = protoMessageToMiraiMessage(req.messageList, bot, contact, req.autoEscape).asMessageChain()
    val messageSource = contact.sendMessage(messageChain).source
    val messageId = if (messageSource.ids.isNotEmpty()) messageSource.ids[0] else 0
    bot.messageSourceLru.put(messageId, messageSource)
    return BSendMsgResp.newBuilder().setMessageId(messageId).build()
}

suspend fun handleDeleteMsg(bot: Bot, req: BDeleteMsgReq): BDeleteMsgResp? {
    val messageSource = bot.messageSourceLru[req.messageId] ?: return null
    messageSource.recall()
    return BDeleteMsgResp.newBuilder().build()
}

suspend fun handleGetMsg(bot: Bot, req: BGetMsgReq): BGetMsgResp? {
    val messageSource = bot.messageSourceLru[req.messageId] ?: return null
    var messageType = "unknown"

    val sender: BGetMsgSender? = when (messageSource.sender) {
        is Bot -> {
            messageType = "self"
            BGetMsgSender.newBuilder()
                    .setUserId(bot.id)
                    .build()
        }
        is Friend -> {
            messageType = "private"
            val friend: Friend = messageSource.sender as Friend
            BGetMsgSender.newBuilder()
                    .setUserId(friend.id)
                    .setNickname(friend.nick)
                    .build()
        }
        is Member -> {
            messageType = "group"
            val member: Member = messageSource.sender as Member
            BGetMsgSender.newBuilder()
                    .setUserId(member.id)
                    .setNickname(member.nick)
                    .setCard(member.nameCard)
                    .setRole(member.permission.name)
                    .setTitle(member.specialTitle)
                    .build()
        }
        else -> null
    }

    val messageId = if (messageSource.ids.isNotEmpty()) messageSource.ids[0] else 0


    return BGetMsgResp.newBuilder()
            .setTime(messageSource.time)
            .setMessageType(messageType)
            .setMessageId(messageId)
            .setRealId(0) // 不知道是什么？
            .setSender(sender)
            .addAllMessage(messageSource.originalMessage.toOnebotMessage())
            .setRawMessage(messageSource.originalMessage.toRawMessage())
            .build()
}

suspend fun handleSetGroupKick(bot: Bot, req: BSetGroupKickReq): BSetGroupKickResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    val member = group[req.userId] ?: return null
    member.kick("")
    return BSetGroupKickResp.newBuilder().build()
}

suspend fun handleSetGroupBan(bot: Bot, req: BSetGroupBanReq): BSetGroupBanResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    val member = group.get(req.userId) ?: return null
    if (req.duration == 0) {
        member.unmute()
    } else {
        member.mute(req.duration)
    }
    return BSetGroupBanResp.newBuilder().build()
}

suspend fun handleSetGroupWholeBan(bot: Bot, req: BSetGroupWholeBanReq): BSetGroupWholeBanResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    group.settings.isMuteAll = req.enable
    return BSetGroupWholeBanResp.newBuilder().build()
}

suspend fun handleSetGroupCard(bot: Bot, req: BSetGroupCardReq): BSetGroupCardResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    val member = group.get(req.userId) ?: return null
    member.nameCard = req.card
    return BSetGroupCardResp.newBuilder().build()
}

suspend fun handleSetGroupName(bot: Bot, req: BSetGroupNameReq): BSetGroupNameResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    group.name = req.groupName
    return BSetGroupNameResp.newBuilder().build()
}

suspend fun handleSetGroupLeave(bot: Bot, req: BSetGroupLeaveReq): BSetGroupLeaveResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    group.quit()
    return BSetGroupLeaveResp.newBuilder().build()
}

suspend fun handleSetGroupSpecialTitle(bot: Bot, req: BSetGroupSpecialTitleReq): BSetGroupSpecialTitleResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    val member = group.get(req.userId) ?: return null
    member.specialTitle = req.specialTitle

    return BSetGroupSpecialTitleResp.newBuilder().build()
}

suspend fun handleSetFriendAddRequest(bot: Bot, req: BSetFriendAddRequestReq): BSetFriendAddRequestResp? {
    val approve = req.approve
    val flag = req.flag
    val request = bot.friendRequestLru[flag.toLongOrNull()] ?: return null
    if (approve) request.accept() else request.reject()
    return BSetFriendAddRequestResp.newBuilder().build()
}

suspend fun handleSetGroupAddRequest(bot: Bot, req: BSetGroupAddRequestReq): BSetGroupAddRequestResp? {
    val approve = req.approve
    val flag = req.flag
    bot.groupRequestLru[flag.toLongOrNull()]?.let {
        if (approve) it.accept() else it.reject()
    }
    bot.botInvitedGroupRequestLru[flag.toLongOrNull()]?.let {
        if (approve) it.accept() else it.ignore()
    }
    return BSetGroupAddRequestResp.newBuilder().build()
}

suspend fun handleGetLoginInfo(bot: Bot, req: BGetLoginInfoReq): BGetLoginInfoResp {
    return BGetLoginInfoResp.newBuilder().setUserId(bot.id).setNickname(bot.nick).build()
}

suspend fun handleGetFriendList(bot: Bot, req: BGetFriendListReq): BGetFriendListResp {
    val friendList = bot.friends.map { friend ->
        BGetFriendListFriend.newBuilder()
                .setUserId(friend.id)
                .setNickname(friend.nick)
                .build()
    }
    return BGetFriendListResp.newBuilder().addAllFriend(friendList).build()
}

suspend fun handleGetGroupInfo(bot: Bot, req: BGetGroupInfoReq): BGetGroupInfoResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    return BGetGroupInfoResp.newBuilder().setGroupId(group.id).setGroupName(group.name).setMemberCount(group.members.size + 1).build()
}

suspend fun handleGetGroupList(bot: Bot, req: BGetGroupListReq): BGetGroupListResp {
    val groupList = bot.groups.map { group ->
        BGetGroupListGroup.newBuilder()
                .setGroupId(group.id)
                .setGroupName(group.name)
                .setMemberCount(group.members.size + 1)
                .build()
    }
    return BGetGroupListResp.newBuilder().addAllGroup(groupList).build()
}

suspend fun handleGetGroupMemberInfo(bot: Bot, req: BGetGroupMemberInfoReq): BGetGroupMemberInfoResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    val member = group.get(req.userId) ?: return null
    return BGetGroupMemberInfoResp.newBuilder()
            .setGroupId(group.id)
            .setUserId(member.id)
            .setNickname(member.nick)
            .setCard(member.nameCard)
            .setRole(member.permission.name)
            .setTitle(member.specialTitle)
            .setCardChangeable(group.botPermission.level > MemberPermission.MEMBER.level)
            .build()
}

suspend fun handleGetGroupMemberList(bot: Bot, req: BGetGroupMemberListReq): BGetGroupMemberListResp? {
    val group = bot.getGroup(req.groupId) ?: return null
    val groupMemberList = group.members.map { member ->
        BGetGroupMemberListGroupMember.newBuilder()
                .setGroupId(group.id)
                .setUserId(member.id)
                .setNickname(member.nick)
                .setCard(member.nameCard)
                .setRole(member.permission.name)
                .setTitle(member.specialTitle)
                .setCardChangeable(group.botPermission.level > MemberPermission.MEMBER.level)
                .build()
    }
    return BGetGroupMemberListResp.newBuilder().addAllGroupMember(groupMemberList).build()
}

