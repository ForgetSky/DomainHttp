package com.forgetsky.domainhttp.listener

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkRequest
import com.forgetsky.domainhttp.utils.getApp

/**
 * Created by ForgetSky on 21-1-28.
 */
object NetworkMonitor {
    @Volatile
    var isNetAvailable = false
    private val listeners = mutableSetOf<NetworkListener>()
    init {
        val mNetworkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
//            L.d("NetworkMonitor Network onAvailable $network" + Thread.currentThread())
                isNetAvailable = true
                listeners.forEach { it.onAvailable(network) }
            }

            override fun onLost(network: Network) {
//            L.d("NetworkMonitor Network onLost $network")
                isNetAvailable = false
                listeners.forEach { it.onLost(network) }
            }
        }
        val connectivityManager = getApp().getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        connectivityManager?.registerNetworkCallback(NetworkRequest.Builder().build(), mNetworkCallback)
    }

    fun registerListener(listener: NetworkListener?) {
        listener?.let { listeners.add(listener) }
    }

    fun unregisterListener(listener: NetworkListener?) {
        listener?.let { listeners.remove(listener) }
    }

}