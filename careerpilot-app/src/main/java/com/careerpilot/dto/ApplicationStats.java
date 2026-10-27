package com.careerpilot.dto;

/**
 * A snapshot of dashboard numbers. Rates are percentages (0–100) of
 * submitted applications, meaning everything except Saved.
 */
public record ApplicationStats(long total, long saved, long applied,
                               long interviews, long offers, long rejected) {

    public static ApplicationStats empty() {
        return new ApplicationStats(0, 0, 0, 0, 0, 0);
    }

    public long submitted() {
        return total - saved;
    }

    public long active() {
        return applied + interviews;
    }

    /** Offers count as having reached interview stage too. */
    public double interviewRate() {
        return percent(interviews + offers, submitted());
    }

    public double offerRate() {
        return percent(offers, submitted());
    }

    private static double percent(long part, long whole) {
        return whole == 0 ? 0.0 : part * 100.0 / whole;
    }
}