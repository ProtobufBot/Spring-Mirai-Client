package net.lz1998.mirai.ext

import com.fasterxml.jackson.databind.util.LRUMap
import net.mamoe.mirai.Bot
import net.mamoe.mirai.event.events.MemberJoinRequestEvent
import net.mamoe.mirai.event.events.NewFriendRequestEvent
import net.mamoe.mirai.message.data.MessageSource

val messageLru = LRUMap<Int, MessageSource>(128, 2048)
val Bot.messageSourceLru
    get() = messageLru


val memberJoinRequestEventLru = LRUMap<Long, MemberJoinRequestEvent>(16, 128)
val Bot.groupRequestLru
    get() = memberJoinRequestEventLru

val newFriendRequestEventLru = LRUMap<Long, NewFriendRequestEvent>(16, 128)
val Bot.friendRequestLru
    get() = newFriendRequestEventLru
