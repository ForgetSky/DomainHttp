package com.forgetsky.domainhttp.dns

import android.net.Network
import android.net.Uri
import com.forgetsky.domainhttp.domain.DomainManager
import com.forgetsky.domainhttp.listener.NetworkListener
import com.forgetsky.domainhttp.listener.NetworkMonitor
import com.forgetsky.domainhttp.logger.L
import okhttp3.Dns
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArraySet
import java.util.concurrent.Executors

/**
 * Created by ForgetSky on 20-6-10.
 */
object DnsManager :NetworkListener{
    private var hostSet = CopyOnWriteArraySet<String>()
    private var cacheMap = ConcurrentHashMap<String, List<InetAddress>>()
    private var cacheLastTimeMap = ConcurrentHashMap<String, Long>()
    private val executor  = Executors.newSingleThreadExecutor()
    @JvmStatic
    fun setIpCache(hostname: String, list: List<InetAddress>) {
        cacheMap[hostname] = list
        cacheLastTimeMap[hostname] = System.currentTimeMillis()
    }

    @JvmStatic
    fun getIpCache(hostname: String): List<InetAddress>? {
        return cacheMap[hostname]
    }

    @JvmStatic
    fun clearOutdatedCache(hostname: String) : Boolean {
        val lastTime = cacheLastTimeMap[hostname]
        lastTime?.let {
            if (System.currentTimeMillis() - lastTime > 60000) {
                return !cacheMap.remove(hostname).isNullOrEmpty()
            }
        }
        return false
    }

    @JvmStatic
    fun clearCache(hostname: String) {
        cacheMap.remove(hostname)
    }

    @JvmStatic
    fun clearAll(hostname: String) {
        return cacheMap.clear()
    }

    @JvmStatic
    fun getIpFromHostName(hostname: String?): String? {
        try {
            val list = lookup(hostname)
            if (list.isNotEmpty()) {
                return list[0].hostAddress
            }
        } catch (e: UnknownHostException) {
            L.e(e)
        }
        return ""
    }

    fun lookup(hostname: String?): List<InetAddress> {
        if (hostname == null) throw UnknownHostException("hostname == null")
        val cacheList = getIpCache(hostname)
        if (cacheList != null && cacheList.size > 0) {
            L.d("Dns use cacheList : $cacheList")
            return cacheList
        }
        return try {
            hostSet.add(hostname)
            val list = Dns.SYSTEM.lookup(hostname)
            setIpCache(hostname, list)
            L.d("Dns.SYSTEM.lookup hostname $hostname; list $list")
            list
        } catch (e: Exception) {
            L.e("Dns.SYSTEM.lookup error" + e.message)
            throw e
        }
    }

    init {
        for (domain in DomainManager.mDomainMap.values) {
            Uri.parse(domain.baseUrl)?.host?.let { hostSet.add(it) }
        }
        NetworkMonitor.registerListener(this)
    }

    override fun onAvailable(network: Network) {
        for (hostname in hostSet) {
            executor.execute {
                try {
                    val list = Dns.SYSTEM.lookup(hostname)
                    setIpCache(hostname, list)
                    L.d("DnsManager get pre dns curr $list; all $cacheMap")
                } catch (e: Exception) {
                    L.e("DnsManager get pre dns result error $e")
                }
            }
        }
    }

}
