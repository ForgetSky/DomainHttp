package com.forgetsky.domainhttp.net.domain1

import android.content.Context
import com.forgetsky.domainhttp.net.APIException
import com.forgetsky.domainhttp.net.response.BaseSubscriber
import com.forgetsky.domainhttp.net.response.EMPTY_DATA
import com.forgetsky.domainhttp.net.response.FAILED


abstract class Domain1Subscriber<T>(context: Context?) : BaseSubscriber<Domain1Response<T>>(context) {

    override fun onNext(t: Domain1Response<T>) {
        if (SUCCESS == t.code) {
            if(t.context != null) {
                OnSucess(t.context)
            } else {
                onError(APIException(EMPTY_DATA, "baseResponse.context is null"))
            }
        } else {
            OnFailure(t.code?:"-1", t.message)
            onFailure(FAILED, t.code + t.message, t.message)
        }
    }

    abstract fun OnSucess(context: T)
    abstract fun OnFailure(code : String?, message: String?)

    final override fun onSuccess(t: Domain1Response<T>) {
    }
}