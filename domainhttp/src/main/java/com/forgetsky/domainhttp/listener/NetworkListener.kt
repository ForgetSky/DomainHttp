package com.forgetsky.domainhttp.listener

import android.net.Network

/**
 * Created by ForgetSky on 21-1-28.
 */
interface NetworkListener {
    fun onAvailable(network: Network){}
    fun onLost(network: Network){}
}