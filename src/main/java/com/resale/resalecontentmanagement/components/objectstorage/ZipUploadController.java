    package com.resale.resalecontentmanagement.components.objectstorage;

    import com.resale.resalecontentmanagement.utils.ReturnObject;
    import org.springframework.http.HttpStatus;
    import org.springframework.http.MediaType;
    import org.springframework.http.ResponseEntity;
    import org.springframework.util.MultiValueMap;
    import org.springframework.web.bind.annotation.*;
    import org.springframework.web.multipart.MultipartFile;

    import java.io.IOException;
    import java.util.Map;

    @RestController
    @RequestMapping("/upload")
    public class ZipUploadController {

        private final ZipUploadService zipUploadService;

        public ZipUploadController(ZipUploadService zipUploadService) {
            this.zipUploadService = zipUploadService;
        }

        @PostMapping("/zip")
    //    @CheckPermission(value = {"admin:login"})
        public ResponseEntity<?> uploadZip(@RequestParam("file") MultipartFile zipFile) throws Exception {
            System.out.println("-----Upload Zip is Called-----");
                ReturnObject returnObject = zipUploadService.handleZipUpload(zipFile);
                if(returnObject == null || returnObject.getStatus()) {
                    return ResponseEntity.ok(returnObject);
                }else{
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);
                }
        }

        @PostMapping("/image")
        public ResponseEntity<?> uploadImage(
                @RequestParam(required = false) String projectCode,
                @RequestParam(required = false) String modelCode,
                @RequestParam(required = false) String folderName,
                @RequestParam(required = false) Integer index,
                @RequestParam(required = false) MultipartFile file
        ) {
            ReturnObject<?> returnObject = zipUploadService.uploadSingleImage(projectCode, modelCode, folderName, index, file);
            if(returnObject == null || returnObject.getStatus()) {
                return ResponseEntity.ok(returnObject);
            }
            else{
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);
            }
        }

        @DeleteMapping("/delete")
        public ResponseEntity<?> deleteImage(
                @RequestParam(required = false) String projectCode,
                @RequestParam(required = false) String modelCode,
                @RequestParam(required = false) String folderName,
                @RequestParam(required = false) Integer index
        ) {
            ReturnObject<?> returnObject = zipUploadService.deleteSingleImage(projectCode, modelCode, folderName, index);
            if(returnObject == null || returnObject.getStatus()) {
                return ResponseEntity.ok(returnObject);
            }
            else{
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);
            }
        }

        @PostMapping(value = "/model", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        public ResponseEntity<ReturnObject<?>> uploadModel(

                @RequestParam String projectCode,
                @RequestParam String modelCode,

                @RequestParam(required = false)
                MultiValueMap<String, MultipartFile> files
        ) throws IOException {

            ReturnObject<?> returnObject =
                    zipUploadService.uploadModel(projectCode, modelCode, files);

            return returnObject.getStatus()
                    ? ResponseEntity.ok(returnObject)
                    : ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);
        }

        @PostMapping("/model/update")
        public ResponseEntity<ReturnObject<?>> updateModel(
                @RequestParam String projectCode,
                @RequestParam String modelCode,
                @RequestParam(required = false) Map<String, String> textParams,
                @RequestParam(required = false)
                MultiValueMap<String, MultipartFile> files
        )throws IOException {
            ReturnObject<?> returnObject = zipUploadService.updateModel(projectCode, modelCode,textParams ,files);
            return returnObject.getStatus()
                    ? ResponseEntity.ok(returnObject)
                    : ResponseEntity.status(HttpStatus.FORBIDDEN).body(returnObject);
        }


        @PostMapping(
                value = "/zipnew",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        )
        public ResponseEntity<ReturnObject<?>> uploadOldZip(
                @RequestPart("zipFile") MultipartFile zipFile
        ) {
            if (zipFile == null || zipFile.isEmpty()) {
                return ResponseEntity.badRequest().body(
                        new ReturnObject<>(
                                "ZIP file is required",
                                false,
                                null
                        )
                );
            }
            ReturnObject<?> response = zipUploadService.uploadOldZipWithReformat(zipFile);
            if (Boolean.TRUE.equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }

        @DeleteMapping("/floor")
        public ResponseEntity<?> deleteFloorPathVariable(
                @RequestParam(required = false) String projectCode,
                @RequestParam(required = false) String modelCode,
                @RequestParam(required = false) Integer floorNo
        ) {
            ReturnObject<?> result = zipUploadService.deleteFloor(projectCode, modelCode, floorNo);
            if (result.getStatus()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        }
        @PostMapping(value = "/modelUploadZip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE
        )
        public ResponseEntity<ReturnObject<?>> modelUploadZip(
                @RequestParam("projectCode") String projectCode,
                @RequestPart("zipFile") MultipartFile zipFile
        ) {

            if (projectCode == null || projectCode.isBlank()) {
                return ResponseEntity.badRequest().body(
                        new ReturnObject<>("Project code is required", false, null)
                );
            }

            ReturnObject<?> response =
                    zipUploadService.uploadOldModelZip(projectCode, zipFile);

            if (Boolean.TRUE.equals(response.getStatus())) {
                return ResponseEntity.ok(response);
            }

            return ResponseEntity.badRequest().body(response);
        }

        @DeleteMapping("/model")
        public ResponseEntity<?> deleteModel(
                @RequestParam(required = false) String projectCode,
                @RequestParam(required = false) String modelCode
        ) {
            ReturnObject<?> result = zipUploadService.deleteModel(projectCode, modelCode);

            if (result.getStatus()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
        }

    }



