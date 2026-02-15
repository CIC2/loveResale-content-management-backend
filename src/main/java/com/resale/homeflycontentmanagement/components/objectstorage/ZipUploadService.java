package com.resale.homeflycontentmanagement.components.objectstorage;

import com.resale.homeflycontentmanagement.feignClient.CommunicationClient;
import com.resale.homeflycontentmanagement.components.objectstorage.dto.OtpMailDTO;
import com.resale.homeflycontentmanagement.components.objectstorage.dto.UploadZipResponseDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class ZipUploadService {

    private final StorageService storageService;
    private final ExecutorService executorService;
    private final ProjectCodeService projectCodeService;
    private final ZipReformatService zipReformatService;
    @Autowired
    CommunicationClient communicationClient;
    // Threshold for using multipart upload (20MB)
    private static final long MULTIPART_THRESHOLD = 20 * 1024 * 1024;

    private static final Set<String> IGNORED_FILES = Set.of(
            ".DS_Store",
            "__MACOSX"
    );

    private static final Set<String> ALLOWED_PROJECT_CODES =
            Set.of("E", "M", "O", "H", "K", "U");

    public ZipUploadService(StorageService storageService, ProjectCodeService projectCodeService, ZipReformatService zipReformatService) {
        this.storageService = storageService;
        this.projectCodeService = projectCodeService;
        this.zipReformatService = zipReformatService;
        // Create thread pool - adjust based on your server capacity
        this.executorService = Executors.newFixedThreadPool(10);
    }

    private static long logInterval(String label, long lastTime) {
        long now = System.currentTimeMillis();
        System.out.println("[TIMER] " + label + " : " + (now - lastTime) + " ms");
        return now;
    }

    public ReturnObject handleZipUpload(MultipartFile zipFile) throws Exception {
        if (!isZipFile(zipFile)) {
            return new ReturnObject<>("Invalid File Format. Must Be a ZIP File.", false, null);
        }
        ReturnObject returnObject = new ReturnObject();
        long t = System.currentTimeMillis();
        System.out.println("[TIMER] handleZipUpload START");

        Path tempDir = Files.createTempDirectory("zipUpload");
        Path zipPath = tempDir.resolve(Objects.requireNonNull(zipFile.getOriginalFilename()));
        Files.write(zipPath, zipFile.getBytes());

        System.out.println("[DEBUG] ZIP path: " + zipPath);
        System.out.println("[DEBUG] ZIP size: " + Files.size(zipPath));

        t = logInterval("ZIP saved to temp", t);

        Path unzipDir = tempDir.resolve("unzipped");
        unzip(zipPath.toFile(), unzipDir.toFile());

        t = logInterval("ZIP unzipped", t);

        List<Path> files = Files.walk(unzipDir)
                .filter(Files::isRegularFile)
                .sorted()
                .toList();

        System.out.println("[DEBUG] Files found after unzip: " + files.size());
        t = logInterval("Files scanned", t);

        if (files.isEmpty()) {
            deleteDirectory(tempDir.toFile());
            throw new RuntimeException("No files found in ZIP");
        }

        String projectCode = null;
        String modelCode = null;
        Set<String> foldersToProcess = new LinkedHashSet<>();

        Map<String, List<Path>> folderToFiles = new LinkedHashMap<>();

        for (Path filePath : files) {
            String filename = filePath.getFileName().toString();

            if (IGNORED_FILES.contains(filename) || filename.startsWith("__MACOSX")) {
                continue;
            }
            String rel = unzipDir.relativize(filePath).toString().replace('\\', '/');
            String[] parts = rel.split("/");

            if (parts.length < 3) continue;

            if (projectCode == null) {
                projectCode = parts[0].trim();
                modelCode = parts[1].trim();
            }

            String projectFolder = parts[0].trim();
            Set<String> validCodes = projectCodeService.getValidProjectCodes();

            if (!validCodes.contains(projectFolder)) {
                deleteDirectory(tempDir.toFile());
                returnObject.setStatus(false);
                returnObject.setData(null);
                returnObject.setMessage("Wrong project code: " + projectFolder +
                        ". Codes are case-sensitive and must be CAPITAL. " +
                        "Allowed values: " + validCodes);
                return returnObject;
            }

            String modelName = parts[1].trim();
            String folderName = parts[2].trim();

            String basePrefix = "assets/assets/Models/" + projectFolder;
            String s3FolderPrefix = basePrefix + "/" + modelName + "/" + folderName + "/";

            folderToFiles.computeIfAbsent(s3FolderPrefix, k -> new ArrayList<>()).add(filePath);
            foldersToProcess.add(modelName + "/" + folderName);
        }

        try {
            validatePdfAnd360Folders(folderToFiles);
        } catch (IllegalArgumentException e) {
            deleteDirectory(tempDir.toFile());
            return new ReturnObject<>(
                    e.getMessage(),
                    false,
                    null
            );
        }

        System.out.println("[DEBUG] S3 folders to update: " + folderToFiles.size());
        t = logInterval("Structure analyzed", t);

        // Generate a unique job ID for tracking
        String jobId = UUID.randomUUID().toString();

        // Start background processing asynchronously
        final String finalProjectCode = projectCode;
        final String finalModelCode = modelCode;

        CompletableFuture.runAsync(() -> {
            processZipInBackground(jobId, finalProjectCode, finalModelCode,
                    folderToFiles, tempDir, foldersToProcess);
        }, executorService);

        returnObject.setStatus(true);
        returnObject.setMessage("Processing started");
        returnObject.setData(new UploadZipResponseDTO(
                projectCode,
                new ArrayList<>(foldersToProcess),
                "PROCESSING"
        ));

        System.out.println("[TIMER] handleZipUpload returned to frontend (async processing started)");
        return returnObject;
    }

    private void processZipInBackground(String jobId, String projectCode, String modelCode,
                                        Map<String, List<Path>> folderToFiles,
                                        Path tempDir, Set<String> foldersToProcess) {
        boolean isSuccess = true;
        String failureReason = null;
        long t = System.currentTimeMillis();
        System.out.println("[BACKGROUND] Job " + jobId + " - Starting background processing");

        List<String> uploadedImages = Collections.synchronizedList(new ArrayList<>());
        Set<String> deletedPrefixes = Collections.synchronizedSet(new HashSet<>());

        try {
            // Phase 1: Delete all folders in parallel
            List<CompletableFuture<Void>> deleteFutures = new ArrayList<>();
            for (String prefix : folderToFiles.keySet()) {
                deletedPrefixes.add(prefix);
                deleteFutures.add(CompletableFuture.runAsync(() ->
                        storageService.deleteFolder(prefix), executorService));
            }

            CompletableFuture.allOf(deleteFutures.toArray(new CompletableFuture[0])).join();
            t = logInterval("[BACKGROUND] Job " + jobId + " - S3 folders deleted", t);

            // Phase 2: Upload all files in parallel
            List<CompletableFuture<String>> uploadFutures = new ArrayList<>();

            for (Map.Entry<String, List<Path>> entry : folderToFiles.entrySet()) {
                String s3FolderPrefix = entry.getKey();
                List<Path> folderFiles = entry.getValue();

                for (Path filePath : folderFiles) {
                    CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                        try {
                            String filename = filePath.getFileName().toString();
                            String objectKey = s3FolderPrefix + filename;

                            long fileSize = Files.size(filePath);
                            if (fileSize > MULTIPART_THRESHOLD) {
                                storageService.uploadLargeFile(filePath.toFile(), objectKey);
                            } else {
                                storageService.uploadFile(filePath.toFile(), objectKey);
                            }

                            return objectKey;
                        } catch (Exception e) {
                            System.err.println("[BACKGROUND] Upload failed: " + filePath + " - " + e.getMessage());
                            return null;
                        }
                    }, executorService);

                    uploadFutures.add(future);
                }
            }

            // Wait for all uploads and collect results
            for (CompletableFuture<String> future : uploadFutures) {
                try {
                    String result = future.get();
                    if (result != null) {
                        uploadedImages.add(result);
                    }
                } catch (Exception e) {
                    isSuccess = false;
                    failureReason = e.getMessage();
                    System.err.println("[BACKGROUND] Job " + jobId + " - Upload collection failed: " + e.getMessage());
                }
            }

            t = logInterval("[BACKGROUND] Job " + jobId + " - All uploads completed", t);

            // Update job status to completed
            updateJobStatus(jobId, "COMPLETED", uploadedImages.size(), null);

            System.out.println("[BACKGROUND] Job " + jobId + " - Processing completed successfully");

        } catch (Exception e) {
            isSuccess = false;
            failureReason = e.getMessage();
            System.err.println("[BACKGROUND] Job " + jobId + " - Processing failed: " + failureReason);
            updateJobStatus(jobId, "FAILED", uploadedImages.size(), failureReason);
        } finally {
            // Cleanup temp directory
            try {
                deleteDirectory(tempDir.toFile());
                t = logInterval("[BACKGROUND] Job " + jobId + " - Temp directory cleaned", t);
            } catch (Exception e) {
                isSuccess = false;
                failureReason = e.getMessage();
                System.err.println("[BACKGROUND] Job " + jobId + " - Processing failed: " + failureReason);
                updateJobStatus(jobId, "FAILED", uploadedImages.size(), failureReason);
            }
            try {
                OtpMailDTO otpMailDTO = new OtpMailDTO();
                otpMailDTO.setEmail("omar.hatem1230@gmail.com");
                otpMailDTO.setMailSubject("ZIP File Processing Status");

                String mailContent;

                if (isSuccess) {
                    mailContent =
                            "<div style='font-family:Arial;padding:20px'>" +
                                    "<h2 style='color:#2e7d32'>ZIP Processing Completed Successfully</h2>" +
                                    "<p><b>Job ID:</b> " + jobId + "</p>" +
                                    "<p><b>Project Code:</b> " + projectCode + "</p>" +
                                    "<p><b>Model Code:</b> " + modelCode + "</p>" +
                                    "<p><b>Uploaded Files:</b> " + uploadedImages.size() + "</p>" +
                                    "<br>" +
                                    "<p style='color:#555'>All files were uploaded successfully.</p>" +
                                    "<hr>" +
                                    "<p style='font-size:12px;color:#999'>TMG Background Processing System</p>" +
                                    "</div>";
                } else {
                    mailContent =
                            "<div style='font-family:Arial;padding:20px'>" +
                                    "<h2 style='color:#c62828'>ZIP Processing Failed</h2>" +
                                    "<p><b>Job ID:</b> " + jobId + "</p>" +
                                    "<p><b>Project Code:</b> " + projectCode + "</p>" +
                                    "<p><b>Model Code:</b> " + modelCode + "</p>" +
                                    "<p><b>Uploaded Files Before Failure:</b> " + uploadedImages.size() + "</p>" +
                                    "<p><b>Error:</b> " + failureReason + "</p>" +
                                    "<br>" +
                                    "<p style='color:#555'>Please review the logs for more details.</p>" +
                                    "<hr>" +
                                    "<p style='font-size:12px;color:#999'>TMG Background Processing System</p>" +
                                    "</div>";
                }

                otpMailDTO.setMailContent(mailContent);
                communicationClient.sendOtpMail(otpMailDTO);

            } catch (Exception exception) {
                System.out.println("[BACKGROUND] Job " + jobId + " - Email sending failed: " + exception.getMessage());
            }

        }
    }

    private void updateJobStatus(String jobId, String status, int uploadedCount, String errorMessage) {
        JobStatus jobStatus = new JobStatus();
        jobStatus.setJobId(jobId);
        jobStatus.setStatus(status);
        jobStatus.setUploadedCount(uploadedCount);
        jobStatus.setErrorMessage(errorMessage);
        jobStatus.setLastUpdated(LocalDateTime.now());

    }

    public ReturnObject<?> uploadOldZipWithReformat(MultipartFile zipFile) {
        if (!isZipFile(zipFile)) {
            return new ReturnObject<>("Invalid File Format. Must Be a ZIP File.", false, null);
        }
        try {
            ReturnObject<File> reformatted = zipReformatService.reformatZip(zipFile);

            if (!reformatted.getStatus()) {
                return new ReturnObject<>(
                        "Reformat failed: " + reformatted.getMessage(),
                        false,
                        null
                );
            }

            File newZip = reformatted.getData();

            return uploadNewFormattedZip(newZip);

        } catch (Exception e) {
            return new ReturnObject<>(
                    "Project ZIP Upload Failed due to wrong ZIP Format",
                    false,
                    null
            );
        }
    }

    private ReturnObject<?> uploadNewFormattedZip(File zipFile) throws Exception {

        Path tempDir = Files.createTempDirectory("new-zip-upload");
        Path unzipDir = tempDir.resolve("unzipped");

        unzip(zipFile.toPath().toFile(), unzipDir.toFile());

        Path projectDir = zipReformatService.findProjectRoot(unzipDir);
        String projectCode = projectDir.getFileName().toString();

        Map<String, List<Path>> s3Uploads = new LinkedHashMap<>();
        Set<String> foldersTouched = new LinkedHashSet<>();

        Files.walk(projectDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {

                    String rel = projectDir.relativize(file)
                            .toString()
                            .replace("\\", "/");

                    String[] parts = rel.split("/");
                    if (parts.length < 2) return;

                    String modelCode = parts[0];

                    String s3Key =
                            "assets/assets/Models/"
                                    + projectCode + "/"
                                    + modelCode + "/"
                                    + String.join("/", Arrays.copyOfRange(parts, 1, parts.length - 1))
                                    + "/";

                    s3Uploads
                            .computeIfAbsent(s3Key, k -> new ArrayList<>())
                            .add(file);

                    foldersTouched.add(modelCode);
                });
        try {
            validatePdfAnd360Folders(s3Uploads);
        } catch (IllegalArgumentException e) {
            deleteDirectory(tempDir.toFile());
            return new ReturnObject<>(
                    e.getMessage(),
                    false,
                    null
            );
        }

        String jobId = UUID.randomUUID().toString();

        CompletableFuture.runAsync(() ->
                processZipInBackground(
                        jobId,
                        projectCode,
                        null,
                        s3Uploads,
                        tempDir,
                        foldersTouched
                ), executorService
        );

        return new ReturnObject<>(
                "Processing started",
                true,
                new UploadZipResponseDTO(
                        projectCode,
                        new ArrayList<>(foldersTouched),
                        "PROCESSING"
                )
        );
    }

    public ReturnObject<?> uploadOldModelZip(String projectCode, MultipartFile zipFile) {

        if (!isZipFile(zipFile)) {
            return new ReturnObject<>("Invalid file format. ZIP required.", false, null);
        }

        try {
            // MODEL CODE = ZIP FILE NAME (without .zip)
            String modelCode = zipFile.getOriginalFilename()
                    .replace(".zip", "")
                    .trim();

            // 1️⃣ Reformat OLD MODEL ZIP
            ReturnObject<File> reformatted =
                    zipReformatService.reformatSingleModelZip(
                            projectCode,
                            modelCode,
                            zipFile
                    );

            if (!reformatted.getStatus()) {
                return new ReturnObject<>(
                        "Reformat failed: " + reformatted.getMessage(),
                        false,
                        null
                );
            }

            return uploadNewFormattedModelZip(
                    projectCode,
                    modelCode,
                    reformatted.getData()
            );

        } catch (Exception e) {
            return new ReturnObject<>(
                    "Model ZIP Upload Failed due to wrong ZIP Format",
                    false,
                    null
            );
        }
    }

    private ReturnObject<?> uploadNewFormattedModelZip(
            String projectCode,
            String modelCode,
            File zipFile
    ) throws Exception {

        Path tempDir = Files.createTempDirectory("model-zip-upload");
        Path unzipDir = tempDir.resolve("unzipped");

        unzip(zipFile, unzipDir.toFile());

        Map<String, List<Path>> s3Uploads = new LinkedHashMap<>();
        Set<String> foldersTouched = new LinkedHashSet<>();

        Files.walk(unzipDir)
                .filter(Files::isRegularFile)
                .forEach(file -> {

                    String rel = unzipDir.relativize(file)
                            .toString()
                            .replace("\\", "/");

                    String[] parts = rel.split("/");
                    if (parts.length < 2) return;

                    String s3Prefix =
                            "assets/assets/Models/"

                                    + String.join("/", Arrays.copyOfRange(parts, 0, parts.length - 1))
                                    + "/";

                    s3Uploads
                            .computeIfAbsent(s3Prefix, k -> new ArrayList<>())
                            .add(file);

                    foldersTouched.add(modelCode);
                });

        try {
            validatePdfAnd360Folders(s3Uploads);
        } catch (IllegalArgumentException e) {
            deleteDirectory(tempDir.toFile());
            return new ReturnObject<>(
                    e.getMessage(),
                    false,
                    null
            );
        }


        String jobId = UUID.randomUUID().toString();

        CompletableFuture.runAsync(() ->
                        processZipInBackground(
                                jobId,
                                projectCode,
                                modelCode,
                                s3Uploads,
                                tempDir,
                                foldersTouched
                        ),
                executorService
        );

        return new ReturnObject<>(
                "Model processing started",
                true,
                new UploadZipResponseDTO(
                        projectCode,
                        List.of(modelCode),
                        "PROCESSING"
                )
        );
    }


    public ReturnObject<?> deleteModel(String projectCode, String modelCode) {

        if (projectCode == null || projectCode.isEmpty()) {
            return new ReturnObject<>("Project Code is Empty", false, null);
        }

        if (modelCode == null || modelCode.isEmpty()) {
            return new ReturnObject<>("Model Code is Empty", false, null);
        }

        Set<String> validCodes = projectCodeService.getValidProjectCodes();
        if (!validCodes.contains(projectCode)) {
            return new ReturnObject<>(
                    "Wrong project code: " + projectCode +
                            ". Allowed values: " + validCodes,
                    false,
                    null
            );
        }

        String modelPath = "assets/assets/Models/" + projectCode + "/" + modelCode + "/";

        try {
            if (!storageService.modelExists(modelPath)) {
                return new ReturnObject<>(
                        "Model '" + modelCode + "' does not exist in project '" + projectCode + "'",
                        false,
                        null
                );
            }

            List<String> objectsToDelete = storageService.listObjectsByPrefix(modelPath);

            if (objectsToDelete.isEmpty()) {
                return new ReturnObject<>(
                        "Model folder exists but contains no files",
                        false,
                        null
                );
            }

            storageService.deleteFolder(modelPath);

            System.out.println("[DELETE_MODEL] Successfully deleted model: " + modelPath);
            System.out.println("[DELETE_MODEL] Total objects deleted: " + objectsToDelete.size());

            return new ReturnObject<>(
                    "Model '" + modelCode + "' and all its contents deleted successfully",
                    true,
                    Map.of(
                            "projectCode", projectCode,
                            "modelCode", modelCode,
                            "modelPath", modelPath,
                            "deletedObjectsCount", objectsToDelete.size(),
                            "deletedObjects", objectsToDelete
                    )
            );

        } catch (Exception e) {
            System.err.println("[DELETE_MODEL] Failed to delete model: " + e.getMessage());
            return new ReturnObject<>(
                    "Failed to delete model: " + e.getMessage(),
                    false,
                    null
            );
        }
    }


    private boolean isZipFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();

        return (contentType != null && contentType.equals("application/zip"))
                || (filename != null && filename.toLowerCase().endsWith(".zip"));
    }

    private void validatePdfAnd360Folders(
            Map<String, List<Path>> folderToFiles
    ) {
        for (Map.Entry<String, List<Path>> entry : folderToFiles.entrySet()) {

            String s3Prefix = entry.getKey();
            List<Path> files = entry.getValue();

            String folderName = s3Prefix.substring(0, s3Prefix.length() - 1);
            folderName = folderName.substring(folderName.lastIndexOf('/') + 1);

            if (folderName.equalsIgnoreCase("PDF")) {

                if (files.size() != 1) {
                    throw new IllegalArgumentException(
                            "PDF folder must contain exactly one PDF file"
                    );
                }

                String filename = files.get(0).getFileName().toString().toLowerCase();
                if (!filename.endsWith(".pdf")) {
                    throw new IllegalArgumentException(
                            "PDF folder must contain a .pdf file only"
                    );
                }
            }
            if (
                    folderName.equalsIgnoreCase("360") ||
                            folderName.equalsIgnoreCase("three60")
            ) {

                if (files.size() != 1) {
                    throw new IllegalArgumentException(
                            "360 folder must contain exactly one HTML file"
                    );
                }

                String filename = files.get(0).getFileName().toString().toLowerCase();
                if (!filename.endsWith(".html")) {
                    throw new IllegalArgumentException(
                            "360 folder must contain an .html file only"
                    );
                }
            }
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "jpg";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    private void unzip(File zipFile, File destDir) throws IOException {
        byte[] buffer = new byte[8192]; // Increased buffer size

        try (ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile))) {
            ZipEntry zipEntry = zis.getNextEntry();

            while (zipEntry != null) {
                File newFile = newFile(destDir, zipEntry);

                if (zipEntry.isDirectory()) {
                    newFile.mkdirs();
                } else {
                    new File(newFile.getParent()).mkdirs();

                    try (FileOutputStream fos = new FileOutputStream(newFile)) {
                        int len;
                        while ((len = zis.read(buffer)) > 0) {
                            fos.write(buffer, 0, len);
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
        }
    }

    private static File newFile(File destinationDir, ZipEntry zipEntry) throws IOException {
        File destFile = new File(destinationDir, zipEntry.getName());

        String destDirPath = destinationDir.getCanonicalPath();
        String destFilePath = destFile.getCanonicalPath();

        if (!destFilePath.startsWith(destDirPath + File.separator)) {
            throw new IOException("Zip entry is outside target dir: " + zipEntry.getName());
        }
        return destFile;
    }

    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }

    public ReturnObject<?> uploadSingleImage(
            String projectCode,
            String modelCode,
            String folderName,
            Integer index,
            MultipartFile file
    ) {
        ReturnObject returnObject = uploadValidation(projectCode, modelCode, folderName, index, file);
        if (returnObject != null) {
            return returnObject;
        }

        Set<String> validCodes = projectCodeService.getValidProjectCodes();
        if (!validCodes.contains(projectCode)) {
            return new ReturnObject<>(
                    "Wrong project code: " + projectCode,
                    false,
                    null
            );
        }

        String basePath = "assets/assets/Models/" + projectCode + "/" + modelCode + "/";
        String objectKey;
        int finalIndex;

        try {
            // Handle Floor images (e.g., Floor/1/1.img)
            if (folderName.matches("Floor\\[\\d+]")) {
                int floorIndex = Integer.parseInt(
                        folderName.substring(folderName.indexOf('[') + 1, folderName.indexOf(']'))
                );
                objectKey = basePath + "Floor/" + floorIndex + "/" + floorIndex + ".img";
                finalIndex = floorIndex;
            }
            // Handle UnitPlan images (e.g., Floor/1/UnitPlan/2.img)
            else if (folderName.matches("Floor\\[\\d+]UnitPlan\\[\\d+]")) {
                int floorIndex = Integer.parseInt(
                        folderName.substring(folderName.indexOf("Floor[") + 6, folderName.indexOf("]"))
                );
                int unitIndex = Integer.parseInt(
                        folderName.substring(folderName.indexOf("UnitPlan[") + 9, folderName.lastIndexOf("]"))
                );
                objectKey = basePath + "Floor/" + floorIndex + "/UnitPlan/" + unitIndex + ".img";
                finalIndex = unitIndex;
            }
            // Handle single-file folders (Finishing, PDF, 360, Basement)
            else if (
                    folderName.startsWith("Finishing") ||
                            folderName.startsWith("PDF") ||
                            folderName.startsWith("360") ||
                            folderName.startsWith("Basement")
            ) {
                finalIndex = 0;
                objectKey = basePath + folderName + "/0.img";
            }
            // Handle MImgs subfolders
            else if (folderName.startsWith("MImgs")) {
                if (index == null) {
                    return new ReturnObject<>("Index is required for this folder", false, null);
                }
                finalIndex = index;

                // Extract subfolder (Main, Medium, Small)
                String subfolder = folderName.replace("MImgs", "").trim();
                if (subfolder.isEmpty()) {
                    return new ReturnObject<>("MImgs subfolder (Main/Medium/Small) is required", false, null);
                }

                objectKey = basePath + "MImgs/" + subfolder + "/" + finalIndex + ".img";
            }
            // Handle other regular folders
            else {
                if (index == null) {
                    return new ReturnObject<>("Index is required for this folder", false, null);
                }
                finalIndex = index;
                objectKey = basePath + folderName + "/" + finalIndex + ".img";
            }

            // Create temp file and upload
            File tempFile = File.createTempFile("upload-", ".tmp");
            file.transferTo(tempFile);

            storageService.uploadFile(tempFile, objectKey);

            tempFile.delete();

            return new ReturnObject<>(
                    "Image uploaded successfully",
                    true,
                    Map.of(
                            "projectCode", projectCode,
                            "modelCode", modelCode,
                            "folderName", folderName,
                            "index", finalIndex,
                            "objectKey", objectKey
                    )
            );

        } catch (Exception e) {
            return new ReturnObject<>(
                    "Upload failed: " + e.getMessage(),
                    false,
                    null
            );
        }
    }


    public ReturnObject<?> deleteSingleImage(
            String projectCode,
            String modelCode,
            String folderName,
            Integer index
    ) {

        ReturnObject returnObject = deleteValidation(projectCode, modelCode, folderName, index);
        if (returnObject != null) {
            return returnObject;
        }

        String basePath = "assets/assets/Models/" + projectCode + "/" + modelCode + "/";
        String objectKey;
        int finalIndex;

        try {
            // Handle Floor images (e.g., Floor/1/1.img or Floor/1/1.png)
            if (folderName.matches("Floor\\[\\d+]")) {
                int floorIndex = Integer.parseInt(
                        folderName.substring(folderName.indexOf('[') + 1, folderName.indexOf(']'))
                );
                String floorPath = basePath + "Floor/" + floorIndex + "/";

                // Find the actual floor image file (regardless of extension)
                List<String> filesInFolder = storageService.listObjectsByPrefix(floorPath);
                objectKey = null;

                for (String file : filesInFolder) {
                    // Look for file matching floorIndex (e.g., "1.png", "1.jpg", "1.img")
                    if (file.matches(".*/" + floorIndex + "\\.[^/]+$")) {
                        objectKey = file;
                        break;
                    }
                }

                if (objectKey == null) {
                    return new ReturnObject<>(
                            "Floor image not found for floor " + floorIndex,
                            false,
                            null
                    );
                }

                finalIndex = floorIndex;
            }
            // Handle UnitPlan images (e.g., Floor/1/UnitPlan/2.img or Floor/1/UnitPlan/2.png)
            else if (folderName.matches("Floor\\[\\d+]UnitPlan\\[\\d+]")) {
                int floorIndex = Integer.parseInt(
                        folderName.substring(folderName.indexOf("Floor[") + 6, folderName.indexOf("]"))
                );
                int unitIndex = Integer.parseInt(
                        folderName.substring(folderName.indexOf("UnitPlan[") + 9, folderName.lastIndexOf("]"))
                );
                String unitPlanPath = basePath + "Floor/" + floorIndex + "/UnitPlan/";

                // Find the actual unit plan file (regardless of extension)
                List<String> filesInFolder = storageService.listObjectsByPrefix(unitPlanPath);
                objectKey = null;

                for (String file : filesInFolder) {
                    // Look for file matching unitIndex (e.g., "2.png", "2.jpg", "2.img")
                    if (file.matches(".*/" + unitIndex + "\\.[^/]+$")) {
                        objectKey = file;
                        break;
                    }
                }

                if (objectKey == null) {
                    return new ReturnObject<>(
                            "UnitPlan image not found for floor " + floorIndex + ", unit " + unitIndex,
                            false,
                            null
                    );
                }

                finalIndex = unitIndex;
            }
            // Handle single-file folders (Finishing, PDF, 360, Basement)
            else if (
                    folderName.startsWith("Finishing") ||
                            folderName.startsWith("PDF") ||
                            folderName.startsWith("360") ||
                            folderName.startsWith("Basement")
            ) {
                finalIndex = 0;
                String folderPath = basePath + folderName + "/";

                // Find the actual file in the folder (regardless of extension)
                List<String> filesInFolder = storageService.listObjectsByPrefix(folderPath);
                if (filesInFolder.isEmpty()) {
                    return new ReturnObject<>(
                            "No files found in " + folderName + " folder",
                            false,
                            null
                    );
                }

                // Use the first (and should be only) file found
                objectKey = filesInFolder.get(0);
            }
            // Handle MImgs subfolders
            else if (folderName.startsWith("MImgs")) {
                if (index == null) {
                    return new ReturnObject<>("Index is required for this folder", false, null);
                }
                finalIndex = index;

                // Extract subfolder (Main, Medium, Small)
                String subfolder = folderName.replace("MImgs", "").trim();
                if (subfolder.isEmpty()) {
                    return new ReturnObject<>("MImgs subfolder (Main/Medium/Small) is required", false, null);
                }

                String mimgsPath = basePath + "MImgs/" + subfolder + "/";

                // Find the actual file (regardless of extension)
                List<String> filesInFolder = storageService.listObjectsByPrefix(mimgsPath);
                objectKey = null;

                for (String file : filesInFolder) {
                    if (file.matches(".*/" + finalIndex + "[_.].*")) {
                        objectKey = file;
                        break;
                    }
                }

                if (objectKey == null) {
                    return new ReturnObject<>(
                            "Image not found in " + folderName + " at index " + finalIndex,
                            false,
                            null
                    );
                }
            }
            // Handle other regular folders
            else {
                if (index == null) {
                    return new ReturnObject<>("Index is required for this folder", false, null);
                }
                finalIndex = index;

                String folderPath = basePath + folderName + "/";

                // Find the actual file (regardless of extension)
                List<String> filesInFolder = storageService.listObjectsByPrefix(folderPath);
                objectKey = null;

                for (String file : filesInFolder) {
                    // Look for file matching the index
                    if (file.matches(".*/" + finalIndex + "\\.[^/]+$")) {
                        objectKey = file;
                        break;
                    }
                }

                if (objectKey == null) {
                    return new ReturnObject<>(
                            "Image not found in " + folderName + " at index " + finalIndex,
                            false,
                            null
                    );
                }
            }

            // Delete the object
            storageService.deleteObject(objectKey);

            return new ReturnObject<>(
                    "Image deleted successfully",
                    true,
                    Map.of(
                            "projectCode", projectCode,
                            "modelCode", modelCode,
                            "folderName", folderName,
                            "index", finalIndex,
                            "objectKey", objectKey
                    )
            );

        } catch (Exception e) {
            return new ReturnObject<>(
                    "Delete failed: " + e.getMessage(),
                    false,
                    null
            );
        }
    }


    private ReturnObject<?> uploadValidation(String projectCode,
                                             String modelCode,
                                             String folderName,
                                             Integer index,
                                             MultipartFile fileName) {
        if (projectCode == null || projectCode.isEmpty()) {
            return new ReturnObject<>("Project Code is Empty", false, null);
        } else if (modelCode == null || modelCode.isEmpty()) {
            return new ReturnObject<>("Model Code is Empty", false, null);
        } else if (folderName == null || folderName.isEmpty()) {
            return new ReturnObject<>("Folder Name is Empty", false, null);
        } else if (fileName == null || fileName.isEmpty()) {
            return new ReturnObject<>("File is Empty", false, null);
        } else if (index == null) {
            return new ReturnObject<>("Index is required for this folder", false, null);
        }
        return null;
    }

    private ReturnObject<?> deleteValidation(String projectCode,
                                             String modelCode,
                                             String folderName,
                                             Integer index) {
        if (projectCode == null || projectCode.isEmpty()) {
            return new ReturnObject<>("Project Code is Empty", false, null);
        } else if (modelCode == null || modelCode.isEmpty()) {
            return new ReturnObject<>("Model Code is Empty", false, null);
        } else if (folderName == null || folderName.isEmpty()) {
            return new ReturnObject<>("Folder Name is Empty", false, null);
        } else if (index == null || index.intValue() < 0) {
            return new ReturnObject<>("Index is Empty", false, null);
        }
        return null;
    }

    public ReturnObject<?> uploadModel(
            String projectCode,
            String modelCode,
            MultiValueMap<String, MultipartFile> files
    ) throws IOException {

        if (projectCode == null || projectCode.isEmpty())
            return new ReturnObject<>("The Project Code is Empty", false, null);

        if (modelCode == null || modelCode.isEmpty())
            return new ReturnObject<>("Model Code is Empty", false, null);

        Set<String> validCodes = projectCodeService.getValidProjectCodes();
        if (!validCodes.contains(projectCode)) {
            return new ReturnObject<>(
                    "Wrong project code: " + projectCode,
                    false,
                    null
            );
        }

        if (modelExists(projectCode, modelCode)) {
            return new ReturnObject<>(
                    "Model code '" + modelCode + "' already exists in project '" + projectCode + "'",
                    false,
                    null
            );
        }

        String basePath = "assets/assets/Models/" + projectCode + "/" + modelCode;
        List<String> uploaded = new ArrayList<>();

        if (files == null) {
            return new ReturnObject<>("No files provided", false, null);
        }

        for (Map.Entry<String, List<MultipartFile>> entry : files.entrySet()) {

            String key = entry.getKey();
            List<MultipartFile> fileList = entry.getValue();
            if (!key.contains("[") || fileList == null) continue;

            MultipartFile file = fileList.get(0);
            if (file == null || file.isEmpty()) continue;

            if (key.matches("floor\\[\\d+]unitPlan\\[\\d+]")) {

                int floorIndex = Integer.parseInt(
                        key.substring(key.indexOf("floor[") + 6, key.indexOf("]"))
                );
                int unitIndex = Integer.parseInt(
                        key.substring(key.indexOf("unitPlan[") + 9, key.lastIndexOf("]"))
                );

                String path = basePath + "/Floor/" + floorIndex + "/UnitPlan/";
                deleteByIndexPrefix(path, unitIndex);

                String fileName = unitIndex + ".img";
                storageService.uploadMultipartFileForModel(file, path + fileName);
                uploaded.add(path + fileName);
                continue;
            }

            if (key.matches("floor\\[\\d+]")) {

                int floorIndex = extractIndex(key);
                String path = basePath + "/Floor/" + floorIndex + "/";

                deleteByIndexPrefix(path, floorIndex);

                String fileName = floorIndex + ".img";
                storageService.uploadMultipartFileForModel(file, path + fileName);
                uploaded.add(path + fileName);
                continue;
            }

            String path = null;
            String fileExtension = ".img";

            if (key.startsWith("finishing[")) {
                path = basePath + "/Finishing/";
            } else if (key.startsWith("pdf[")) {
                path = basePath + "/PDF/";
                fileExtension = ".pdf";
            } else if (key.startsWith("three60[")) {
                path = basePath + "/360/";
                fileExtension = ".html";
            } else if (key.startsWith("basement[")) {
                path = basePath + "/Basement/";
            } else if (key.startsWith("mImgsmain[")) {
                path = basePath + "/MImgs/Main/";
            } else if (key.startsWith("mImgsmedium[")) {
                path = basePath + "/MImgs/Medium/";
            } else if (key.startsWith("mImgssmall[")) {
                path = basePath + "/MImgs/Small/";
            } else {
                continue;
            }

            int index = extractIndex(key);

            if (path.endsWith("/PDF/") && !hasExtension(file, ".pdf"))
                return new ReturnObject<>("Only PDF files allowed", false, null);

            if (path.endsWith("/360/") && !hasExtension(file, ".html"))
                return new ReturnObject<>("Only HTML files allowed", false, null);

            deleteByIndexPrefix(path, index);

            String fileName = index + fileExtension;
            storageService.uploadMultipartFileForModel(file, path + fileName);
            uploaded.add(path + fileName);
        }

        return new ReturnObject<>(
                "Model assets uploaded successfully",
                true,
                uploaded
        );
    }
    private boolean modelExists(String projectCode, String modelCode) {
        String prefix = "assets/assets/Models/" + projectCode + "/" + modelCode + "/";
        List<String> objects = storageService.listObjectsByPrefix(prefix);
        return !objects.isEmpty();
    }

    private void deleteByIndexPrefix(String prefix, int index) {
        List<String> existingKeys = storageService.listObjectsByPrefix(prefix);

        for (String key : existingKeys) {
            if (key.matches(".*/" + index + "\\..*") || key.endsWith("/" + index + ".img")) {
                storageService.deleteObject(key);
            }
        }
    }


    private int extractIndex(String key) {
        int start = key.indexOf('[') + 1;
        int end = key.indexOf(']');
        return Integer.parseInt(key.substring(start, end));
    }

    public ReturnObject<?> updateModel(
            String projectCode,
            String modelCode,
            @RequestParam(required = false) Map<String, String> textParams,
            @RequestParam(required = false) MultiValueMap<String, MultipartFile> files
    ) throws IOException {

        if (projectCode == null || projectCode.isEmpty())
            return new ReturnObject<>("The Project Code is Empty", false, null);

        if (modelCode == null || modelCode.isEmpty())
            return new ReturnObject<>("Model Code is Empty", false, null);

        if (!projectCodeService.getValidProjectCodes().contains(projectCode))
            return new ReturnObject<>("Wrong project code", false, null);

        String basePath = "assets/assets/Models/" + projectCode + "/" + modelCode;

        if (!storageService.modelExists(basePath))
            return new ReturnObject<>("Model does not exist", false, null);

        boolean hasFiles = files != null && !files.isEmpty();
        boolean hasText = textParams != null && !textParams.isEmpty();

        if (!hasFiles && !hasText)
            return new ReturnObject<>("No data provided", false, null);

        List<String> uploaded = new ArrayList<>();

        if (hasText) {
            for (String rawKey : textParams.keySet()) {

                String key = rawKey.toLowerCase().trim();

                if (!isFloorIndexChange(key)) continue;

                int[] indices = extractFloorIndexChange(key);
                if (indices == null) continue;

                int oldIndex = indices[0];
                int newIndex = indices[1];

                if (oldIndex == newIndex) continue;

                String oldPath = basePath + "/Floor/" + oldIndex + "/";
                String newPath = basePath + "/Floor/" + newIndex + "/";

                if (!storageService.folderExists(oldPath)) {
                    return new ReturnObject<>(
                            "Floor " + oldIndex + " does not exist",
                            false,
                            null
                    );
                }

                storageService.moveFolder(oldPath, newPath);

                String oldFloorImage = newPath + oldIndex + ".img";
                String newFloorImage = newPath + newIndex + ".img";

                if (storageService.objectExists(oldFloorImage)) {
                    storageService.moveObject(oldFloorImage, newFloorImage);
                }

                uploaded.add("Floor moved from " + oldIndex + " to " + newIndex);
            }
        }


        if (hasFiles) {
            for (Map.Entry<String, List<MultipartFile>> entry : files.entrySet()) {

                String rawKey = entry.getKey();
                String key = rawKey.toLowerCase().trim();

                MultipartFile file =
                        entry.getValue() == null ? null : entry.getValue().get(0);

                if (file == null) continue;

                Integer floorIndex = extractFloorIndexSafe(key);
                Integer unitIndex = extractUnitIndexSafe(key);

                if (file.isEmpty()) {
                    if (floorIndex != null && unitIndex != null) {
                        String path = basePath + "/Floor/" + floorIndex + "/UnitPlan/";
                        deleteByIndexPrefix(path, unitIndex);
                        uploaded.add("Deleted: " + path + unitIndex + ".img");
                    } else if (floorIndex != null) {
                        String path = basePath + "/Floor/" + floorIndex + "/";
                        deleteByIndexPrefix(path, floorIndex);
                        uploaded.add("Deleted: " + path + floorIndex + ".img");
                    } else {
                        String path = null;
                        String fileExtension = ".img";
                        int index = extractIndex(key);

                        if (key.startsWith("finishing")) {
                            path = basePath + "/Finishing/";
                        } else if (key.startsWith("pdf")) {
                            path = basePath + "/PDF/";
                            fileExtension = ".pdf";
                        } else if (key.startsWith("three60")) {
                            path = basePath + "/360/";
                            fileExtension = ".html";
                        } else if (key.startsWith("basement")) {
                            path = basePath + "/Basement/";
                        } else if (key.startsWith("mimgsmain")) {
                            path = basePath + "/MImgs/Main/";
                        } else if (key.startsWith("mimgsmedium")) {
                            path = basePath + "/MImgs/Medium/";
                        } else if (key.startsWith("mimgssmall")) {
                            path = basePath + "/MImgs/Small/";
                        }

                        if (path != null) {
                            deleteByIndexPrefix(path, index);
                            uploaded.add("Deleted: " + path + index + fileExtension);
                        }
                    }
                    continue;
                }

                if (floorIndex != null && unitIndex != null) {

                    String path = basePath + "/Floor/" + floorIndex + "/UnitPlan/";
                    deleteByIndexPrefix(path, unitIndex);

                    storageService.uploadMultipartFileForModel(
                            file,
                            path + unitIndex + ".img"
                    );

                    uploaded.add(path + unitIndex + ".img");
                    continue;
                }

                if (floorIndex != null) {

                    String path = basePath + "/Floor/" + floorIndex + "/";
                    deleteByIndexPrefix(path, floorIndex);

                    storageService.uploadMultipartFileForModel(
                            file,
                            path + floorIndex + ".img"
                    );

                    uploaded.add(path + floorIndex + ".img");
                    continue;
                }

                String path = null;
                String fileExtension = ".img"; // Default extension
                int index = extractIndex(key);

                if (key.startsWith("finishing")) {
                    path = basePath + "/Finishing/";
                } else if (key.startsWith("pdf")) {
                    path = basePath + "/PDF/";
                    fileExtension = ".pdf";
                } else if (key.startsWith("three60")) {
                    path = basePath + "/360/";
                    fileExtension = ".html";
                } else if (key.startsWith("basement")) {
                    path = basePath + "/Basement/";
                } else if (key.startsWith("mimgsmain")) {
                    path = basePath + "/MImgs/Main/";
                } else if (key.startsWith("mimgsmedium")) {
                    path = basePath + "/MImgs/Medium/";
                } else if (key.startsWith("mimgssmall")) {
                    path = basePath + "/MImgs/Small/";
                } else {
                    continue;
                }

                if (path.endsWith("/PDF/") && !hasExtension(file, ".pdf"))
                    return new ReturnObject<>("Only PDF files allowed", false, null);

                if (path.endsWith("/360/") && !hasExtension(file, ".html"))
                    return new ReturnObject<>("Only HTML files allowed", false, null);

                deleteByIndexPrefix(path, index);

                String fullPath = path + index + fileExtension;

                storageService.uploadMultipartFileForModel(file, fullPath);

                uploaded.add(fullPath);
            }
        }

        if (uploaded.isEmpty()) {
            return new ReturnObject<>(
                    "No changes were applied",
                    false,
                    null
            );
        }

        return new ReturnObject<>(
                "Model updated successfully",
                true,
                uploaded
        );
    }


    private boolean hasExtension(MultipartFile file, String extension) {
        if (file.getOriginalFilename() == null) return false;
        return file.getOriginalFilename().toLowerCase().endsWith(extension);
    }

    private Integer extractFloorIndexSafe(String key) {
        Matcher m = Pattern.compile("floor\\[(\\d+)]").matcher(key);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private Integer extractUnitIndexSafe(String key) {
        Matcher m = Pattern.compile("unitplan\\[(\\d+)]").matcher(key);
        return m.find() ? Integer.parseInt(m.group(1)) : null;
    }

    private boolean isFloorIndexChange(String key) {
        return key.matches(".*floor\\[\\d+->\\d+].*");
    }

    private int[] extractFloorIndexChange(String key) {
        Matcher m = Pattern.compile("floor\\[(\\d+)->(\\d+)]").matcher(key);
        if (!m.find()) return null;
        return new int[]{
                Integer.parseInt(m.group(1)),
                Integer.parseInt(m.group(2))
        };
    }

    public ReturnObject<?> deleteFloor(String projectCode, String modelCode, Integer floorNo
    ) {
        if (projectCode == null || projectCode.isEmpty()) {
            return new ReturnObject<>("Project Code is Empty", false, null);
        }

        if (modelCode == null || modelCode.isEmpty()) {
            return new ReturnObject<>("Model Code is Empty", false, null);
        }

        if (floorNo == null || floorNo < 0) {
            return new ReturnObject<>("Invalid Floor Number", false, null);
        }

        // Validate project code
        Set<String> validCodes = projectCodeService.getValidProjectCodes();
        if (!validCodes.contains(projectCode)) {
            return new ReturnObject<>(
                    "Wrong project code: " + projectCode +
                            ". Allowed values: " + validCodes,
                    false,
                    null
            );
        }

        String floorPath = "assets/assets/Models/" + projectCode + "/" + modelCode + "/Floor/" + floorNo + "/";

        try {
            if (!storageService.folderExists(floorPath)) {
                return new ReturnObject<>(
                        "Floor " + floorNo + " does not exist for model " + modelCode,
                        false,
                        null
                );
            }

            List<String> objectsToDelete = storageService.listObjectsByPrefix(floorPath);

            if (objectsToDelete.isEmpty()) {
                return new ReturnObject<>(
                        "Floor folder exists but contains no files",
                        false,
                        null
                );
            }
            storageService.deleteFolder(floorPath);

            return new ReturnObject<>(
                    "Floor " + floorNo + " and all associated unit plans deleted successfully",
                    true,
                    Map.of(
                            "projectCode", projectCode,
                            "modelCode", modelCode,
                            "floorNo", floorNo,
                            "deletedObjects", objectsToDelete.size(),
                            "floorPath", floorPath
                    )
            );

        } catch (Exception e) {
            return new ReturnObject<>("Failed to delete floor: " + e.getMessage(), false, null
            );
        }
    }
}

