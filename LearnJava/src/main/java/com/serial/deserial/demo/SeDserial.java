package com.serial.deserial.demo;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

public class SeDserial implements Serializable {
	Employees emp = new Employees();
	private HashMap<String, String> movieDetail;

	// Movies mo = new Movies().setMovieDetail(movieDetail );

	private static final long serialVersionUID = 1L;

	void searialize() throws IOException {
		// FileInputStream fis = new FileInputStream(file)
		String s = "D://Iacsd_stuff//java_workspace//java_cdac_practice//src//printMydate//serial2.txt";
		FileOutputStream fos = new FileOutputStream("ser.txt");
		ObjectOutputStream objout = new ObjectOutputStream(fos);
		objout.writeObject(emp);
		objout.close();
		fos.close();
		System.out.println("Serializaton Done");
	}

	protected Employees Deserial() throws Exception {
		FileInputStream fin = new FileInputStream(
				"D://Iacsd_stuff//java_workspace//java_cdac_practice//src//printMydate//serial2.txt");
		ObjectInputStream objins = new ObjectInputStream(fin);
		Employees e = (Employees) objins.readObject();
		objins.close();

		System.out.println(e);
		System.out.println("Deserialization done");
		return e;
	}

	public static void main(String[] args) throws Exception {
		SeDserial d = new SeDserial();
		d.searialize();

		Employees e2 = d.Deserial();

		System.out.println("OBjECT E2 =" + e2 + e2.empid);

	}

}
