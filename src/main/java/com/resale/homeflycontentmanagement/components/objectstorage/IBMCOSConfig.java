package com.resale.homeflycontentmanagement.components.objectstorage;


import com.ibm.cloud.objectstorage.ClientConfiguration;
import com.ibm.cloud.objectstorage.auth.AWSStaticCredentialsProvider;
import com.ibm.cloud.objectstorage.client.builder.AwsClientBuilder;
import com.ibm.cloud.objectstorage.oauth.BasicIBMOAuthCredentials;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3;
import com.ibm.cloud.objectstorage.services.s3.AmazonS3ClientBuilder;
import com.ibm.cloud.objectstorage.auth.AWSCredentials;
import jakarta.servlet.MultipartConfigElement;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

@Configuration
public class IBMCOSConfig {
    @Value("${ibm.cos.api-key}")
    private String apiKey;

    @Value("${ibm.cos.service-instance-id}")
    private String serviceInstanceId;

    @Value("${ibm.cos.endpoint-url}")
    private String endpointUrl;

    @Value("${ibm.cos.location}")
    private String location;

    @Bean
    public AmazonS3 ibmCOSClient() {

        AWSCredentials credentials = new BasicIBMOAuthCredentials(apiKey, serviceInstanceId);

        ClientConfiguration clientConfig = new ClientConfiguration()
                .withConnectionTimeout(120_000)   // 2 min
                .withSocketTimeout(120_000)       // 2 min
                .withRequestTimeout(0)            // disable timeout entirely
                .withTcpKeepAlive(true)
                .withMaxErrorRetry(5);

        return AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpointUrl, location))
                .withPathStyleAccessEnabled(true)
                .withClientConfiguration(clientConfig)
                .build();
    }
    @Bean
    public MultipartConfigElement multipartConfigElement() {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(DataSize.ofGigabytes(2));      // Allow up to 2GB
        factory.setMaxRequestSize(DataSize.ofGigabytes(2));
        return factory.createMultipartConfig();
    }

}


