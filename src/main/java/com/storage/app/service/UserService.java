package com.storage.app.service;

import com.storage.app.exception.UserAlreadyExistsException;
import com.storage.app.dto.user.UserDto;
import com.storage.app.mapper.UserDetailsMapper;
import com.storage.app.model.User;
import com.storage.app.repository.UserRepository;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserDetailsMapper userDetailsMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(userDetailsMapper::toUserDetails)
                .orElseThrow(() -> UsernameNotFoundException.fromUsername(username));
    }

    @Transactional
    public void save(UserDto userDto) {
        if (userRepository.existsUserByUsername(userDto.getUsername())) {
            throw new UserAlreadyExistsException("User with name " + userDto.getUsername() + " already exists");
        }
        User user = userDetailsMapper.toUser(userDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
    }
}
