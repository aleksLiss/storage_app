package com.storage.app.minio;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class MinioServiceTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private UserDto userDto;
    private UserDto userDto2;
    private JwtRequest jwtRequest;
    private JwtRequest jwtRequest2;

    private static final String rootFolder = "user-%s-files/";
    private static final String FOLDER_URL = "/api/directory";
    private static final String RESOURCE_URL = "/api/resource";
    private static final String SEARCH_RESOURCE_URL = "/api/resource/search";
    private static final String MOVE_OR_RENAME_RESOURCE_URL = "/api/resource/move";
    private static final String DOWNLOAD_RESOURCE_URL = "/api/resource/download";
    private static final String SIGN_UP_URL = "/api/auth/sign-up";
    private static final String SIGN_IN_URL = "/api/auth/sign-in";
    private static final String FILE = "file";
    private static final String DIRECTORY = "data";

    @Container
    static PostgreSQLContainer postgreSQLContainer =
            new PostgreSQLContainer(DockerImageName.parse("postgres:16"));

    @Container
    static GenericContainer<?> redisContainer =
            new GenericContainer(DockerImageName.parse("redis:latest"))
                    .withExposedPorts(6379);

    @Container
    static GenericContainer<?> minioContainer =
            new GenericContainer(DockerImageName.parse("minio/minio:latest"))
                    .withCommand("server /data")
                    .withEnv("MINIO_ROOT_USER", "minioadmin")
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
                () -> String.format(
                        "http:%s:%d",
                        minioContainer.getHost(),
                        minioContainer.getMappedPort(9000)));
        registry.add("spring.minio.credentials.login", () -> "minioadmin");
        registry.add("spring.minio.credentials.password", () -> "minioadmin");
    }

    @BeforeEach
    void setUp() throws Exception {
        userRepository.deleteAll();
        userDto = new UserDto();
        userDto.setUsername("user@mail.com");
        userDto.setPassword("password");
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user@mail.com"));
        jwtRequest = new JwtRequest(userDto.getUsername(), userDto.getPassword());
        userDto2 = new UserDto();
        userDto2.setUsername("user2@mail.com");
        userDto2.setPassword("password");
        mockMvc.perform(post(SIGN_UP_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("user2@mail.com"));
        jwtRequest2 = new JwtRequest(userDto2.getUsername(), userDto2.getPassword());

    }

    // ===== CREATE FOLDER =====
    // 401
    @Test
    void whenCreateFolderAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        mockMvc.perform(post(FOLDER_URL)
                        .param("path", "data")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenCreateFolderThenOk() throws Exception {
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
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());
        mockMvc.perform(post(FOLDER_URL)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(rootFolderByUserId))
                .andExpect(jsonPath("$.name").value("data"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
    }

    @Test
    void whenCreateFolderAndFolderExistsThenReturnBadRequest() throws Exception {
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
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());
        mockMvc.perform(post(FOLDER_URL)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.path").value(rootFolderByUserId))
                .andExpect(jsonPath("$.name").value("data"))
                .andExpect(jsonPath("$.type").value("DIRECTORY"));
        mockMvc.perform(post(FOLDER_URL)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isConflict());
    }

    // ===== UPLOAD =====
    @Test
    void whenUploadResourceAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        MockMultipartFile multipartFile = new MockMultipartFile(FILE, FILE.getBytes());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .param("path", DIRECTORY)
                        .principal(() -> "test-user"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenUploadResourceAndThenReturnCreated() throws Exception {
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
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", DIRECTORY)
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
    }

    @Test
    void whenUploadResourceAndResourceExistsThenReturnConflict() throws Exception {
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
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", "data/")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", "data/")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    // ===== SEARCH =====
    @Test
    void whenSearchResourceAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        mockMvc.perform(get(SEARCH_RESOURCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void whenSearchResourceAndNotUploadResourceThenReturnEmptyList() throws Exception {
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
        mockMvc.perform(get(SEARCH_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("query", FILE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void whenSearchResourceAndUploadResourceThenReturnListWithUploadedResource() throws Exception {
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
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", DIRECTORY)
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(SEARCH_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("query", "test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].path").value(DIRECTORY + "/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(multipartFile.getSize()))
                .andExpect(jsonPath("$[0].type").value("FILE"));
    }

    @Test
    void whenSearchResourceAndUploadResourceTwoDifferentUserThenReturnListTwoDifferentUser() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        boolean isExists2 = userRepository.existsUserByUsername(userDto2.getUsername());
        assertThat(isExists).isTrue();
        assertThat(isExists2).isTrue();
        MvcResult signInResult = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jwtRequest)))
                .andExpect(status().isOk()).andReturn();
        HttpSession session = signInResult.getRequest().getSession();
        assertThat(session).isNotNull();
        Cookie sessionCookie = signInResult.getResponse().getCookie("SESSION");
        assertThat(sessionCookie).isNotNull();
        MvcResult signInResult2 = mockMvc.perform(post(SIGN_IN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(jwtRequest2)))
                .andExpect(status().isOk()).andReturn();
        HttpSession session2 = signInResult2.getRequest().getSession();
        assertThat(session2).isNotNull();
        Cookie sessionCookie2 = signInResult2.getResponse().getCookie("SESSION");
        assertThat(sessionCookie2).isNotNull();
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", DIRECTORY)
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(SEARCH_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("query", "test")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].path").value(DIRECTORY + "/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].size").value(multipartFile.getSize()))
                .andExpect(jsonPath("$[0].type").value("FILE"));
        mockMvc.perform(get(SEARCH_RESOURCE_URL)
                        .cookie(sessionCookie2)
                        .param("query", "test")
                        .principal(new UsernamePasswordAuthenticationToken("user2@mail.com", null))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

    // ===== GET =====
    // 401
    @Test
    void whenGetResourceAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        mockMvc.perform(get(RESOURCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isUnauthorized());
    }

    // 404
    @Test
    void whenGetResourceAndNotUploadResourceThenReturnNotFound() throws Exception {
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
        mockMvc.perform(get(RESOURCE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .cookie(sessionCookie)
                        .content(objectMapper.writeValueAsString(userDto))
                        .param("path", "random/path"))
                .andExpect(status().isNotFound());
    }

    // 200
    @Test
    void whenGetResourceThenReturnUploadedResource() throws Exception {
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
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("path", rootFolderByUserId + "data/test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value(rootFolderByUserId + "data/"))
                .andExpect(jsonPath("$.name").value("test.txt"))
                .andExpect(jsonPath("$.type").value("FILE"));

    }

    // === GET DIRECTORY RESOURCES ===
    // 401
    @Test
    void whenGetResourcesFromDirectoryAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();

        mockMvc.perform(get(FOLDER_URL)
                        .param("path", "data/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isUnauthorized());
    }

    // 404
    @Test
    void whenGetResourceFromDirectoryAndNotCreateFolderThenReturnNotFound() throws Exception {
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
        mockMvc.perform(get(FOLDER_URL)
                        .cookie(sessionCookie)
                        .param("path", "data/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andDo(print())
                .andExpect(status().isNotFound());
    }

    // 200
    @Test
    void whenUploadResourceThenReturnListFromDirectoryWithUploadedFile() throws Exception {
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
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(FOLDER_URL)
                        .cookie(sessionCookie)
                        .param("path", "data/")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].path").value(rootFolderByUserId + "data/"))
                .andExpect(jsonPath("$[0].name").value("test.txt"))
                .andExpect(jsonPath("$[0].type").value("FILE"));

    }

    // ==== RENAME AND RELOCATE ===
    // 401
    @Test
    void whenRenameResourceAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        mockMvc.perform(get(MOVE_OR_RENAME_RESOURCE_URL)
                        .param("from", "test.txt")
                        .param("to", "test2.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isUnauthorized());
    }

    // 404
    @Test
    void whenRenameResourceAndResourceNotFoundThenReturnNotFound() throws Exception {
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
        MockMultipartFile multipartFile = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFile)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(MOVE_OR_RENAME_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("from", "newtest.txt")
                        .param("to", "test2.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isNotFound());
    }

    // 409
    @Test
    void whenRenameResourceAndResourceAlreadyExistThenReturnConflict() throws Exception {
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
        MockMultipartFile multipartFileFrom = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        MockMultipartFile multipartFileTo = new MockMultipartFile(
                "object",
                "test2.txt",
                "text/plain",
                "hello2".getBytes()
        );
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFileFrom)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFileTo)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(MOVE_OR_RENAME_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("from", rootFolderByUserId + "data/test.txt")
                        .param("to", rootFolderByUserId + "data/test2.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andDo(print())
                .andExpect(status().isConflict());
    }

    // 200
    @Test
    void whenRenameResourceThenReturnOk() throws Exception {
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
        MockMultipartFile multipartFileFrom = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        MockMultipartFile multipartFileTo = new MockMultipartFile(
                "object",
                "test2.txt",
                "text/plain",
                "hello2".getBytes()
        );
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());

        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFileFrom)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFileTo)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(MOVE_OR_RENAME_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("from", rootFolderByUserId + "data/test.txt")
                        .param("to", rootFolderByUserId + "data/newTest.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ==== DOWNLOAD ====
    // 401
    @Test
    void whenDownloadResourceAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        mockMvc.perform(get(DOWNLOAD_RESOURCE_URL)
                        .param("path", "test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isUnauthorized());
    }

    // 404
    @Test
    void whenDownloadResourceAndResourceNotFoundThenReturnNotFound() throws Exception {
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
        mockMvc.perform(get(DOWNLOAD_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("path", "test2.txt")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isNotFound());
    }

    // 200
    @Test
    void whenDownloadResourceAndThenOk() throws Exception {
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
        MockMultipartFile multipartFileFrom = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFileFrom)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(get(DOWNLOAD_RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("path", rootFolderByUserId + "data/test.txt")
                        .contentType(MediaType.APPLICATION_OCTET_STREAM)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andDo(print())
                .andExpect(status().isOk());
    }

    // ==== DELETE ====
    // 401
    @Test
    void whenDeleteResourceAndNotSignInThenReturnUnauthorized() throws Exception {
        boolean isExists = userRepository.existsUserByUsername(userDto.getUsername());
        assertThat(isExists).isTrue();
        mockMvc.perform(delete(RESOURCE_URL)
                        .param("path", "test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isUnauthorized());
    }

    // 404
    @Test
    void whenDeleteResourceAndResourceNotFoundThenReturnNotFound() throws Exception {
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
        mockMvc.perform(delete(RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("path", "test2.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isNotFound());
    }

    // 200
    @Test
    void whenDeleteResourceAndOkThenReturnNoContent() throws Exception {
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
        MockMultipartFile multipartFileFrom = new MockMultipartFile(
                "object",
                "test.txt",
                "text/plain",
                "hello".getBytes()
        );
        UUID userId = userRepository.findByUsername(userDto.getUsername()).get().getUuid();
        String rootFolderByUserId = String.format(rootFolder, userId.toString());
        mockMvc.perform(multipart(RESOURCE_URL)
                        .file(multipartFileFrom)
                        .cookie(sessionCookie)
                        .param("path", "data")
                        .principal(new UsernamePasswordAuthenticationToken("user@mail.com", null)))
                .andDo(print())
                .andExpect(status().isCreated());
        mockMvc.perform(delete(RESOURCE_URL)
                        .cookie(sessionCookie)
                        .param("path", rootFolderByUserId + "data/test.txt")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isNoContent());
    }
}
