package com.safenavi.backend.favorite;

import java.time.LocalDateTime;

public record FavoriteResponse(Long id, String label, double lat, double lng, LocalDateTime createdAt) {

    public static FavoriteResponse from(Favorite f) {
        return new FavoriteResponse(f.getId(), f.getLabel(), f.getLat(), f.getLng(), f.getCreatedAt());
    }
}
