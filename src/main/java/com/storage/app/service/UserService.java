package com.storage.app.service;

import com.storage.app.exception.user.UserAlreadyExistsException;
import com.storage.app.dto.user.UserDto;
import com.storage.app.mapper.UserPrincipalMapper;
import com.storage.app.model.User;
import com.storage.app.repository.UserRepository;
import com.storage.app.security.UserPrincipal;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    private final PasswordEncoder passwordEncoder;
    private final MinioFolderService minioFolderService;
    private final UserPrincipalMapper userPrincipalMapper;

    @Override
    public @NonNull UserDetails loadUserByUsername(@NonNull String username) throws UsernameNotFoundException {
        return userRepository.findByUsername(username)
                .map(userPrincipalMapper::toUserPrincipal)
                .orElseThrow(() -> UsernameNotFoundException.fromUsername(username));
    }

    @Transactional
    public void save(UserDto userDto, @AuthenticationPrincipal UserPrincipal userDetails) {
        if (userRepository.existsUserByUsername(userDto.getUsername())) {
            throw new UserAlreadyExistsException("User with name " + userDto.getUsername() + " already exists");
        }
        User user = userPrincipalMapper.toUser(userDto);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);
        minioFolderService.createRootFolderForUserByUsername(userDetails);
    }
}
