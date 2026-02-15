package com.resale.homeflycontentmanagement.components.objectstorage;

import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/zipreformat")
public class ZipReformatController {

    @Autowired
    ZipReformatService zipReformatService;
    @PostMapping(value = "/reformat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Resource> reformatZip(@RequestParam MultipartFile file) throws IOException {

        ReturnObject<File> result = zipReformatService.reformatZip(file);

        if (!result.getStatus()) {
            return ResponseEntity.badRequest().build();
        }

        File zipFile = result.getData();
        Resource resource = new FileSystemResource(zipFile);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + zipFile.getName() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(zipFile.length())
                .body(resource);
    }


}


