package com.resale.resalecontentmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class LoveResaleContentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(LoveResaleContentManagementApplication.class, args);
    }

}


