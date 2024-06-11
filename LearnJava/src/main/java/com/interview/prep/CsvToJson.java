package com.interview.prep;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

import org.json.CDL;
import org.json.JSONArray;
import org.json.JSONTokener;

/*https://examples.javacodegeeks.com/convert-csv-to-json-using-java
  Input Path to The CSV FILE:C:\Users\jojoh\eclipse-workspace\interview\src\main\resources\test_csv.txt
  static String in_path = "D:\\Prateek_Interview_Prepration_2024\\test_csv.txt";
*/
public class CsvToJson {

	static String in_path_csv = "C:\\Users\\jojoh\\eclipse-workspace\\interview\\src\\main\\resources\\test_csv.txt";
	static String in_path_json = "C:\\Users\\jojoh\\eclipse-workspace\\interview\\src\\main\\resources\\out_1.json";
	static String out_path = "C:\\Users\\jojoh\\eclipse-workspace\\interview\\src\\main\\resources\\";

	public static void main(String[] args) throws IOException {

		// Converting to CSV
		String json = convertCsvToJson(in_path_csv);
		System.out.println("Input Path to The CSV FILE:" + in_path_csv);
		writeToFile(out_path, json, "json");

		// Converting to json
		String csv = convertJsonToCsv(in_path_json);
		writeToFile(out_path, csv, "csv");

	}

	// Converting csv or content from txt file to Json without using POJO.
	public static String convertCsvToJson(String in_path) throws FileNotFoundException {

		// Reading data using buffered reader from IO package
		@SuppressWarnings("resource")
		String in = new BufferedReader(new FileReader(in_path)).lines().collect(Collectors.joining("\n"));

		return CDL.toJSONArray(in).toString();
	}

	public static String convertJsonToCsv(String in_path) throws IOException {

		// Reading data using InputStreamReader from IO package
		// For top efficiency, consider wrapping an InputStreamReader within a
		// BufferedReader.

		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(in_path)));
		JSONArray jarr = new JSONArray(new JSONTokener(in));
		in.close();
		return CDL.toString(jarr);
	}

	public static void writeToFile(String out_path, String content, String contentType) throws IOException {

		// 1// Using buffered Writer from IO package to write to the file
		BufferedWriter bw = new BufferedWriter(new FileWriter(new File(out_path + "out_1." + contentType)));
		bw.write(content);
		bw.close();

		// 2// Second way is to use FILES from nio package.
		Files.write(Path.of(out_path + "out_2." + contentType), content.getBytes(StandardCharsets.UTF_8));

		System.out.println("Content Saved to path: " + out_path + contentType);
	}

}
