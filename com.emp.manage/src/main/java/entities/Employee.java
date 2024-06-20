package entities;

public interface Employee extends Convertible, Workable{
    Long getId();
    String getName();
    String getEmail();
    double getSalary();
    void setSalary(double salary);
    void work();
}
