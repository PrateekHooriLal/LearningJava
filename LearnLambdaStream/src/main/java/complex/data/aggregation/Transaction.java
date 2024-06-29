package complex.data.aggregation;

public class Transaction {
	private String user;
	private String category;
	private double amount;

	public Transaction(String user, String category, double amount) {
		this.user = user;
		this.category = category;
		this.amount = amount;
	}

	public String getUser() {
		return user;
	}

	public String getCategory() {
		return category;
	}

	public double getAmount() {
		return amount;
	}
}
