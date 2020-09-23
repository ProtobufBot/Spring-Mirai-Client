package net.lz1998.mirai.service

import kotlinx.coroutines.CompletableDeferred
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.LoginSolver
import org.springframework.stereotype.Service

@Service
class MyLoginSolver : LoginSolver() {
    // TODO 通过轮询查询 loginMap
    val loginMap = mutableMapOf<Long, LoginData>()

    // 图片验证码登陆
    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        val def = CompletableDeferred<String>()
        loginMap[bot.id] = LoginData(LoginDataType.PIC_CAPTCHA, def, data, null)
        return def.await().trim()
    }

    // 滑动验证
    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
        val def = CompletableDeferred<String>()
        loginMap[bot.id] = LoginData(LoginDataType.SLIDER_CAPTCHA, def, null, url)
        return def.await().trim()
    }

    // 设备锁扫码验证
    override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
        val def = CompletableDeferred<String>()
        loginMap[bot.id] = LoginData(LoginDataType.UNSAFE_DEVICE_LOGIN_VERIFY, def, null, url)
        return def.await().trim()
    }

    fun solveLogin(botId: Long, result: String) {
        val loginData = loginMap[botId] ?: return
        loginMap.remove(botId)
        loginData.def.complete(result)
    }

    fun getLoginData(botId: Long): LoginData? {
        return loginMap[botId]
    }
}

data class LoginData(
        val type: LoginDataType,
        val def: CompletableDeferred<String>,
        var data: ByteArray?,
        val url: String?
)

enum class LoginDataType(val type: String) {
    PIC_CAPTCHA("pic_captcha"),
    SLIDER_CAPTCHA("slider_captcha"),
    UNSAFE_DEVICE_LOGIN_VERIFY("unsafe_device_login)verify"),
}

val myLoginSolver = MyLoginSolver()