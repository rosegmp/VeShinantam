package app.veshinantam.data.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncRequestTest {
    @Test
    fun magicLinkUsesTheRawAuthApiRedirectQueryParameter() {
        val body = JSONObject(magicLinkRequestBody(" user@example.com "))
        val path = magicLinkRequestPath("app.veshinantam://auth")

        assertEquals("user@example.com", body.getString("email"))
        assertTrue(body.getBoolean("create_user"))
        assertEquals("/auth/v1/otp?redirect_to=app.veshinantam%3A%2F%2Fauth", path)
        assertFalse(body.has("redirect_to"))
        assertFalse(body.has("email_redirect_to"))
    }
}
