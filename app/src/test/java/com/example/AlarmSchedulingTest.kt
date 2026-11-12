package com.example

import com.example.simulated.clock.AlarmScheduler
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmSchedulingTest {

    @Test
    fun testOneTimeAlarmNextDay() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Alarm set for 08:00 (which has already passed today)
        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(
            hour = 8,
            minute = 0,
            daysOfWeekMask = 0,
            currentCalendar = now
        )

        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(2026, triggerCal.get(Calendar.YEAR))
        assertEquals(Calendar.NOVEMBER, triggerCal.get(Calendar.MONTH))
        assertEquals(13, triggerCal.get(Calendar.DAY_OF_MONTH)) // next day!
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
    }

    @Test
    fun testOneTimeAlarmSameDay() {
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 14, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Alarm set for 18:00 (later today)
        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(
            hour = 18,
            minute = 0,
            daysOfWeekMask = 0,
            currentCalendar = now
        )

        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(12, triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(18, triggerCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testRepeatingAlarm() {
        // Thursday Nov 12, 2026.
        // Calendar.THURSDAY is day 5. bit 4 = 1 << 4 = 16
        // Suppose repeating on Mondays only: Monday is day 2, bit 1 = 1 << 1 = 2
        val now = Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 12, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val mondayMask = 1 shl (Calendar.MONDAY - 1)
        val triggerMillis = AlarmScheduler.calculateNextTriggerMillis(
            hour = 9,
            minute = 0,
            daysOfWeekMask = mondayMask,
            currentCalendar = now
        )

        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }
        assertEquals(Calendar.MONDAY, triggerCal.get(Calendar.DAY_OF_WEEK))
        assertTrue(triggerMillis > now.timeInMillis)
    }
}
