package com.resale.homeflycontentmanagement.components.objectstorage;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class ZipReformatService {

    private static final long MIMG_THRESHOLD = 500 * 1024; // 500 KB

    public ReturnObject<File> reformatZip(MultipartFile zipFile) {

        try {
            Path workDir = Files.createTempDirectory("zip-reformat-");
            Path unzipDir = workDir.resolve("input");
            Path outputDir = workDir.resolve("output");

            Files.createDirectories(unzipDir);
            Files.createDirectories(outputDir);

            Path zipPath = workDir.resolve(zipFile.getOriginalFilename());
            Files.copy(zipFile.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);

            unzip(zipPath, unzipDir);

            Path projectRoot = findProjectRoot(unzipDir);
            String projectCode = projectRoot.getFileName().toString();

            Path finalProjectDir = outputDir.resolve(projectCode);
            Files.createDirectories(finalProjectDir);

            try (DirectoryStream<Path> models = Files.newDirectoryStream(projectRoot)) {
                for (Path modelDir : models) {
                    if (Files.isDirectory(modelDir)) {
                        processModel(
                                modelDir,
                                finalProjectDir.resolve(modelDir.getFileName())
                        );
                    }
                }
            }

            File resultZip = zip(finalProjectDir, workDir.resolve(projectCode + ".zip"));

            return new ReturnObject<>(
                    "ZIP reformatted successfully",
                    true,
                    resultZip
            );

        } catch (Exception e) {
            return new ReturnObject<>(
                    "Failed to reformat ZIP: " + e.getMessage(),
                    false,
                    null
            );
        }
    }

    public ReturnObject<File> reformatSingleModelZip(
            String projectCode,
            String modelCode,
            MultipartFile zipFile
    ) {

        try {
            Path workDir = Files.createTempDirectory("model-reformat");
            Path unzipDir = workDir.resolve("input");
            Path outputDir = workDir.resolve("output");

            Files.createDirectories(unzipDir);
            Files.createDirectories(outputDir);

            Path zipPath = workDir.resolve(zipFile.getOriginalFilename());
            Files.copy(zipFile.getInputStream(), zipPath, StandardCopyOption.REPLACE_EXISTING);

            unzip(zipPath, unzipDir);

            Path modelDir = findModelRoot(unzipDir);

            Path projectDir = outputDir.resolve(projectCode);
            Path targetModelDir = projectDir.resolve(modelCode);

            Files.createDirectories(targetModelDir);

            processModel(modelDir, targetModelDir);

            File resultZip = zip(projectDir, workDir.resolve(modelCode + ".zip"));

            return new ReturnObject<>(
                    "Model ZIP reformatted successfully",
                    true,
                    resultZip
            );

        } catch (Exception e) {
            return new ReturnObject<>(
                    "Model reformat failed: " + e.getMessage(),
                    false,
                    null
            );
        }
    }

    private Path findModelRoot(Path unzipDir) throws IOException {
        try (DirectoryStream<Path> ds = Files.newDirectoryStream(unzipDir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) return p;
            }
        }
        throw new IllegalStateException("No model folder found in ZIP");
    }


    private void processModel(Path modelDir, Path targetModelDir) throws IOException {

        Files.createDirectories(targetModelDir);

        Map<String, Path> floorImages = readImages(modelDir.resolve("Floor"));
        Map<String, List<Path>> unitPlans = readUnitPlans(modelDir);

        Path newFloorDir = targetModelDir.resolve("Floor");
        Files.createDirectories(newFloorDir);

        for (String floor : floorImages.keySet()) {

            Path floorFolder = newFloorDir.resolve(floor);
            Files.createDirectories(floorFolder);

            Files.copy(
                    floorImages.get(floor),
                    floorFolder.resolve(floorImages.get(floor).getFileName()),
                    StandardCopyOption.REPLACE_EXISTING
            );

            Path unitPlanDir = floorFolder.resolve("UnitPlan");
            Files.createDirectories(unitPlanDir);

            List<Path> plans = unitPlans.get(floor);
            if (plans != null) {
                for (Path plan : plans) {
                    Files.copy(
                            plan,
                            unitPlanDir.resolve(plan.getFileName()),
                            StandardCopyOption.REPLACE_EXISTING
                    );
                }
            }
        }

        processMImgs(modelDir.resolve("MImgs"), targetModelDir.resolve("MImgs"));
        copySingle(modelDir, targetModelDir, "PDF");
        copySingle(modelDir, targetModelDir, "360");
        copySingle(modelDir, targetModelDir, "Finishing");
        copySingle(modelDir, targetModelDir, "Basement");
    }


    private Map<String, Path> readImages(Path dir) throws IOException {
        Map<String, Path> map = new HashMap<>();
        if (!Files.exists(dir)) return map;

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(dir)) {
            for (Path p : ds) {
                map.put(nameNoExt(p), p);
            }
        }
        return map;
    }

    private Map<String, List<Path>> readUnitPlans(Path modelDir) throws IOException {
        Map<String, List<Path>> map = new HashMap<>();

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(modelDir)) {
            for (Path floorDir : ds) {
                if (!Files.isDirectory(floorDir)) continue;
                String floor = floorDir.getFileName().toString();
                if (!floor.matches("\\d+")) continue;
                List<Path> images = new ArrayList<>();
                try (DirectoryStream<Path> imgs = Files.newDirectoryStream(floorDir)) {
                    for (Path img : imgs) {
                        if (Files.isRegularFile(img)) {
                            images.add(img);
                        }
                    }
                }
                if (!images.isEmpty()) {
                    map.put(floor, images);
                }
            }
        }
        return map;
    }

    private void processMImgs(Path src, Path target) throws IOException {

        if (!Files.exists(src)) return;

        Path main = target.resolve("Main");
        Path medium = target.resolve("Medium");
        Path small = target.resolve("Small");

        Files.createDirectories(main);
        Files.createDirectories(medium);
        Files.createDirectories(small);

        try (DirectoryStream<Path> imgs = Files.newDirectoryStream(src)) {
            for (Path img : imgs) {
                if (!Files.isRegularFile(img)) continue;
                String fileName = img.getFileName().toString();
                String nameNoExt = fileName.substring(0, fileName.lastIndexOf('.'));
                Path dest;
                if (nameNoExt.contains("_42")) {
                    dest = small;
                } else if (nameNoExt.contains("_500")) {
                    dest = medium;
                } else if (nameNoExt.matches("\\d+")) {
                    dest = main;
                } else {
                    dest = main;
                }
                Files.copy(
                        img,
                        dest.resolve(fileName),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
    }
    private void copySingle(Path srcRoot, Path targetRoot, String folder) throws IOException {
        Path src = srcRoot.resolve(folder);
        if (!Files.exists(src)) return;

        Path target = targetRoot.resolve(folder);
        Files.createDirectories(target);

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(src)) {
            for (Path f : ds) {
                Files.copy(f, target.resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }


    private void unzip(Path zip, Path dest) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                Path p = dest.resolve(e.getName());
                if (e.isDirectory()) {
                    Files.createDirectories(p);
                } else {
                    Files.createDirectories(p.getParent());
                    Files.copy(zis, p, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    private File zip(Path sourceDir, Path zipPath) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(Files.newOutputStream(zipPath))) {
            Files.walk(sourceDir).filter(Files::isRegularFile).forEach(p -> {
                try {
                    String entry = sourceDir.getParent().relativize(p).toString();
                    zos.putNextEntry(new ZipEntry(entry));
                    Files.copy(p, zos);
                    zos.closeEntry();
                } catch (IOException ignored) {}
            });
        }
        return zipPath.toFile();
    }

    private String nameNoExt(Path p) {
        String n = p.getFileName().toString();
        return n.substring(0, n.lastIndexOf('.'));
    }


    Path findProjectRoot(Path unzipDir) throws IOException {

        try (DirectoryStream<Path> ds = Files.newDirectoryStream(unzipDir)) {
            for (Path p : ds) {
                if (Files.isDirectory(p)) {
                    return p;
                }
            }
        }
        throw new IllegalStateException("No project folder found in ZIP");
    }

}



