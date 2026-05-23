package com.dani.bff.dto;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.util.StringUtils;

/**
 * User representation returned by the downstream user service.
 *
 * @param id downstream user identifier
 * @param firstName user's first name when the service exposes separate name fields
 * @param lastName user's last name when the service exposes separate name fields
 * @param displayName display-ready name when supplied directly by the service
 * @param email email address used by the React dashboard
 */
public record DownstreamUserResponse(String id, String firstName, String lastName, String displayName, String email) {

    /**
     * Converts the downstream contract into the stable BFF user contract.
     *
     * @param requestedUserId user identifier requested by the BFF
     * @return user profile used by the dashboard response
     */
    public UserProfile toUserProfile(String requestedUserId) {
        String resolvedId = StringUtils.hasText(id) ? id : requestedUserId;
        String resolvedName = StringUtils.hasText(displayName) ? displayName : fullName();
        if (!StringUtils.hasText(resolvedName)) {
            resolvedName = "User " + resolvedId;
        }

        return new UserProfile(
                resolvedId,
                resolvedName,
                StringUtils.hasText(email) ? email : "unknown@example.invalid");
    }

    private String fullName() {
        return Stream.of(firstName, lastName)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining(" "));
    }
}
