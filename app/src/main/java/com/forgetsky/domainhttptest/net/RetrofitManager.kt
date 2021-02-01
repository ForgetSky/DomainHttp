package com.forgetsky.domainhttptest.net

import android.util.Log
import com.forgetsky.domainhttp.dns.CacheDns
import com.forgetsky.domainhttp.dns.DnsRetryInterceptor
import com.forgetsky.domainhttp.domain.Domain
import com.forgetsky.domainhttp.domain.DomainManager
import com.forgetsky.domainhttp.domain.DomainPolicyInterceptor
import com.forgetsky.domainhttp.domain.IDomainPolicy
import com.forgetsky.domainhttp.logger.HttpEventListener
import com.forgetsky.domainhttp.logger.L
import com.forgetsky.domainhttp.logger.LoggingInterceptor
import com.forgetsky.domainhttp.utils.isEncryptInterface
import com.forgetsky.domainhttp.utils.isHandShakeInterface
import com.forgetsky.domainhttptest.BuildConfig
import com.forgetsky.domainhttptest.net.domain1.Test1DomainPolicy
import com.forgetsky.domainhttptest.net.domain2.Test2DomainPolicy

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Created by ForgetSky on 20-10-29.
 */
object RetrofitManager {

    @Volatile
    private var mOkHttpClient: OkHttpClient? = null

    @Volatile
    private var mRetrofit: Retrofit? = null

    @JvmStatic
    fun getOkHttpClient(): OkHttpClient {
        if (mOkHttpClient == null) {
            synchronized(RetrofitManager::class) {
                if (mOkHttpClient == null) {
                    mOkHttpClient = okHttpClient()
                }
            }
        }
        return mOkHttpClient!!
    }

    @JvmStatic
    fun getRetrofit(): Retrofit {
        if (mRetrofit == null) {
            synchronized(RetrofitManager::class) {
                if (mRetrofit == null) {
                    mRetrofit = retrofit()
                }
            }
        }
        return mRetrofit!!
    }

    @JvmStatic
    private fun initDomain() {
        DomainManager.initDomains(
            Domain(
                DOMAIN_DEFAULT,
                "https://aaa.test111",
                Test1DomainPolicy::class.java,
                true
            ),
//            Domain(DOMAIN_TEST1, "https://aaa.test111", Test1DomainPolicy::class.java, true),
            Domain(DOMAIN_TEST2, "https://aaa.test222", Test2DomainPolicy::class.java, false)
        )
    }

    private fun retrofit(): Retrofit {
        //先初始化Domain
        initDomain()
        return Retrofit.Builder()
            .baseUrl(DomainManager.getDefaultBaseUrl())
            .client(getOkHttpClient())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun okHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DomainPolicyInterceptor())
            .addInterceptor(DnsRetryInterceptor())
            .addInterceptor(loggingInterceptor())
            .eventListenerFactory(getEventListenerFactory())
            .dns(CacheDns())
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private fun loggingInterceptor(): Interceptor {
        val interceptor = LoggingInterceptor(object : L.Logger {
            override fun log(level: Int, msg: String, vararg throwable: Throwable?) {
                Log.w("Http", msg)
            }
        }, object : LoggingInterceptor.DecryptAdapter {
            override fun decrypt(chain: Interceptor.Chain, inputStream: InputStream): String {
                if (isEncryptInterface(chain.request()) &&
                        !isHandShakeInterface(chain.request())) {
                    return try {
                        val domainPolicy: IDomainPolicy? =
                            DomainManager.getDomainPolicy(chain.request())
                        val tem = HttpUtil.inputStream2Bytes(inputStream)
                        domainPolicy?.decryptBodyForLog(tem) ?: ""
                    } catch (e: Exception) {
                        L.e("loggingInterceptor 解密失败")
                        ""
                    }
                }
                return ""
            }
        })
        interceptor.level =
            if (BuildConfig.DEBUG) LoggingInterceptor.Level.BODY else LoggingInterceptor.Level.ERROR
        return interceptor
    }

    private fun getEventListenerFactory(): HttpEventListener.Factory {
        return HttpEventListener.Factory(
            object : L.Logger {
                override fun log(level: Int, msg: String, vararg throwable: Throwable?) {
                    Log.w("HttpEvent", msg)
                }

            },
            if (BuildConfig.DEBUG) HttpEventListener.Type.ERROR else HttpEventListener.Type.ERROR
        )
    }

}