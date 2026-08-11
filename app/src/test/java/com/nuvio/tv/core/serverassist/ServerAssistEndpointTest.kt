package com.nuvio.tv.core.serverassist

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ServerAssistEndpointTest {
    @Test
    fun `normalizes explicit tailnet address and MagicDNS endpoint`() {
        assertEquals(
            "http://100.105.18.59:8765/",
            ServerAssistEndpoint.normalize("100.105.18.59:8765").getOrThrow(),
        )
        assertEquals(
            "https://node.example.ts.net:9443/",
            ServerAssistEndpoint.normalize("HTTPS://Node.Example.ts.net:9443/").getOrThrow(),
        )
    }

    @Test
    fun `rejects credentials query fragments and resource paths`() {
        listOf(
            "ftp://100.90.1.2:8765",
            "http://user:secret@100.90.1.2:8765",
            "http://100.90.1.2:8765/?token=secret",
            "http://100.90.1.2:8765/v1/health",
            "http://100.90.1.2:8765/#fragment",
        ).forEach { endpoint ->
            assertTrue(endpoint, ServerAssistEndpoint.normalize(endpoint).isFailure)
        }
    }
}
