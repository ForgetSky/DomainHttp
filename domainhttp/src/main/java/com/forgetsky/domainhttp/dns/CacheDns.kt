package com.forgetsky.domainhttp.dns

import okhttp3.Dns
import java.net.InetAddress


/**
 * Created by ForgetSky on 20-6-10.
 */

class CacheDns : Dns {

    override fun lookup(hostname: String): List<InetAddress> {
        return DnsManager.lookup(hostname)
    }

}