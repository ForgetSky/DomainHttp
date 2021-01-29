package com.forgetsky.domainhttp.domain

import java.io.IOException

/**
 * 握手过程抛出异常,握手重试3次后抛出异常
 */
var SHAKE_HAND_ERROR = 700

/**
 * 加密 抛出异常
 */
@JvmField
var ENCRYPT_ERROR = 701

/**
 * 本地秘钥无效 抛出异常
 */
@JvmField
var ENCRYPT_TOKEN_INVALID = 702

/**
 * 解密 抛出异常
 */
@JvmField
var DECRYPT_ERROR = 703

/**
 * 加解密失败异常
 */
class AesException(val code: Int, override val message: String) : IOException()