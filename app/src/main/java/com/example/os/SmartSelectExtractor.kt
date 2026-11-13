package com.example.os

/**
 * Represents a selectable text snippet extracted from real screen context,
 * active Live Activities, notifications, and app metadata for Smart Select / Circle to Search.
 */
data class SmartSelectSnippet(
    val id: String,
    val label: String,
    val text: String,
    val category: String = "Text"
)

object SmartSelectExtractor {

    fun extractSnippets(
        appId: String,
        appDisplayName: String,
        liveStatusText: String? = null,
        extraContent: List<String> = emptyList()
    ): List<SmartSelectSnippet> {
        val snippets = mutableListOf<SmartSelectSnippet>()

        if (appDisplayName.isNotBlank()) {
            snippets.add(
                SmartSelectSnippet(
                    id = "${appId}_title",
                    label = "Screen",
                    text = appDisplayName.trim(),
                    category = "Screen"
                )
            )
        }

        if (!liveStatusText.isNullOrBlank()) {
            snippets.add(
                SmartSelectSnippet(
                    id = "${appId}_live",
                    label = "Live Activity",
                    text = liveStatusText.trim(),
                    category = "Live"
                )
            )
        }

        extraContent.filter { it.isNotBlank() }.forEachIndexed { index, raw ->
            val value = raw.trim()
            val category = when {
                value.startsWith("http://", ignoreCase = true) || value.startsWith("https://", ignoreCase = true) -> "Link"
                Regex("""^[+\d][\d\s\-()]{5,}$""").matches(value) -> "Phone"
                else -> "Content"
            }
            snippets.add(
                SmartSelectSnippet(
                    id = "${appId}_extra_$index",
                    label = category,
                    text = value,
                    category = category
                )
            )
        }

        return snippets.distinctBy { it.text }
    }
}
