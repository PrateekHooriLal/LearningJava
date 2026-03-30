package com.interview.assignment.lead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// scanBasePackages added so Spring also picks up com.interview.altimetrik.banktransfer
// Without this, @Service/@RestController/@Repository in other packages are ignored
@SpringBootApplication(scanBasePackages = "com.interview")
public class WalletApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletApplication.class, args);
    }

}
