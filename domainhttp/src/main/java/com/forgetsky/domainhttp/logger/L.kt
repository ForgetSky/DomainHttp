package com.forgetsky.domainhttp.logger

import android.util.Log
import okhttp3.internal.platform.Platform

/**
 * Created by ForgetSky on 21-1-28.
 */
object L {
    private var logger: Logger? = null
    fun setLogger(logger: Logger?) {
        this.logger = logger
    }

    fun d(msg: String?) {
        log(Log.DEBUG, msg)
    }

    fun e(msg: String?, throwable: Throwable? = null) {
        log(Log.ERROR, msg, throwable)
    }

    fun e(throwable: Throwable?) {
        log(Log.ERROR, "", throwable)
    }

    fun log(level: Int, msg: String?, throwable: Throwable? = null) {
        logger?.log(level, msg?:"", throwable)
    }

    interface Logger {

        fun log(msg: String?){
            log(Log.INFO, msg?:"")
        }

        fun log(level: Int, msg: String, vararg throwable: Throwable?)

        companion object {
            /** A [Logger] defaults output appropriate for the current platform. */
            @JvmField
            val DEFAULT: Logger = object : Logger {
                override fun log(level: Int, msg: String, vararg throwable: Throwable?) {
                    Platform.get().log(msg)
                }
            }
        }
    }
}