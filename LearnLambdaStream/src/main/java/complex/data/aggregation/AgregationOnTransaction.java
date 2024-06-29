package complex.data.aggregation;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/*Problem 1: Complex Data Aggregation
Given a list of transactions, calculate the total amount spent by each user in each category*/

public class AgregationOnTransaction {
	static List<Transaction> transactions = Arrays.asList(
		    new Transaction("Alice", "Groceries", 150.0),
		    new Transaction("Alice", "Electronics", 200.0),
		    new Transaction("Bob", "Groceries", 100.0),
		    new Transaction("Alice", "Groceries", 50.0),
		    new Transaction("Bob", "Electronics", 300.0)
		);
	
	public static void main(String[] args) {
		System.out.println(aggregateTransactions(transactions));
	}
	public static Map<String, Map<String, Double>> aggregateTransactions(List<Transaction> transactions) {
	    return (Map<String, Map<String, Double>>) transactions.stream().
	    		collect(Collectors.groupingBy(Transaction::getUser,
	    				Collectors.groupingBy(Transaction::getCategory,Collectors.summingDouble(Transaction::getAmount))));
	    		}
}
