package entities;

import java.util.List;

public interface Department {
    long getId();
    String getName();
    Employee getManager();
    List<Employee> getEmployees();
    void addEmployee(Employee employee);
    void removeEmployee(Employee employee);
}
