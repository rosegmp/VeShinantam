package app.veshinantam.data.sync

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncRequestTest {
    @Test
    fun magicLinkUsesTheRawAuthApiRedirectField() {
        val body = JSONObject(magicLinkRequestBody(" user@example.com ", "app.veshinantam://auth"))

        assertEquals("user@example.com", body.getString("email"))
        assertTrue(body.getBoolean("create_user"))
        assertEquals("app.veshinantam://auth", body.getString("redirect_to"))
        assertFalse(body.has("email_redirect_to"))
    }
}
