package com.careerpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed access to every "careerpilot.*" setting in application.properties.
 * Records are perfect for this: immutable, with accessors generated for you.
 */
@ConfigurationProperties(prefix = "careerpilot")
public record AppProperties(
        @DefaultValue("CareerPilot AI") String name,
        @DefaultValue("Track applications, close skill gaps and prepare for interviews.") String tagline,
        @DefaultValue("Africa/Nairobi") String timeZone) {
}