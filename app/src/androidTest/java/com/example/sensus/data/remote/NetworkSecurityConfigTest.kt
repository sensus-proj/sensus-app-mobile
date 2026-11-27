package com.example.sensus.data.remote

import android.content.Context
import android.content.res.XmlResourceParser
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.sensus.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

@RunWith(AndroidJUnit4::class)
class NetworkSecurityConfigTest {

    @Test
    fun xmlResource_pinsExecuteApiDomainWithAmazonRootsAndExpiration() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser: XmlResourceParser = context.resources.getXml(R.xml.network_security_config)
        val domains = mutableListOf<String>()
        val pins = mutableSetOf<String>()
        var expiration: String? = null

        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG) {
                when (parser.name) {
                    "domain" -> domains += parser.nextText().trim()
                    "pin-set" -> expiration = parser.getAttributeValue(null, "expiration")
                    "pin" -> pins += parser.nextText().trim()
                }
            }
        }
        assertTrue(domains.contains("execute-api.us-east-2.amazonaws.com"))
        assertEquals("2027-11-26", expiration)
        assertEquals(5, pins.size)
        assertTrue(pins.contains("++MBgDH5WGvL9Bcn5Be30cRcL0f5O+NyoXuWtQdX1aI="))
        assertTrue(pins.contains("KwccWaCgrnaw6tsrrSO61FgLacNgG2MMLq8GE6+oP5I="))
    }
}
