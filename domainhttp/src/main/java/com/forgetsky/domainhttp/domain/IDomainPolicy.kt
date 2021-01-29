package com.forgetsky.domainhttp.domain

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * Created by ForgetSky on 20-10-29.
 */
interface IDomainPolicy {
    @Throws(Exception::class)
    fun process(domain: Domain, request: Request, chain: Interceptor.Chain): Response
    @Throws(Exception::class)
    fun processRequest(domain: Domain, request: Request): Request
    @Throws(Exception::class)
    fun processResponse(domain: Domain, response: Response): Response
    fun decryptBodyForLog(byteArray: ByteArray?) : String?
}