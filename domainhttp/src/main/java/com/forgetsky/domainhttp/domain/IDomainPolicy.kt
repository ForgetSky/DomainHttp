package com.forgetsky.domainhttp.domain

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * Created by ForgetSky on 20-10-29.
 */
interface IDomainPolicy {
    @Throws(IOException::class)
    fun process(domain: Domain, request: Request, chain: Interceptor.Chain): Response
    @Throws(IOException::class)
    fun processRequest(domain: Domain, request: Request): Request
    @Throws(IOException::class)
    fun processResponse(domain: Domain, response: Response): Response
    fun decryptBodyForLog(byteArray: ByteArray?) : String
}