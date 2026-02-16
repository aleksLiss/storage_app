package com.storage.app.controller;

import com.storage.app.dto.resource.request.*;
import com.storage.app.dto.resource.response.DownloadResourceDto;
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
    public ResponseEntity<?> getResource(@Valid
                                         @ModelAttribute FoundResourceDto foundResourceDto) throws Exception {
        LinkedHashMap<String, String> response = minioService.getResource(foundResourceDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @DeleteMapping("/resource")
    public ResponseEntity<?> deleteResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto) throws  Exception {
        minioService.deleteResource(foundResourceDto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/directory")
    public ResponseEntity<?> getResourceFromDirectory(@Valid @ModelAttribute FoundResourceDto foundResourceDto) throws Exception {
        List<LinkedHashMap<String, String>> response = minioService.getResourcesFromFolder(foundResourceDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/resource/move")
    public ResponseEntity<?> moveResource(@Valid @ModelAttribute MoveResourceDto moveResourceDto) throws Exception {
        Map<String, String> response = minioService.moveResource(moveResourceDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/resource/search")
    public ResponseEntity<?> searchResource(@Valid @ModelAttribute SearchResourceDto searchResourceDto) {
        List<LinkedHashMap<String, String>> response = minioService.searchResource(searchResourceDto);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }

    @GetMapping("/resource/download")
    public ResponseEntity<@NonNull StreamingResponseBody> downloadResource(@Valid @ModelAttribute FoundResourceDto foundResourceDto) throws  Exception {
        String path = foundResourceDto.getPath();
        if (path.endsWith("/")) {
            String fileName = path.substring(0, path.length() - 1).concat(".zip");
            String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");
            return ResponseEntity
                    .ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(minioService.downloadFolder(foundResourceDto));
        }
        DownloadResourceDto downloadResourceDto = minioService.downloadFile(foundResourceDto);
        StreamingResponseBody streamingResponseBody = downloadResourceDto.getStreamingResponseBody();
        String extensionFile = downloadResourceDto.getFileExtension();
        String fileName = downloadResourceDto.getFileName().concat(".").concat(extensionFile);
        String encodedFilename = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replaceAll("\\+", "%20");
        return ResponseEntity
                .ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedFilename + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(streamingResponseBody);
    }

    @PostMapping("/directory")
    public ResponseEntity<?> createFolder(@Valid
                                          @ModelAttribute
                                          CreateFolderDto folderDto) throws Exception {
        Map<String, String> response = minioService.createFolder(folderDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/resource")
    public ResponseEntity<?> createResource(@Valid
                                            @ModelAttribute UploadResourceDto uploadResourceDto,
                                            @RequestParam("file") MultipartFile[] file) throws Exception {
        List<LinkedHashMap<String, String>> response = minioService.uploadResource(file, uploadResourceDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
