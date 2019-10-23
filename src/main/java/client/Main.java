package client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import query.Query;
import query.Suggestions;

public class Main {
	public static void main(String[] argv) throws Exception {
		JFrame f = new JFrame("Ally and Sammy's Wiki Search");

		JTextField textfield = new JTextField(30);
		textfield.setBounds(20, 20, 450, 20);
		JButton b = new JButton("search");
		b.setBounds(465, 20, 80, 20);

		JTextArea suggestions = new JTextArea();
		suggestions.setBounds(20, 50, 940, 80);
		suggestions.setRows(5);
		suggestions.setLineWrap(true);
		suggestions.setWrapStyleWord(true);

		JTextArea result1 = new JTextArea();
		result1.setBounds(20, 140, 940, 80);
		result1.setRows(5);
		result1.setLineWrap(true);
		result1.setWrapStyleWord(true);

		JTextArea result2 = new JTextArea();
		result2.setBounds(20, 230, 940, 80);
		result2.setRows(5);
		result2.setLineWrap(true);
		result2.setWrapStyleWord(true);

		JTextArea result3 = new JTextArea();
		result3.setBounds(20, 320, 940, 80);
		result3.setRows(5);
		result3.setLineWrap(true);
		result3.setWrapStyleWord(true);

		JTextArea result4 = new JTextArea();
		result4.setBounds(20, 410, 940, 80);
		result4.setRows(5);
		result4.setLineWrap(true);
		result4.setWrapStyleWord(true);

		JTextArea result5 = new JTextArea();
		result5.setBounds(20, 500, 940, 80);
		result5.setRows(5);
		result5.setLineWrap(true);
		result5.setWrapStyleWord(true);

		// add to frame
		f.add(textfield);
		f.add(b);
		f.add(suggestions);
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
				result1.setText("");
				result2.setText("");
				result3.setText("");
				result4.setText("");
				result5.setText("");

				Query q = new Query(textfield.getText());
				ArrayList<String[]> results = q.getResults();
				if (results.size() == 0) {
					result1.setText("no results for: " + textfield.getText());
				} else {
					if (results.size() > 0)
						result1.setText(results.get(0)[0] + "\n" + results.get(0)[1] + "\n" + results.get(0)[2]);
					if (results.size() > 1)
						result2.setText(results.get(1)[0] + "\n" + results.get(1)[1] + "\n" + results.get(1)[2]);
					if (results.size() > 2)
						result3.setText(results.get(2)[0] + "\n" + results.get(2)[1] + "\n" + results.get(2)[2]);
					if (results.size() > 3)
						result4.setText(results.get(3)[0] + "\n" + results.get(3)[1] + "\n" + results.get(3)[2]);
					if (results.size() > 4)
						result5.setText(results.get(4)[0] + "\n" + results.get(4)[1] + "\n" + results.get(4)[2]);
				}
			}
		});

		textfield.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_SPACE) {
					System.out.println("getting suggestion queries....");
					Suggestions s = new Suggestions();
					try {
						Set<String> suggest = s.getCandidates(textfield.getText());
						String sug = "suggested queries:";
						for (String query : suggest) {
							sug += "\n" + query;
						}
						suggestions.setText(sug);

					} catch (FileNotFoundException e1) {
						e1.printStackTrace();
					}
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
