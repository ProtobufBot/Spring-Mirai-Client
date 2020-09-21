package net.lz1998.mirai.ext

import com.fasterxml.jackson.databind.util.LRUMap
import net.mamoe.mirai.Bot
import net.mamoe.mirai.message.data.MessageSource

val lru = LRUMap<Int, MessageSource>(128, 1024)
val Bot.messageSourceLru
    get() = lru
