package com.example

import com.example.data.SettingsDataStore
import com.example.domain.search.FuzzySearch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FuzzySearchTest {

    @Test
    fun testLevenshteinDistanceCalculation() {
        assertEquals(0, FuzzySearch.levenshteinDistance("camera", "camera"))
        assertEquals(1, FuzzySearch.levenshteinDistance("camera", "camra"))
        assertEquals(1, FuzzySearch.levenshteinDistance("phone", "phne"))
        assertEquals(2, FuzzySearch.levenshteinDistance("phone", "fone"))
        assertEquals(2, FuzzySearch.levenshteinDistance("settings", "setingz"))
    }

    @Test
    fun testFuzzyMatchingAppsAndLabels() {
        // Exact substring
        assertTrue(FuzzySearch.matches("cam", "Camera"))
        assertTrue(FuzzySearch.matches("calc", "Calculator"))

        // Word prefix
        assertTrue(FuzzySearch.matches("net", "Wi-Fi & Network"))
        assertTrue(FuzzySearch.matches("tooth", "Bluetooth & Devices"))

        // Typo tolerance with Levenshtein
        assertTrue(FuzzySearch.matches("galery", "Gallery"))
        assertTrue(FuzzySearch.matches("mesages", "Messages"))
        assertTrue(FuzzySearch.matches("calender", "Calendar"))

        // Unrelated query
        assertFalse(FuzzySearch.matches("xyz", "Clock"))
    }

    @Test
    fun testSearchEngineUrlTemplates() {
        val googleUrl = SettingsDataStore.getSearchUrl("google", "android compose")
        assertEquals("https://www.google.com/search?q=android%20compose", googleUrl)

        val ddgUrl = SettingsDataStore.getSearchUrl("duckduckgo", "pixel launcher")
        assertEquals("https://duckduckgo.com/?q=pixel%20launcher", ddgUrl)

        val bingUrl = SettingsDataStore.getSearchUrl("bing", "honor x9d")
        assertEquals("https://www.bing.com/search?q=honor%20x9d", bingUrl)
    }
}
