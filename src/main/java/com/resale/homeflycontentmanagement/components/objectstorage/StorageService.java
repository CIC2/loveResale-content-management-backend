package com.resale.homeflycontentmanagement.components.objectstorage;

import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.model.*;
import com.resale.homeflycontentmanagement.components.objectstorage.dto.ImagesDTO;
import com.resale.homeflycontentmanagement.utils.ReturnObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class StorageService {
    @Autowired
    private AmazonS3 s3Client;

    @Value("${ibm.cos.bucket-name}")
    private String bucketName;

    @Value("${ibm.cos.endpoint-url}")
    private String endpointUrl;

    @Value("${ibm.cos.location}")
    private String location;

    public ReturnObject<String> uploadImage(MultipartFile file) throws IOException {
        if (!s3Client.doesBucketExistV2(bucketName)) {
            return new ReturnObject<>("Bucket Does Not Exist", false, null);
        }

        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        // Upload to IBM COS
        PutObjectRequest putObjectRequest = new PutObjectRequest(
                bucketName,
                fileName,
                file.getInputStream(),
                metadata
        );

        s3Client.putObject(putObjectRequest);

        return new ReturnObject<>("Image Uploaded Successfully", true, fileName);
    }

    // Helper method to ensure bucket exists
    private ReturnObject<Boolean> ensureBucketExists() {
        if (!s3Client.doesBucketExistV2(bucketName)) {

            CreateBucketRequest createBucketRequest = new CreateBucketRequest(bucketName, location);
            s3Client.createBucket(createBucketRequest);

            System.out.println("Bucket '" + bucketName + "' created successfully!");
        }
        return new ReturnObject<>("Bucket '" + bucketName + "' exists", true, true);

    }

    public String uploadImageFromUrl(ImagesDTO dto) throws IOException {
        if (!s3Client.doesBucketExistV2(bucketName)) {
            return null;
        }

        // Extract filename from URL or generate a unique one
        String originalFileName = extractFileNameFromUrl(dto.getUrl());
        String fileName = UUID.randomUUID().toString() + "-" + originalFileName;

        URL url = new URL(dto.getUrl());
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);

        try (InputStream inputStream = connection.getInputStream()) {
            String contentType = connection.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new IOException("URL does not point to an image");
            }

            long contentLength = connection.getContentLengthLong();

            ObjectMetadata metadata = new ObjectMetadata();
            if (contentLength > 0) {
                metadata.setContentLength(contentLength);
            }
            metadata.setContentType(contentType);

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    fileName,
                    inputStream,
                    metadata
            );

            s3Client.putObject(putObjectRequest);

            return endpointUrl + "/" + bucketName + "/" + fileName;
        } finally {
            connection.disconnect();
        }
    }

    private String extractFileNameFromUrl(String imageUrl) {
        try {
            String path = new URL(imageUrl).getPath();
            String fileName = path.substring(path.lastIndexOf('/') + 1);

            if (fileName.isEmpty() || !fileName.contains(".")) {
                return "image.jpg";
            }
            return fileName;
        } catch (Exception e) {
            return "image.jpg";
        }
    }

    public void deleteImage(String fileName) {
        s3Client.deleteObject(bucketName, fileName);
    }

    // Additional method to check if bucket exists
    public boolean doesBucketExist(String bucketName) {
        return s3Client.doesBucketExistV2(bucketName);
    }

    // Method to get a pre-signed URL (for temporary access)
    public String generatePresignedUrl(String fileName, int expirationMinutes) {
        java.util.Date expiration = new java.util.Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 1000 * 60 * expirationMinutes;
        expiration.setTime(expTimeMillis);

        return s3Client.generatePresignedUrl(bucketName, fileName, expiration).toString();
    }

    // Add these methods to your StorageService class

    public void uploadFile(File file, String keyName) {
        try {
            // Simple upload for files under 20MB
            PutObjectRequest request = new PutObjectRequest(bucketName, keyName, file);

            // Optional: Add metadata for better performance
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(file.length());
            request.setMetadata(metadata);

            s3Client.putObject(request);
            System.out.println("Uploaded → " + keyName);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to upload: " + keyName + " → " + e.getMessage());
        }
    }

    public void deleteFolder(String prefix) {
        try {
            var request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(prefix)
                    .withMaxKeys(1000); // Maximum allowed per request

            ListObjectsV2Result result;

            do {
                result = s3Client.listObjectsV2(request);

                if (result.getObjectSummaries().isEmpty()) {
                    break;
                }

                // Collect keys for batch delete
                var keys = new ArrayList<DeleteObjectsRequest.KeyVersion>();
                for (var obj : result.getObjectSummaries()) {
                    keys.add(new DeleteObjectsRequest.KeyVersion(obj.getKey()));
                }

                // Batch delete (up to 1000 at a time)
                var deleteReq = new DeleteObjectsRequest(bucketName)
                        .withKeys(keys)
                        .withQuiet(true);

                s3Client.deleteObjects(deleteReq);

                // Set token for next iteration if truncated
                request.setContinuationToken(result.getNextContinuationToken());

            } while (result.isTruncated());

            System.out.println("Deleted folder: " + prefix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete folder: " + prefix, e);
        }
    }

    public String uploadLargeFile(File file, String objectKey) {
        String bucket = bucketName;
        long partSize = 100 * 1024 * 1024; // Increased to 100MB per part for better performance

        try {
            InitiateMultipartUploadRequest initRequest =
                    new InitiateMultipartUploadRequest(bucket, objectKey);

            InitiateMultipartUploadResult initResponse =
                    s3Client.initiateMultipartUpload(initRequest);

            List<PartETag> partETags = new ArrayList<>();

            long contentLength = file.length();
            long filePosition = 0;
            int partNumber = 1;

            while (filePosition < contentLength) {
                long size = Math.min(partSize, (contentLength - filePosition));

                UploadPartRequest uploadRequest = new UploadPartRequest()
                        .withBucketName(bucket)
                        .withKey(objectKey)
                        .withUploadId(initResponse.getUploadId())
                        .withPartNumber(partNumber++)
                        .withFileOffset(filePosition)
                        .withFile(file)
                        .withPartSize(size);

                partETags.add(s3Client.uploadPart(uploadRequest).getPartETag());
                filePosition += size;
            }

            CompleteMultipartUploadRequest compRequest =
                    new CompleteMultipartUploadRequest(
                            bucket,
                            objectKey,
                            initResponse.getUploadId(),
                            partETags
                    );

            s3Client.completeMultipartUpload(compRequest);
            return objectKey;

        } catch (Exception e) {
            throw new RuntimeException("Multipart upload failed → " + objectKey, e);
        }
    }

    public void uploadMultipartFile(MultipartFile file, String key) throws IOException {
        File temp = File.createTempFile("news_", file.getOriginalFilename());
        file.transferTo(temp);
        uploadFile(temp, key);
        temp.delete();
    }
    public void uploadMultipartFileForModel(MultipartFile file, String key) throws IOException {

        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(file.getSize());
        metadata.setContentType(file.getContentType());

        try (InputStream inputStream = file.getInputStream()) {

            PutObjectRequest putObjectRequest = new PutObjectRequest(
                    bucketName,
                    key,
                    inputStream,
                    metadata
            );

            s3Client.putObject(putObjectRequest);
        }
    }


    public void deleteObject(String key) {
        s3Client.deleteObject(bucketName, key);
    }

    public List<String> listObjectsByPrefix(String prefix) {

        List<String> keys = new ArrayList<>();

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withMaxKeys(1000);

        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(request);

            for (S3ObjectSummary summary : result.getObjectSummaries()) {
                // Skip "folders"
                if (!summary.getKey().endsWith("/")) {
                    keys.add(summary.getKey());
                }
            }

            request.setContinuationToken(result.getNextContinuationToken());

        } while (result.isTruncated());

        return keys;
    }

    public String buildPublicUrl(String objectKey) {
        return endpointUrl + "/" + bucketName + "/" + objectKey;
    }


    public List<String> listCommonPrefixes(String prefix) {

        List<String> folders = new ArrayList<>();

        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withDelimiter("/");

        ListObjectsV2Result result;

        do {
            result = s3Client.listObjectsV2(request);
            folders.addAll(result.getCommonPrefixes());
            request.setContinuationToken(result.getNextContinuationToken());
        } while (result.isTruncated());

        return folders;
    }

    public boolean objectExists(String key) {
        try {
            s3Client.getObject(bucketName, key);
            return true;
        } catch (AmazonS3Exception e) {
            if (e.getStatusCode() == 404) {
                return false;
            }
            throw e; // real error
        }
    }

    public boolean modelExists(String prefix) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withMaxKeys(1);
        ListObjectsV2Result result = s3Client.listObjectsV2(request);
        return !result.getObjectSummaries().isEmpty();
    }

    public boolean folderExists(String prefix) {
        ListObjectsV2Request request = new ListObjectsV2Request()
                .withBucketName(bucketName)
                .withPrefix(prefix)
                .withMaxKeys(1);

        ListObjectsV2Result response = s3Client.listObjectsV2(request);

        return !response.getObjectSummaries().isEmpty();
    }


    public void moveObject(String sourceKey, String destinationKey) {
        try {
            s3Client.copyObject(
                    bucketName,
                    sourceKey,
                    bucketName,
                    destinationKey
            );

            s3Client.deleteObject(
                    bucketName,
                    sourceKey
            );
        } catch (Exception e) {
            throw new RuntimeException("Error moving object from " + sourceKey + " to " + destinationKey, e);
        }
    }



    public void moveFolder(String sourcePrefix, String destinationPrefix) {
        try {
            ListObjectsV2Request request = new ListObjectsV2Request()
                    .withBucketName(bucketName)
                    .withPrefix(sourcePrefix);

            ListObjectsV2Result response;
            do {
                response = s3Client.listObjectsV2(request);

                if (!response.getObjectSummaries().isEmpty()) {
                    for (S3ObjectSummary object : response.getObjectSummaries()) {
                        String sourceKey = object.getKey();
                        String relativePath = sourceKey.substring(sourcePrefix.length());
                        String destinationKey = destinationPrefix + relativePath;

                        s3Client.copyObject(
                                bucketName,
                                sourceKey,
                                bucketName,
                                destinationKey
                        );

                        s3Client.deleteObject(
                                bucketName,
                                sourceKey
                        );
                    }
                }

                request.setContinuationToken(response.getNextContinuationToken());
            } while (response.isTruncated());
            System.out.println("Successfully moved objects from " + sourcePrefix + " to " + destinationPrefix);
        } catch (Exception e) {
            throw new RuntimeException("Error moving folder: " + sourcePrefix + " to " + destinationPrefix, e);
        }
    }



}


