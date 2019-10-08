package index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Scanner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class Indexing {

	private static ArrayList<String> stopwords;
	private static String pathToCSV = "src/main/resources/collection/wikipedia_text_files.csv";
	private static String pathToVocab = "src/main/resources/vocab.txt";
	private static String pathToIndex = "src/main/resources/index.txt";

	public static String caseAndCharacters(String line) {
		String s1 = line.toLowerCase();
		s1 = Normalizer.normalize(s1, Normalizer.Form.NFD); // replaces non-English with English equivalent
		s1 = s1.replaceAll("[^\\x00-\\x7F]", ""); // remove everything outside the ASCII list of characters
		s1 = s1.replaceAll("[\\x2F|\\x5F|\\x5C]", " "); // / _ \
		s1 = s1.replaceAll("[\\x21-\\x2C]", ""); // ! " # $ % & ' ( ) * + ,
		s1 = s1.replaceAll("[\\x2E]", ""); // .
		s1 = s1.replaceAll("[\\x3A-\\x40]", ""); // : ; < = > ? @
		s1 = s1.replaceAll("[\\x5B-\\x60]", ""); // [ ] ^ `
		s1 = s1.replaceAll("[\\x7B-\\x7E]", ""); // { | } ~
		s1 = s1.replaceAll("\\s[0-9]{1,3}\\s", " ");  // numbers with one to three digits
		s1 = s1.replaceAll("\\s[0-9]{5,}\\s", " ");  // numbers with five or more digits
		return s1;
	}

	public static void getStopwords() throws FileNotFoundException {
		System.out.println("get list of stopwords....");
		// stop words from https://www.ranks.nl/stopwords - the default long stop word list with ' removed
		stopwords = new ArrayList<String>();
		Scanner s = new Scanner(new File("src/main/resources/stopwords"));
		while (s.hasNext()) {
			stopwords.add(s.nextLine());
		}
		s.close();
	}

	public static String removeStopwords(String line) {
		for (String word : stopwords) {
			line = line.replaceAll("\\s+" + word + "\\s+", " ");
		}
		return line;
	}

	public static String[] stemContent(String content) {
		// stemmer from https://opennlp.apache.org/ - apache openNLP 1.9.1
		PorterStemmer stemmer = new PorterStemmer();
		String[] tokens = content.split("\\s+");
		for (int i = 0; i < tokens.length; i++) {
			String token = stemmer.stem(tokens[i]);
			tokens[i] = token;
		}
		return tokens;
	}
	
	public static int getWordIndex(String word, ArrayList<String> index) {
		for (int i = 0; i < index.size(); i++) {
			String w = index.get(i).split(" ")[0];
			if (w.compareTo(word) == 0) {
				return i;
			}
		}
		return -1;
	}

	private static void buildIndex() throws IOException {
		getStopwords();
		buildVocab();
		combineSortWrite();
		System.out.println("index built!");
	}

	private static void buildVocab() throws IOException {
		Reader in = new FileReader(pathToCSV);
		PrintWriter vocabWriter = new PrintWriter(new FileWriter(new File(pathToVocab)));
		
		System.out.println("get collection/build unsorted vocab....");
		CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);
		for (CSVRecord record : records) {
			System.out.println("on wiki " + record.get("id") + " of 1,662,756");
			String content = " " + record.get("content") + " ";
			content = caseAndCharacters(content);
			content = removeStopwords(content);
			String[] tokens = stemContent(content);
			addWordsToVocab(tokens, record.get("id"), vocabWriter);
		}
		vocabWriter.close();
	}

	private static void addWordsToVocab(String[] words, String docID, PrintWriter writer) {
		ArrayList<String> content = new ArrayList<String>();
		ArrayList<Integer> freq = new ArrayList<Integer>();
		for (String word : words) {
			if (content.contains(word)) {
				int f = freq.get(content.indexOf(word)) + 1;
				freq.set(content.indexOf(word), f);
			} else {
				content.add(word);
				freq.add(1);
			}
		}
		for (int i = 0; i < content.size(); i++) {
			writer.println(content.get(i) + " {" + docID + "," + freq.get(i) + "}");
		}
	}

	private static void combineSortWrite() throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToIndex)));
		Scanner s = new Scanner(new File(pathToVocab));
		ArrayList<String> index = new ArrayList<String>();
		
		System.out.println("combine vocab into index....");
		int lineNumber = 1;
		while (s.hasNext()) {
			System.out.println("on line " + lineNumber + " of about 260,016,921");
			lineNumber++;
			String line = s.nextLine();
			String[] entry = line.split(" ");
			int wordIndex = getWordIndex(entry[0], index);
			if (wordIndex != -1) {
				String indexEntry = index.get(wordIndex);
				for (int i = 1; i < entry.length; i++) {
					indexEntry += " " + entry[i];
				}
				index.set(wordIndex, indexEntry);
			}
			else {
				index.add(line);
			}
		}
		s.close();

		System.out.println("sort index....");
		Collections.sort(index, new Comparator<String>() {
			public int compare(String strings, String otherStrings) {
				return strings.compareTo(otherStrings);
			}
		});		

		System.out.println("write index to file....");
		for (String entry : index) {
			writer.println(entry);
		}		
		writer.close();
	}

	public static void main(String[] args) throws IOException {
		combineSortWrite();
	}
}