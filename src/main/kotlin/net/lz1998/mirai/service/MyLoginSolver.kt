package net.lz1998.mirai.service

import com.google.protobuf.ByteString
import dto.HttpDto
import kotlinx.coroutines.CompletableDeferred
import net.mamoe.mirai.Bot
import net.mamoe.mirai.utils.LoginSolver
import org.springframework.stereotype.Service

@Service
object MyLoginSolver : LoginSolver() {
    // TODO 通过轮询查询 loginMap
    val loginMap = mutableMapOf<Long, LoginData>()

    // 图片验证码登陆
    override suspend fun onSolvePicCaptcha(bot: Bot, data: ByteArray): String? {
        val def = CompletableDeferred<String>()
        val loginData = LoginData(bot.id, LoginDataType.PIC_CAPTCHA, def, data, null)
        loginMap[bot.id] = loginData
        return def.await().trim()
    }

    // 滑动验证
    override suspend fun onSolveSliderCaptcha(bot: Bot, url: String): String? {
        val def = CompletableDeferred<String>()
        val loginData = LoginData(bot.id, LoginDataType.SLIDER_CAPTCHA, def, null, url)
        loginMap[bot.id] = loginData
        return def.await().trim()
    }

    // 设备锁扫码验证
    override suspend fun onSolveUnsafeDeviceLoginVerify(bot: Bot, url: String): String? {
        val def = CompletableDeferred<String>()
        val loginData = LoginData(bot.id, LoginDataType.UNSAFE_DEVICE_LOGIN_VERIFY, def, null, url)
        loginMap[bot.id] = loginData
        println(loginMap.toString())
        return def.await().trim()
    }

    fun solveLogin(botId: Long, result: String) {
        val loginData = loginMap[botId] ?: return
        loginData.def.complete(result)
        loginMap.remove(botId)
    }

    fun getCaptchaList(): Collection<HttpDto.Captcha> {
        return loginMap.values.map { loginData ->
            val captcha = HttpDto.Captcha.newBuilder()
            captcha.botId = loginData.botId
            when (loginData.type) {
                LoginDataType.PIC_CAPTCHA -> {
                    captcha.captchaType = HttpDto.Captcha.CaptchaType.PIC_CAPTCHA
                    captcha.image = ByteString.copyFrom(loginData.data)
                }
                LoginDataType.SLIDER_CAPTCHA -> {
                    captcha.captchaType = HttpDto.Captcha.CaptchaType.SLIDER_CAPTCHA
                    captcha.url = loginData.url
                }
                LoginDataType.UNSAFE_DEVICE_LOGIN_VERIFY -> {
                    captcha.captchaType = HttpDto.Captcha.CaptchaType.UNSAFE_DEVICE_LOGIN_VERIFY
                    captcha.url = loginData.url
                }
            }
            captcha.build()
        }
    }
}

data class LoginData(
        val botId: Long,
        val type: LoginDataType,
        val def: CompletableDeferred<String>,
        var data: ByteArray?,
        val url: String?
)

enum class LoginDataType(val type: String) {
    PIC_CAPTCHA("pic_captcha"),
    SLIDER_CAPTCHA("slider_captcha"),
    UNSAFE_DEVICE_LOGIN_VERIFY("unsafe_device_login_verify"),
}

//val myLoginSolver = MyLoginSolver()