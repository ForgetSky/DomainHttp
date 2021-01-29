package com.forgetsky.domainhttp.utils

import android.annotation.SuppressLint
import android.app.Application
import com.forgetsky.domainhttp.domain.Domain
import com.forgetsky.domainhttp.domain.DomainManager
import com.forgetsky.domainhttp.requesttag.DomainTag
import com.forgetsky.domainhttp.requesttag.EncryptTag
import com.forgetsky.domainhttp.requesttag.HandShakeTag
import okhttp3.Request
import okio.Buffer
import java.io.EOFException
import java.lang.reflect.InvocationTargetException

//环境策略请求头
const val KEY_DOMAIN = "domain"

//默认策略
const val DOMAIN_DEFAULT = "default"

//添加在接口请求头中，标识当前接口是否加密，只对当前接口生效
const val KEY_ENCRYPT_HEADER = "isEncrypt"
const val HEADER_ENCRYPT = "$KEY_ENCRYPT_HEADER: true"
const val HEADER_NOT_ENCRYPT = "$KEY_ENCRYPT_HEADER: false"

//添加在接口请求头中，标识当前接口是否是握手接口
const val KEY_HANDSHAKE_HEADER = "handshake"
const val HEADER_HANDSHAKE = "$KEY_HANDSHAKE_HEADER: true"

fun getApp(): Application {
    try {
        @SuppressLint("PrivateApi") val activityThread =
            Class.forName("android.app.ActivityThread")
        val thread = activityThread.getMethod("currentActivityThread").invoke(null)
        val app = activityThread.getMethod("getApplication").invoke(thread)
            ?: throw NullPointerException("u should init first")
        return app as Application
    } catch (e: NoSuchMethodException) {
        e.printStackTrace()
    } catch (e: IllegalAccessException) {
        e.printStackTrace()
    } catch (e: InvocationTargetException) {
        e.printStackTrace()
    } catch (e: ClassNotFoundException) {
        e.printStackTrace()
    }
    throw NullPointerException("u should init first")
}

internal fun Buffer.isProbablyUtf8(): Boolean {
    try {
        val prefix = Buffer()
        val byteCount = size.coerceAtMost(64)
        copyTo(prefix, 0, byteCount)
        for (i in 0 until 16) {
            if (prefix.exhausted()) {
                break
            }
            val codePoint = prefix.readUtf8CodePoint()
            if (Character.isISOControl(codePoint) && !Character.isWhitespace(codePoint)) {
                return false
            }
        }
        return true
    } catch (_: EOFException) {
        return false // Truncated UTF-8 sequence.
    }
}

fun Request.isPost() : Boolean {
    return "POST" == this.method
}

fun isEncryptInterface(request: Request) : Boolean {
    val domainTag = request.tag(DomainTag::class.java)
    val domainKey = domainTag?.domain
    return isEncryptInterface(
        request,
        DomainManager.mDomainMap[domainKey]
    )
}

fun isEncryptInterface(request: Request, domain: Domain?) : Boolean {
    val tag = request.tag(EncryptTag::class.java)
    if (tag != null) return tag.isEncrypt
    return domain?.isEncrypt ?: false
}

fun isHandShakeInterface(request: Request) : Boolean {
    val tag = request.tag(HandShakeTag::class.java)
    return tag?.isHandShake ?: false
}