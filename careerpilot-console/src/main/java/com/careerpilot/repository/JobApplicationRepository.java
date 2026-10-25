package com.careerpilot.repository;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * In-memory storage, now written with streams.
 * In Week 4 this becomes a Spring Data JPA interface backed by PostgreSQL.
 */
public class JobApplicationRepository {

    private final Map<Long, JobApplication> store = new LinkedHashMap<>(); // keeps insertion order
    private long nextId = 1;

    /** Assigns an id to new applications, then stores or replaces them. */
    public JobApplication save(JobApplication application) {
        if (application.getId() == null) {
            application.setId(nextId++);
        }
        store.put(application.getId(), application);
        return application;
    }

    /** Optional makes "might be missing" part of the method's return type. */
    public Optional<JobApplication> findById(long id) {
        return Optional.ofNullable(store.get(id));
    }

    /** A mutable copy: callers may sort or clear it without touching the store. */
    public List<JobApplication> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Newest applications first; saved-only jobs (no date) go last. Returns an unmodifiable list. */
    public List<JobApplication> findAllSortedByDate() {
        return store.values().stream()
                .sorted(Comparator.comparing(
                        JobApplication::getApplicationDate,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<JobApplication> findByStatus(Status status) {
        return store.values().stream()
                .filter(app -> app.getStatus() == status)
                .toList();
    }

    public List<JobApplication> findByCompanyContaining(String text) {
        String needle = text.toLowerCase();
        return store.values().stream()
                .filter(app -> app.getCompanyName().toLowerCase().contains(needle))
                .toList();
    }

    public List<JobApplication> findByLocation(String location) {
        String needle = location.toLowerCase();
        return store.values().stream()
                .filter(app -> app.getLocation() != null)
                .filter(app -> app.getLocation().toLowerCase().contains(needle))
                .toList();
    }

    public boolean deleteById(long id) {
        return store.remove(id) != null;
    }

    public int count() {
        return store.size();
    }

    /** Every status appears in the result, even with a count of 0. */
    public Map<Status, Long> countByStatus() {
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            counts.put(status, 0L);
        }
        counts.putAll(store.values().stream()
                .collect(Collectors.groupingBy(
                        JobApplication::getStatus,
                        () -> new EnumMap<Status, Long>(Status.class),
                        Collectors.counting())));
        return counts;
    }
}