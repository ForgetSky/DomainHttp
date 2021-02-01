package com.forgetsky.domainhttptest.net.domain1

import android.util.Log
import com.forgetsky.domainhttp.domain.AesException
import com.forgetsky.domainhttp.domain.DECRYPT_ERROR
import com.forgetsky.domainhttp.domain.ENCRYPT_ERROR
import com.forgetsky.domainhttptest.net.HttpUtil
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer

internal object EncryptUtil {
    private val mSessionId: String? = null

    /**
     * 加密 http requestBody参数
     * param @str
     */
    @Throws(AesException::class)
    fun encryptBody(str: String): ByteArray {
        return encryptBody(str.toByteArray())
    }

    @Throws(AesException::class)
    fun encryptBody(bytes: ByteArray?): ByteArray {
        return try {
            //TODO 加密实现
            ByteArray(0)
        } catch (e: Exception) {
            throw AesException(
                ENCRYPT_ERROR,
                "Domain1AesException: body encrypt error " + e.message
            )
        }
    }

    @Throws(AesException::class)
    fun encryptRequestBody(requestBody: RequestBody): RequestBody {
        val buffer = Buffer()
        requestBody.writeTo(buffer)
        val contentType = requestBody.contentType()
        val bytes: ByteArray = HttpUtil.inputStream2Bytes(buffer.inputStream())
        return encryptBody(bytes).toRequestBody(contentType)
    }

    @Throws(AesException::class)
    fun decryptResult(result: ByteArray?): String {
        return try {
            //TODO 解密实现
            String()
        } catch (e: Exception) {
            throw AesException(DECRYPT_ERROR, "decrypt error " + e.message)
        }
    }

    fun decryptResponseBody(responseBody: ResponseBody): ResponseBody {
        val tem = HttpUtil.inputStream2Bytes(responseBody.byteStream())
        val resultStr = decryptResult(tem)
        return resultStr.toResponseBody(responseBody.contentType())

    }

    /**
     * 解密数据用于打印log,解密出错也不抛出AesException,防止影响正常的业务流程
     * param @result
     */
    fun decryptResultForLog(result: ByteArray?): String {
        try {
            decryptResult(result)
            return String()
        } catch (e: Exception) {
            Log.e("decryptResultForLog", "e:" + e.message)
        }
        return ""
    }

    fun isEncryptInvalid(): Boolean {
        //TODO 秘钥无效的判断
        return mSessionId.isNullOrEmpty()
    }

}