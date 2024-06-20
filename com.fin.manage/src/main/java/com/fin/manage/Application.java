package com.fin.manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan ({"com.fin.transaction.service","com.fin.entities"})
@EnableJpaRepositories("com.fin.entities.TransactionRepository")

public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
	}

}
