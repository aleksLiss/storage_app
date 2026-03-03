package com.storage.app.controller;

import com.storage.app.dto.resource.request.*;
import com.storage.app.service.MinioService;
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
public class ResourceController {

    private final MinioService minioService;

    @GetMapping("/resource")
    public ResponseEntity<?> getResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto) {
        LinkedHashMap<String, String> response = minioService.getResource(foundResourceDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/resource")
    public ResponseEntity<?> deleteResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto) {
        minioService.deleteResource(foundResourceDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/resource")
    public ResponseEntity<?> uploadResource(@ModelAttribute UploadResourceDto uploadResourceDto,
                                            @RequestParam("object") MultipartFile[] files,
                                            Principal principal) {
        List<LinkedHashMap<String, String>> response =
                minioService.uploadResource(files, uploadResourceDto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/directory")
    public ResponseEntity<?> createFolder(Principal principal,
                                          @Valid @ModelAttribute CreateFolderDto folderDto) {
        Map<String, String> response = minioService.createFolder(folderDto, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/directory")
    public ResponseEntity<?> getResourceFromDirectory(Principal principal,
                                                      @Valid @ModelAttribute FoundResourceDto foundResourceDto) {
        List<LinkedHashMap<String, String>> response = minioService.getResourcesFromFolder(foundResourceDto, principal);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }


    @GetMapping("/resource/move")
    public ResponseEntity<?> moveResource(@Valid @ModelAttribute MoveResourceDto moveResourceDto,
                                          Principal principal) {
        Map<String, String> response = minioService.moveResource(moveResourceDto, principal);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/resource/search")
    public ResponseEntity<?> searchResource(@Valid @ModelAttribute SearchResourceDto searchResourceDto) {
        List<LinkedHashMap<String, String>> response = minioService.searchResource(searchResourceDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/resource/download")
    public ResponseEntity<@NonNull StreamingResponseBody> downloadResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto) {
        String path = foundResourceDto.getPath();
        String fileName = path.endsWith("/")
                ? path.substring(0, path.length() - 1).concat(".zip")
                : path.substring(0, path.length() - 1);
        String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        if (path.endsWith("/")) {
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(minioService.downloadFolder(foundResourceDto));
        }
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(minioService.downloadFile(foundResourceDto));
    }
}
