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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

// http://fastutil.di.unimi.it/
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

public class Indexing {

	// path to the different files used
	private String pathToCSV = "src/main/resources/collection/wikipedia_text_files.csv";
	private String pathToSERIndex = "src/main/resources/index.ser";
	private String pathToUnsortedIndex = "src/main/resources/unsorted-index.txt";
	private String pathToFreq = "src/main/resources/freq.txt";
	private String pathToFreq49 = "src/main/resources/freq49.txt";
	private String pathToMaxDocFreq = "src/main/resources/maxDocFreq.ser";
	private String pathToStopwords = "src/main/resources/stopwords";

	/**
	 * Lower cases the string and removes certain characters
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
	 * Remove stopwords from string and stem remaining words
	 * 
	 * @param line to have stopwords removed and remaining words to stemmed
	 * @return line after stopwords removed and remaining words are stemmed
	 */
	public String stopAndStem(String line) {
		ObjectOpenHashSet<String> stopwords = getStopwords(); // get the set of stopwords
		PorterStemmer stemmer = new PorterStemmer();
		String[] tokens = line.split("\\s+"); // split the string into an array of tokens
		String result = "";
		for (String token : tokens) {
			// if token isn't a stopword, stem it and add it to the result
			// if token is a stopword, don't include it in the result
			if (!stopwords.contains(token)) {
				result += " " + stemmer.stem(token);
			}
		}
		return result;
	}

