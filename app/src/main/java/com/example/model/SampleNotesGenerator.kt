package com.example.model

object SampleNotesGenerator {
    fun generateSamples(): List<Note> {
        val now = System.currentTimeMillis()
        val hour = 3600 * 1000L
        val day = 24 * hour

        val note1Title = "Tea with team & sprint recap"
        val note1Body = "Discussed Q4 roadmap milestones over evening masala chai. Action items: finalize prototype by Thursday, review UI test coverage, and schedule deployment sync. #work #imp #roadmap"

        val note2Title = "Kannada poetry excerpts & thoughts"
        val note2Body = "Reading Da.Ra. Bendre's verses on twilight and nature. 'Naku Tanthi' conveys resonance between internal contemplation and the vibrant outside world. #reading #poetry"

        val note4Title = "Architecture idea: Local-first offline sync"
        val note4Body = "Local SQLite with Room provides ultra-fast zero-latency startup. Backups can be serialized into formatted JSON and restored directly without cloud dependency. #engineering #offline #imp"

        val note5Title = "Mandakki & snacks evening ingredients"
        val note5Body = "Recipe for Davanagere style spicy puffed rice:\n1. Mustard seeds, curry leaves tempering\n2. Crushed garlic and green chillies\n3. Turmeric and pinch of asafoetida\n4. Roasted gram & peanuts garnish\n#food #cooking"

        val note6Title = "System design notes: Operating Systems"
        val note6Body = "Virtual memory paging, TLB hit rates, inverted page tables, and LRU eviction policies compared. Practice concurrency primitives next. #study #systems"

        val note8Title = "Morning mindfulness & gratitude log"
        val note8Body = "Grateful for quiet morning hours before notifications start, a hot cup of filter coffee, and clear skies for an early morning run. #mindfulness #health"

        return listOf(
            Note(
                title = note1Title,
                content = note1Body,
                categoryId = "work",
                noteType = NOTE_FORMAT_NOTES,
                tags = TagUtils.extractTagsString(note1Title, note1Body, NOTE_FORMAT_NOTES),
                isPinned = false,
                wordCount = 31,
                readingTimeMinutes = 1,
                createdAt = now - (1 * hour),
                updatedAt = now - (1 * hour)
            ),
            Note(
                title = note2Title,
                content = note2Body,
                categoryId = "journal",
                noteType = NOTE_FORMAT_NOTES,
                tags = TagUtils.extractTagsString(note2Title, note2Body, NOTE_FORMAT_NOTES),
                isPinned = false,
                wordCount = 26,
                readingTimeMinutes = 1,
                createdAt = now - (3 * hour),
                updatedAt = now - (3 * hour)
            ),
            Note(
                title = "Weekend grocery & household list",
                content = "- [ ] Roasted peanuts\n- [ ] Coffee beans (Chikmagalur dark roast)\n- [ ] Desi ghee\n- [x] A4 notebook refills\n- [x] Microfiber cleaning cloths",
                categoryId = "personal",
                noteType = NOTE_FORMAT_CHECKLIST,
                tags = "",
                isPinned = false,
                wordCount = 21,
                readingTimeMinutes = 1,
                createdAt = now - (5 * hour),
                updatedAt = now - (5 * hour)
            ),
            Note(
                title = note4Title,
                content = note4Body,
                categoryId = "ideas",
                noteType = NOTE_FORMAT_NOTES,
                tags = TagUtils.extractTagsString(note4Title, note4Body, NOTE_FORMAT_NOTES),
                isPinned = false,
                wordCount = 27,
                readingTimeMinutes = 1,
                createdAt = now - (1 * day),
                updatedAt = now - (1 * day)
            ),
            Note(
                title = note5Title,
                content = note5Body,
                categoryId = "personal",
                noteType = NOTE_FORMAT_NOTES,
                tags = TagUtils.extractTagsString(note5Title, note5Body, NOTE_FORMAT_NOTES),
                isPinned = false,
                wordCount = 34,
                readingTimeMinutes = 1,
                createdAt = now - (1 * day + 2 * hour),
                updatedAt = now - (1 * day + 2 * hour)
            ),
            Note(
                title = note6Title,
                content = note6Body,
                categoryId = "study",
                noteType = NOTE_FORMAT_NOTES,
                tags = TagUtils.extractTagsString(note6Title, note6Body, NOTE_FORMAT_NOTES),
                isPinned = false,
                wordCount = 24,
                readingTimeMinutes = 1,
                createdAt = now - (2 * day),
                updatedAt = now - (2 * day)
            ),
            Note(
                title = "Trip packing checklist: Coorg",
                content = "- [ ] Trekking boots\n- [ ] Rain jacket\n- [ ] Power bank 20,000mAh\n- [x] First aid & essential medicines\n- [x] Camera with 35mm lens",
                categoryId = "tasks",
                noteType = NOTE_FORMAT_CHECKLIST,
                tags = "",
                isPinned = false,
                wordCount = 20,
                readingTimeMinutes = 1,
                createdAt = now - (3 * day),
                updatedAt = now - (3 * day)
            ),
            Note(
                title = note8Title,
                content = note8Body,
                categoryId = "journal",
                noteType = NOTE_FORMAT_NOTES,
                tags = TagUtils.extractTagsString(note8Title, note8Body, NOTE_FORMAT_NOTES),
                isPinned = false,
                wordCount = 27,
                readingTimeMinutes = 1,
                createdAt = now - (4 * day),
                updatedAt = now - (4 * day)
            )
        )
    }
}
