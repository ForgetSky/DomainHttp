package com.forgetsky.domainhttp.logger

import okhttp3.*
import java.io.IOException
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.TimeUnit

/**
 * Created by ForgetSky on 20-6-9.
 */

class HttpEventListener private constructor(
    private val logger: L.Logger,
    private val type: Type
) : EventListener() {
    enum class Type {
        NONE,
        ALL,
        ERROR
    }

    private var startNs: Long = 0
    private var errorBuffer: StringBuilder? = null
    private var id: Int = 0
    private var isSuccess: Boolean = false

    override fun callStart(call: Call) {
        startNs = System.nanoTime()
        id = startNs.and(255).toInt()
        if (type == Type.ERROR) errorBuffer = StringBuilder()
        logOrCache("callStart: ${call.request()}")
    }

    override fun dnsStart(call: Call, domainName: String) {
        logOrCache("dnsStart: $domainName")
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        logOrCache("dnsEnd: $inetAddressList")
    }

    override fun connectStart(call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy) {
        logOrCache("connectStart: $inetSocketAddress $proxy")
    }

    override fun secureConnectStart(call: Call) {
        logOrCache("secureConnectStart")
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        logOrCache("secureConnectEnd: $handshake")
    }

    override fun connectEnd(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?
    ) {
        logOrCache("connectEnd: $protocol")
    }

    override fun connectFailed(
            call: Call,
            inetSocketAddress: InetSocketAddress,
            proxy: Proxy,
            protocol: Protocol?,
            ioe: IOException
    ) {
        if (type == Type.ERROR) {
            logAllCache()
        }
        logOrCache("connectFailed: $protocol $ioe")
    }

    override fun connectionAcquired(call: Call, connection: Connection) {
        logOrCache("connectionAcquired: $connection")
    }

    override fun connectionReleased(call: Call, connection: Connection) {
        logOrCache("connectionReleased")
    }

    override fun requestHeadersStart(call: Call) {
        logOrCache("requestHeadersStart")
    }

    override fun requestHeadersEnd(call: Call, request: Request) {
        logOrCache("requestHeadersEnd")
    }

    override fun requestBodyStart(call: Call) {
        logOrCache("requestBodyStart")
    }

    override fun requestBodyEnd(call: Call, byteCount: Long) {
        logOrCache("requestBodyEnd: byteCount=$byteCount")
    }

    override fun responseHeadersStart(call: Call) {
        logOrCache("responseHeadersStart")
    }

    override fun responseHeadersEnd(call: Call, response: Response) {
        logOrCache("responseHeadersEnd: $response")
    }

    override fun responseBodyStart(call: Call) {
        logOrCache("responseBodyStart")
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        logOrCache("responseBodyEnd: byteCount=$byteCount")
    }

    override fun callEnd(call: Call) {
        logOrCache("callEnd")
        clearBuffer()
    }

    override fun callFailed(call: Call, ioe: IOException) {
        if (type == Type.ERROR) {
            logAllCache()
        }
        logOrCache("callFailed: $ioe")
    }

    private fun logOrCache(message: String) {
        if (isSuccess) return
        var timeMs: String = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startNs).toString()
        timeMs = "$id-[$timeMs ms] $message"
        errorBuffer?.append("$timeMs\n") ?: logger.log(timeMs)
    }

    private fun logAllCache() {
        logger.log(errorBuffer?.toString() ?: "")
        clearBuffer()
    }

    private fun clearBuffer() {
        if (errorBuffer != null) {
            errorBuffer?.clear()
            errorBuffer = null
        }
    }

    open class Factory @JvmOverloads constructor(
        private val logger: L.Logger = L.Logger.DEFAULT,
        private val type: Type = Type.ALL
    ) : EventListener.Factory {
        override fun create(call: Call): EventListener = if (type == Type.NONE) EventListener.NONE else HttpEventListener(logger, type)
    }
}