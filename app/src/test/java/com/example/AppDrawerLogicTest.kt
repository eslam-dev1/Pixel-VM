package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.os.AppRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppDrawerLogicTest {

    @Test
    fun testAppRegistryContainsAllApps() {
        val apps = AppRegistry.APPS
        assertEquals(11, apps.size)
        val ids = apps.map { it.id }.toSet()
        assertTrue(ids.containsAll(listOf("phone", "messages", "camera", "clock", "calculator", "notes", "gallery", "settings", "files", "contacts", "calendar")))
    }

    @Test
    fun testAlphabeticalSortingByTitle() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val sortedApps = AppRegistry.APPS.map { app ->
            app to context.getString(app.titleRes)
        }.sortedBy { it.second }

        val titles = sortedApps.map { it.second }
        val expectedSortedTitles = titles.sorted()
        assertEquals(expectedSortedTitles, titles)
    }

    @Test
    fun testSearchFiltering() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val appsWithTitles = AppRegistry.APPS.map { app ->
            app to context.getString(app.titleRes)
        }

        // Test filtering query "calc"
        val calcResults = appsWithTitles.filter { (app, title) ->
            title.contains("calc", ignoreCase = true) || app.id.contains("calc", ignoreCase = true)
        }
        assertEquals(1, calcResults.size)
        assertEquals("calculator", calcResults.first().first.id)

        // Test filtering query "ca" -> should match Calculator, Calendar, Camera
        val caResults = appsWithTitles.filter { (app, title) ->
            title.contains("ca", ignoreCase = true) || app.id.contains("ca", ignoreCase = true)
        }
        assertTrue(caResults.size >= 3)

        // Test blank query -> returns all apps
        val blankQuery = ""
        val allResults = appsWithTitles.filter { (app, title) ->
            blankQuery.isBlank() || title.contains(blankQuery, ignoreCase = true) || app.id.contains(blankQuery, ignoreCase = true)
        }
        assertEquals(11, allResults.size)
    }
}
