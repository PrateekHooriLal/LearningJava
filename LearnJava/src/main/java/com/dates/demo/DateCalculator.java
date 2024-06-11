package com.dates.demo;

public class DateCalculator {

	public static void main(String... args) {
		DateCalculator dc = new DateCalculator();
		System.out.println(dc.calculateDate("01/12/2017", 3568779));
	}// man ends

	private int[] normalYear = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };
	private int[] leapYear = { 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

	public String calculateDate(String date, int numberOfDays) {
		String[] date_parts = date.split("/");
		int year = Integer.parseInt(date_parts[2]);
		int month = Integer.parseInt(date_parts[1]);
		int day = Integer.parseInt(date_parts[0]);
		int days = numberOfDays;

		int[] refYear = getRefYear(year);
		while (true) {
			int diff = days - (refYear[month - 1] - day);
			if (diff > 0) {
				days = diff;
				month++;
				day = 0;
				if (month <= 12) {
					continue;
				}
			} else {
				day += days;
				break;
			}
			year++;
			month = 1;
			refYear = getRefYear(year);
		}

		StringBuilder finalDate = new StringBuilder();
		finalDate.append(day);
		finalDate.append("/");
		finalDate.append(formaMonth(month));
		finalDate.append("/");
		finalDate.append(year);

		return finalDate.toString();

	}

	private int[] getRefYear(int year) {

		return isLeapYear(year) ? leapYear : normalYear;
	}

	private boolean isLeapYear(int year) {
		if ((year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)) {
			return true;
		}
		return false;
	}

	public String formaMonth(int mon) {

		switch (mon) {
		case 1:
			return "JAN";
		case 2:
			return "FEB";
		case 3:
			return "MAR";
		case 4:
			return "APR";
		case 5:
			return "MAY";
		case 6:
			return "JUNE";
		case 7:
			return "JULY";
		case 8:
			return "AUG";
		case 9:
			return "SEP";
		case 10:
			return "OCT";
		case 11:
			return "NOV";
		case 12:
			return "DEC";
		default:
			return "";
		}
	}

}
