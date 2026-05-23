package com.dani.bff.dto;

import java.util.Objects;

/**
 * User information exposed to the React application.
 *
 * @param id stable user identifier
 * @param displayName display-ready user name
 * @param email email address shown in account-oriented UI
 */
public record UserProfile(String id, String displayName, String email) {

    public UserProfile {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(displayName, "displayName must not be null");
        Objects.requireNonNull(email, "email must not be null");
    }

    /**
     * Creates a safe placeholder used when the user service is unavailable.
     *
     * @param userId identifier from the authenticated JWT
     * @return stable fallback profile
     */
    public static UserProfile unavailable(String userId) {
        return new UserProfile(userId, "Guest User", "unknown@example.invalid");
    }
}
