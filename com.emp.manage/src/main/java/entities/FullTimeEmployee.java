package entities;

public class FullTimeEmployee implements Employee {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3018079713612118972L;
	private final Long id;
	private final String name;
	private String email;
	private double salary;

	// param constructor
	public FullTimeEmployee(Long id, String name, String email, Double salary) {

		this.id = id;
		this.name = name;
		this.email = email;
		this.salary = salary;
	}

	public Long getId() {
		return id;
	}

	public String getName() {
		return name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	@Override
	public double getSalary() {
		// TODO Auto-generated method stub
		return salary;
	}

	@Override
	public void setSalary(double salary) {
		// TODO Auto-generated method stub
		this.salary = salary;
	}

	@Override
	public int compareTo(Employee o) {
		// TODO Auto-generated method stub
		return (int) (this.getId() - o.getId());
	}

	@Override
	public void work() {
		// TODO Auto-generated method stub

	}

}
