package app.veshinantam.data.sync

import app.veshinantam.data.local.SyncOutboxEntity
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseSyncRequestTest {
    @Test
    fun passwordSignupUsesTheRawAuthApiRedirectQueryParameter() {
        val body = JSONObject(emailPasswordRequestBody(" user@example.com ", "test-password"))
        val path = passwordSignUpRequestPath("app.veshinantam://auth")

        assertEquals("user@example.com", body.getString("email"))
        assertEquals("test-password", body.getString("password"))
        assertEquals("/auth/v1/signup?redirect_to=app.veshinantam%3A%2F%2Fauth", path)
        assertFalse(body.has("redirect_to"))
        assertFalse(body.has("email_redirect_to"))
    }

    @Test
    fun authFailureUsesTheSupabaseMessage() {
        val status = authFailureStatus("signing in", IllegalStateException("{\"msg\":\"Invalid login credentials\"}"))

        assertEquals("Error signing in: Invalid login credentials", status)
    }

    @Test
    fun entityMutationsAreEncodedAsOneBatchRpcArgument() {
        val body = JSONObject(
            mutationBatchRequestBody(
                listOf(
                    SyncOutboxEntity("TASK", "task-1", "00000000-0000-0000-0000-000000000001", 7, "{\"completed\":true}", false, "now"),
                    SyncOutboxEntity("TASK", "task-2", "00000000-0000-0000-0000-000000000002", 8, null, true, "now"),
                ),
            ),
        )
        val mutations = body.getJSONArray("p_mutations")

        assertEquals(100, ENTITY_SYNC_PUSH_BATCH_SIZE)
        assertEquals(2, mutations.length())
        assertEquals("task-1", mutations.getJSONObject(0).getString("entity_id"))
        assertTrue(mutations.getJSONObject(0).getJSONObject("payload").getBoolean("completed"))
        assertEquals(JSONObject.NULL, mutations.getJSONObject(1).get("payload"))
        assertTrue(mutations.getJSONObject(1).getBoolean("deleted"))
    }

    @Test
    fun entityPayloadComparisonIgnoresJsonOrderNumericFormattingAndSyncMetadata() {
        val remote = """{"quantity":1,"nested":{"b":2,"a":1},"updatedAt":"old","revision":7}"""
        val local = """{"nested":{"a":1.0,"b":2.0},"quantity":1.0,"updatedAt":"new","revision":0}"""

        assertTrue(syncPayloadsEquivalent(remote, local))
        assertFalse(syncPayloadsEquivalent(remote, local.replace("1.0", "3.0")))
    }

    @Test
    fun syncFailureIncludesThePhaseWithoutGrowingUnbounded() {
        val status = syncFailureStatus(IllegalStateException("Uploading device changes failed: network unavailable"))

        assertTrue(status.startsWith("Sync error: Uploading device changes failed:"))
        assertTrue(status.endsWith("Device data is unchanged."))
        assertTrue(status.length < 250)
    }
}
