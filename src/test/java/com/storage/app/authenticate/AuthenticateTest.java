package com.storage.app.authenticate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.app.dto.authenticate.SignInRequest;
import com.storage.app.dto.user.UserDto;
import com.storage.app.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
public class AuthenticateTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SIGN_UP_URL = "/api/auth/sign-up";
    private static final String SIGN_IN_URL = "/api/auth/sign-in";
    private static final String SIGN_OUT_URL = "/api/auth/sign-out";

    @Container
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer<>(DockerImageName.parse("redis:latest"))
                    .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minioContainer =
            new GenericContainer<>(DockerImageName.parse("minio/minio:latest"))
                    .withCommand("server /data")
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withExposedPorts(9000);

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
        registry.add("spring.jpa.generate-ddl", () -> true);

        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));

        registry.add("spring.minio.endpoint",
                () -> String.format("http://%s:%d",
                        minioContainer.getHost(),
                        minioContainer.getMappedPort(9000)));
        registry.add("spring.minio.credentials.login", () -> "minioadmin");
        registry.add("spring.minio.credentials.password", () -> "minioadmin");
    }

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void whenCorrectUsernameThenSaveUser() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setUsername("user@mail.com");
        userDto.setPassword("password");
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user@mail.com"));
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
    }

    @Test
    void whenIncorrectUsernameThenTrowException() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setUsername("usernameWithoutEmail");
        userDto.setPassword("password");
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void whenSaveUserThenSignIn() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setUsername("user@mail.com");
        userDto.setPassword("password");
        SignInRequest signInRequest = new SignInRequest(userDto.getUsername(), userDto.getPassword());
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user@mail.com"));
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        MvcResult signInResult = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk()).andReturn();
        HttpSession session = signInResult.getRequest().getSession();
        assertThat(session).isNotNull();
        mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userDto.getUsername()));
    }

    @Test
    void whenDontSaveUserThenNotSignIn() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setUsername("user@mail.com");
        userDto.setPassword("password");
        SignInRequest signInRequest = new SignInRequest(userDto.getUsername(), userDto.getPassword());
        MvcResult signInResult = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized()).andReturn();
        HttpSession session = signInResult.getRequest().getSession(false);
        assertThat(session).isNull();
        mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenSignOutUserThenReturnNoContent() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setUsername("user@mail.com");
        userDto.setPassword("password");
        SignInRequest signInRequest = new SignInRequest(userDto.getUsername(), userDto.getPassword());
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user@mail.com"));
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        MvcResult signInResult = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk()).andReturn();
        HttpSession session = signInResult.getRequest().getSession();
        assertThat(session).isNotNull();
        mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(signInRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userDto.getUsername()));
        mockMvc.perform(post(SIGN_OUT_URL)
                        .session((MockHttpSession) session))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/user/me")
                        .session((MockHttpSession) session))
                .andExpect(status().isUnauthorized());
    }
}
