package net.lz1998.mirai.controller

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


    @RequestMapping("/createBot")
    fun createBot(botId: Long, password: String): String {
        println(botId)
        println(password)
        runBlocking { // TODO 是否可以优化？ suspend报错怎么解决？
            botService.createBot(botId, password)
        }
        return "ok"
    }

    // 通过轮询获取登陆验证url
    @RequestMapping("/getLoginUrl")
    fun getLoginUrl(botId: Long): String? {
        val loginData = myLoginSolver.getLoginData(botId) ?: return null
        return if (loginData.type == LoginDataType.PIC_CAPTCHA) {
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