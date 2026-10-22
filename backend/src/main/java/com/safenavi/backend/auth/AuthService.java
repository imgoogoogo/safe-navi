package com.safenavi.backend.auth;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.safenavi.backend.auth.dto.LoginRequest;
import com.safenavi.backend.auth.dto.SignupRequest;
import com.safenavi.backend.auth.dto.TokenResponse;
import com.safenavi.backend.auth.security.AuthUser;
import com.safenavi.backend.auth.security.JwtTokenProvider;
import com.safenavi.backend.common.exception.BusinessException;
import com.safenavi.backend.common.exception.ErrorCode;
import com.safenavi.backend.user.User;
import com.safenavi.backend.user.UserRepository;
import com.safenavi.backend.user.UserResponse;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
        User user = new User(email, passwordEncoder.encode(request.password()), request.name().trim());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        requireActive(user);
        return new TokenResponse("Bearer",
                tokenProvider.createAccessToken(user.getId(), user.getRole()),
                tokenProvider.getAccessExpiration().toSeconds(),
                tokenProvider.createRefreshToken(user.getId(), user.getRole()));
    }

    /** Re-reads the user so a suspension or role change takes effect on the next refresh. */
    @Transactional(readOnly = true)
    public TokenResponse refresh(String refreshToken) {
        AuthUser principal = tokenProvider.parse(refreshToken, JwtTokenProvider.TYPE_REFRESH);
        User user = userRepository.findById(principal.id())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
        requireActive(user);
        return new TokenResponse("Bearer",
                tokenProvider.createAccessToken(user.getId(), user.getRole()),
                tokenProvider.getAccessExpiration().toSeconds(),
                null);
    }

    private void requireActive(User user) {
        if (!User.STATUS_ACTIVE.equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.USER_SUSPENDED);
        }
    }
}
