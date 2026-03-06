package com.storage.app.controller;

import com.storage.app.dto.resource.request.*;
import com.storage.app.service.MinioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@Slf4j
@RequiredArgsConstructor
@Tag(name = "Resource-controller", description = "RESOURCE API")
public class ResourceController {

    private final MinioService minioService;

    @Operation(summary = "Get resource by path")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"path\":\"folder1/folder2/\"," +
                                    "\"name\":\"file.txt\"," +
                                    "\"size\":\"321\"," +
                                    "\"type\":\"FILE\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect path",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Incorrect path to resource\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource not found\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            )
    })
    @GetMapping("/resource")
    public ResponseEntity<?> getResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto,
                                         Principal principal) {
        LinkedHashMap<String, String> response = minioService.getResource(foundResourceDto, principal);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Delete resource")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "204"
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect path",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Incorrect path to resource\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource was not found\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            ),
    })
    @DeleteMapping("/resource")
    public ResponseEntity<?> deleteResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto,
                                            Principal principal) {
        minioService.deleteResource(foundResourceDto, principal);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @Operation(summary = "Upload resource or resources in input path")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                            [
                                            {
                                            \"path\":\"folder1/folder2/\",
                                            \"name\":\"file.txt\",
                                            \"size\":\"123\",
                                            \"type\":\"FILE\"}]
                                            """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid body of request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Invalid body of request\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resource already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource already exists\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            )
    })
    @PostMapping("/resource")
    public ResponseEntity<?> uploadResource(@ModelAttribute UploadResourceDto uploadResourceDto,
                                            @RequestParam("object") MultipartFile[] files,
                                            Principal principal) {
        List<LinkedHashMap<String, String>> response =
                minioService.uploadResource(files, uploadResourceDto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Create folder by input path")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Created",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"path\":\"folder1/folder2/\",\"name\":\"folder3\",\"type\":\"DIRECTORY\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect path",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"\":\"\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Root folder not found\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resource already exist",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource already exist\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            ),
    })
    @PostMapping("/directory")
    public ResponseEntity<?> createFolder(Principal principal,
                                          @Valid @ModelAttribute CreateFolderDto folderDto) {
        Map<String, String> response = minioService.createFolder(folderDto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "Get resources from input path to directory")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "[{\"path\":\"folder1/folder2/\"," +
                                            "\"name\":\"file.txt\"," +
                                            "\"size\":\"321\"," +
                                            "\"type\":\"FILE\"}]")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect path",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Incorrect path to resource\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource not found\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            )
    })
    @GetMapping("/directory")
    public ResponseEntity<?> getResourceFromDirectory(Principal principal,
                                                      @Valid @ModelAttribute FoundResourceDto foundResourceDto) {
        List<LinkedHashMap<String, String>> response = minioService.getResourcesFromFolder(foundResourceDto, principal);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Relocate or rename resource")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "{\"path\":\"folder1/folder2/\"," +
                                    "\"name\":\"file.txt\"," +
                                    "\"size\":\"321\"," +
                                    "\"type\":\"FILE\"}")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect path",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Incorrect path to resource\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource not found\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Resource already exists",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource already exists\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            )
    })
    @GetMapping("/resource/move")
    public ResponseEntity<?> moveResource(@Valid @ModelAttribute MoveResourceDto moveResourceDto,
                                          Principal principal) {
        Map<String, String> response = minioService.moveResource(moveResourceDto, principal);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Search resource by input name")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(value = "[{\"path\":\"folder1/folder2/\"," +
                                    "\"name\":\"file.txt\"," +
                                    "\"size\":\"321\"," +
                                    "\"type\":\"FILE\"}]")
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect path",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Incorrect path to resource\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            )
    })
    @GetMapping("/resource/search")
    public ResponseEntity<?> searchResource(@Valid @ModelAttribute SearchResourceDto searchResourceDto, Principal principal) {
        List<LinkedHashMap<String, String>> response = minioService.searchResource(searchResourceDto, principal);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @Operation(summary = "Download folder in 'folder.zip' or file")
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/octet-stream")
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Incorrect path",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Incorrect path to resource\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Unauthorized user",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Unauthorized user\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Resource not found",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Resource not found\"}"
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal error",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = "{\"message\":\"Internal server error\"}"
                            )
                    )
            )
    })
    @GetMapping("/resource/download")
    public ResponseEntity<@NonNull StreamingResponseBody> downloadResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto,
                                                                           Principal principal) {
        String path = foundResourceDto.getPath();
        String fileName = path.endsWith("/")
                ? path.substring(0, path.length() - 1).concat(".zip")
                : path.substring(0, path.length() - 1);
        String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        minioService.checkResourceExists(foundResourceDto, principal);
        if (path.endsWith("/")) {
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(minioService.downloadFolder(foundResourceDto, principal));
        }
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(minioService.downloadFile(foundResourceDto, principal));
    }
}
