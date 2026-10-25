package com.careerpilot.ui;

import com.careerpilot.model.Status;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

/** Reads keyboard input and keeps asking until the input is valid. */
public class InputHelper {

    private final Scanner scanner;

    public InputHelper(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readRequired(String prompt) {
        while (true) {
            System.out.print(prompt);
            String line = scanner.nextLine().trim();
            if (!line.isEmpty()) {
                return line;
            }
            System.out.println("  This field is required.");
        }
    }

    /** Returns null when the user just presses Enter. */
    public String readOptional(String prompt) {
        System.out.print(prompt);
        String line = scanner.nextLine().trim();
        return line.isEmpty() ? null : line;
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readRequired(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) {
                    return value;
                }
                System.out.printf("  Enter a number from %d to %d.%n", min, max);
            } catch (NumberFormatException e) {
                System.out.println("  That's not a number.");
            }
        }
    }

    public long readId(String prompt) {
        while (true) {
            String line = readRequired(prompt);
            try {
                return Long.parseLong(line);
            } catch (NumberFormatException e) {
                System.out.println("  Enter a numeric id.");
            }
        }
    }

    /** Accepts yyyy-MM-dd or "today". If optional, a blank line returns null. */
    public LocalDate readDate(String prompt, boolean optional) {
        while (true) {
            String line = optional ? readOptional(prompt) : readRequired(prompt);
            if (line == null) {
                return null;
            }
            if (line.equalsIgnoreCase("today")) {
                return LocalDate.now();
            }
            try {
                return LocalDate.parse(line);
            } catch (DateTimeParseException e) {
                System.out.println("  Use the format yyyy-MM-dd, e.g. 2026-11-03.");
            }
        }
    }

    public Status readStatus(String prompt) {
        Status[] statuses = Status.values();
        for (int i = 0; i < statuses.length; i++) {
            System.out.printf("  %d. %s%n", i + 1, statuses[i].getLabel());
        }
        int choice = readInt(prompt, 1, statuses.length);
        return statuses[choice - 1];
    }

    public boolean confirm(String prompt) {
        String answer = readRequired(prompt + " (y/n): ");
        return answer.equalsIgnoreCase("y") || answer.equalsIgnoreCase("yes");
    }
}