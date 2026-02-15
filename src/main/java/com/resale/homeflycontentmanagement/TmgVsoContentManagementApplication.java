package com.resale.homeflycontentmanagement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableFeignClients
@SpringBootApplication
public class TmgVsoContentManagementApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmgVsoContentManagementApplication.class, args);
    }

}


