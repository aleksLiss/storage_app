package com.storage.app.service;

import com.storage.app.config.MinioConfig;
import com.storage.app.dto.resource.request.*;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.exception.resource.*;
import com.storage.app.exception.resource.file.FileDownloadException;
import com.storage.app.exception.resource.folder.FolderCreateException;
import com.storage.app.exception.resource.folder.FolderDownloadException;
import com.storage.app.mapper.*;
import com.storage.app.model.User;
import com.storage.app.repository.UserRepository;
import com.storage.app.util.path.PathParser;
import com.storage.app.util.resource.ResourceChecker;
import com.storage.app.util.resource.ResourceFinder;
import com.storage.app.util.file.FileChecker;
import io.minio.*;
import io.minio.messages.Item;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.Principal;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Data
@Slf4j
public class MinioService {

    private final MinioConfig minioConfig;
    private final UserRepository userRepository;
    private final FileChecker fileChecker;
    private final PathParser pathParser;
    private final ResourceFinder resourceFinder;
    private final AnswerResponseDtoMapper answerResponseDtoMapper;
    private final ResourceChecker resourceChecker;
    private static final String DIRECTORY = "DIRECTORY";
    private static final String FILE = "FILE";
    private static final String ROOT_FOLDER = "user-%s-files/";

    public LinkedHashMap<String, String> getResource(FoundResourceDto foundResourceDto) {
        Iterable<Result<Item>> results = checkCorrectPathToResource(false, foundResourceDto);
        List<LinkedHashMap<String, String>> allFound = answerResponseDtoMapper
                .getListAnswerResponseDtos(results, resourceFinder, foundResourceDto.getPath());
        if (allFound.isEmpty()) {
            throw new ResourceNotFoundException("Resource not found");
        }
        return allFound.stream()
                .filter(map -> foundResourceDto.getPath().equals(map.get("path")))
                .findFirst()
                .orElse(allFound.getFirst());
    }

