package com.forgetsky.domainhttptest.net.response

import android.accounts.NetworkErrorException
import android.content.Context
import android.text.TextUtils
import android.util.Log
import androidx.annotation.CallSuper
import com.blankj.utilcode.util.GsonUtils
import com.forgetsky.domainhttp.domain.AesException
import com.forgetsky.domainhttptest.net.APIException
import com.forgetsky.domainhttptest.net.NETERROR
import com.forgetsky.domainhttptest.net.UNKONWERROR
import io.reactivex.subscribers.DisposableSubscriber
import retrofit2.HttpException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * 返回数据为空
 */
const val EMPTY_DATA = -3000
const val FAILED = -4000

/**
 * RESTful风格API的RxJava订阅器, 可继承扩展对其它结构的response进行处理
 */
abstract class BaseSubscriber<T>(private val context: Context?) : DisposableSubscriber<T>() {

    abstract fun onSuccess(t: T)

    @CallSuper
    open fun onFailure(code: Int, message: String?) {
    }

    /**
     * 失败回调，保存错误信息
     *
     * @param code     错误码
     * @param message  错误信息
     * @param toastMsg 错误提示信息
     */
    internal fun onFailure(code: Int, message: String?, toastMsg: String?) {
        onFailure(code, toastMsg ?: "")
        Log.e("onFailure", "onFailure code :$code ,message: $message toastMsg : $toastMsg")
    }

    override fun onError(e: Throwable) {
        if (e is UnknownHostException) {
            onFailure(NETERROR, e.message, "net_not_connected")
        } else if (e is NetworkErrorException || e is SocketTimeoutException
                || e is ConnectException) {
            onFailure(NETERROR, e.message, "net_error")
        } else if (e is APIException) {
            onFailure(e.code, e.message, "api_error")
        } else if (e is AesException) {
            onFailure(e.code, e.message, "aes_error")
        } else if (e is HttpException) {
            val code = e.code()
            val message = e.message
            var errorBodyMsg :String? = "unknown_err"
            var errMsg = ""
            val responseBody = e.response()!!.errorBody()
            var msg = message
            if (responseBody != null) {
                try {
                    errMsg = responseBody.string()
                    if (!TextUtils.isEmpty(errMsg)) {
                        errMsg = errMsg.replace("[\\[][^\\[\\]]+[\\]]".toRegex(), "")
                        val errorMsg = GsonUtils.fromJson(errMsg, BaseErrorBean::class.java)
                        if (errorMsg != null) errorBodyMsg = errorMsg.message
                        msg = "$message, errorBodyMsg ：$errorBodyMsg"
                    }
                } catch (e1: Exception) {
                    Log.e("", errMsg)
                }
                onFailure(code, msg, (if (TextUtils.isEmpty(errorBodyMsg)) "unknown_err" else errorBodyMsg))
            }
        } else {
            onFailure(UNKONWERROR, e.message, "unknown_err")
            //TODO  未知异常，保存StackTrace
            Log.e("UNKONWERROR", e.message)
        }
    }

    override fun onStart() {
        request(Long.MAX_VALUE)
    }

    override fun onComplete() {}
    override fun onNext(t: T) {
        t?.let { onSuccess(t) } ?: onFailure(EMPTY_DATA, "data is null", "data is null")
    }

}