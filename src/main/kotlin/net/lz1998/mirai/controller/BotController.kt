package net.lz1998.mirai.controller

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.lz1998.mirai.service.BotService
import net.lz1998.mirai.service.LoginDataType
import net.lz1998.mirai.service.myLoginSolver
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class BotController {

    @Autowired
    lateinit var botService: BotService


    // 创建一个机器人并登陆
    @RequestMapping("/createBot")
    fun createBot(botId: Long, password: String): String {
        GlobalScope.launch { // TODO 是否可以优化？ suspend报错怎么解决？
            val bot = botService.botMap[botId]
            if (bot != null) { // 机器人已存在，直接登陆
                bot.bot.login()
                return@launch
            } else { // 机器人不存在，创建
                botService.createBot(botId, password)
            }
        }
        return "ok"
    }

    // 获取机器人状态
    @RequestMapping("/getStatus")
    fun getStatus(botId: Long): String {
        // 机器人不存在
        val bot = botService.botMap[botId] ?: return "NOT_CREATED"

        // 机器人在线
        if (bot.bot.isOnline) {
            return "ONLINE"
        }

        // 机器人需要登陆
        val loginData = myLoginSolver.getLoginData(botId)
        if (loginData != null) {
            return loginData.type.name // 登陆类型
        }

        // 其他状态
        return "UNKNOWN"
    }

    // 通过轮询获取登陆验证url
    @RequestMapping("/getLoginUrl")
    fun getLoginUrl(botId: Long): String? {
        val loginData = myLoginSolver.getLoginData(botId) ?: return null
        return if (loginData.type != LoginDataType.PIC_CAPTCHA) {
            loginData.url
        } else {
            null
        }
    }

    // 通过轮询获取登陆图片验证码
    @RequestMapping("/getLoginImage", produces = [MediaType.IMAGE_JPEG_VALUE])
    fun getLoginData(botId: Long): ByteArray? {
        val loginData = myLoginSolver.getLoginData(botId) ?: return null
        return if (loginData.type == LoginDataType.PIC_CAPTCHA) {
            loginData.data
        } else {
            null
        }
    }

    // 处理登陆
    @RequestMapping("/solveLogin")
    fun solveLogin(botId: Long, result: String): String {
        myLoginSolver.solveLogin(botId, result)
        return "ok"
    }
}