package com.whiplash;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class CountFiles {

	public static void main(String[] args) throws IOException {
		File fs = new File("C:/softwares/new_gospel_songs");

		File[] items = fs.listFiles();
		Arrays.sort(items);
		int f = 0, d = 0;

		for (File i : items) {
			// System.out.println(i.getName()
			// +": Size="+i.getTotalSpace()+i.getAbsoluteFile());
			if (i.isFile())
				f++;
			else if (i.isDirectory())
				d++;
		}
		System.out.println(items.length + " " + f + ":" + d);

	}

}
