package com.forgetsky.domainhttp.net.domain1

import com.forgetsky.domainhttp.requesttag.EncryptTag
import com.forgetsky.domainhttp.requesttag.HandShakeTag
import com.forgetsky.domainhttp.utils.HEADER_HANDSHAKE
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    /**
     * 握手接口需要加上请求头 (handshake : true)
     * 或添加requestTag [HandShakeTag] 用来区别握手接口和普通接口
     *
     * @param body
     * @return
     */
    @Headers(HEADER_HANDSHAKE)
    @POST("security/handshake1")
    fun handshake1(@Body body: RequestBody?): Call<ResponseBody>

    @Headers(HEADER_HANDSHAKE)
    @POST("security/handshake2")
    fun handshake2(@Body body: RequestBody?): Call<ResponseBody>

    @POST("/xxxx/xxx")
    fun getData1(@Body body: RequestBody?): Call<ResponseBody>

    /**
     * 可以使用请求头（isEncrypt ： true or isEncrypt ： false）
     * 或添加requestTag [EncryptTag] 来单独标识该接口是否加密
     */
    @POST("/xxxx/xxx")
    fun getData2(@Body body: RequestBody?): Call<ResponseBody>
}