package com.example.domain.search

/**
 * Utility for fuzzy string matching using Levenshtein distance.
 * Enables forgiving search across app titles, contacts, and system settings.
 */
object FuzzySearch {

    /**
     * Calculates the Levenshtein distance between two character sequences.
     */
    fun levenshteinDistance(s1: CharSequence, s2: CharSequence): Int {
        val len1 = s1.length
        val len2 = s2.length
        var prev = IntArray(len2 + 1) { it }
        var curr = IntArray(len2 + 1)

        for (i in 1..len1) {
            curr[0] = i
            for (j in 1..len2) {
                val cost = if (s1[i - 1].equals(s2[j - 1], ignoreCase = true)) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,       // insertion
                    prev[j] + 1,           // deletion
                    prev[j - 1] + cost     // substitution
                )
            }
            val temp = prev
            prev = curr
            curr = temp
        }
        return prev[len2]
    }

    /**
     * Determines whether [query] matches [target] using a combination of:
     * 1. Substring containment (case-insensitive).
     * 2. Word prefix matching (e.g. "set" matches "Settings").
     * 3. Fuzzy Levenshtein distance within a scaled tolerance.
     */
    fun matches(query: String, target: String): Boolean {
        val q = query.trim().lowercase()
        val t = target.trim().lowercase()
        if (q.isEmpty()) return true
        if (t.contains(q)) return true

        val words = t.split(" ", "_", "-", ".", "/").filter { it.isNotBlank() }
        for (word in words) {
            if (word.startsWith(q)) return true

            val maxWordDistance = when {
                q.length <= 3 -> 0
                q.length <= 5 -> 1
                else -> 2
            }
            val compareChunk = word.take(q.length)
            if (levenshteinDistance(q, compareChunk) <= maxWordDistance) {
                return true
            }
        }

        val fullThreshold = when {
            q.length <= 3 -> 0
            q.length <= 6 -> 1
            else -> 2
        }
        return levenshteinDistance(q, t.take(q.length)) <= fullThreshold
    }
}
