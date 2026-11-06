package com.careerpilot.dto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * One note, parsed from the stored "[2026-11-16] text" lines for the timeline.
 * The entity keeps notes as simple text; this record is only for displaying them.
 */
public record NoteEntry(LocalDate date, String text) {

    /** Group 1 = the date, group 2 = the text. */
    private static final Pattern LINE = Pattern.compile("^\\[(\\d{4}-\\d{2}-\\d{2})] (.*)$");

    /** Newest first. A line without a valid date is kept, with a null date. */
    public static List<NoteEntry> parse(String notes) {
        if (notes == null || notes.isBlank()) {
            return List.of();
        }
        List<NoteEntry> entries = new ArrayList<>();
        for (String line : notes.split("\\R")) { // \R matches \n, \r\n and \r
            if (line.isBlank()) {
                continue;
            }
            entries.add(parseLine(line.strip()));
        }
        Collections.reverse(entries);
        return entries;
    }

    private static NoteEntry parseLine(String line) {
        Matcher matcher = LINE.matcher(line);
        if (matcher.matches()) {
            try {
                return new NoteEntry(LocalDate.parse(matcher.group(1)), matcher.group(2));
            } catch (DateTimeParseException e) {
                // e.g. [2026-13-40]: fall through and keep the whole line as text
            }
        }
        return new NoteEntry(null, line);
    }
}