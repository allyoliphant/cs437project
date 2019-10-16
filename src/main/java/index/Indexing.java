package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.carrotsearch.hppc.IntIntHashMap;
import com.google.api.client.util.ArrayMap;

// http://fastutil.di.unimi.it/
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class Indexing {

	private static String pathToCSV = "src/main/resources/collection/wikipedia_text_files.csv";
	private static String pathToIndex = "src/main/resources/index.txt";
	private static String pathToUnsortedIndex = "src/main/resources/unsorted-index.txt";
	private static String pathToUnsortedIndex2 = "src/main/resources/unsorted-index2.txt";
	private static String pathToUnsortedIndex3 = "src/main/resources/unsorted-index3.txt";
	private static String pathToRank = "src/main/resources/rank.txt";
	private static String pathToRankUnder5 = "src/main/resources/rankUnder5.txt";
	private static String pathToRank6 = "src/main/resources/rank6.txt";
	private static ObjectOpenHashSet<String> stopwords = new ObjectOpenHashSet<String>();

	/** Methods for both building the index and processing the query **/

	/**
	 * Lower cases and removes certain characters
	 * 
	 * @param line that needs to be tokenized
	 * @return tokenized line
	 */
	public static String caseAndCharacters(String line) {
		String s1 = line.toLowerCase();
		s1 = Normalizer.normalize(s1, Normalizer.Form.NFD); // replaces non-English with English equivalent

		// see src/main/resources/asciifull.gif for ascii code translations
		s1 = s1.replaceAll("[^\\x00-\\x7F]", ""); // remove everything outside the ASCII list of characters
		s1 = s1.replaceAll("[\\x2F|\\x5F|\\x5C]", " "); // / _ \
		s1 = s1.replaceAll("[\\x21-\\x2C]", ""); // ! " # $ % & ' ( ) * + ,
		s1 = s1.replaceAll("[\\x2E]", ""); // .
		s1 = s1.replaceAll("[\\x3A-\\x40]", ""); // : ; < = > ? @
		s1 = s1.replaceAll("[\\x5B-\\x60]", ""); // [ ] ^ `
		s1 = s1.replaceAll("[\\x7B-\\x7E]", ""); // { | } ~
		s1 = s1.replaceAll("\\s[0-9]{1,3}\\s", " "); // numbers with one to three digits
		s1 = s1.replaceAll("\\s[0-9]{5,}\\s", " "); // numbers with five or more digits

		return s1;
	}

	/**
	 * Builds a HashMap of stopwords to be use when removing stopwords. Must be
	 * called before removing stopwords but should only be called once. Results in
	 * just over 4 million stopwords (way it should only be called once).
	 * 
	 * @throws IOException
	 */
	public static void getStopwords() throws IOException {
		System.out.println("get list of stopwords....");

		// stopwords from:
		// https://github.com/igorbrigadir/stopwords/blob/master/en/alir3z4.txt
		// https://drive.google.com/file/d/1GgXVQg11M2h0RMftEH_-o1HPcJgsdWSw/view
		BufferedReader s = new BufferedReader(new FileReader("src/main/resources/stopwords"));
		String online;
		while ((online = s.readLine()) != null) {
			String word = online.replaceAll(" ", "");
			if (!stopwords.contains(word)) {
				stopwords.add(word);
			}
		}
		s.close();

		// words with a really low frequency in the entire collection
		// there are like 4.3 million of them, they must be removed :)
		BufferedReader r = new BufferedReader(new FileReader(pathToRankUnder5));
		String rank;
		while ((rank = r.readLine()) != null) {
			String word = rank.replaceAll(" ", "");
			if (!stopwords.contains(word)) {
				stopwords.add(word);
			}
		}
		r.close();

		// top two ranked words
		stopwords.add("time");
		stopwords.add("school");
	}

	/**
	 * Remove stopwords and stem remaining words
	 * 
	 * @param line to have stopwords removed and remaining words stemmed
	 * @return line after stopwords removed and remaining words stemmed
	 */
	public static String stopAndStem(String line) {
		// stemmer from https://opennlp.apache.org/ - apache openNLP 1.9.1
		PorterStemmer2 stemmer = new PorterStemmer2();
		String[] tokens = line.split("\\s+");
		String result = "";
		for (String token : tokens) {
			// if token isn't a stopword, stem it and add to result
			if (!stopwords.contains(token)) {
				result += " " + stemmer.stem(token);
			}
		}
		return result;
	}

	/** Methods for just building the index **/

	/**
	 * Main method to build the index
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private static void buildIndex() throws IOException {
		frequencies();
		getStopwords();
		buildUnsortedIndex();
		stemSortWrite();
		System.out.println("index built!");
	}

	/**
	 * Parse through the csv file and add words, document ids, and frequency to
	 * index in no particular order
	 * 
	 * @throws IOException
	 */
	private static void buildUnsortedIndex() throws IOException {
		Object2ObjectOpenHashMap<String, String> index = new Object2ObjectOpenHashMap<String, String>();

		System.out.println("get collection/build unsorted index....");
		CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(pathToCSV));
		for (CSVRecord record : records) {
			System.out.println("on wiki " + record.get("id") + " of 1,662,756");
			String[] content = caseAndCharacters(" " + record.get("content") + " ").split("\\s+");
			ObjectOpenHashSet<String> unique = new ObjectOpenHashSet<String>(); // set of unique words in current doc
			for (String word : content) {
				// if word isn't a stopword or a duplicate
				if (!stopwords.contains(word) && !unique.contains(word)) {
					// nummber of times word occurs in current doc
					int freq = Collections.frequency(Arrays.asList(content), word);
					unique.add(word);
					// if word already in index, append docID and frequency, else just add docID and
					// frequency
					index.put(word,
							index.containsKey(word) ? index.get(word) + " {" + record.get("id") + "," + freq + "}"
									: "{" + record.get("id") + "," + freq + "}");
				}
			}
		}

		System.out.println("write unsorted index to file....");
		PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToUnsortedIndex)));
		index.forEach((key, value) -> {
			writer.println(key + " " + value);
		});
		writer.close();
	}

	/**
	 * Sort the index then write it to a file
	 * 
	 * @throws IOException
	 */
	private static void stemSortWrite() throws IOException {
		Object2ObjectOpenHashMap<String, IntIntHashMap> index = new Object2ObjectOpenHashMap<String, IntIntHashMap>();

		System.out.println("stem index....");
		PorterStemmer2 stemmer = new PorterStemmer2();
		BufferedReader in = new BufferedReader(new FileReader(pathToUnsortedIndex3));
		int count = 1;
		String line;
		while ((line = in.readLine()) != null) {
			System.out.println("on word " + count + " of 607,341");
			String[] entry = line.split("\\s+"); // entry[0] is the word and entry[1 to n-1] are the docIDs and freqs
			count++;
			if (entry.length > 1) {
				String stemmed = stemmer.stem(entry[0]);
				if (index.containsKey(stemmed)) {
					IntIntHashMap updatedDocList = index.get(stemmed);
					for (int i = 1; i < entry.length; i++) {
						// doc[0] is the id and doc[1] is the freq
						String[] doc = entry[i].replaceAll("[{}]", "").split(",");
						if (updatedDocList.containsKey(Integer.parseInt(doc[0]))) {
							// if doc and term combo already in index, add freqs
							updatedDocList.addTo(Integer.parseInt(doc[0]), Integer.parseInt(doc[1]));
						} else {
							// add doc to list
							updatedDocList.put(Integer.parseInt(doc[0]), Integer.parseInt(doc[1]));
						}
					}
					index.put(stemmed, updatedDocList);
				} else {
					// new word
					IntIntHashMap docList = new IntIntHashMap(); // map of docIDs and freqs
					for (int i = 1; i < entry.length; i++) {
						String[] doc = entry[i].replaceAll("[{}]", "").split(",");
						docList.put(Integer.parseInt(doc[0]), Integer.parseInt(doc[1]));
					}
					index.put(stemmed, docList);
				}
			}
		}
		in.close();

		System.out.println("sort index....");
		List<String> words = new ArrayList<String>(index.keySet());
		Collections.sort(words, new Comparator<String>() {
			public int compare(String o1, String o2) {
				return (o1).compareTo(o2);
			}
		});

		System.out.println("write sorted index to file....");
		PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToIndex)));
		int w = 1;
		for (String word : words) {
			if (word.compareTo("Invalid term") != 0) {
				IntIntHashMap docMap = index.get(word);
				String docList = "";
				// combine map of docIDs and freqs into a string
				int k = 1;
				for (int key : docMap.keys) {
					System.out.println("writing" + w + " of " + words.size() + " | doc " + k + " of " + docMap.keys.length);
					k++;
					if (key != 0) {
						docList += " {" + key + "," + docMap.get(key) + "}";
					}
					writer.println(word + docList);
				}
			}
			w++;
		}
		writer.close();
	}

	/**
	 * Method used to get the frequency in the collection of each word, besides the
	 * stopwords from the two listed links
	 * 
	 * @throws IOException
	 */
	private static void frequencies() throws IOException {
		// stopwords from:
		// https://github.com/igorbrigadir/stopwords/blob/master/en/alir3z4.txt
		// https://drive.google.com/file/d/1GgXVQg11M2h0RMftEH_-o1HPcJgsdWSw/view
		ObjectOpenHashSet<String> stopwordsTemp = new ObjectOpenHashSet<String>();
		BufferedReader s = new BufferedReader(new FileReader("src/main/resources/stopwords"));
		String line;
		while ((line = s.readLine()) != null) {
			String word = line.replaceAll(" ", "");
			if (!stopwordsTemp.contains(word)) {
				stopwordsTemp.add(word);
			}
		}
		s.close();

		PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToRank)));
		PrintWriter writerUnder5 = new PrintWriter(new FileWriter(new File(pathToRankUnder5)));
		PrintWriter writer6 = new PrintWriter(new FileWriter(new File(pathToRank6)));

		HashMap<String, Integer> vocab = new HashMap<String, Integer>();
		Reader in = new FileReader(pathToCSV);
		CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);

		for (CSVRecord record : records) {
			System.out.println("on wiki " + record.get("id") + " of 1,662,756");
			String content = " " + record.get("content") + " ";
			content = caseAndCharacters(content);
			String[] words = content.split("\\s+");

			HashMap<String, String> unique = new HashMap<String, String>();
			List<String> wordsAsList = Arrays.asList(words);

			for (String word : words) {
				if (!unique.containsKey(word) && !stopwordsTemp.contains(word)) {
					if (word.length() > 0) {
						unique.put(word, word);
						int freq = Collections.frequency(wordsAsList, word);
						if (vocab.containsKey(word)) {
							vocab.replace(word, vocab.get(word) + freq);
						} else {
							vocab.put(word, 1);
						}
					}
				}
			}
		}

		System.out.println("sort words by their frequency....");
		List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(vocab.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
			public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
				return (o1.getValue()).compareTo(o2.getValue());
			}
		});

		System.out.println("print words to the two files....");
		for (Map.Entry<String, Integer> entry : list) {
			writer.println(entry.getValue() + " " + entry.getKey());
			if (entry.getValue() < 6) {
				writerUnder5.println(entry.getKey());
			}
			if (entry.getValue() > 5 && entry.getValue() < 11) {
				writer6.println(entry.getKey());
			}
		}
		writer.close();
		writerUnder5.close();
		writer6.close();
		System.out.println("done getting rank!");
	}

	public static void move() throws IOException {
		ObjectOpenHashSet<String> rank6 = new ObjectOpenHashSet<String>();
		BufferedReader r = new BufferedReader(new FileReader(pathToRank6));
		String rank;
		while ((rank = r.readLine()) != null) {
			String word = rank.replaceAll(" ", "");
			if (!rank6.contains(word)) {
				rank6.add(word);
			}
		}
		r.close();

		System.out.println("write sorted index to file....");
		PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToUnsortedIndex3)));
		BufferedReader in = new BufferedReader(new FileReader(pathToUnsortedIndex2));
		int count = 1;
		String line;
		while ((line = in.readLine()) != null) {
			System.out.println("on word " + count + " of 805,742 | " + rank6.size());
			count++;
			String[] entry = line.split("\\s+");
			if (entry.length > 0 && !rank6.contains(entry[0])) {
				writer.println(line);
			}
		}
		in.close();
		writer.close();
		System.out.println("done");
	}

	public static void main(String[] args) throws IOException {
		stemSortWrite();
	}
}