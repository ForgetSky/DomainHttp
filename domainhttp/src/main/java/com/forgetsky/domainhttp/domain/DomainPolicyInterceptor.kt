package com.forgetsky.domainhttp.domain

import com.forgetsky.domainhttp.requesttag.DomainTag
import com.forgetsky.domainhttp.requesttag.EncryptTag
import com.forgetsky.domainhttp.requesttag.HandShakeTag
import com.forgetsky.domainhttp.utils.DOMAIN_DEFAULT
import com.forgetsky.domainhttp.utils.KEY_DOMAIN
import com.forgetsky.domainhttp.utils.KEY_ENCRYPT_HEADER
import com.forgetsky.domainhttp.utils.KEY_HANDSHAKE_HEADER
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException

/**
 * Created by ForgetSky on 20-10-29.
 */
class DomainPolicyInterceptor : Interceptor {

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val requestBuilder = request.newBuilder()

        //根据tag或者header的配置，选择对应的domain
        val domainTag = request.tag(DomainTag::class.java)
        var domainKey = DOMAIN_DEFAULT
        if (domainTag == null) {
            val domainKeyFromHeader = request.header(KEY_DOMAIN)
            domainKeyFromHeader?.let {
                domainKey = domainKeyFromHeader
                requestBuilder.removeHeader(KEY_DOMAIN)
            }
            //后面的流程中统一使用tag来判断
            requestBuilder.tag(DomainTag::class.java, DomainTag(domainKey))
        } else {
            domainKey = domainTag.domain
        }
        val domain = DomainManager.mDomainMap[domainKey]
                ?: throw IllegalArgumentException("No domain for $domainKey")

        //接口是否加密，可以通过tag或者header配置
        var isEncrypt = domain.isEncrypt
        val encryptTag = request.tag(EncryptTag::class.java)
        if (encryptTag == null) {
            val isEncryptHeader = request.header(KEY_ENCRYPT_HEADER)
            isEncryptHeader?.let {
                isEncrypt = isEncryptHeader == "true"
                requestBuilder.removeHeader(KEY_ENCRYPT_HEADER)
            }

            //后面的流程中统一使用tag来判断
            requestBuilder.tag(EncryptTag::class.java, EncryptTag(isEncrypt))
        }

        //处理接口是否是握手接口，可以通过tag或者header配置
        val handShakeTag = request.tag(HandShakeTag::class.java)
        if (handShakeTag == null) {
            val handShakeHeader = request.header(KEY_HANDSHAKE_HEADER)
            handShakeHeader?.let {
                //后面的流程中统一使用tag来判断
                requestBuilder.tag(HandShakeTag::class.java, HandShakeTag(handShakeHeader == "true"))
                requestBuilder.removeHeader(KEY_HANDSHAKE_HEADER)
            }
        }

        //替换BaseUrl
        val domainUrl = domain.baseUrl
        if (domainUrl.isNullOrEmpty()) throw IllegalArgumentException("You've configured an invalid url : EMPTY_OR_NULL_URL")
        val domainHttpUrl = domainUrl.toHttpUrlOrNull()
                ?: throw IllegalArgumentException("You've configured an invalid url : $domainUrl")
        val newFullUrl: HttpUrl = request.url
                .newBuilder()
                .scheme(domainHttpUrl.scheme) //更换网络协议
                .host(domainHttpUrl.host) //更换主机名
                .port(domainHttpUrl.port) //更换端口
                .build()

        //创建新Request
        val newRequest = requestBuilder.url(newFullUrl).build()
        val policy: IDomainPolicy = DomainManager.getDomainPolicy(domain)
                ?: throw RuntimeException("getDomainPolicy return null")
        //使用域名对应的策略去处理请求
        return policy.process(domain, newRequest, chain)
    }

}