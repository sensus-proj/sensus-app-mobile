package com.example.sensus.data.remote

import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.security.KeyPairGenerator
import java.security.PublicKey
import java.security.cert.X509Certificate
import java.time.LocalDate
import javax.net.ssl.SSLPeerUnverifiedException
import javax.xml.parsers.DocumentBuilderFactory
import okhttp3.CertificatePinner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkPinningTest {

    private val expectedPins = setOf(
        "++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI=",
        "f0KW/FtqTjs108NpYj42SrGvOB2PpxIVM8nWxjPqJGE=",
        "NqvDJlas/GRcYbcWE8S/IceH9cq77kg0jVhZeAPXq8k=",
        "9+ze1cZgR9KO1kZrVDxA4HQ6voHRCSVNz4RdTCx4U8U=",
        "KwccWaCgrnaw6tsrrSO61FgLacNgG2MMLq8GE6+oP5I="
    )

    @Test
    fun networkSecurityConfig_containsValidAwsPinSetAndBackupRoots() {
        val xmlFile = locateNetworkConfigFile()
        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlFile)
        val domainConfigs = doc.getElementsByTagName("domain-config")
        assertTrue(domainConfigs.length >= 1)

        val configElement = domainConfigs.item(0)
        assertEquals("false", configElement.attributes.getNamedItem("cleartextTrafficPermitted").nodeValue)

        val domainNode = doc.getElementsByTagName("domain").item(0)
        assertEquals("execute-api.us-east-2.amazonaws.com", domainNode.textContent.trim())
        assertEquals("true", domainNode.attributes.getNamedItem("includeSubdomains").nodeValue)

        val pinSetNode = doc.getElementsByTagName("pin-set").item(0)
        val expiration = pinSetNode.attributes.getNamedItem("expiration").nodeValue
        assertEquals("2027-11-26", expiration)
        assertTrue(LocalDate.parse(expiration).isAfter(LocalDate.now()))

        val pins = (0 until doc.getElementsByTagName("pin").length).map { index ->
            val pinNode = doc.getElementsByTagName("pin").item(index)
            assertEquals("SHA-256", pinNode.attributes.getNamedItem("digest").nodeValue)
            pinNode.textContent.trim()
        }.toSet()
        assertEquals(expectedPins, pins)
    }

    @Test(expected = SSLPeerUnverifiedException::class)
    fun certificatePinner_rejectsUntrustedLeafCertificate() {
        val builder = CertificatePinner.Builder()
        expectedPins.forEach { digest -> builder.add("*.execute-api.us-east-2.amazonaws.com", "sha256/$digest") }
        val pinner = builder.build()

        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val publicKey: PublicKey = keyPair.public
        val cert = mockk<X509Certificate>(relaxed = true) {
            every { this@mockk.publicKey } returns publicKey
        }
        pinner.check("zqqdwovx81.execute-api.us-east-2.amazonaws.com", listOf(cert))
    }

    private fun locateNetworkConfigFile(): File {
        val direct = File("src/main/res/xml/network_security_config.xml")
        if (direct.isFile) return direct
        return File("app/src/main/res/xml/network_security_config.xml")
    }
}
