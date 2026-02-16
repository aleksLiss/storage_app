package com.storage.app.dto.user;

import lombok.Data;
import lombok.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Data
public class UserDetailsImpl implements UserDetails {

    private final UUID userId;
    private final String username;
    private final String password;

    @Override
    public @NonNull Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    public @Nullable String getPassword() {
        return password;
    }

    @Override
    public @NonNull String getUsername() {
        return username;
    }
}
