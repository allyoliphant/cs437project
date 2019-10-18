package index;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
// http://fastutil.di.unimi.it/
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class Indexing {

	private String pathToCSV = "src/main/resources/collection/wikipedia_text_files.csv";
	private String pathToSERIndex = "src/main/resources/index.ser";
	private String pathToUnsortedIndex = "src/main/resources/unsorted-index.txt";
	private String pathToFreq = "src/main/resources/freq.txt";
	private String pathToFreqUnder15 = "src/main/resources/freqUnder15.txt";

	/**
	 * Lower cases and removes certain characters
	 * 
	 * @param line that needs to be tokenized
	 * @return tokenized line
	 */
	public String caseAndCharacters(String line) {

		String s1 = line.toLowerCase();

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
	 * Remove stopwords and stem remaining words
	 * 
	 * @param line to have stopwords removed and remaining words stemmed
	 * @return line after stopwords removed and remaining words stemmed
	 * @throws IOException
	 */
	public String stopAndStem(String line) {
		ObjectOpenHashSet<String> stopwords = getStopwords();

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

	@SuppressWarnings("unchecked")
	public Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> getIndex() {
		Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> index = new Object2ObjectOpenHashMap<String, Int2IntOpenHashMap>();
		try {
			FileInputStream fileIn = new FileInputStream(pathToSERIndex);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			index = (Object2ObjectOpenHashMap<String, Int2IntOpenHashMap>) in.readObject();
			in.close();
		} catch (IOException e) {
			System.err.println(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return index;
	}

	/**
	 * Main method to build the index
	 * 
	 * @throws IOException
	 */
	@SuppressWarnings("unused")
	private void buildIndex() {
		frequencies();
		System.gc();
		buildUnsortedIndex();
		System.gc();
		stemWrite();
		System.out.println("index built!");
	}

	/**
	 * Method used to get the frequency in the collection of each word, besides the
	 * stopwords from the two listed links
	 * 
	 * @throws IOException
	 */
	private void frequencies() {
		try {
			// stopwords from:
			// https://github.com/igorbrigadir/stopwords/blob/master/en/alir3z4.txt
			// https://drive.google.com/file/d/1GgXVQg11M2h0RMftEH_-o1HPcJgsdWSw/view
			ObjectOpenHashSet<String> stopwordsTemp = new ObjectOpenHashSet<String>();
			BufferedReader s = new BufferedReader(new FileReader("src/main/resources/stopwords"));
			String line;
			while ((line = s.readLine()) != null) {
				String word = line.replaceAll("\\s+", "");
				if (!stopwordsTemp.contains(word)) {
					stopwordsTemp.add(word);
				}
			}
			s.close();

			HashMap<String, Integer> vocab = new HashMap<String, Integer>();
			Reader in = new FileReader(pathToCSV);
			CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);

			for (CSVRecord record : records) {
				System.out.println("freq: on wiki " + record.get("id") + " of 1,662,756");
				String content = " " + record.get("content") + " ";
				content = caseAndCharacters(content);
				String[] words = content.split("\\s+");

				HashSet<String> unique = new HashSet<String>();
				List<String> wordsAsList = Arrays.asList(words);

				for (String word : words) {
					word = word.replaceAll("\\s+", "");
					if (!unique.contains(word) && !stopwordsTemp.contains(word)) {
						if (word.length() > 0) {
							unique.add(word);
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
			PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToFreq)));
			PrintWriter writerUnder15 = new PrintWriter(new FileWriter(new File(pathToFreqUnder15)));
			for (Map.Entry<String, Integer> entry : list) {
				writer.println(entry.getValue() + " " + entry.getKey());
				if (entry.getValue() < 16) {
					writerUnder15.println(entry.getKey());
				}
			}
			writer.close();
			writerUnder15.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse through the csv file and add words, document ids, and frequency to
	 * index in no particular order
	 * 
	 * @throws IOException
	 */
	private void buildUnsortedIndex() {
		try {
			Object2ObjectOpenHashMap<String, String> index = new Object2ObjectOpenHashMap<String, String>();
			ObjectOpenHashSet<String> stopwords = getStopwords();

			System.out.println("get collection/build unsorted index....");
			CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(pathToCSV));
			for (CSVRecord record : records) {
				System.out.println("build: on wiki " + record.get("id") + " of 1,662,756");
				String[] content = caseAndCharacters(" " + record.get("content") + " ").split("\\s+");
				ObjectOpenHashSet<String> unique = new ObjectOpenHashSet<String>(); // set of unique words in current
																					// doc
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
			int w = 1;
			index.forEach((key, value) -> {
				System.out.println("write unsort: " + w + " word of " + index.size());
				writer.println(key + " " + value);
			});
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds a HashMap of stopwords to be use when removing stopwords.
	 * 
	 * @throws IOException
	 */
	private ObjectOpenHashSet<String> getStopwords() {
		ObjectOpenHashSet<String> stopwords = new ObjectOpenHashSet<String>();
		try {
			// stopwords from:
			// https://github.com/igorbrigadir/stopwords/blob/master/en/alir3z4.txt
			// https://drive.google.com/file/d/1GgXVQg11M2h0RMftEH_-o1HPcJgsdWSw/view
			BufferedReader s = new BufferedReader(new FileReader("src/main/resources/stopwords"));
			String online;
			while ((online = s.readLine()) != null) {
				String word = online.replaceAll("\\s+", "");
				if (!stopwords.contains(word)) {
					stopwords.add(word);
				}
			}
			s.close();

			// words with a really low frequency in the entire collection
			// there are like 4.2 million of them, they must be removed :)
			BufferedReader f = new BufferedReader(new FileReader(pathToFreqUnder15));

			String freq;
			while ((freq = f.readLine()) != null) {
				String word = freq.replaceAll("\\s+", "");
				if (!stopwords.contains(word)) {
					stopwords.add(word);
				}
			}
			f.close();

			// top two ranked words
			stopwords.add("time");
			stopwords.add("school");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stopwords;
	}

	/**
	 * Stem words and add to the index, sort the index, then write it to a file
	 * 
	 * @throws IOException
	 */
	private void stemWrite() {
		try {
			Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> index = new Object2ObjectOpenHashMap<String, Int2IntOpenHashMap>();

			System.out.println("stem index....");
			PorterStemmer2 stemmer = new PorterStemmer2();
			BufferedReader in = new BufferedReader(new FileReader(pathToUnsortedIndex));
			int count = 1;
			String line;
			while ((line = in.readLine()) != null) {
				System.out.println("stem: on word " + count + " of 467,011");
				String[] entry = line.split("\\s+"); // entry[0] is the word and entry[1 to n-1] are the docIDs and
														// freqs
				count++;
				if (entry.length > 1) {
					String stemmed = stemmer.stem(entry[0]);
					if (index.containsKey(stemmed)) {
						Int2IntOpenHashMap updatedDocList = index.get(stemmed);
						for (int i = 1; i < entry.length; i++) {
							// doc[0] is the id and doc[1] is the freq
							String[] doc = entry[i].replaceAll("[{}]", "").split(",");
							if (updatedDocList.containsKey(Integer.parseInt(doc[0]))) {
								// if doc and term combo already in index, add freqs
								updatedDocList.replace(Integer.parseInt(doc[0]),
										updatedDocList.get(Integer.parseInt(doc[0])) + Integer.parseInt(doc[1]));
							} else {
								// add doc to list
								updatedDocList.put(Integer.parseInt(doc[0]), Integer.parseInt(doc[1]));
							}
						}
						index.replace(stemmed, updatedDocList);
					} else {
						// new word
						Int2IntOpenHashMap docList = new Int2IntOpenHashMap(); // map of docIDs and freqs
						for (int i = 1; i < entry.length; i++) {
							String[] doc = entry[i].replaceAll("[{}]", "").split(",");
							docList.put(Integer.parseInt(doc[0]), Integer.parseInt(doc[1]));
						}
						index.put(stemmed, docList);
					}
				}
			}
			in.close();

			System.out.println("write index of size " + index.size() + " to file....");
			FileOutputStream fileout = new FileOutputStream(pathToSERIndex);
			ObjectOutputStream out = new ObjectOutputStream(fileout);
			out.writeObject(index);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
	}
}