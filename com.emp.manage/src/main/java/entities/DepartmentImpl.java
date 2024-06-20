package entities;

import java.util.ArrayList;
import java.util.List;

public class DepartmentImpl implements Department {
	private long id;
	private String name;
	private Employee manager;
	private List<Employee> employees;

	public DepartmentImpl(long id, String name, Employee manager) {
        this.id = id;
        this.name = name;
        this.manager = manager;
        this.employees = new ArrayList<>();
    }

	public DepartmentImpl() {

	}

	@Override
	public long getId() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Employee getManager() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Employee> getEmployees() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addEmployee(Employee employee) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeEmployee(Employee employee) {
		// TODO Auto-generated method stub

	}

}
