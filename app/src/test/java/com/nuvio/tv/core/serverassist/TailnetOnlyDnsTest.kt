package com.nuvio.tv.core.serverassist

import okhttp3.Dns
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.net.InetAddress
import java.net.UnknownHostException

class TailnetOnlyDnsTest {
    @Test
    fun `accepts Tailscale IPv4 and IPv6 results`() {
        val addresses =
            listOf(
                InetAddress.getByName("100.93.42.71"),
                InetAddress.getByName("fd7a:115c:a1e0::1"),
            )
        val dns = TailnetOnlyDns(Dns { addresses })

        assertEquals(addresses, dns.lookup("server.tailnet.ts.net"))
    }

    @Test
    fun `rejects LAN public and mixed DNS answers`() {
        listOf(
            listOf(InetAddress.getByName("192.168.1.4")),
            listOf(InetAddress.getByName("203.0.113.2")),
            listOf(
                InetAddress.getByName("100.93.42.71"),
                InetAddress.getByName("203.0.113.2"),
            ),
        ).forEach { addresses ->
            val dns = TailnetOnlyDns(Dns { addresses })
            assertThrows(UnknownHostException::class.java) {
                dns.lookup("untrusted.example")
            }
        }
    }
}