	/**
	 * Get a subset of the index that contains all the terms that are in the index
	 * Doesn't return the whole index (causes an out-of-memory error)
	 * 
	 * @param terms we want from the index
	 * @return subset of the index
	 */
	@SuppressWarnings("unchecked")
	public Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> getIndex(String[] terms) {
		Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> index = new Object2ObjectOpenHashMap<String, Int2IntOpenHashMap>();
		Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> result = new Object2ObjectOpenHashMap<String, Int2IntOpenHashMap>();
		try {
			FileInputStream fileIn = new FileInputStream(pathToSERIndex);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			index = (Object2ObjectOpenHashMap<String, Int2IntOpenHashMap>) in.readObject();
			in.close();

			// get just the entries of each term (if in the index)
			for (String word : terms) {
				if (index.containsKey(word)) {
					result.put(word, index.get(word));
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			System.err.println(e);
		}
		return result;
	}

	/**
	 * Get the max frequency of terms for the provided documents
	 * 
	 * @param set of document ids
	 * @return map of doc ids and max frequencies
	 */
	public Int2IntOpenHashMap getMaxDocFreq(IntSet resources) {
		Int2IntOpenHashMap maxFreq = new Int2IntOpenHashMap();
		try {
			FileInputStream fileIn = new FileInputStream(pathToMaxDocFreq);
			ObjectInputStream in = new ObjectInputStream(fileIn);
			Int2IntOpenHashMap allMaxFreq = (Int2IntOpenHashMap) in.readObject();
			in.close();

			// get the entries of each document in resources
			for (int doc : resources) {
				maxFreq.put(doc, allMaxFreq.get(doc));
			}
		} catch (IOException e) {
			System.err.println(e);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		return maxFreq;
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
			BufferedReader s = new BufferedReader(new FileReader(pathToStopwords));
			String line;
			while ((line = s.readLine()) != null) {
				String word = line.replaceAll("\\s+", "");
				if (!stopwordsTemp.contains(word)) {
					stopwordsTemp.add(word);
				}
			}
			s.close();

			HashMap<String, Integer> vocab = new HashMap<String, Integer>();
			Int2IntOpenHashMap maxFreq = new Int2IntOpenHashMap();
			Reader in = new FileReader(pathToCSV);
			CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);

			// parse the wiki collection
			for (CSVRecord record : records) {
				System.out.println("freq: on wiki " + record.get("id") + " of 1,662,756");
				String content = " " + record.get("content") + " ";
				content = caseAndCharacters(content);
				String[] words = content.split("\\s+");

				Object2IntOpenHashMap<String> unique = new Object2IntOpenHashMap<String>();

				// get the frequency of the unstemmed word in the entire collection
				for (String word : words) {
					word = word.replaceAll("\\s+", "");
					if (!stopwordsTemp.contains(word) && word.length() > 0) {
						int freq = 1;
						if (unique.containsKey(word)) {
							freq += unique.getInt(word);
							unique.replace(word, freq);
						} else {
							unique.put(word, freq);
						}
						if (vocab.containsKey(word)) {
							vocab.replace(word, vocab.get(word) + freq);
						} else {
							vocab.put(word, freq);
						}
					}
				}

				// get max frequency of stemmed words in document (for ranking documents)
				int max = 0;
				Object2IntOpenHashMap<String> stemUnique = new Object2IntOpenHashMap<String>();
				PorterStemmer stemmer = new PorterStemmer();
				for (String word : unique.keySet()) {
					// not an additional stopword
					if (vocab.get(word) > 49 && word.compareTo("-") != 0 && word.compareTo("school") != 0) {
						String stem = stemmer.stem(word);
						if (stemUnique.containsKey(stem)) {
							stemUnique.addTo(stem, unique.getInt(word));
						} else {
							stemUnique.put(stem, unique.getInt(word));
						}
						if (stemUnique.getInt(stem) > max) {
							max = stemUnique.getInt(stem);
						}
					}
				}
				maxFreq.put(Integer.parseInt(record.get("id")), max);
			}

			FileOutputStream fileout = new FileOutputStream(pathToMaxDocFreq);
			ObjectOutputStream out = new ObjectOutputStream(fileout);
			out.writeObject(maxFreq);
			out.close();

			System.out.println("sort words by their frequency....");
			List<Map.Entry<String, Integer>> list = new LinkedList<Map.Entry<String, Integer>>(vocab.entrySet());
			Collections.sort(list, new Comparator<Map.Entry<String, Integer>>() {
				public int compare(Map.Entry<String, Integer> o1, Map.Entry<String, Integer> o2) {
					return (o1.getValue()).compareTo(o2.getValue());
				}
			});

			System.out.println("print words to the two files....");
			PrintWriter writer = new PrintWriter(new FileWriter(new File(pathToFreq)));
			PrintWriter writer49 = new PrintWriter(new FileWriter(new File(pathToFreq49)));
			for (Map.Entry<String, Integer> entry : list) {
				writer.println(entry.getValue() + " " + entry.getKey());
				if (entry.getValue() < 50) {
					writer49.println(entry.getKey());
				}
			}
			writer.close();
			writer49.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Parse through the csv file and add unstemmed words, document ids, and
	 * frequency to index in no particular order
	 * 
	 * @throws IOException
	 */
	private void buildUnsortedIndex() {
		try {
			Object2ObjectOpenHashMap<String, String> index = new Object2ObjectOpenHashMap<String, String>();
			ObjectOpenHashSet<String> stopwords = getStopwords();

			System.out.println("get collection/build unsorted index....");
			CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(new FileReader(pathToCSV));

			// parse through the wiki collection
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
			index.forEach((key, value) -> {
				writer.println(key + " " + value);
			});
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Builds a HashSet of stopwords to be used when removing stopwords.
	 * 
	 * @throws IOException
	 */
	public ObjectOpenHashSet<String> getStopwords() {
		ObjectOpenHashSet<String> stopwords = new ObjectOpenHashSet<String>();
		try {
			// stopwords from:
			// https://github.com/igorbrigadir/stopwords/blob/master/en/alir3z4.txt
			// https://drive.google.com/file/d/1GgXVQg11M2h0RMftEH_-o1HPcJgsdWSw/view
			BufferedReader s = new BufferedReader(new FileReader(pathToStopwords));
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
			BufferedReader f = new BufferedReader(new FileReader(pathToFreq49));
			String freq;
			while ((freq = f.readLine()) != null) {
				String word = freq.replaceAll("\\s+", "");
				if (!stopwords.contains(word)) {
					stopwords.add(word);
				}
			}
			f.close();

			// top three ranked words
			stopwords.add("-");
			stopwords.add("school");
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stopwords;
	}

	/**
	 * Stem words and add them to the index, then write the index to a file
	 * 
	 * @throws IOException
	 */
	private void stemWrite() {
		try {
			Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> index = new Object2ObjectOpenHashMap<String, Int2IntOpenHashMap>();

			System.out.println("stem index....");
			PorterStemmer stemmer = new PorterStemmer();
			BufferedReader in = new BufferedReader(new FileReader(pathToUnsortedIndex));
			int count = 1;
			String line;

			// each entry in the unstemmed index
			while ((line = in.readLine()) != null) {
				System.out.println("stem: on word " + count + " of 400,135");
				// entry[0] is the word and entry[1 to n-1] are the docIDs and frequencies
				String[] entry = line.split("\\s+");
				count++;
				if (entry.length > 1) {
					String stemmed = stemmer.stem(entry[0]);
					if (index.containsKey(stemmed)) {
						Int2IntOpenHashMap updatedDocList = index.get(stemmed);
						for (int i = 1; i < entry.length; i++) {
							// doc[0] is the id and doc[1] is the freq
							String[] doc = entry[i].replaceAll("[{}]", "").split(",");
							if (updatedDocList.containsKey(Integer.parseInt(doc[0]))) {
								// if doc and term combo already in index, just add frequencies
								updatedDocList.replace(Integer.parseInt(doc[0]),
										updatedDocList.get(Integer.parseInt(doc[0])) + Integer.parseInt(doc[1]));
							} else {
								// add doc to list
								updatedDocList.put(Integer.parseInt(doc[0]), Integer.parseInt(doc[1]));
							}
						}
						index.replace(stemmed, updatedDocList);
					} else {
						// new stemmed word
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