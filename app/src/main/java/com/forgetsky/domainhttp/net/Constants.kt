package com.forgetsky.domainhttp.net

import com.forgetsky.domainhttp.utils.KEY_DOMAIN

/**
 * Created by ForgetSky on 21-1-28.
 */
/**
 * 请求成功
 */
var SUCCESS = 200

/**
 * 网络中断，请检查您的网络状态
 */
const val NETERROR = -1000

/**
 * 未知错误
 */
const val UNKONWERROR = -2000

/**
 * 安全平台拦截
 */
const val RETURN_STATUS_REQ_DATA_INTERECPT = 404

/**
 * 握手失败
 */
const val RETURN_STATUS_SHAKEHANDS_FAILED = 440

/**
 * 握手失效
 */
const val RETURN_STATUS_SESSION_TIMEOUT = 444

//默认策略
const val DOMAIN_DEFAULT = "default"

//运营环境策略
const val DOMAIN_TEST1 = "test1"

//金融环境策略
const val DOMAIN_TEST2 = "test2"

//添加在接口请求头中，标识当前接口的环境策略
const val HEADER_TEST1: String = "$KEY_DOMAIN: $DOMAIN_TEST1"
const val HEADER_TEST2: String = "$KEY_DOMAIN: $DOMAIN_TEST2"