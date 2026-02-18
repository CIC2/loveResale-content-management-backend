package com.resale.resalecontentmanagement.components.adminModels;

import com.resale.resalecontentmanagement.components.adminModels.dto.*;
import org.springframework.stereotype.Service;

import com.resale.resalecontentmanagement.components.objectstorage.StorageService;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class InternalService {

    @Autowired
    private StorageService storageService;

    public ReturnObject<ModelImagesResponseDTO> getModelMedia(
            String projectCode,
            String modelCode
    ) {

        String basePrefix =
                "assets/assets/Models/" + projectCode + "/" + modelCode + "/";

        List<String> keys = storageService.listObjectsByPrefix(basePrefix);

        List<FolderImagesDTO> resultFolders = new ArrayList<>();

        if (keys.isEmpty()) {
            return new ReturnObject<>(
                    "No images found for this model",
                    true,
                    new ModelImagesResponseDTO(projectCode, modelCode, resultFolders)
            );
        }

        Set<String> singleImageFolders = Set.of(
                "Basement",
                "PDF",
                "360",
                "Finishing"
        );

        for (String folder : singleImageFolders) {

            String folderPrefix = basePrefix + folder + "/";

            keys.stream()
                    .filter(k -> k.startsWith(folderPrefix) && !k.endsWith("/"))
                    .findFirst()
                    .ifPresent(key -> resultFolders.add(
                            new FolderImagesDTO(
                                    folder,
                                    List.of(storageService.buildPublicUrl(key)),
                                    null,
                                    null,
                                    null
                            )
                    ));
        }


        Map<Integer, String> floorImages = new HashMap<>();
        Map<Integer, Map<Integer, String>> floorUnitPlans = new HashMap<>();

        for (String key : keys) {

            if (key.matches(".*/Floor/\\d+/\\d+\\..*")) {
                int floorIndex = extractFloorIndex(key);


                floorImages.putIfAbsent(
                        floorIndex,
                        storageService.buildPublicUrl(key)
                );
            } else if (key.matches(".*/Floor/\\d+/UnitPlan/\\d+\\..*")) {
                int floorIndex = extractFloorIndex(key);
                int unitIndex = extractIndexFromFilename(key);

                floorUnitPlans
                        .computeIfAbsent(floorIndex, f -> new HashMap<>())
                        .putIfAbsent(
                                unitIndex,
                                storageService.buildPublicUrl(key)
                        );
            }
        }

        List<IndexedImagesDTO> floorItems = new ArrayList<>();

        List<Integer> sortedFloors = new ArrayList<>(floorImages.keySet());
        Collections.sort(sortedFloors);

        for (int floorIndex : sortedFloors) {

            Map<Integer, String> unitPlansForFloor =
                    floorUnitPlans.getOrDefault(floorIndex, Map.of());

            List<UnitPlanDTO> unitPlanList = new ArrayList<>();

            if (!unitPlansForFloor.isEmpty()) {
                int maxUnitIndex = Collections.max(unitPlansForFloor.keySet());
                for (int i = 0; i <= maxUnitIndex; i++) {
                    unitPlanList.add(
                            new UnitPlanDTO(
                                    "UnitPlan " + i,
                                    unitPlansForFloor.get(i)
                            )
                    );
                }
            }

            floorItems.add(
                    new IndexedImagesDTO(
                            floorIndex,
                            floorImages.get(floorIndex),
                            unitPlanList
                    )
            );
        }

        if (!floorItems.isEmpty()) {
            resultFolders.add(
                    new FolderImagesDTO("Floors", floorItems)
            );
        }

        return new ReturnObject<>(
                "Model images retrieved successfully",
                true,
                new ModelImagesResponseDTO(
                        projectCode,
                        modelCode,
                        resultFolders
                )
        );
    }

    private int extractFloorIndex(String key) {
        Matcher matcher = Pattern.compile("/Floor/(\\d+)/").matcher(key);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Invalid floor path: " + key);
    }


    public ReturnObject<List<ModelImageDTO>> getProjectModelsImages(String projectCode) {

        String basePrefix = "assets/assets/Models/" + projectCode + "/";

        List<String> modelFolders = storageService.listCommonPrefixes(basePrefix);

        if (modelFolders.isEmpty()) {
            return new ReturnObject<>(
                    "No models found for this project",
                    true,
                    List.of()
            );
        }

        List<ModelImageDTO> result = new ArrayList<>();

        for (String modelFolder : modelFolders) {

            String modelCode = modelFolder
                    .substring(basePrefix.length())
                    .replace("/", "");

            String mimgsPrefix = modelFolder + "MImgs/";

            List<String> imageKeys = storageService.listObjectsByPrefix(mimgsPrefix);

            String imageUrl = imageKeys.stream()
                    .findFirst()
                    .map(key -> storageService.buildPublicUrl(key))
                    .orElse(null);

            result.add(
                    new ModelImageDTO(
                            modelCode,
                            "MImgs",
                            imageUrl
                    )
            );
        }

        return new ReturnObject<>(
                "Project models cover images retrieved successfully",
                true,
                result
        );
    }

    private int extractIndexFromFilename(String key) {
        Matcher matcher = Pattern.compile("/(\\d+)\\.").matcher(key);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Invalid indexed file: " + key);
    }
}

