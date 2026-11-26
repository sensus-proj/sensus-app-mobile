package com.example.sensus.presentation

import com.example.sensus.presentation.auth.PasswordResetLink
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PasswordResetLinkTest {
    @Test
    fun acceptsOfficialResetDestinations() {
        assertEquals("example-token", PasswordResetLink.parse("sensus://reset?token=example-token"))
        assertEquals("example-token", PasswordResetLink.parse("https://rodnix.com.mx/sensus/reset/?token=example-token"))
    }

    @Test
    fun rejectsDestinationsOutsideTheResetContract() {
        listOf(
            "http://rodnix.com.mx/sensus/reset?token=example",
            "https://rodnix.com.mx/sensus/reset/extra?token=example",
            "https://rodnix.com.mx:443/sensus/reset?token=example",
            "https://someone@rodnix.com.mx/sensus/reset?token=example",
            "sensus://reset/extra?token=example",
            "https://example.com/sensus/reset?token=example"
        ).forEach { assertNull(it, PasswordResetLink.parse(it)) }
    }

    @Test
    fun rejectsAmbiguousOrMalformedTokens() {
        listOf(
            "sensus://reset?token=one&%74oken=two",
            "sensus://reset?token=%ZZ",
            "sensus://reset?token=%0A",
            "sensus://reset?token=",
            "sensus://reset?token=example#fragment"
        ).forEach { assertNull(it, PasswordResetLink.parse(it)) }
    }
}
