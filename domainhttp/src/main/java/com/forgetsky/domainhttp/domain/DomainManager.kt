package com.forgetsky.domainhttp.domain

import com.forgetsky.domainhttp.requesttag.DomainTag
import com.forgetsky.domainhttp.utils.DOMAIN_DEFAULT
import okhttp3.Request

/**
 * Created by ForgetSky on 21-1-27.
 */
object DomainManager {
    var mDomainMap: MutableMap<String, Domain> = mutableMapOf()
    private val mPolicyMap = mutableMapOf<Class<out IDomainPolicy>, IDomainPolicy?>()

    @JvmStatic
    fun initDomains(vararg domains: Domain) {
        initDomains(domains.asList())
    }

    @JvmStatic
    fun initDomains(domains: List<Domain>) {
        if (domains.isNullOrEmpty()) {
            throw IllegalArgumentException("Domain must be initialized.")
        }
        mDomainMap.clear()

        domains.forEach {
            if (!it.name.isNullOrEmpty()) {
                mDomainMap[it.name] = it
            }
        }
        //确保有一个名为"default"的默认Domain
        if (mDomainMap[DOMAIN_DEFAULT] == null) {
            mDomainMap[DOMAIN_DEFAULT] = domains[0]
        }
    }


    @JvmStatic
    fun getDomainPolicy(request: Request): IDomainPolicy? {
        val domainTag = request.tag(DomainTag::class.java)
        val domainKey = domainTag?.domain
        val domain = mDomainMap[domainKey]
        domain?.let {
            return getDomainPolicy(
                domain
            )
        }
        return null
    }

    @JvmStatic
    fun getDomainPolicy(domain: Domain): IDomainPolicy? {
        val clazz = domain.domainPolicy ?: throw IllegalArgumentException(domain.name+"'s domainPolicy is null")
        var domainPolicy: IDomainPolicy? = mPolicyMap[clazz]
        if (domainPolicy == null) {
            domainPolicy = clazz.newInstance()
            mPolicyMap[clazz] = domainPolicy
        }
        return domainPolicy
    }

    fun getDefaultDomain(): Domain {
        return mDomainMap[DOMAIN_DEFAULT]
            ?: throw IllegalArgumentException("Default domain not found.")
    }

    fun getDefaultBaseUrl(): String {
        return getDefaultDomain().baseUrl?: throw IllegalArgumentException("Default domain baseUrl is null.")
    }
}