package com.careerpilot.repository;

import com.careerpilot.model.JobApplication;
import com.careerpilot.model.Status;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * In-memory storage. In Week 4 this becomes a Spring Data JPA interface
 * backed by PostgreSQL, with almost the same method names.
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

    /** Returns null when not found. In Week 2 this changes to Optional<JobApplication>. */
    public JobApplication findById(long id) {
        return store.get(id);
    }

    /** Returns a copy, so callers can't modify the internal map. */
    public List<JobApplication> findAll() {
        return new ArrayList<>(store.values());
    }

    /** Newest applications first; saved-only jobs (no date) go last. */
    public List<JobApplication> findAllSortedByDate() {
        List<JobApplication> result = findAll();
        result.sort(Comparator.comparing(
                JobApplication::getApplicationDate,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return result;
    }

    public List<JobApplication> findByStatus(Status status) {
        List<JobApplication> result = new ArrayList<>();
        for (JobApplication app : store.values()) {
            if (app.getStatus() == status) {
                result.add(app);
            }
        }
        return result;
    }

    public List<JobApplication> findByCompanyContaining(String text) {
        String needle = text.toLowerCase();
        List<JobApplication> result = new ArrayList<>();
        for (JobApplication app : store.values()) {
            if (app.getCompanyName().toLowerCase().contains(needle)) {
                result.add(app);
            }
        }
        return result;
    }

    /** Exact match ignoring case, so "nairobi" finds "Nairobi". Apps with no location never match. */
    public List<JobApplication> findByLocation(String location) {
        List<JobApplication> result = new ArrayList<>();
        for (JobApplication app : store.values()) {
            if (location.equalsIgnoreCase(app.getLocation())) {
                result.add(app);
            }
        }
        return result;
    }

    public boolean deleteById(long id) {
        return store.remove(id) != null;
    }

    public int count() {
        return store.size();
    }

    /** Every status appears in the result, even with a count of 0. */
    public Map<Status, Integer> countByStatus() {
        Map<Status, Integer> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            counts.put(status, 0);
        }
        for (JobApplication app : store.values()) {
            counts.merge(app.getStatus(), 1, Integer::sum);
        }
        return counts;
    }
}