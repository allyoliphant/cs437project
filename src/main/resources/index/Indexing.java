
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

public class Indexing {
	
	private static ArrayList<String[]> collection = new ArrayList<String[]>();
	private static ArrayList<String[]> collectionStopped = new ArrayList<String[]>();
	private static ArrayList<String[]> collectionStemmed = new ArrayList<String[]>();	
	
	private static void textAcquisition() throws IOException {
		// get the collection
		PrintWriter writer = new PrintWriter(new FileWriter(new File("src/main/resources/textAcquisition.txt")));
		File dir = new File("src/main/resources/collection");				
		for (File file : dir.listFiles()) {
		    Scanner s = new Scanner(file);		    
		    String content = "";
		    while (s.hasNext()) {
		    	String line = s.nextLine();
		    	line = line.toLowerCase();
		    	line = line.replaceAll("[.,:!?;()']", "");  // remove unnecessary characters
		    	content += line + " ";		    	
		    }		    		    
			String[] data = {file.getName(), content};
			writer.println(data[0]);
			writer.println(data[1]);
			collection.add(data);			
		    s.close();
		}
		writer.close();
	}
	
	private static void textTransformation() throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(new File("src/main/resources/textTransformation.txt")));
		
		// remove stop words
		// stop words from https://www.ranks.nl/stopwords - the default English list
		File file = new File("src/main/resources/stopwords");		
		ArrayList<String> stopwords = new ArrayList<String>();	
		Scanner s = new Scanner(file);		
		while (s.hasNext()) {
			stopwords.add(s.nextLine());			
		}
		s.close();
		for (int i = 0; i < collection.size(); i++) {
			String[] words = collection.get(i)[1].split(" ");
			String content = "";
			for(String word : words) {
				if (!stopwords.contains(word)) {
					content += word + " ";
				}
			}
			String[] data = {collection.get(i)[0], content};
			collectionStopped.add(data);
		}
		
		// stem words
		// stemmer from https://opennlp.apache.org/ - apache openNLP 1.9.1
		PorterStemmer stemmer = new PorterStemmer();
		for(int i = 0; i < collectionStopped.size(); i++) {
			String[] words = collectionStopped.get(i)[1].split(" ");
			String content = "";
			for(String word : words) {
				content += stemmer.stem(word) + " ";
			}
			String[] data = {collectionStopped.get(i)[0], content};
			writer.println(data[0]);
			writer.println(data[1]);
			collectionStemmed.add(data);
		}
		writer.close();	
	}
	
	private static void creation() throws IOException {
		PrintWriter writerStart = new PrintWriter(new FileWriter(new File("src/main/resources/creationStart.txt")));
		PrintWriter writerSort = new PrintWriter(new FileWriter(new File("src/main/resources/creationSort.txt")));
		PrintWriter writerComplete = new PrintWriter(new FileWriter(new File("src/main/resources/creationComplete.txt")));
		
		// get vocabulary
		ArrayList<String[]> vocab = new ArrayList<String[]>();
		for (int i = 0; i < collectionStemmed.size(); i++) {
			String[] words = collectionStemmed.get(i)[1].split(" ");
			for (String word : words) {
				String[] w = {word, Integer.toString(i+1)};
				writerStart.println(w[0] + " " + w[1]);
				vocab.add(w);
			}
		}
		writerStart.close();
		
		// sort vocabulary
		Collections.sort(vocab, new Comparator<String[]>() {
            public int compare(String[] strings, String[] otherStrings) {
                return strings[0].compareTo(otherStrings[0]);
            }
        });
		for (String[] word : vocab) {
			writerSort.println(word[0] + " " + word[1]);
		}
		writerSort.close();
		
		// combine words
		ArrayList<ArrayList<Object>> index = new ArrayList<ArrayList<Object>>();
		for (int i = 0; i < vocab.size(); i++) {
			// word already in list - add document id or increase frequency
			if (index.size() > 0 && ((String) index.get(index.size() - 1).get(0)).compareTo(vocab.get(i)[0]) == 0) {
				ArrayList<Object> word = index.get(index.size() - 1);
				boolean newDoc = true;
				for (int w = 1; w < word.size(); w++) {
					String[] doc = (String[]) word.get(w);
					if (doc[0].compareTo(vocab.get(i)[1]) == 0) {
						String freq = Integer.toString(Integer.parseInt(doc[1]) + 1);
						word.set(w, new String[] {doc[0], freq});
						newDoc = false;
					}
				}
				if (newDoc) {
					word.add(new String[] {vocab.get(i)[1], "1"});
				}
				index.set(index.size() - 1, word);
			} 
			else {  // new word
				ArrayList<Object> word = new ArrayList<Object>();
				word.add(vocab.get(i)[0]);  // add word
				word.add(new String[] {vocab.get(i)[1], "1"});  // add doc id and frequency
				index.add(word);
			}
		}
		
		// print to file
		for (int i = 0; i < index.size(); i++) {
			writerComplete.print(index.get(i).get(0));
			for (int w = 1; w < index.get(i).size(); w++) {
				writerComplete.print(" {" + ((String[])index.get(i).get(w))[0] +", " 
						+ ((String[])index.get(i).get(w))[1] + "}");
			}
			writerComplete.print("\n");
		}
		writerComplete.close();	
	}
	
	
	public static void main(String[] args) throws IOException {
		textAcquisition();
		textTransformation();
		creation();
		System.out.println("done");
	}
}