    public void deleteResource(FoundResourceDto foundResourceDto, Principal principal) {
        MinioClient minioClient = minioConfig.getMinioClient();
        String root = createPathToRootFolder(principal);
        String rawPath = foundResourceDto.getPath();
        String fullPath = rawPath.startsWith(root) ? rawPath : root + rawPath;
        List<Result<Item>> results
                = checkCorrectPathToResource(true, new FoundResourceDto(fullPath));
        try {
            boolean deletedAtLeastOne = false;
            for (Result<Item> result : results) {
                Item item = result.get();
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(item.objectName())
                                .build());
                deletedAtLeastOne = true;
            }
            if (!deletedAtLeastOne) {
                String folderPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(folderPath)
                                .build());
            }
        } catch (Exception ex) {
            log.error("Delete failed: {}", ex.getMessage());
            throw new DeleteResourceException("Could not delete resource");
        }
    }

    public void createRootFolderForUserByUsername(String username) {
        MinioClient minioClient = minioConfig.getMinioClient();
        try {
            String id = String.valueOf(userRepository.findByUsername(username).get().getUuid());
            String baseFolderName = String.format(ROOT_FOLDER, id);
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .prefix(baseFolderName)
                            .recursive(false)
                            .build()
            );
            if (!results.iterator().hasNext()) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(minioConfig.getBucketName())
                                .object(baseFolderName)
                                .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                                .build()
                );
            }
        } catch (Exception ex) {
            String exception = "Exception during create root folder for user: " + username;
            log.warn(exception);
            throw new FolderCreateException(exception);
        }
    }

    public StreamingResponseBody downloadFolder(FoundResourceDto foundResourceDto) {
        return outputStream -> {
            MinioClient minioClient = minioConfig.getMinioClient();
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
                    String fullPath = item.objectName();
                    if (fullPath.equals(pathToResource)) continue;
                    String entryName = fullPath.startsWith(pathToResource)
                            ? fullPath.substring(pathToResource.length())
                            : fullPath;

                    if (entryName.isEmpty()) continue;
                    try (InputStream is = minioClient.getObject(
                            GetObjectArgs.builder()
                                    .bucket(minioConfig.getBucketName())
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

    public StreamingResponseBody downloadFile(FoundResourceDto foundResourceDto) {
        return outputStream -> {
            MinioClient minioClient = minioConfig.getMinioClient();
            Iterable<Result<Item>> results =
                    getListResource(true, foundResourceDto.getPath());
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
                String msg = "Exception during download file";
                log.warn(msg);
                throw new FileDownloadException(msg);
            }
        };
    }

    public List<LinkedHashMap<String, String>> getResourcesFromFolder(FoundResourceDto foundResourceDto,
                                                                      Principal principal) {
        String rootFolder = createPathToRootFolder(principal);
        String requestPath = foundResourceDto.getPath();
        String findFolder = (requestPath == null || requestPath.isEmpty() || requestPath.equals(rootFolder))
                ? rootFolder
                : createPathToResource(rootFolder, requestPath);
        Iterable<Result<Item>> results =
                checkCorrectPathToResource(false, new FoundResourceDto(findFolder));
        return answerResponseDtoMapper.getListAnswerResponseDtos(results, resourceFinder, findFolder);
    }

    public Map<String, String> createFolder(CreateFolderDto folderDto, Principal principal) {
        String pathToFolder = createPathToResource(createPathToRootFolder(principal), folderDto.getPath());
        MinioClient minioClient = minioConfig.getMinioClient();
        if (resourceChecker.isResourceExists(pathToFolder)) {
            String[] resourseName = getSplitPath(pathToFolder);
            String name = resourseName[resourseName.length - 1];
            String msg = "Folder '" + name + "' already exists";
            log.warn(msg);
            throw new ResourceAlreadyExistsException(msg);
        }
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .object(pathToFolder)
                            .stream(new ByteArrayInputStream(new byte[0]), 0, -1)
                            .build()
            );
        } catch (Exception ex) {
            throw new FolderCreateException("Exception during create folder");
        }
        return answerResponseDtoMapper.answerResponseDtoToMap(resourceFinder, pathToFolder);
    }

    public List<LinkedHashMap<String, String>> uploadResource(MultipartFile[] files,
                                                              UploadResourceDto uploadResourceDto,
                                                              Principal principal) {
        MinioClient minioClient = minioConfig.getMinioClient();
        List<LinkedHashMap<String, String>> response = new ArrayList<>();
        String rootFolder = String.format(ROOT_FOLDER, getUUIDFromFoundUser(principal));
        for (MultipartFile file : files) {
            fileChecker.checkFileSize(file);
            String checkPathForUploadResource = createPathToRootFolder(principal) + uploadResourceDto.getPath();
            fileChecker.fileExistsInDirectory(minioClient, file, checkPathForUploadResource);
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
                                    .bucket(minioConfig.getBucketName())
                                    .object(pathToResource)
                                    .contentType(file.getContentType())
                                    .stream(inputStream, file.getSize(), -1)
                                    .build()
                    );
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
            } catch (Exception ex) {
                throw new ResourceUploadException("Exception during upload resource");
            }
        }
        return response;
    }

    public List<LinkedHashMap<String, String>> searchResource(SearchResourceDto searchResourceDto, Principal principal) {
        String pathToRootFolder = createPathToRootFolder(principal);
        Iterable<Result<Item>> allResourcesFromRoot = getListResource(true, pathToRootFolder);
        Iterable<Result<Item>> filteredList = StreamSupport.stream(allResourcesFromRoot.spliterator(), false)
                .filter(result -> {
                    try {
                        String searchRes = searchResourceDto.getQuery() + "/";
                        String searchResTwo = searchResourceDto.getQuery() + ".";
                        if (result.get().objectName().contains(searchRes)
                                || result.get().objectName().contains(searchResTwo)) {
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        log.error("Error getting object name", e);
                        return false;
                    }
                }).toList();
        List<AnswerResponseDto> answerResponseDtos = pathParser.parsePath(filteredList);
        for (AnswerResponseDto answerResponseDto : answerResponseDtos) {
            log.warn(answerResponseDto.getPath());
        }
        return answerResponseDtos.stream()
                .map(answerResponseDtoMapper::answerResponseDtoToMap)
                .toList();
    }

    public Map<String, String> moveResource(MoveResourceDto movedResourceDto, Principal principal) {
        String resourceFrom = movedResourceDto.getFrom();
        String resourceTo = movedResourceDto.getTo();
        String rootFolder = createPathToRootFolder(principal);
        String fullPathFrom = resourceFrom.startsWith(rootFolder)
                ? resourceFrom :
                createPathToRootFolder(principal) + resourceFrom;

        String fullPathTo = resourceTo.startsWith(rootFolder)
                ? resourceTo :
                createPathToRootFolder(principal) + resourceTo;
        if (!resourceChecker.isResourceExists(fullPathFrom)) {
            throw new ResourceNotFoundException("Resource " + resourceFrom + " not found");
        }
        if (resourceChecker.isResourceExists(fullPathTo)) {
            throw new ResourceAlreadyExistsException("Resource " + resourceTo + " already exists");
        }
        if (isRelocate(fullPathFrom, fullPathTo, principal) && isRename(fullPathFrom, fullPathTo, principal)) {
            throw new IllegalArgumentException("Must be only rename or relocate");
        }
        return answerResponseDtoMapper.answerResponseDtoToMap(
                changeResource(
                        fullPathFrom, fullPathTo, principal
                )
        );
    }

    private String getUUIDFromFoundUser(Principal principal) {
        Optional<User> foundUser = userRepository.findByUsername(principal.getName());
        return String.valueOf(foundUser.get().getUuid());
    }

    private AnswerResponseDto changeResource(String resourceFrom, String resourceTo, Principal principal) {
        Iterable<Result<Item>> results = getListResource(true, resourceFrom);
        AnswerResponseDto answerDto = new AnswerResponseDto();
        MinioClient client = minioConfig.getMinioClient();
        String rootFolder = createPathToRootFolder(principal);
        String newResourceToName = resourceTo;
        if (isRelocate(resourceFrom, resourceTo, principal)) {
            if (!resourceTo.startsWith(rootFolder)) {
                newResourceToName = createPathToResource(rootFolder, resourceTo);
            }
        }
        try {
            for (Result<Item> result : results) {
                Item item = result.get();
                String oldPath = item.objectName();
                String newPath;
                if (oldPath.equals(resourceFrom)) {
                    if (newResourceToName.endsWith("/")) {
                        String fileName = oldPath.substring(oldPath.lastIndexOf("/") + 1);
                        newPath = newResourceToName + fileName;
                    } else {
                        newPath = newResourceToName;
                    }
                } else {
                    newPath = oldPath.replaceFirst(Pattern.quote(resourceFrom), newResourceToName);
                }
                newPath = newPath.replace("//", "/");
                if (newPath.startsWith("/")) {
                    newPath = newPath.substring(1);
                }
                answerDto.setPath(resourceFinder.getPathToResourceFromPath(newPath));
                answerDto.setName(resourceFinder.getResourceNameFromPath(newPath));
                if (resourceFinder.getResourceNameFromPath(oldPath).contains(".")) {
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

        } catch (Exception ex) {
            throw new ResourceChangeException("Exception during change resource");
        }
        return answerDto;
    }

    private boolean isRename(String pathFromStr, String pathToStr, Principal principal) {
        Map<String, String> parentsAndNamesResources = getParentsAndNamesResources(pathFromStr, pathToStr, principal);
        return !parentsAndNamesResources.get("nameFrom").equals(parentsAndNamesResources.get("nameTo"))
                && parentsAndNamesResources.get("parentFrom").equals(parentsAndNamesResources.get("parentTo"));
    }

    private boolean isRelocate(String pathFromStr, String pathToStr, Principal principal) {
        Map<String, String> parentsAndNamesResources = getParentsAndNamesResources(pathFromStr, pathToStr, principal);
        return parentsAndNamesResources.get("nameFrom").equals(parentsAndNamesResources.get("nameTo"))
                || !parentsAndNamesResources.get("parentFrom").equals(parentsAndNamesResources.get("parentTo"));
    }

    private Map<String, String> getParentsAndNamesResources(String pathToResourceFrom, String pathToResourceTo, Principal principal) {
        Path from = Paths.get(pathToResourceFrom).normalize();
        Path to = Paths.get(pathToResourceTo).normalize();
        String rootFolder = createPathToRootFolder(principal);
        if (!to.startsWith(rootFolder)) {
            to = Paths.get(createPathToResource(rootFolder, pathToResourceTo)).normalize();
        }
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

    private List<Result<Item>> checkCorrectPathToResource(boolean isRecursive, FoundResourceDto findResource) {

        Iterable<Result<Item>> iterable = getListResource(isRecursive, findResource.getPath());
        List<Result<Item>> results = new ArrayList<>();
        iterable.forEach(results::add);
        log.warn("Found objects count: " + results.size());
        for (Result<Item> res : results) {
            try {
                log.warn("Target: " + res.get().objectName());
            } catch (Exception e) {
            }
        }
        return results;
    }

    private static String[] getSplitPath(String path) {
        return path.split("/");
    }

    private Iterable<Result<Item>> getListResource(boolean isRecursive, String resource) {
        MinioClient minioClient = minioConfig.getMinioClient();
        Iterable<Result<Item>> results = Collections.emptyList();
        if (resource != null) {
            results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioConfig.getBucketName())
                            .recursive(isRecursive)
                            .prefix(resource)
                            .build());
        }
        return results;
    }

    private String createPathToRootFolder(Principal principal) {
        return String.format(ROOT_FOLDER, getUUIDFromFoundUser(principal));
    }

    private String createPathToResource(String rootFolder, String resource) {
        return rootFolder + resource;
    }

    private String createPathToResource(String rootFolder, String... resources) {
        return rootFolder + String.join("/", resources);
    }
}