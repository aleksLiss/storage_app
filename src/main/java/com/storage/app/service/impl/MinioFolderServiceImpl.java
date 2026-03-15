package com.storage.app.service.impl;

import com.storage.app.config.MinioProperties;
import com.storage.app.dto.resource.request.CreateFolderDto;
import com.storage.app.dto.resource.request.FoundResourceDto;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.exception.resource.ResourceAlreadyExistsException;
import com.storage.app.exception.resource.folder.FolderCreateException;
import com.storage.app.exception.resource.folder.FolderDownloadException;
import com.storage.app.mapper.AnswerResponseDtoMapper;
import com.storage.app.security.UserPrincipal;
import com.storage.app.service.MinioFolderService;
import com.storage.app.util.resource.ResourceChecker;
import io.minio.*;
import io.minio.messages.Item;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class MinioFolderServiceImpl implements MinioFolderService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final AnswerResponseDtoMapper answerResponseDtoMapper;
    private static final String ROOT_FOLDER = "user-%s-files/";

    @Override
    public StreamingResponseBody downloadFolder(FoundResourceDto foundResourceDto, UserPrincipal  userDetails) {
        return outputStream -> {
            String root = createPathToRootFolder(userDetails);
            String rawPath = foundResourceDto.getPath();
            String fullPath = rawPath.startsWith(root) ? rawPath : root + rawPath;
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                String pathToResource = foundResourceDto.getPath();
                if (!pathToResource.endsWith("/")) {
                    pathToResource += "/";
                }
                Iterable<Result<Item>> results =
                        getListResource(true, foundResourceDto.getPath());
                for (Result<Item> result : results) {
                    Item item = result.get();
                    if (item.isDir()) continue;
                    if (fullPath.equals(pathToResource)) continue;
                    String entryName = fullPath.startsWith(pathToResource)
                            ? fullPath.substring(pathToResource.length())
                            : fullPath;

                    if (entryName.isEmpty()) continue;
                    try (InputStream is = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioProperties.bucketName())
                                    .object(fullPath)
                                    .build())) {
                        zos.putNextEntry(new ZipEntry(entryName));
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
                String msg = "Exception during download folder";
                log.warn(msg);
                throw new FolderDownloadException(msg);
            }
        };
    }

    @Override
    public List<@NonNull AnswerResponseDto> getResourceFromDirectory(UserPrincipal  userDetails, FoundResourceDto foundResourceDto) {
        String rootFolder = createPathToRootFolder(userDetails);
        String requestPath = foundResourceDto.getPath();
        String findFolder = (requestPath == null || requestPath.isEmpty() || requestPath.equals(rootFolder))
                ? rootFolder
                : createPathToResource(rootFolder, requestPath);
        Iterable<Result<Item>> results =
                checkCorrectPathToResource(false, new FoundResourceDto(findFolder));
        return answerResponseDtoMapper.getListAnswerResponseDtos(results, findFolder, userDetails);
    }

    @Override
    public AnswerResponseDto createFolder(UserPrincipal  userDetails, CreateFolderDto folderDto) {
        String pathToFolder = createPathToResource(createPathToRootFolder(userDetails), folderDto.getPath());
        if (ResourceChecker.isResourceExists(pathToFolder, minioProperties.bucketName(), minioClient)) {
            String[] resourseName = getSplitPath(pathToFolder);
            String name = resourseName[resourseName.length - 1];
            String msg = "Folder '" + name + "' already exists";
            log.warn(msg);
            throw new ResourceAlreadyExistsException(msg);
        }
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioProperties.bucketName())
                            .object(pathToFolder)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception ex) {
            throw new FolderCreateException("Exception during create folder");
        }
        Iterable<Result<Item>> results = checkCorrectPathToResource(
                false,
                new FoundResourceDto(pathToFolder));
        return answerResponseDtoMapper
                .getListAnswerResponseDtos(results, pathToFolder, userDetails)
                .getFirst();
    }

    @Override
    public void createRootFolderForUserByUsername(UserPrincipal  userDetails) {
        try {
            UUID id = userDetails.getUserId();
            String baseFolderName = String.format(ROOT_FOLDER, id);
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.bucketName())
                            .prefix(baseFolderName)
                            .recursive(false)
                            .build()
            );
            if (!results.iterator().hasNext()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioProperties.bucketName())
                                .object(baseFolderName)
                                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                .build()
                );
            }
        } catch (Exception ex) {
            String exception = "Exception during create root folder";
            log.warn(exception);
            throw new FolderCreateException(exception);
        }
    }

    private List<Result<Item>> checkCorrectPathToResource(boolean isRecursive, FoundResourceDto findResource) {
        Iterable<Result<Item>> iterable = getListResource(isRecursive, findResource.getPath());
        List<Result<Item>> results = new ArrayList<>();
        iterable.forEach(results::add);
        return results;
    }

    private static String[] getSplitPath(String path) {
        return path.split("/");
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

    private String createPathToRootFolder(UserPrincipal  userDetails) {
        return String.format(ROOT_FOLDER, userDetails.getUserId());
    }

    private String createPathToResource(String rootFolder, String resource) {
        return rootFolder + resource;
    }
}
