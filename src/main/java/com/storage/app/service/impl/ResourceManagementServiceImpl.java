package com.storage.app.service.impl;

import com.storage.app.config.MinioProperties;
import com.storage.app.dto.resource.request.FoundResourceDto;
import com.storage.app.dto.resource.request.MoveResourceDto;
import com.storage.app.dto.resource.request.SearchResourceDto;
import com.storage.app.dto.resource.response.AnswerResponseDto;
import com.storage.app.exception.resource.DeleteResourceException;
import com.storage.app.exception.resource.ResourceAlreadyExistsException;
import com.storage.app.exception.resource.ResourceChangeException;
import com.storage.app.exception.resource.ResourceNotFoundException;

import com.storage.app.security.UserPrincipal;
import com.storage.app.service.ResourceManagementService;
import com.storage.app.util.path.PathParser;
import com.storage.app.util.resource.ResourceChecker;
import com.storage.app.util.resource.ResourceFinder;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

@Slf4j
@RequiredArgsConstructor
@Service
public class ResourceManagementServiceImpl implements ResourceManagementService {

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final PathParser pathParser;
    private final ResourceChecker resourceChecker;
    private final ResourceFinder resourceFinder;
    private static final String ROOT_FOLDER = "user-%s-files/";
    private static final String DIRECTORY = "DIRECTORY";
    private static final String FILE = "FILE";


    @Override
    public List<AnswerResponseDto> searchResource(SearchResourceDto searchResourceDto, UserPrincipal userDetails) {
        String pathToRootFolder = createPathToRootFolder(userDetails);
        Iterable<Result<Item>> allResourcesFromRoot = getListResource(true, pathToRootFolder);
        Iterable<Result<Item>> filteredList = StreamSupport.stream(allResourcesFromRoot.spliterator(), false)
                .filter(result -> {
                    try {
                        String queryLower = searchResourceDto.getQuery().toLowerCase();
                        String resourceName = resourceFinder
                                .getResourceNameFromPath(result.get().objectName()).toLowerCase();
                        if (resourceName.contains(queryLower)) {
                            return true;
                        }
                        return false;
                    } catch (Exception e) {
                        log.error("Error getting object name", e);
                        return false;
                    }
                }).toList();
        return pathParser.parsePath(filteredList);
    }

    @Override
    public AnswerResponseDto moveResource(MoveResourceDto movedResourceDto, UserPrincipal userDetails) {
        String resourceFrom = movedResourceDto.getFrom();
        String resourceTo = movedResourceDto.getTo();
        String rootFolder = createPathToRootFolder(userDetails);
        String fullPathFrom = resourceFrom.startsWith(rootFolder)
                ? resourceFrom :
                createPathToRootFolder(userDetails) + resourceFrom;
        String fullPathTo = resourceTo.startsWith(rootFolder)
                ? resourceTo :
                createPathToRootFolder(userDetails) + resourceTo;
        if (!resourceChecker.isResourceExists(fullPathFrom)) {
            throw new ResourceNotFoundException("Resource " + resourceFrom + " not found");
        }
        if (resourceChecker.isResourceExists(fullPathTo)) {
            throw new ResourceAlreadyExistsException("Resource " + resourceTo + " already exists");
        }
        if (fullPathFrom.equals(fullPathTo)) {
            throw new IllegalArgumentException("Source and destination are the same");
        }
        return changeResource(
                fullPathFrom, fullPathTo, userDetails
        );
    }

    @Override
    public void deleteResource(FoundResourceDto foundResourceDto, UserPrincipal userDetails) {
        String root = createPathToRootFolder(userDetails);
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
                                .bucket(minioProperties.bucketName())
                                .object(item.objectName())
                                .build());
                deletedAtLeastOne = true;
            }
            if (!deletedAtLeastOne) {
                String folderPath = fullPath.endsWith("/") ? fullPath : fullPath + "/";
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.bucketName())
                                .object(folderPath)
                                .build());
            }
        } catch (Exception ex) {
            log.error("Delete failed: {}", ex.getMessage());
            throw new DeleteResourceException("Could not delete resource");
        }
    }

    private boolean isRename(String pathFromStr, String pathToStr, UserPrincipal userDetails) {
        Map<String, String> parentsAndNamesResources = getParentsAndNamesResources(pathFromStr, pathToStr, userDetails);
        return !parentsAndNamesResources.get("nameFrom").equals(parentsAndNamesResources.get("nameTo"))
                && parentsAndNamesResources.get("parentFrom").equals(parentsAndNamesResources.get("parentTo"));
    }

    private boolean isRelocate(String pathFromStr, String pathToStr, UserPrincipal userDetails) {
        Map<String, String> parentsAndNamesResources = getParentsAndNamesResources(pathFromStr, pathToStr, userDetails);
        return parentsAndNamesResources.get("nameFrom").equals(parentsAndNamesResources.get("nameTo"))
                || !parentsAndNamesResources.get("parentFrom").equals(parentsAndNamesResources.get("parentTo"));
    }

    private String createPathToRootFolder(UserPrincipal userDetails) {
        return String.format(ROOT_FOLDER, userDetails.getUserId());
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

    private AnswerResponseDto changeResource(String resourceFrom, String resourceTo,
                                             UserPrincipal userDetails) {
        Iterable<Result<Item>> results = getListResource(true, resourceFrom);
        AnswerResponseDto answerDto = new AnswerResponseDto();
        String rootFolder = createPathToRootFolder(userDetails);
        String newResourceToName = resourceTo;
        if (isRelocate(resourceFrom, resourceTo, userDetails)) {
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
                String userRootPrefix = String.format(ROOT_FOLDER, userDetails.getUserId());
                String relativePath = newPath.startsWith(userRootPrefix)
                        ? newPath.substring(userRootPrefix.length())
                        : newPath;
                answerDto.setPath(relativePath);
                answerDto.setName(resourceFinder.getResourceNameFromPath(newPath));
                if (resourceFinder.getResourceNameFromPath(oldPath).contains(".")) {
                    answerDto.setSize(item.size());
                    answerDto.setType(FILE);
                } else {
                    answerDto.setType(DIRECTORY);
                }
                minioClient.copyObject(
                        CopyObjectArgs.builder()
                                .bucket(minioProperties.bucketName())
                                .object(newPath)
                                .source(
                                        CopySource.builder()
                                                .bucket(minioProperties.bucketName())
                                                .object(oldPath)
                                                .build()
                                )
                                .build()
                );
                minioClient.removeObject(
                        RemoveObjectArgs.builder()
                                .bucket(minioProperties.bucketName())
                                .object(oldPath)
                                .build()
                );
            }

        } catch (Exception ex) {
            throw new ResourceChangeException("Exception during change resource");
        }
        return answerDto;
    }

    private List<Result<Item>> checkCorrectPathToResource(boolean isRecursive, FoundResourceDto findResource) {
        Iterable<Result<Item>> iterable = getListResource(isRecursive, findResource.getPath());
        List<Result<Item>> results = new ArrayList<>();
        iterable.forEach(results::add);
        return results;
    }

    private Map<String, String> getParentsAndNamesResources(String pathToResourceFrom, String pathToResourceTo,
                                                            UserPrincipal userDetails) {
        Path from = Paths.get(pathToResourceFrom).normalize();
        Path to = Paths.get(pathToResourceTo).normalize();
        String rootFolder = createPathToRootFolder(userDetails);
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

    private String createPathToResource(String rootFolder, String resource) {
        return rootFolder + resource;
    }
}
