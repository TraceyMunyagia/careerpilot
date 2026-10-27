package com.careerpilot.dto;

import com.careerpilot.model.Status;

/**
 * Interface projection: Spring Data creates an object implementing this for each row
 * of a query that selects "status" and "total" columns.
 */
public interface StatusCount {

    Status getStatus();

    Long getTotal();
}