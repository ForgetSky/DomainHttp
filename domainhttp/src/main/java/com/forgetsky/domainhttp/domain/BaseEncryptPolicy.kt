package com.forgetsky.domainhttp.domain

import com.forgetsky.domainhttp.logger.L
import com.forgetsky.domainhttp.utils.isEncryptInterface
import com.forgetsky.domainhttp.utils.isHandShakeInterface
import com.forgetsky.domainhttp.utils.isPost
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.internal.closeQuietly
import java.io.IOException
import java.util.*

/**
 * Created by ForgetSky on 20-11-10.
 */
abstract class BaseEncryptPolicy :
    IDomainPolicy {
    protected val maxTimes = 3
    protected var retryTimes = 0
    @Volatile
    var handShakeSuccess = false
    @Volatile
    var handShakeSuccessTime = 0L

    @Throws(IOException::class)
    override fun process(domain: Domain, request: Request, chain: Interceptor.Chain): Response {
        val requestTime = System.currentTimeMillis()
        return try {
            val newRequest = processRequest(domain, request)
            val response = chain.proceed(newRequest)
            processResponse(domain, response)
        } catch (e: AesException) {
            //如果是握手接口，则抛出异常给握手方法tryHandShake()去处理
            if (request.isHandShakeInterface()) throw e
            //请求时间早于握手成功时间，则不用重新握手，直接重新发起请求
            if (requestTime > handShakeSuccessTime) {
                handShakeSuccess = false
                //协商秘钥
                tryHandShake(requestTime, e)
            }
            //重新请求
            process(domain, request, chain)
        }
    }

    @Throws(IOException::class)
    override fun processRequest(domain: Domain, request: Request): Request {
        val builder = request.newBuilder()
        return builder.also {
            addHeaders(domain, request, it)
            //如果是加密环境，且是POST接口，则执行加密或握手相关的逻辑
            if (request.isEncryptInterface() && request.isPost()) {
                if (request.isHandShakeInterface()) {
                    processHandShakeRequest(domain, it.build(), it)
                } else {
                    //如果不是握手接口，则添加公共请求参数，对请求体加密
                    addRequestParams(domain, request, it)
                    encryptRequestBodyInternal(domain, it.build(), it)
                }
            }
        }.build()
    }

    @Throws(IOException::class)
    override fun processResponse(domain: Domain, response: Response): Response {
        //如果是加密环境，且是POST接口，则执行解密密或握手相关的逻辑
        if (response.request.isEncryptInterface() && response.request.isPost()) {
            val code = response.code
            if (response.isSuccessful) {
                //如果是握手接口
                return if (response.request.isHandShakeInterface()){
                    processHandShakeResponse(domain, response)
                } else {
                    //如果不是握手接口，则对响应体解密
                    checkEncryptInvalid("decryptResponse ${response.request.url}" )
                    decryptResponse(domain, response)
                }
                //加解密异常
            } else if (needHandShakeFromResponse(response)) {
                val message = response.message
                //关闭流
                response.closeQuietly()
                throw AesException(code, code.toString() + message)
            }
        }
        return response
    }

    /**
     * 添加公共请求头
     * [domain]
     * [request] 原始request
     * [builder] 新建的Request.Builder， 用于对request重新组装
     */
    abstract fun addHeaders(domain: Domain, request: Request, builder: Request.Builder)

    /**
     * 添加公共请求参数
     * [domain]
     * [request] 原始request
     * [builder] 新建的Request.Builder， 用于对request重新组装
     */
    abstract fun addRequestParams(domain: Domain, request: Request, builder: Request.Builder)

    @Throws(AesException::class)
    fun encryptRequestBodyInternal(domain: Domain, request: Request, builder: Request.Builder){
        checkEncryptInvalid("encryptRequestBody ${request.url}" )
        encryptRequestBody(domain, request, builder)
    }

    /**
     * 加密请求体
     * [domain]
     * [request] 原始request
     * [builder] 新建的Request.Builder， 用于对request重新组装
     */
    @Throws(AesException::class)
    abstract fun encryptRequestBody(domain: Domain, request: Request, builder: Request.Builder)

    /**
     * 处理握手接口的响应体，例如保存token等操作
     * [domain]
     * [request] 原始request
     * [builder] 新建的Request.Builder， 用于对request重新组装
     */
    abstract fun processHandShakeRequest(domain: Domain, request: Request, builder: Request.Builder)

    /**
     * 通过response来判断是否是加解密异常,由子类实现，例如密钥过期时返回特定code
     */
    abstract fun needHandShakeFromResponse(response: Response): Boolean

    /**
     * 响应体解密
     * [domain]
     * [response] 响应体
     */
    @Throws(AesException::class)
    abstract fun decryptResponse(domain: Domain, response: Response): Response

    /**
     * 处理握手接口的响应体，例如保存token等操作
     * [domain]
     * [response] 响应体
     */
    abstract fun processHandShakeResponse(domain: Domain, response: Response): Response

    /**
     * 同步握手流程，重试[maxTimes]次
     */
    @Throws(IOException::class)
    @Synchronized
    private fun tryHandShake(requestTime: Long, e: Exception) {
        checkHandShakeDisaster()
        if (handShakeSuccess) return
        if (requestTime <= handShakeSuccessTime) return

        checkRetryTimes(e)
        try {
            if (handShakeSync()) {
                handShakeSuccess = true
                handShakeSuccessTime = System.currentTimeMillis()
                retryTimes = 0
                sList.add(handShakeSuccessTime)
            } else {
                //握手失败，重新握手
                tryHandShake(requestTime, e)
            }
        } catch (e: Exception) {
            //握手失败，重新握手
            tryHandShake(requestTime, e)
        }
    }

    private fun checkRetryTimes(e: Exception) {
        if (retryTimes++ >= maxTimes) {
            retryTimes = 0
            sList.clear()
            L.e("握手重试超过最大次数" + e.message)
            throw e
        }
    }

    /**
     * 容灾策略，避免未知错误导致的频繁握手或握手进入循环，例如加解密服务出现错误
     * 在握手成功的情况下，90秒内最多允许握手三次
     */
    private val sList = LinkedList<Long>()
    private fun checkHandShakeDisaster() {
        if (sList.size >= 3) {
            val first = sList.first
            if (first > System.currentTimeMillis() - 90000) {
                throw IOException("握手频繁异常")
            } else {
                sList.removeFirst()
            }
        }
    }

    /**
     * 握手具体实现,返回[true]代表握手成功，返回[false]或抛出异常，代表握手失败
     */
    @Throws(Exception::class)
    abstract fun handShakeSync(): Boolean

    /**
     * 密钥是否有效，由子类实现
     */
    @Throws(Exception::class)
    abstract fun isEncryptInvalid(): Boolean

    /**
     * 密钥无效则抛出异常
     */
    @Throws(AesException::class)
    protected fun checkEncryptInvalid(msg: String) {
        if (isEncryptInvalid()) throw AesException(ENCRYPT_TOKEN_INVALID, msg)
    }
}