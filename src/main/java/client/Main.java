package client;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.Border;

import query.Query;

public class Main {
	public static void main(String[] argv) throws Exception {
		JFrame f = new JFrame("Wiki Search");
		
		JTextField textfield = new JTextField(30);
		textfield.setBounds(20, 20, 450, 20);
		JButton b = new JButton("search");
		b.setBounds(465, 20, 80, 20);
		
		
		

		JTextArea result1 = new JTextArea();
		result1.setBounds(20, 50, 940, 80);
		result1.setRows(5);
		result1.setLineWrap(true);
		result1.setWrapStyleWord(true);

		JTextArea result2 = new JTextArea();
		result2.setBounds(20, 140, 940, 80);
		result2.setRows(5);
		result2.setLineWrap(true);
		result2.setWrapStyleWord(true);

		JTextArea result3 = new JTextArea();
		result3.setBounds(20, 230, 940, 80);
		result3.setRows(5);
		result3.setLineWrap(true);
		result3.setWrapStyleWord(true);

		JTextArea result4 = new JTextArea();
		result4.setBounds(20, 320, 940, 80);
		result4.setRows(5);
		result4.setLineWrap(true);
		result4.setWrapStyleWord(true);

		JTextArea result5 = new JTextArea();
		result5.setBounds(20, 410, 940, 80);
		result5.setRows(5);
		result5.setLineWrap(true);
		result5.setWrapStyleWord(true);
		
		
		
		
		// add to frame
		f.add(textfield);
		f.add(b);
		f.add(result1);
		f.add(result2);
		f.add(result3);
		f.add(result4);
		f.add(result5);
		f.setSize(1000, 700);
		f.setLayout(null);
		f.setVisible(true);
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		// action listener
		b.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				Query q = new Query(textfield.getText());
				ArrayList<String[]> results = q.getResults();				
				result1.setText(results.get(0)[0] + "\n" + results.get(0)[1] + "\n" + results.get(0)[2]);
				result2.setText(results.get(1)[0] + "\n" + results.get(1)[1] + "\n" + results.get(1)[2]);
				result3.setText(results.get(2)[0] + "\n" + results.get(2)[1] + "\n" + results.get(2)[2]);
				result4.setText(results.get(3)[0] + "\n" + results.get(3)[1] + "\n" + results.get(3)[2]);
				result5.setText(results.get(4)[0] + "\n" + results.get(4)[1] + "\n" + results.get(4)[2]);
			}
		});
		
		textfield.addKeyListener(new KeyAdapter() {
	        @Override
	        public void keyPressed(KeyEvent e) {
	            if(e.getKeyCode() == KeyEvent.VK_SPACE){
	                System.out.println(textfield.getText());
	            }
	        }

	    });

	}
}

class MyKeyListener extends KeyAdapter {
	public void keyPressed(KeyEvent evt) {
		if (evt.getKeyChar() == 'a') {
			System.out.println("Check for key characters: " + evt.getKeyChar());
		}
		if (evt.getKeyCode() == KeyEvent.VK_HOME) {
			System.out.println("Check for key codes: " + evt.getKeyCode());
		}
	}
}
