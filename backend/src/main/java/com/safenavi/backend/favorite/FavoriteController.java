package com.safenavi.backend.favorite;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.safenavi.backend.auth.security.AuthUser;
import com.safenavi.backend.common.response.ApiResponse;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<FavoriteResponse> create(@AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody FavoriteCreateRequest request) {
        return ApiResponse.ok(favoriteService.create(authUser.id(), request));
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> list(@AuthenticationPrincipal AuthUser authUser) {
        return ApiResponse.ok(favoriteService.list(authUser.id()));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@AuthenticationPrincipal AuthUser authUser, @PathVariable Long id) {
        favoriteService.delete(authUser.id(), id);
        return ApiResponse.ok();
    }
}
