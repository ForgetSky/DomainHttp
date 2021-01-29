package com.forgetsky.domainhttp.dns

import com.forgetsky.domainhttp.listener.NetworkMonitor
import com.forgetsky.domainhttp.logger.L
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException

/**
 * Created by ForgetSky on 20-12-21.
 */
class DnsRetryInterceptor : Interceptor {
    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val url = request.url

        while (true) {
            try {
                return chain.proceed(request)
            } catch (e: Exception) {
                L.e("DnsCacheInterceptor" + e.message)
                retry(url, e)
                continue
            }
        }
    }

    private fun retry(url: HttpUrl, e: Exception) {
        val needRetry = DnsManager.clearOutdatedCache(url.host)
        if (needRetry && (e is ConnectException || e is SocketTimeoutException) && NetworkMonitor.isNetAvailable) {
            L.e("DnsCacheInterceptor 清除DNS缓存并重试$url")
        } else {
            throw e
        }
    }
}