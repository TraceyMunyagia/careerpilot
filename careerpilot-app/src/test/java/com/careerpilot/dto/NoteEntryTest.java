package com.careerpilot.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class NoteEntryTest {

    @Test
    void emptyOrMissingNotesGiveAnEmptyList() {
        assertThat(NoteEntry.parse(null)).isEmpty();
        assertThat(NoteEntry.parse("")).isEmpty();
    }

    @Test
    void entriesAreNewestFirst() {
        assertThat(NoteEntry.parse("[2026-11-15] First\n[2026-11-16] Second")).containsExactly(
                new NoteEntry(LocalDate.of(2026, 11, 16), "Second"),
                new NoteEntry(LocalDate.of(2026, 11, 15), "First"));
    }

    @Test
    void windowsLineEndingsAndUndatedLinesAreHandled() {
        assertThat(NoteEntry.parse("[2026-11-15] Dated\r\nOld free text\n\n")).containsExactly(
                new NoteEntry(null, "Old free text"),
                new NoteEntry(LocalDate.of(2026, 11, 15), "Dated"));
    }

    @Test
    void anImpossibleDateIsKeptAsText() {
        assertThat(NoteEntry.parse("[2026-13-40] Typo"))
                .containsExactly(new NoteEntry(null, "[2026-13-40] Typo"));
    }
}