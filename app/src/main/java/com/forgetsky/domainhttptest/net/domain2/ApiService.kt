package com.forgetsky.domainhttptest.net.domain2

import com.forgetsky.domainhttp.utils.HEADER_HANDSHAKE
import com.forgetsky.domainhttptest.net.DOMAIN_TEST2
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface ApiService {
    /**
     * 如果有多套域名环境，非默认环境的所有接口需要添加
     * 请求头 (domain : [自定义name,例如test2])
     * 或requestTag [DomainTag] 用来区分不同的环境
     * 不添加则使用默认环境的策略；
     * 默认环境可以在初始化的时候通过设置domain.name 为[DOMAIN_DEFAULT]来指定，
     * 如果没有指定“default”环境，则将初始化时DomainList的第一个元素作为默认环境
     *
     * @param body
     * @return
     */
    @Headers(DOMAIN_TEST2, HEADER_HANDSHAKE)
    @POST("security/handshake1")
    fun handshake1(@Body body: RequestBody?): Call<ResponseBody>

    @Headers(DOMAIN_TEST2, HEADER_HANDSHAKE)
    @POST("security/handshake2")
    fun handshake2(@Body body: RequestBody?): Call<ResponseBody>

    @Headers(DOMAIN_TEST2)
    @POST("/xxxx/xxx")
    fun getData1(@Body body: RequestBody?): Call<ResponseBody>

}