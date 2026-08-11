package com.nuvio.tv.core.serverassist

import okhttp3.Dns
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.UnknownHostException

class TailnetOnlyDns(
    private val delegate: Dns = Dns.SYSTEM,
) : Dns {
    override fun lookup(hostname: String): List<InetAddress> {
        val addresses = delegate.lookup(hostname)
        if (addresses.isEmpty() || addresses.any { !it.isTailnetAddress() }) {
            throw UnknownHostException("$hostname does not resolve exclusively to the tailnet")
        }
        return addresses
    }
}

internal fun InetAddress.isTailnetAddress(): Boolean =
    when (this) {
        is Inet4Address -> {
            val octets = address.map { it.toInt() and 0xff }
            octets[0] == 100 && octets[1] in 64..127
        }

        is Inet6Address -> {
            val octets = address.map { it.toInt() and 0xff }
            octets[0] == 0xfd && octets[1] == 0x7a &&
                octets[2] == 0x11 && octets[3] == 0x5c &&
                octets[4] == 0xa1 && octets[5] == 0xe0
        }

        else -> {
            false
        }
    }
