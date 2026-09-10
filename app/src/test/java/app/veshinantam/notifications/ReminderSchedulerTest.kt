package app.veshinantam.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class ReminderSchedulerTest {
    private val zone = ZoneId.of("America/New_York")

    @Test
    fun `next occurrence uses today when reminder time is still ahead`() {
        val now = ZonedDateTime.of(2026, 9, 8, 10, 15, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 9, 8, 20, 0, 0, 0, zone),
            ReminderScheduler.nextOccurrence(now, 20, 0),
        )
    }

    @Test
    fun `next occurrence advances a day when reminder time has passed`() {
        val now = ZonedDateTime.of(2026, 9, 8, 20, 1, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 9, 9, 20, 0, 0, 0, zone),
            ReminderScheduler.nextOccurrence(now, 20, 0),
        )
    }

    @Test
    fun `next occurrence remains at selected local time across daylight saving change`() {
        val now = ZonedDateTime.of(2026, 10, 31, 21, 0, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 11, 1, 20, 0, 0, 0, zone),
            ReminderScheduler.nextOccurrence(now, 20, 0),
        )
    }
}
