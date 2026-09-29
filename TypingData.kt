package com.example.typing

data class TypingSentence(
    val id: String,
    val text: String,
    val category: String, // "Easy", "Medium", "Hard", "Speed"
    val rewardCoins: Int = 5
)

object TypingChallengeRepository {

    // Chhote vaakya: Easy = 2 shabd, Medium = 3-4 shabd, Hard = 5-6 shabd (punctuation ke saath)
    val allSentences: List<TypingSentence> = listOf(
        // Easy (2 words)
        TypingSentence("easy_1", "Type fast", "Easy", 5),
        TypingSentence("easy_2", "Good luck", "Easy", 5),
        TypingSentence("easy_3", "Well done", "Easy", 5),
        TypingSentence("easy_4", "Stay calm", "Easy", 5),
        TypingSentence("easy_5", "Keep going", "Easy", 5),
        TypingSentence("easy_6", "Nice work", "Easy", 5),
        TypingSentence("easy_7", "Start now", "Easy", 5),
        TypingSentence("easy_8", "Be happy", "Easy", 5),

        // Medium (3-4 words)
        TypingSentence("med_1", "Practice makes perfect", "Medium", 8),
        TypingSentence("med_2", "Small steps every day", "Medium", 8),
        TypingSentence("med_3", "Never give up hope", "Medium", 8),
        TypingSentence("med_4", "Focus on your goal", "Medium", 8),
        TypingSentence("med_5", "Learn something new", "Medium", 8),
        TypingSentence("med_6", "Welcome to Typing Rewards", "Medium", 8),

        // Hard (5-6 words, punctuation)
        TypingSentence("hard_1", "Hard work beats talent, always.", "Hard", 12),
        TypingSentence("hard_2", "Believe in yourself, keep going!", "Hard", 12),
        TypingSentence("hard_3", "Dream big, start small, win big!", "Hard", 12),
        TypingSentence("hard_4", "Speed comes with daily practice.", "Hard", 12)
    )

    fun getRandomSentence(category: String? = null): TypingSentence {
        val pool = if (category.isNullOrEmpty() || category == "All") {
            allSentences
        } else {
            allSentences.filter { it.category.equals(category, ignoreCase = true) }
        }
        return pool.randomOrNull() ?: allSentences.first()
    }

    /**
     * Calculates standard Words Per Minute (1 word = 5 characters)
     */
    fun calculateWpm(charCount: Int, elapsedMillis: Long): Int {
        if (elapsedMillis <= 1000 || charCount == 0) return 0
        val minutes = elapsedMillis / 60000.0
        val words = charCount / 5.0
        val wpm = (words / minutes).toInt()
        return wpm.coerceIn(0, 200)
    }

    /**
     * Calculates accuracy percentage
     */
    fun calculateAccuracy(totalKeystrokes: Int, errors: Int): Int {
        if (totalKeystrokes <= 0) return 100
        val correct = (totalKeystrokes - errors).coerceAtLeast(0)
        return ((correct.toDouble() / totalKeystrokes) * 100).toInt().coerceIn(0, 100)
    }
}
