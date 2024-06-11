package com.print.Mydate;

import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class Thread_demo extends Frame implements Runnable, ActionListener {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Thread t1, t2, t3;
	Button start, stop;
	int t1x, t2x, t3x, t1y, t2y, t3y;
	boolean flag = true;

	public Thread_demo() {
		super("My_Demo");
		t1 = new Thread(this);
		t2 = new Thread(this);
		t3 = new Thread(this);
		start = new Button("start");
		stop = new Button("stop");
		setLayout(new FlowLayout());
		this.add(start);
		this.add(stop);
		start.addActionListener(this);
		stop.addActionListener(this);
		setparam();
		addWindowListener(new WindowAdapter() {

			public void windowClosing(WindowEvent e) {
				System.exit(0);

			}
		});

	}// constructor ends

	private void setparam() {

		t1y = 60;
		t2y = 80;
		t3y = 100;
		t1x = t2x = t3x = 200;
	}// parameter function ends

	public void start_thread() {
		t1 = new Thread(this);
		t2 = new Thread(this);
		t3 = new Thread(this);
		flag = true;
		t1.start();
		t2.start();
		t3.start();
	}

	// coloring the balls
	public void paint(Graphics g) {
		g.fillOval(t1x, t1y, 20, 20);
		g.setColor(Color.blue);

		g.fillOval(t2x, t2y, 20, 20);
		g.setColor(Color.BLACK);

		g.fillOval(t3x, t3y, 20, 20);
		g.setColor(Color.GREEN);

	}

	@Override
	public void run() {
		while (flag) {
			synchronized (this) {
				try {
					if (Thread.currentThread() == t1) {
						t1x = t1x + 15;
						Thread.sleep(90);// as sleep is a static method should
											// called using class name..
						if (t1x > 500) {
							t1.wait();
						}
						repaint();
					} // t1 if ends
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} // t1 sync block ends

			synchronized (this) {
				try {
					if (Thread.currentThread() == t2) {
						t2x = t2x + 20;
						Thread.sleep(90);// as sleep is a static method should
											// called using class name..
						if (t2x > 500) {
							t2.wait();

						}
						repaint();
					} // t2 if ends
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} // t2 sync block ends

			synchronized (this) {
				try {
					if (Thread.currentThread() == t3) {
						t3x = t3x + 5;
						Thread.sleep(90);// as sleep is a static method should
											// called using class name..
						if (t3x > 500) {
							notifyAll();
							setparam();
							// t3.wait();
						}
						repaint();
					} // t3 if ends
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} // t3 sync block ends

		} // while ends

	}// run methods

	@Override
	public void actionPerformed(ActionEvent e) {
	}

	public static void main(String[] args) {
		Thread_demo memo = new Thread_demo();
		memo.setSize(500, 200);
		memo.setVisible(true);
		memo.start_thread();
	}// main ends

}// class ends
