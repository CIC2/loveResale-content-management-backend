package com.resale.resalecontentmanagement.components.objectstorage;

import com.resale.resalecontentmanagement.feignClient.ProjectInterface;
import com.resale.resalecontentmanagement.components.objectstorage.dto.ProjectDTO;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectCodeService {

    private final ProjectInterface projectInterface;

    private volatile Set<String> cachedCodes;
    private volatile long lastFetch;

    private static final long TTL = 5 * 60 * 1000; // 5 minutes

    public Set<String> getValidProjectCodes() {
        long now = System.currentTimeMillis();

        if (cachedCodes == null || now - lastFetch > TTL) {
            ReturnObject<List<ProjectDTO>> response =
                    projectInterface.getAllProjects();

            if (response == null || !response.getStatus() || response.getData() == null) {
                throw new RuntimeException("Failed to fetch project codes from Project Service");
            }

            cachedCodes = response.getData().stream()
                    .map(ProjectDTO::getCode)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());

            lastFetch = now;
        }

        return cachedCodes;
    }
}



