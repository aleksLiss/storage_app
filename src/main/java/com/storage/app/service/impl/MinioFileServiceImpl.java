package com.storage.app.service.impl;

import com.storage.app.config.MinioProperties;
import com.storage.app.dto.resource.request.FoundResourceDto;
import com.storage.app.dto.resource.request.UploadResourceDto;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.exception.resource.ResourceNotFoundException;
import com.storage.app.exception.resource.ResourceUploadException;
import com.storage.app.exception.resource.file.FileDownloadException;
import com.storage.app.mapper.AnswerResponseDtoMapper;
import com.storage.app.security.UserPrincipal;
import com.storage.app.service.MinioFileService;
import com.storage.app.util.file.FileChecker;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.*;
import io.minio.messages.Item;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFileServiceImpl implements MinioFileService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final AnswerResponseDtoMapper answerResponseDtoMapper;
    private static final String DIRECTORY = "DIRECTORY";
    private static final String FILE = "FILE";
    private static final String ROOT_FOLDER = "user-%s-files/";

    @Override
    public StreamingResponseBody downloadFile(FoundResourceDto foundResourceDto, UserPrincipal userDetails) {
        return outputStream -> {
            String root = createPathToRootFolder(userDetails);
            String rawPath = foundResourceDto.getPath();
            String fullPath = rawPath.startsWith(root) ? rawPath : root + rawPath;
            Iterable<Result<Item>> results =
                    getListResource(true, fullPath);
            try {
                for (Result<Item> result : results) {
                    Item item = result.get();
                    try (InputStream inputStream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioProperties.bucketName())
                                    .object(item.objectName())
                                    .build()
                    )) {
                        byte[] bytes = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = inputStream.read(bytes)) != -1) {
                            outputStream.write(bytes, 0, bytesRead);
                        }
                        outputStream.flush();
                        break;
                    }
                }
            } catch (Exception ex) {
                String msg = "Exception during download file";
                log.warn(msg);
                throw new FileDownloadException(msg);
            }
        };
    }

    @Override
    public List<@NonNull AnswerResponseDto> uploadResource(UploadResourceDto uploadResourceDto,
                                                           MultipartFile[] files,
                                                           UserPrincipal userDetails) {
        List<AnswerResponseDto> response = new ArrayList<>();
        String rootFolder = String.format(ROOT_FOLDER, userDetails.getUserId());
        for (MultipartFile file : files) {
            FileChecker.checkFileSize(file, minioProperties.maxSizeFile());
            String checkPathForUploadResource = createPathToRootFolder(userDetails)
                    + uploadResourceDto.getPath();
            FileChecker.fileExistsInDirectory(minioClient, file, checkPathForUploadResource, minioProperties.bucketName());
            try {
                try (InputStream inputStream = file.getInputStream()) {
                    String pathToResource =
                            createPathToResource(
                                    rootFolder,
                                    uploadResourceDto.getPath(),
                                    file.getOriginalFilename());
                    if (pathToResource.endsWith("/")) {
                        pathToResource = pathToResource.substring(0, pathToResource.length() - 1);
                    }
                    minioClient.putObject(
                            PutObjectArgs.builder()
                                    .bucket(minioProperties.bucketName())
                                    .object(pathToResource)
                                    .contentType(file.getContentType())
                                    .stream(inputStream, file.getSize(), -1)
                                    .build()
                    );
                    response.add(
                            new AnswerResponseDto(
                                    uploadResourceDto.getPath(),
                                    Objects.requireNonNull(file.getOriginalFilename()),
                                    file.getSize(),
                                    file.getOriginalFilename().contains(".") ? FILE : DIRECTORY
                            )
                    );
                }
            } catch (Exception ex) {
                throw new ResourceUploadException("Exception during upload resource");
            }
        }
        return response;
    }

    @Override
    public AnswerResponseDto getResource(FoundResourceDto foundResourceDto, UserPrincipal userDetails) {
        String root = createPathToRootFolder(userDetails);
        String rawPath = foundResourceDto.getPath();
        String fullPath = rawPath.startsWith(root) ? rawPath : root + rawPath;
        Iterable<Result<Item>> results = checkCorrectPathToResource(false, new FoundResourceDto(fullPath));
        List<AnswerResponseDto> allFound = answerResponseDtoMapper
                .getListAnswerResponseDtos(results, foundResourceDto.getPath(), userDetails);
        if (allFound.isEmpty()) {
            throw new ResourceNotFoundException("Resource not found");
        }
        return allFound.stream()
                .filter(answerResponseDto -> foundResourceDto.getPath().equals(answerResponseDto.getPath()))
                .findFirst()
                .orElse(allFound.getFirst());
    }

    private List<Result<Item>> checkCorrectPathToResource(boolean isRecursive, FoundResourceDto findResource) {
        Iterable<Result<Item>> iterable = getListResource(isRecursive, findResource.getPath());
        List<Result<Item>> results = new ArrayList<>();
        iterable.forEach(results::add);
        return results;
    }

    private Iterable<Result<Item>> getListResource(boolean isRecursive, String resource) {
        Iterable<Result<Item>> results = Collections.emptyList();
        if (resource != null) {
            results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.bucketName())
                            .recursive(isRecursive)
                            .prefix(resource)
                            .build());
        }
        return results;
    }

    private String createPathToRootFolder(UserPrincipal userDetails) {
        return String.format(ROOT_FOLDER, userDetails.getUserId());
    }

    private String createPathToResource(String rootFolder, String... resources) {
        return rootFolder + String.join("/", resources);
    }
}
