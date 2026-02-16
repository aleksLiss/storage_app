package com.storage.app.service;

import com.storage.app.config.MinioConfig;
import com.storage.app.dto.resource.request.*;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.dto.resource.response.DownloadResourceDto;
import com.storage.app.exception.*;
import com.storage.app.mapper.*;
import com.storage.app.util.path.PathParser;
import com.storage.app.util.path.PathValidator;
import com.storage.app.util.resource.ResourceChecker;
import com.storage.app.util.resource.ResourceFinder;
import com.storage.app.util.resource.file.FileChecker;
import com.storage.app.util.resource.folder.FolderChecker;
import io.minio.*;
import io.minio.messages.Item;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Data
@Slf4j
public class MinioService {

    private final MinioConfig minioConfig;
    private final FolderChecker folderChecker;
    private final FileChecker fileChecker;
    private final PathParser pathParser;
    private final ResourceFinder resourceFinder;
    private final PathValidator pathValidator;
    private final AnswerResponseDtoMapper answerResponseDtoMapper;
    private final ResourceChecker resourceChecker;
    private static final String DIRECTORY = "DIRECTORY";
    private static final String FILE = "FILE";

    public List<LinkedHashMap<String, String>> getResourcesFromFolder(@Valid FoundResourceDto foundResourceDto) throws Exception {
        Iterable<Result<Item>> results = getListResource(false, foundResourceDto.getPath());
        if (!isDirectory(foundResourceDto.getPath()) || !results.iterator().hasNext()) {
            throw new FolderNotExistsException("Folder by name " + foundResourceDto.getPath() + " does not exist");
        }
        return answerResponseDtoMapper.getListAnswerResponseDtos(results, resourceFinder, foundResourceDto.getPath());
    }

