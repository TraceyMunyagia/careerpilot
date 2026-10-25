package com.careerpilot.ui;

import com.careerpilot.model.Status;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Each test "types" input by building a Scanner from a String. "\n" is the Enter key. */
class InputHelperTest {

    private InputHelper typing(String keystrokes) {
        return new InputHelper(new Scanner(keystrokes));
    }

    @Test
    void readRequiredSkipsBlankLines() {
        assertEquals("Andela", typing("\n   \nAndela\n").readRequired("> "));
    }

    @Test
    void readOptionalReturnsNullForBlank() {
        assertNull(typing("\n").readOptional("> "));
    }

    @Test
    void readIntRetriesUntilANumberInRange() {
        // "abc" is not a number, 42 is out of range, 5 is accepted
        assertEquals(5, typing("abc\n42\n5\n").readInt("> ", 0, 9));
    }

    @Test
    void readDateRejectsWrongFormatThenAcceptsIso() {
        LocalDate date = typing("03/11/2026\n2026-11-03\n").readDate("> ", false);
        assertEquals(LocalDate.of(2026, 11, 3), date);
    }

    @Test
    void optionalDateCanBeLeftBlank() {
        assertNull(typing("\n").readDate("> ", true));
    }

    @Test
    void readStatusMapsMenuNumberToEnum() {
        assertEquals(Status.INTERVIEW, typing("3\n").readStatus("> "));
    }

    @Test
    void confirmAcceptsYAndYes() {
        assertTrue(typing("y\n").confirm("Sure?"));
        assertTrue(typing("YES\n").confirm("Sure?"));
        assertFalse(typing("n\n").confirm("Sure?"));
    }
}