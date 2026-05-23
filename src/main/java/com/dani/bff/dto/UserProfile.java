package com.dani.bff.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

/**
 * User information exposed to the React application.
 *
 * @param id stable user identifier
 * @param displayName display-ready user name
 * @param email email address shown in account-oriented UI
 */
@Schema(description = "User identity fields exposed to the React dashboard.")
public record UserProfile(
        @Schema(description = "Stable user identifier.", example = "user-123")
        String id,
        @Schema(description = "Display-ready user name.", example = "Demo User")
        String displayName,
        @Schema(description = "Email address shown in account UI.", example = "demo@example.com")
        String email) {

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
