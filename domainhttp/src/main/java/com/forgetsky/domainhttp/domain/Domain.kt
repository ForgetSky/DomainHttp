package com.forgetsky.domainhttp.domain

/**
 * 环境配置
 * [name] 名字
 * [baseUrl] 域名
 * [domainPolicy] 对应的处理策略
 * [isEncrypt] 数据是否加密的全局配置
 */
data class Domain(val name: String?, val baseUrl: String?,
                  val domainPolicy: Class<out IDomainPolicy>?, var isEncrypt: Boolean = false) {
}