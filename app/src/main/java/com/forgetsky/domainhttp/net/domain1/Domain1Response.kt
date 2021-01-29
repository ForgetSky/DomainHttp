package com.forgetsky.domainhttp.net.domain1

const val SUCCESS = "00"

data class Domain1Response<T>(val code: String?, val message: String?, val context: T?) {
}