package com.forgetsky.domainhttp.net.domain1

import android.text.TextUtils
import com.forgetsky.domainhttp.domain.AesException
import com.forgetsky.domainhttp.domain.BaseEncryptPolicy
import com.forgetsky.domainhttp.domain.Domain
import com.forgetsky.domainhttp.net.HttpUtil
import com.forgetsky.domainhttp.net.HttpUtil.TYPE_JSON
import com.forgetsky.domainhttp.net.RETURN_STATUS_SESSION_TIMEOUT
import com.forgetsky.domainhttp.net.RETURN_STATUS_SHAKEHANDS_FAILED
import com.forgetsky.domainhttp.net.RetrofitManager
import okhttp3.Request

import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import okio.Buffer
import org.json.JSONObject
import retrofit2.Call
import java.nio.charset.StandardCharsets

/**
 * Created by ForgetSky on 20-10-30.
 */
class Test1DomainPolicy : BaseEncryptPolicy() {
    @Volatile
    private var mToken: String? = null

    private var mApiService: ApiService =
        RetrofitManager.getRetrofit().create(ApiService::class.java)

    override fun addHeaders(domain: Domain, request: Request, builder: Request.Builder) {
        //TODO 添加公共请求头
        builder.addHeader("test1", "xxxx")
            .addHeader("appVersion", "VERSION_NAME")
            .addHeader("Accept", "application/json")
    }

    override fun addRequestParams(domain: Domain, request: Request, builder: Request.Builder) {
        val requestBody = request.body
        lateinit var map: JSONObject
        //TODO 添加公共参数
        if (HttpUtil.isJsonType(requestBody?.contentType())) {
            val buffer = Buffer()
            requestBody?.writeTo(buffer)
            val json = buffer.readString(StandardCharsets.UTF_8)
            map = JSONObject(json)

        } else if (requestBody?.contentType() == null && 0 == requestBody?.contentLength()
                ?.toInt()
        ) {
            map = JSONObject()
        } else {
            return
        }

        map.put("qqqq", "q")
        map.put("wwww", "w")
        builder.post(HttpUtil.createRequestBodyByParams(map.toString()))
    }

    /**
     * 请求体加密
     */
    override fun encryptRequestBody(domain: Domain, request: Request, builder: Request.Builder) {
        val requestBody = request.body
        requestBody?.let {
            builder.post(EncryptUtil.encryptRequestBody(it))
        }
    }

    override fun processHandShakeRequest(
        domain: Domain,
        request: Request,
        builder: Request.Builder
    ) {
        //TODO 处理加密相关的请求头，请求体
        mToken?.let {
            builder.addHeader("ATOKEN", it)
        }
    }

    override fun decryptResponse(domain: Domain, response: Response): Response {
        val responseBody = response.body
        responseBody?.let {
            val responseBuilder = response.newBuilder()
            responseBuilder.body(EncryptUtil.decryptResponseBody(it))
            return responseBuilder.build()

        }
        return response

    }

    override fun needHandShakeFromResponse(response: Response): Boolean {
        val code = response.code
        return code == RETURN_STATUS_SHAKEHANDS_FAILED || code == RETURN_STATUS_SESSION_TIMEOUT
    }

    override fun processHandShakeResponse(domain: Domain, response: Response): Response {
        //TODO 处理握手响应
        if (!TextUtils.isEmpty(response.header("TOKEN"))) {
            mToken = response.header("TOKEN")
        }

        return response
    }

    /**
     * 同步握手流程
     */
    override fun handShakeSync(): Boolean {
        //TODO 握手接口1
        val call1: Call<ResponseBody> =
            mApiService.handshake1(HttpUtil.createRequestBodyByParams("{}".toByteArray()))
        val response1 = call1.execute()

        if (response1.isSuccessful && response1.body() != null) {
            //TODO 握手接口2
            val bytes = byteArrayOf()
            val call2: Call<ResponseBody> =
                mApiService.handshake2(HttpUtil.createRequestBodyByParams(bytes))
            val response2 = call2.execute()

            if (response2.isSuccessful && response2.body() != null) {
                return true
            } else {
                throw AesException(response2.code(), "setAesKey error: " + response2.message())
            }
        } else {
            throw AesException(response1.code(), "getPubKey error: " + response1.message())
        }
    }

    override fun isEncryptInvalid() : Boolean {
        return (!handShakeSuccess || EncryptUtil.isEncryptInvalid())
    }

    override fun decryptBodyForLog(byteArray: ByteArray?): String? {
        if (isEncryptInvalid()) return ""
        return EncryptUtil.decryptResultForLog(byteArray)
    }

}