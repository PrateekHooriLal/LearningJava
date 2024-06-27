package Learning.Lambda.stream.com;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

//convert  string to map of key : name , Value:salary usin streams
//3rd high salary in department

public class Main {
	static String str = "Prateek:9000,Ram:8000";

	public static void main(String[] args) {

		Map<String, String> map = Arrays.stream(str.split(",")).map(e -> e.split(":")).collect(Collectors.toMap(v -> v[0], v -> v[1]));
		System.out.println(map);

	}
}
