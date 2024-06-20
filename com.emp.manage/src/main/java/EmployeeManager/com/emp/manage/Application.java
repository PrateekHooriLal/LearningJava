package EmployeeManager.com.emp.manage;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import entities.Department;
import entities.DepartmentImpl;

@SpringBootApplication
public class Application {

	public static void main(String[] args) {
		SpringApplication.run(Application.class, args);
		Department obj = new DepartmentImpl();
	}

	
}
