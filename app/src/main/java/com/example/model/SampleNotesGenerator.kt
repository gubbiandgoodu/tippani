package com.example.model

object SampleNotesGenerator {
    fun generateSamples(): List<Note> {
        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L
        val day = 24 * hour

        return listOf(
            Note(
                title = "Tea with team & sprint recap",
                content = "Discussed Q4 roadmap milestones over evening masala chai. Action items: finalize prototype by Thursday, review UI test coverage, and schedule deployment sync.",
                categoryId = "work",
                noteType = "Meeting Notes",
                tags = "Work, Team, Roadmap",
                isPinned = true,
                wordCount = 31,
                readingTimeMinutes = 1,
                createdAt = now - (1 * hour),
                updatedAt = now - (1 * hour)
            ),
            Note(
                title = "Kannada poetry excerpts & thoughts",
                content = "Reading Da.Ra. Bendre's verses on twilight and nature. 'Naku Tanthi' conveys resonance between internal contemplation and the vibrant outside world.",
                categoryId = "journal",
                noteType = "Reflections",
                tags = "Reading, Poetry, Thoughts",
                isPinned = true,
                wordCount = 26,
                readingTimeMinutes = 1,
                createdAt = now - (3 * hour),
                updatedAt = now - (3 * hour)
            ),
            Note(
                title = "Weekend grocery & household list",
                content = "- Roasted peanuts\n- Coffee beans (Chikmagalur dark roast)\n- Desi ghee\n- A4 notebook refills\n- Microfiber cleaning cloths",
                categoryId = "personal",
                noteType = "Checklist",
                tags = "Shopping, Home, Weekly",
                isPinned = false,
                wordCount = 21,
                readingTimeMinutes = 1,
                createdAt = now - (5 * hour),
                updatedAt = now - (5 * hour)
            ),
            Note(
                title = "Architecture idea: Local-first offline sync",
                content = "Local SQLite with Room provides ultra-fast zero-latency startup. Backups can be serialized into formatted JSON and restored directly without cloud dependency.",
                categoryId = "ideas",
                noteType = "Tech Architecture",
                tags = "Engineering, Offline, Room",
                isPinned = false,
                wordCount = 27,
                readingTimeMinutes = 1,
                createdAt = now - (1 * day),
                updatedAt = now - (1 * day)
            ),
            Note(
                title = "Mandakki & snacks evening ingredients",
                content = "Recipe for Davanagere style spicy puffed rice:\n1. Mustard seeds, curry leaves tempering\n2. Crushed garlic and green chillies\n3. Turmeric and pinch of asafoetida\n4. Roasted gram & peanuts garnish",
                categoryId = "personal",
                noteType = "Recipe",
                tags = "Food, Cooking, Snacks",
                isPinned = false,
                wordCount = 34,
                readingTimeMinutes = 1,
                createdAt = now - (1 * day + 2 * hour),
                updatedAt = now - (1 * day + 2 * hour)
            ),
            Note(
                title = "System design notes: Operating Systems",
                content = "Virtual memory paging, TLB hit rates, inverted page tables, and LRU eviction policies compared. Practice concurrency primitives next.",
                categoryId = "study",
                noteType = "Study Summary",
                tags = "CS, Systems, Memory",
                isPinned = false,
                wordCount = 24,
                readingTimeMinutes = 1,
                createdAt = now - (2 * day),
                updatedAt = now - (2 * day)
            ),
            Note(
                title = "Trip packing checklist: Coorg",
                content = "- Trekking boots\n- Rain jacket\n- Power bank 20,000mAh\n- First aid & essential medicines\n- Camera with 35mm lens",
                categoryId = "tasks",
                noteType = "Checklist",
                tags = "Travel, Coorg, Essentials",
                isPinned = false,
                wordCount = 20,
                readingTimeMinutes = 1,
                createdAt = now - (3 * day),
                updatedAt = now - (3 * day)
            ),
            Note(
                title = "Morning mindfulness & gratitude log",
                content = "Grateful for quiet morning hours before notifications start, a hot cup of filter coffee, and clear skies for an early morning run.",
                categoryId = "journal",
                noteType = "Daily Journal",
                tags = "Mindfulness, Gratitude, Health",
                isPinned = false,
                wordCount = 27,
                readingTimeMinutes = 1,
                createdAt = now - (4 * day),
                updatedAt = now - (4 * day)
            )
        )
    }
}
