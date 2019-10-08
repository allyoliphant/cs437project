package index;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

public class Indexing {

	private static ArrayList<String> stopwords;
	private static String pathToCSV = "src/main/resources/collection/mini-collection.csv";
	private static String pathToVocab = "src/main/resources/mini-vocab.txt";
	private static String pathToIndex = "src/main/resources/mini-index.txt";
	private static Path path = FileSystems.getDefault().getPath(pathToVocab);

	private static void buildIndex() throws IOException {
		getStopwords();
		Reader in = new FileReader(pathToCSV);
		PrintWriter vocabWriter = new PrintWriter(new FileWriter(new File(pathToVocab)));

		// get collection and build unsorted index
		System.out.println("get collection/build unsorted index...");
		CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);
		for (CSVRecord record : records) {
			System.out.println("on wiki " + record.get("id") + " of 1,662,756");
			String content = record.get("content");
			content = caseAndCharacters(content);
			content = removeStopwords(content);
			String[] tokens = stemContent(content);
			addWordsToVocab(tokens, record.get("id"), vocabWriter);
		}
		vocabWriter.close();

		// sort and combine vocab into index
		System.out.println("sort and combine vocab into index...");
		sortAndCombine();

		System.out.println("index built");
	}

	public static String caseAndCharacters(String line) {
		String s1 = line.toLowerCase();
		s1 = Normalizer.normalize(s1, Normalizer.Form.NFD);  // replaces non-English with English equivalent
		s1 = s1.replaceAll("[^\\x00-\\x7A]", "");  // remove everything outside the ASCII list of characters
		s1 = s1.replaceAll("[\\x2F|\\x5F|\\x5C]", " ");	 // / _ \
		s1 = s1.replaceAll("[\\x2E]", "");  // .
		s1 = s1.replaceAll("[\\x3A-\\x40]", "");
		s1 = s1.replaceAll("[\\x21-\\x2C]", "");
		s1 = s1.replaceAll("[\\x5B-\\x60]", "");
		s1 = s1.replaceAll(" " + "[\\x30-\\x39]{1,3}" + " ", " ");
		s1 = s1.replaceAll(" " + "[\\x30-\\x39]{5,}" + " ", " ");
		return s1;
	}

	public static void getStopwords() throws FileNotFoundException {
		// stop words from https://www.ranks.nl/stopwords - the default English list
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
	
	private static void addWordsToVocab(String[] words, String docID, PrintWriter writer) {
		ArrayList<String> content = new ArrayList<String>();
		ArrayList<Integer> freq = new ArrayList<Integer>();
		for (String word : words) {
			if (content.contains(word)) {
				int f = freq.get(content.indexOf(word)) + 1;
				freq.set(content.indexOf(word), f);
			}
			else {
				content.add(word);
				freq.add(1);
			}
		}
		for (int i = 0; i < content.size(); i++) {
			writer.println(content.get(i) + " {" + docID + "," + freq.get(i) + "}");
		}
	}

	private static void sortAndCombine() throws IOException {
		PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToIndex)));
		
		ArrayList<String> vocab = new ArrayList<String>();
		Scanner s = new Scanner(new File(pathToVocab));
		while (s.hasNext()) {
			vocab.add(s.nextLine());
		}
		s.close();
		
		Collections.sort(vocab, new Comparator<String>() {
			public int compare(String strings, String otherStrings) {
				return strings.compareTo(otherStrings);
			}
		});
		
		String wordDocFreq = "";
		String previousWord = "";
		for (int i = 0; i < vocab.size(); i++) {
			if (i > 0) {
				String[] v = vocab.get(i).split(" ");
				if (v[0].compareTo(previousWord) == 0) {  // same word
					for (int j = 1; j < v.length; j++) {
						wordDocFreq += " " + v[j];
					}
				}
				else {  // new word
					writer.println(wordDocFreq);
					previousWord = vocab.get(i).split(" ")[0];
					wordDocFreq = vocab.get(i);
				}			
			}
			else {
				previousWord = vocab.get(i).split(" ")[0];
				wordDocFreq = vocab.get(i);
			}
		}
		writer.println(wordDocFreq);
		writer.close();
	}

	public static void main(String[] args) throws IOException {
		buildIndex();
	}
}