    public List<LinkedHashMap<String, String>> uploadResource(MultipartFile[] files, @Valid UploadResourceDto uploadResourceDto) throws Exception {
        MinioClient minioClient = minioConfig.getMinioClient();
        List<LinkedHashMap<String, String>> response = new ArrayList<>();
        for (MultipartFile file : files) {
            fileChecker.fileIsEmpty(file);
            fileChecker.checkFileSize(file);
            fileChecker.fileExistsInDirectory(minioClient, file, uploadResourceDto.getPath());
            try (InputStream inputStream = file.getInputStream()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(uploadResourceDto.getPath() + file.getOriginalFilename())
                                .contentType(file.getContentType())
                                .stream(inputStream, file.getSize(), -1)
                                .build()
                );
            }
            response.add(
                    answerResponseDtoMapper.answerResponseDtoToMap(
                            new AnswerResponseDto(
                                    uploadResourceDto.getPath(),
                                    Objects.requireNonNull(file.getOriginalFilename()),
                                    String.valueOf(file.getSize()),
                                    file.getOriginalFilename().contains(".") ? FILE : DIRECTORY
                            )
                    )
            );
        }
        return response;
    }

    public List<LinkedHashMap<String, String>> searchResource(@Valid SearchResourceDto searchResourceDto) {
        Iterable<Result<Item>> results = getListResource(true, null);
        return answerResponseDtoMapper.hashsetSearchAnswerDtoToMap(
                pathParser.parsePath(results, searchResourceDto)
        );
    }

    public Map<String, String> moveResource(@Valid MoveResourceDto movedResourceDto) throws Exception {
        String resourceFrom = movedResourceDto.getFrom();
        String resourceTo = movedResourceDto.getTo();
        LinkedHashMap<String, String> response;
        pathValidator.validatePaths(resourceFrom, resourceTo);
        Iterable<Result<Item>> results = getListResource(true, resourceFrom);
        resourceChecker.checkResourceNotFound(results);
        Iterable<Result<Item>> results2 = getListResource(true, resourceTo);
        resourceChecker.checkResourceAlreadyExists(resourceTo, results2);
        boolean isRelocateResource = isRelocate(resourceFrom, resourceTo);
        boolean isRenameResource = isRename(resourceFrom, resourceTo);
        if (isRelocateResource && isRenameResource) {
            throw new IllegalArgumentException("Must be only rename or relocate");
        }
        response = answerResponseDtoMapper.answerResponseDtoToMap(
                changeResource(
                        resourceFrom, resourceTo
                )
        );
        return response;
    }

    public LinkedHashMap<String, String> getResource(@Valid FoundResourceDto foundResourceDto) throws Exception {
        String[] path = getSplitPath(foundResourceDto);
        Iterable<Result<Item>> results = checkCorrectPathToResource(path, foundResourceDto);
        return answerResponseDtoMapper
                .getListAnswerResponseDtos(results, resourceFinder, foundResourceDto.getPath())
                .getFirst();
    }

    public void deleteResource(@Valid FoundResourceDto foundResourceDto) throws Exception {
        MinioClient minioClient = minioConfig.getMinioClient();
        String[] path = getSplitPath(foundResourceDto);
        Iterable<Result<Item>> results = checkCorrectPathToResource(path, foundResourceDto);
        for (Result<Item> result : results) {
            Item item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(item.objectName())
                            .build());
        }
    }

    public StreamingResponseBody downloadFolder(@Valid FoundResourceDto foundResourceDto) {
        return outputStream -> {
            MinioClient minioClient = minioConfig.getMinioClient();
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                Iterable<Result<Item>> results = getListResource(true, foundResourceDto.getPath());
                for (Result<Item> result : results) {
                    Item item = result.get();
                    if (item.isDir()) continue;
                    try (InputStream is = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioConfig.getBucketName())
                                    .object(item.objectName())
                                    .build())) {
                        zos.putNextEntry(new ZipEntry(item.objectName()));
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        zos.closeEntry();
                    }
                }
                zos.finish();
            } catch (Exception e) {
                log.warn("Error downloading folder", e);
            }
        };
    }

    public DownloadResourceDto downloadFile(@Valid FoundResourceDto foundResourceDto) throws Exception {
        String[] path = getSplitPath(foundResourceDto);
        DownloadResourceDto downloadResourceDto = new DownloadResourceDto();
        Iterable<Result<Item>> results = checkCorrectPathToResource(path, foundResourceDto);
        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            String extension = objectName.split("\\.")[1];
            downloadResourceDto.setFileExtension(extension);
            downloadResourceDto.setFileName(objectName);
            break;
        }
        downloadResourceDto.setStreamingResponseBody(downloadFileAsStream(foundResourceDto));
        return downloadResourceDto;
    }

    private AnswerResponseDto changeResource(String resourceFrom, String resourceTo) throws Exception {
        Iterable<Result<Item>> results = getListResource(true, resourceFrom);
        AnswerResponseDto answerDto = new AnswerResponseDto();
        MinioClient client = minioConfig.getMinioClient();
        for (Result<Item> result : results) {
            Item item = result.get();
            String oldPath = item.objectName();
            String newPath;
            if (oldPath.equals(resourceFrom)) {
                if (resourceTo.endsWith("/")) {
                    String fileName = oldPath.substring(oldPath.lastIndexOf("/") + 1);
                    newPath = resourceTo + fileName;
                } else {
                    newPath = resourceTo;
                }
            } else {
                newPath = oldPath.replaceFirst(Pattern.quote(resourceFrom), resourceTo);
            }
            newPath = newPath.replace("//", "/");
            if (newPath.startsWith("/")) {
                newPath = newPath.substring(1);
            }
            answerDto.setPath(resourceFinder.getPathToResource(newPath));
            answerDto.setName(resourceFinder.getResourceName(newPath));
            if (resourceFinder.getResourceName(oldPath).contains(".")) {
                answerDto.setSize(String.valueOf(item.size()));
                answerDto.setType(FILE);
            } else {
                answerDto.setType(DIRECTORY);
            }
            client.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(newPath)
                            .source(
                                    CopySource.builder()
                                            .bucket(minioConfig.getBucketName())
                                            .object(oldPath)
                                            .build()
                            )
                            .build()
            );
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(oldPath)
                            .build()
            );
        }
        return answerDto;
    }

    public Map<String, String> createFolder(@Valid CreateFolderDto folderDto) throws Exception {
        MinioClient minioClient = minioConfig.getMinioClient();
        folderChecker.isFolderNotExist(minioClient, folderDto.getPath());
        folderChecker.isFolderExist(minioClient, folderDto.getPath());
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(minioConfig.getBucketName())
                        .object(folderDto.getPath())
                        .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                        .build()
        );
        AnswerResponseDto answerResponseDto = new AnswerResponseDto();
        String path = resourceFinder.getPathToResource(folderDto.getPath());
        String name = resourceFinder.getResourceName(folderDto.getPath());
        answerResponseDto.setPath(path);
        answerResponseDto.setName(name);
        answerResponseDto.setType(DIRECTORY);
        return answerResponseDtoMapper.answerResponseDtoToMap(answerResponseDto);
    }

    private boolean isRename(String pathFromStr, String pathToStr) {
        Map<String, String> parentsAndNamesResources = getParentsAndNamesResources(pathFromStr, pathToStr);
        return !parentsAndNamesResources.get("nameFrom").equals(parentsAndNamesResources.get("nameTo"))
                && parentsAndNamesResources.get("parentFrom").equals(parentsAndNamesResources.get("parentTo"));
    }

    private boolean isRelocate(String pathFromStr, String pathToStr) {
        Map<String, String> parentsAndNamesResources = getParentsAndNamesResources(pathFromStr, pathToStr);
        return parentsAndNamesResources.get("nameFrom").equals(parentsAndNamesResources.get("nameTo"))
                || !parentsAndNamesResources.get("parentFrom").equals(parentsAndNamesResources.get("parentTo"));
    }

    private Map<String, String> getParentsAndNamesResources(String pathToResourceFrom, String pathToResourceTo) {
        Path from = Paths.get(pathToResourceFrom).normalize();
        Path to = Paths.get(pathToResourceTo).normalize();
        String nameFrom = from.getFileName().toString();
        String nameTo = to.getFileName().toString();
        String parentFrom = from.getParent().toString();
        String parentTo = to.getParent().toString();
        return Map.of(
                "nameFrom", nameFrom,
                "nameTo", nameTo,
                "parentFrom", parentFrom,
                "parentTo", parentTo
        );
    }

    private Iterable<Result<Item>> checkCorrectPathToResource(String[] path, FoundResourceDto foundResourceDto) {
        boolean isDirectory = isDirectory(foundResourceDto.getPath());
        Iterable<Result<Item>> results;
        if (path.length == 1) {
            results = getListResource(false, foundResourceDto.getPath());
            if (isDirectory && !results.iterator().hasNext()) {
                throw new FolderNotExistsException("Folder by name " + foundResourceDto.getPath() + " not exists");
            }
            if (!isDirectory && !results.iterator().hasNext()) {
                throw new FileNotExistsException("File by name " + foundResourceDto.getPath() + " not exists");
            }
        } else {
            StringBuilder pathBuilder = new StringBuilder();
            for (int i = 0; i < path.length - 1; i++) {
                pathBuilder.append(path[i]);
            }
            results = getListResource(false, pathBuilder.toString());
            resourceChecker.checkResourceNotFound(results);
        }
        return results;
    }

    private boolean isDirectory(String path) {
        return path.endsWith("/");
    }

    private StreamingResponseBody downloadFileAsStream(@Valid FoundResourceDto foundResourceDto) {
        String[] path = getSplitPath(foundResourceDto);
        return outputStream -> {
            MinioClient minioClient = minioConfig.getMinioClient();
            Iterable<Result<Item>> results = checkCorrectPathToResource(path, foundResourceDto);
            try {
                for (Result<Item> result : results) {
                    Item item = result.get();
                    try (InputStream inputStream = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioConfig.getBucketName())
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
                log.warn("Error downloading file", ex);
            }
        };
    }

    private static String[] getSplitPath(FoundResourceDto foundResourceDto) {
        return foundResourceDto.getPath().split("/");
    }

    private Iterable<Result<Item>> getListResource(boolean isRecursive, String resource) {
        MinioClient minioClient = minioConfig.getMinioClient();
        Iterable<Result<Item>> results;
        if (resource == null) {
            results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .recursive(true)
                            .build()
            );
        } else {
            results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .recursive(isRecursive)
                            .prefix(resource)
                            .build());
        }
        return results;
    }
}