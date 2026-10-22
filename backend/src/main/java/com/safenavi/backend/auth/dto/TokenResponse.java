package com.safenavi.backend.auth.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** refreshToken is omitted on /token/refresh, which only reissues the access token. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record TokenResponse(String tokenType, String accessToken, long accessTokenExpiresIn, String refreshToken) {
}
