package app.veshinantam.notifications

import android.content.Intent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderRescheduleReceiverTest {
    @Test
    fun `device and package timing events reschedule reminders`() {
        listOf(
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_MY_PACKAGE_REPLACED,
        ).forEach { action ->
            assertTrue(action, ReminderRescheduleReceiver.shouldReschedule(action))
        }
    }

    @Test
    fun `unrelated and missing actions do not reschedule reminders`() {
        assertFalse(ReminderRescheduleReceiver.shouldReschedule(Intent.ACTION_AIRPLANE_MODE_CHANGED))
        assertFalse(ReminderRescheduleReceiver.shouldReschedule(null))
    }
}
