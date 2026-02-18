package com.resale.resalecontentmanagement.components.adminModels;

import com.resale.resalecontentmanagement.components.adminModels.dto.*;
import com.resale.resalecontentmanagement.components.objectstorage.StorageService;
import com.resale.resalecontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Service
public class UserModelService {

    @Autowired
    private StorageService storageService;

    public ReturnObject<ModelImagesResponseDTO> getModelImages(
            String projectCode,
            String modelCode
    ) {
    try {


        String basePrefix =
                "assets/assets/Models/" + projectCode + "/" + modelCode + "/";

        List<String> keys = storageService.listObjectsByPrefix(basePrefix);

        if (keys.isEmpty()) {
            return new ReturnObject<>(
                    "No images found for this model",
                    true,
                    new ModelImagesResponseDTO(projectCode, modelCode, List.of())
            );
        }

        List<FolderImagesDTO> resultFolders = new ArrayList<>();

        // floorIndex -> floor image
        Map<Integer, String> floorIndexMap = new HashMap<>();

        // floorIndex -> unitIndex -> unit plan image
        Map<Integer, Map<Integer, String>> floorUnitPlanMap = new HashMap<>();

        // ---------- COLLECT KEYS ----------
        for (String key : keys) {

            // Floor image
            if (key.matches(".*/Floor/\\d+/\\d+\\..*")) {
                int floorIndex = extractFloorIndex(key);
                floorIndexMap.putIfAbsent(
                        floorIndex,
                        storageService.buildPublicUrl(key)
                );
            }

            // Unit plan image
            else if (key.matches(".*/Floor/\\d+/UnitPlan/[\\d_]+\\..*")) {

                int floorIndex = extractFloorIndex(key);

                String rawUnit = extractUnitPlanNumber(key);
                int unitIndex = Integer.parseInt(rawUnit.replace("_", ""));

                floorUnitPlanMap
                        .computeIfAbsent(floorIndex, f -> new HashMap<>())
                        .putIfAbsent(
                                unitIndex,
                                storageService.buildPublicUrl(key)
                        );
            }
        }

        // ---------- BUILD FLOOR ITEMS ----------
        List<IndexedImagesDTO> floorItems = new ArrayList<>();

        List<Integer> floorIndices = new ArrayList<>(floorIndexMap.keySet());
        Collections.sort(floorIndices);

        for (int floorIndex : floorIndices) {

            String floorImage = floorIndexMap.get(floorIndex);

            Map<Integer, String> unitPlansForFloor =
                    floorUnitPlanMap.getOrDefault(floorIndex, Map.of());

            List<UnitPlanDTO> unitPlanFolders = new ArrayList<>();

            if (!unitPlansForFloor.isEmpty()) {

                int maxUnitIndex = unitPlansForFloor.keySet()
                        .stream()
                        .max(Integer::compareTo)
                        .orElse(0);

                for (int i = 0; i <= maxUnitIndex; i++) {
                    unitPlanFolders.add(
                            new UnitPlanDTO(
                                    "UnitPlan " + i,
                                    unitPlansForFloor.get(i)
                            )
                    );
                }
            }

            floorItems.add(
                    new IndexedImagesDTO(
                            floorIndex,     // index
                            floorImage,     // images
                            unitPlanFolders // unitPlans - now using UnitPlanDTO
                    )
            );
        }

        // ---------- FLOOR ROOT ----------
        resultFolders.add(
                new FolderImagesDTO("Floor", floorItems)
        );

        resultFolders.add(
                FolderImagesDTO.forMimgs(
                        "MImgs",
                        List.of(
                                buildMImgs("Main", keys, basePrefix + "MImgs/Main/"),
                                buildMImgs("Medium", keys, basePrefix + "MImgs/Medium/"),
                                buildMImgs("Small", keys, basePrefix + "MImgs/Small/")
                        )
                )
        );


        // ---------- OTHER FOLDERS ----------
        Set<String> handledPrefixes = Set.of("Floor/", "MImgs/");
        Map<String, String> otherFolders = new HashMap<>();

        for (String key : keys) {
            if (handledPrefixes.stream().anyMatch(key::contains)) continue;
            if (key.endsWith("/")) continue;

            String folderName = extractFolderName(key, basePrefix);

            otherFolders.putIfAbsent(
                    folderName,
                    storageService.buildPublicUrl(key)
            );
        }

        for (Map.Entry<String, String> entry : otherFolders.entrySet()) {
            resultFolders.add(
                    new FolderImagesDTO(
                            entry.getKey(),
                            Collections.singletonList(entry.getValue()),
                            null,
                            null,
                            null
                    )
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
    catch (Exception e) {
        return new ReturnObject<>("Error Fetching Models",false,null);
    }
    }


    private int extractFloorIndex(String key) {
        Matcher matcher = Pattern.compile("/Floor/(\\d+)/").matcher(key);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        throw new IllegalArgumentException("Invalid floor path: " + key);
    }

    private String extractUnitPlanNumber(String key) {
        Matcher matcher = Pattern.compile("/UnitPlan/([\\d_]+)").matcher(key);
        if (matcher.find()) {
            return matcher.group(1);
        }
        throw new IllegalArgumentException("Invalid unit plan path: " + key);
    }

    private String extractFolderName(String key, String basePrefix) {
        String remaining = key.substring(basePrefix.length());
        String[] parts = remaining.split("/");
        return parts.length > 1 ? parts[0] : "root";
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

    public ReturnObject<List<UnitPlanDTO>> getUnitPlansForFloor(
            String projectCode,
            String modelCode,
            String floorNumber
    ) {

        if(projectCode == null || projectCode.isEmpty()) {
            return new ReturnObject<>("Project Code is Empty", false, null);
        }
        if(modelCode == null || modelCode.isEmpty()) {
            return new ReturnObject<>("Model Code is Empty", false, null);
        }
        if(floorNumber == null || floorNumber.isEmpty()) {
            return new ReturnObject<>("Floor Number is Empty", false, null);
        }

        String basePrefix = "assets/assets/Models/" + projectCode + "/" + modelCode + "/";
        List<String> keys = storageService.listObjectsByPrefix(basePrefix);

        if (keys.isEmpty()) {
            return new ReturnObject<>("No images found for this model", true, List.of());
        }

        Map<Integer, String> unitPlansMap = new HashMap<>();

        for (String key : keys) {

            if (!key.contains("/Floor/" + floorNumber + "/UnitPlan/")) {
                continue;
            }

            String afterUnitPlan = key.substring(
                    key.indexOf("/UnitPlan/") + "/UnitPlan/".length()
            );

            int dotIndex = afterUnitPlan.lastIndexOf('.');
            if (dotIndex == -1) continue;

            int unitPlanIndex;
            try {
                unitPlanIndex = Integer.parseInt(afterUnitPlan.substring(0, dotIndex));
            } catch (NumberFormatException e) {
                continue;
            }

            // Store only the first image for each unit plan
            unitPlansMap.putIfAbsent(unitPlanIndex, storageService.buildPublicUrl(key));
        }

        if (unitPlansMap.isEmpty()) {
            return new ReturnObject<>(
                    "No UnitPlans found for this floor",
                    true,
                    List.of()
            );
        }

        int maxUnitPlan = unitPlansMap.keySet()
                .stream()
                .max(Integer::compareTo)
                .orElse(0);

        List<UnitPlanDTO> result = new ArrayList<>();

        for (int i = 0; i <= maxUnitPlan; i++) {
            result.add(
                    new UnitPlanDTO(
                            "" + i,
                            unitPlansMap.get(i) // null if not present
                    )
            );
        }

        return new ReturnObject<>(
                "UnitPlans retrieved successfully",
                true,
                result
        );
    }

    public ReturnObject<List<MImgsFolderDTO>> getMImgs(
            String projectCode,
            String modelCode
    ) {
        if(projectCode == null || projectCode.isEmpty()) {
            return new ReturnObject<>("Project Code is Empty", false, null);
        }
        if(modelCode == null || modelCode.isEmpty()) {
            return new ReturnObject<>("Model Code is Empty", false, null);
        }

        String basePrefix = "assets/assets/Models/" + projectCode + "/" + modelCode + "/MImgs/";

        List<String> keys = storageService.listObjectsByPrefix(basePrefix);

        if (keys.isEmpty()) {
            return new ReturnObject<>(
                    "No MImgs found",
                    true,
                    List.of()
            );
        }

        MImgsFolderDTO mainFolder =
                buildMImgs("Main", keys, basePrefix + "Main/");

        MImgsFolderDTO mediumFolder =
                buildMImgs("Medium", keys, basePrefix + "Medium/");

        MImgsFolderDTO smallFolder =
                buildMImgs("Small", keys, basePrefix + "Small/");

        return new ReturnObject<>(
                "MImgs retrieved successfully",
                true,
                List.of(mainFolder, mediumFolder, smallFolder)
        );
    }

    private MImgsFolderDTO buildMImgs(
            String folderName,
            List<String> keys,
            String prefix
    ) {
        Map<Integer, String> map = new HashMap<>();

        for (String key : keys) {
            if (!key.startsWith(prefix)) continue;

            String name = key.substring(prefix.length());
            int dot = name.indexOf('.');
            if (dot == -1) continue;

            String base = name.substring(0, dot);
            String numericPart = base.contains("_")
                    ? base.substring(0, base.indexOf("_"))
                    : base;

            int index;
            try {
                index = Integer.parseInt(numericPart);
            } catch (NumberFormatException e) {
                continue;
            }

            map.put(index, storageService.buildPublicUrl(key));
        }

        List<Integer> sortedIndices = new ArrayList<>(map.keySet());
        Collections.sort(sortedIndices);

        List<MImgsItemDTO> items = new ArrayList<>();

        for (int i = 0; i < sortedIndices.size(); i++) {
            int originalIndex = sortedIndices.get(i);
            items.add(
                    new MImgsItemDTO(
                            i,
                            map.get(originalIndex)
                    )
            );
        }

        return new MImgsFolderDTO(folderName, items);
    }
}

