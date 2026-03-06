package com.storage.app.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.storage.app.dto.authenticate.JwtRequest;
import com.storage.app.dto.user.UserDto;
import com.storage.app.repository.UserRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Testcontainers
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SIGN_IN_URL = "/api/auth/sign-in";
    private static final String SIGN_UP_URL = "/api/auth/sign-up";
    private static final String USER_ME_URL = "/api/user/me";

    @Container
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer("postgres:16");

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer("redis:latest")
                    .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minioContainer =
            new GenericContainer("minio/minio:latest")
                    .withCommand("server /data")
                    .withEnv("MINIO_ROOT_USERNAME", "minioadmin")
                    .withEnv("MINIO_ROOT_PASSWORD", "minioadmin")
                    .withExposedPorts(9000);

    @DynamicPropertySource
    static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgreSQLContainer::getJdbcUrl);
        registry.add("spring.datasource.username", postgreSQLContainer::getUsername);
        registry.add("spring.datasource.password", postgreSQLContainer::getPassword);
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
    void whenSignInUserAndGetMeThenReturnUsername() throws Exception {
        UserDto userDto = new UserDto();
        userDto.setUsername("user@mail.com");
        userDto.setPassword("password");
        JwtRequest jwtRequest = new JwtRequest(userDto.getUsername(), userDto.getPassword());
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user@mail.com"));
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        MvcResult signInResult = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jwtRequest)))
                .andExpect(status().isOk()).andReturn();
        HttpSession session = signInResult.getRequest().getSession();
        assertThat(session).isNotNull();

        Cookie sessionCookie = signInResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();

        mockMvc.perform(get(USER_ME_URL)
                        .cookie(sessionCookie))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(userDto.getUsername()));
    }

    @Test
    void whenDontSignInUserAndGetMeThenThrowUnauthorized() throws Exception {
        MockCookie sessionCookie = new MockCookie("SESSION", null);
        mockMvc.perform(get(USER_ME_URL)
                        .cookie(sessionCookie))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }
}