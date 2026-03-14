package com.storage.app.controller;

import com.storage.app.dto.authenticate.JwtRequest;
import com.storage.app.dto.user.UserDto;
import com.storage.app.service.MinioService;
import com.storage.app.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authenticate-controller", description = "AUTHENTICATE API")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserService userService;
    private final MinioService minioService;
    private final SecurityContextRepository securityContextRepository;

    @Operation(summary = "Sign-up user")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"username\":\"max@google\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid username",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"message\":\"Incorrect username\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Username already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"message\":\"Username already exists\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"message\":\"Internal server error\"}")
                    )
            ),
    })
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody UserDto userDto) {
        userService.save(userDto);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Map.of("username", userDto.getUsername()));
    }

    @Operation(summary = "Sign-in user by login and password")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"username\":\"max@google\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect username or password",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"message\":\"Password must be not empty\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Username was not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"message\":\"Username was not found\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject("{\"message\":\"Internal server error\"}")
                    )
            )
    })
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody JwtRequest jwtRequest,
                                    HttpServletRequest request,
                                    HttpServletResponse response) {
        String username = jwtRequest.username();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(username, jwtRequest.password())
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        securityContextRepository.saveContext(context, request, response);
        return ResponseEntity.ok(Map.of("username", username));
    }
}