package com.lyzer.lyzerrecime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing  // activates @CreatedDate / @LastModifiedDate population
public class LyzerRecimeApplication {

    public static void main(String[] args) {
        SpringApplication.run(LyzerRecimeApplication.class, args);
    }

}
