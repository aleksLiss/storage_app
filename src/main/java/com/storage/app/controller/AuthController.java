package com.storage.app.controller;

import com.storage.app.dto.authenticate.JwtRequest;
import com.storage.app.dto.user.UserDto;
import com.storage.app.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final SecurityContextRepository securityContextRepository = new DelegatingSecurityContextRepository(
            new RequestAttributeSecurityContextRepository(),
            new HttpSessionSecurityContextRepository()
    );

    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody UserDto userDto) {
        userService.save(userDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("username", userDto.getUsername()));
    }

    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody JwtRequest jwtRequest,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(jwtRequest.username(), jwtRequest.password())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);
        return ResponseEntity.ok(Map.of("username", jwtRequest.username()));
    }

    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(HttpSession session) {
        session.invalidate();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}


