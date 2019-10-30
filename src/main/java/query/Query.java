package query;

import index.Indexing;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectSet;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.google.common.collect.Sets;

// gets input from User class
// returns docs to User
public class Query {

	private String pathToCSV = "src/main/resources/collection/wikipedia_text_files.csv";
	private Indexing indexing;
	private Object2IntOpenHashMap<String> queryTermFreq = new Object2IntOpenHashMap<String>();
	private Object2DoubleOpenHashMap<String> queryTF = new Object2DoubleOpenHashMap<String>();
	private String processedQuery;
	private String[] queryTerms;

	// keeps track of the number of times w appears in d
	private Int2ObjectOpenHashMap<Object2IntOpenHashMap<String>> docFreq = new Int2ObjectOpenHashMap<Object2IntOpenHashMap<String>>();
	// keeps track of the number of documents in DC in which w appears at least once
	private Object2IntOpenHashMap<String> collectFreq = new Object2IntOpenHashMap<String>();

	// max five candidate resources with the top relevance scores
	private List<Integer> top5DocIDs = new ArrayList<Integer>();
	private IntOpenHashSet top5 = new IntOpenHashSet();
	private Int2ObjectOpenHashMap<String[]> snippets = new Int2ObjectOpenHashMap<String[]>();

	public Query(String q) {
		System.out.println();
		indexing = new Indexing();

		// process the query and get the necessary values for later
		System.out.println("processing the query....");
		processedQuery = indexing.stopAndStem(indexing.caseAndCharacters(q)).trim();
		queryTerms = processedQuery.split("\\s+");
		int maxFreq = 1;
		for (String term : queryTerms) {
			if (!queryTermFreq.containsKey(term)) {
				queryTermFreq.addTo(term, 1);
			} else {
				queryTermFreq.put(term, 1);
			}
			if (queryTermFreq.getInt(term) > maxFreq) {
				maxFreq = queryTermFreq.getInt(term);
			}
		}
		for (String term : queryTermFreq.keySet()) {
			double tf = (double) queryTermFreq.getInt(term) / (double) maxFreq;
			queryTF.put(term, tf);
		}
	}

	/**
	 * Main method for getting the results of a query
	 * 
	 * @return snippets of the top 5 results
	 */
	public ArrayList<String[]> getResults() {
		ArrayList<String[]> result = new ArrayList<String[]>();
		identifyCandidateResources();

		// if results were found
		if (docFreq.size() != 0) {
			relevanceRanking();
			snippetGeneration();
			int i = 1;
			for (int docID : top5DocIDs) {
				String[] snippet = snippets.get(docID);
				result.add(snippet);
				System.out.println();
				System.out.println(i + " of " + top5DocIDs.size() + " results");
				System.out.println(snippet[0]);
				System.out.println(snippet[1]);
				System.out.println(snippet[2]);
				i++;
			}
		}

		System.out.println("done getting results");
		return result;
	}

	/**
	 * Given query q, create a set of docs comprised of all the docs in the
	 * collection that contain each of the terms in q. If there are less than 50
	 * docs, then consider resources in the collection that contain n-1 terms in q.
	 */
	private void identifyCandidateResources() {
		System.out.println("getting the index....");
		Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> index = indexing.getIndex(queryTerms);

		System.out.println("finding the candidates....");
		Set<Integer> cr = new HashSet<Integer>();
		Int2IntOpenHashMap allDocs = new Int2IntOpenHashMap(); // docIDs and number of terms from query they contain
		for (String term : queryTerms) {
			if (term.length() > 0 && !collectFreq.containsKey(term) && index.containsKey(term)) {
				Int2IntOpenHashMap docs = index.get(term);
				System.out.println("  term '" + term + "' in index with " + docs.size() + " docs");
				collectFreq.put(term, docs.size());

				Set<Integer> termDocs = new HashSet<Integer>();
				// figure out how many terms from the query each document contains
				for (int id : docs.keySet()) {
					termDocs.add(id);
					if (allDocs.containsKey(id)) {
						allDocs.addTo(id, 1);
					} else {
						allDocs.put(id, 1);
					}
				}

				// build set of documents were all have the terms from the query
				if (term.compareTo(queryTerms[0]) != 0) {
					// intersection performs faster when smaller set is first
					if (cr.size() > termDocs.size()) {
						cr = Sets.intersection(termDocs, cr);
					} else {
						cr = Sets.intersection(cr, termDocs);
					}
				} else {
					cr = termDocs;
				}
			} else if (term.length() > 0 && !index.containsKey(term)) {
				System.out.println("  term '" + term + "' not in index");
			}
		}
		System.out.println("  found " + cr.size() + " docs having all terms in index");

		// set of candidate resources is too small (under 50 documents)
		// add documents that have all but one terms to the set of candidate resources
		if (cr.size() < 50) {
			Set<Integer> subSet = new HashSet<Integer>();
			for (int id : allDocs.keySet()) {
				if (allDocs.get(id) == collectFreq.size() - 1) {
					subSet.add(id);
				}
			}
			cr = Sets.union(cr, subSet);
			System.out.println("  added to cr, now " + cr.size());
		}

		// build a map of documents, terms from the query and the frequency in the
		// document
		for (int doc : cr) {
			Object2IntOpenHashMap<String> termFreq = new Object2IntOpenHashMap<String>();
			for (String term : collectFreq.keySet()) {
				if (index.get(term).containsKey(doc)) {
					termFreq.put(term, index.get(term).get(doc));
				}
			}
			docFreq.put(doc, termFreq);
		}
	}

	/**
	 * Compute for each document in CR its corresponding relevance score with
	 * respect to q. Then sort by score (highest to lowest) and keep the top 5.
	 */
	private void relevanceRanking() {
		System.out.println("calculating relevance ranking....");
		Int2IntOpenHashMap maxDocFreq = indexing.getMaxDocFreq(docFreq.keySet());
		HashMap<Integer, Double> relevanceScore = new HashMap<Integer, Double>();

		// get the score for each document
		for (int doc : docFreq.keySet()) {
			Object2IntOpenHashMap<String> freqInDoc = docFreq.get(doc);
			double score = 0.0;
			for (String word : collectFreq.keySet()) {
				if (freqInDoc.containsKey(word)) {
					if (maxDocFreq.get(doc) != 0 && collectFreq.getInt(word) != 0) {
						double tf = (double) freqInDoc.getInt(word) / (double) maxDocFreq.get(doc);
						double idf = (Math.log(1660931.0 / (double) collectFreq.getInt(word)) / Math.log(2.0) + 1e-10);
						score += (tf * idf);
					}
				}
			}
			relevanceScore.put(doc, score);
		}

		// sort from highest score to lowest
		List<Map.Entry<Integer, Double>> list = new ArrayList<Map.Entry<Integer, Double>>(relevanceScore.entrySet());
		Collections.sort(list, new Comparator<Map.Entry<Integer, Double>>() {
			public int compare(Map.Entry<Integer, Double> o1, Map.Entry<Integer, Double> o2) {
				return (o2.getValue()).compareTo(o1.getValue());
			}
		});

		int r = 0;
		// get the top 5 scores or all if there are less than 5 total
		while (r < 5 && r < list.size()) {
			Map.Entry<Integer, Double> entry = list.get(r);
			top5DocIDs.add(entry.getKey());
			top5.add((int) entry.getKey());
			System.out.println("  " + (r + 1) + ". " + list.get(r).getValue()); // print rank score of document
			r++;
		}
	}

	/**
	 * For each selected result, create the corresponding snippet. This snippet
	 * includes the title and the two sentences that have the highest cosine
	 * similarity with respect to the query; with TF-IDF as the term weighting
	 * scheme.
	 */
	private void snippetGeneration() {
		try {
			ObjectSet<String> queryWords = queryTermFreq.keySet();
			System.out.println("generate snippets....");
			BreakIterator bi = BreakIterator.getSentenceInstance(Locale.US); // used to parse the sentences
			Reader in = new FileReader(pathToCSV);
			CSVParser records = CSVFormat.EXCEL.withFirstRecordAsHeader().parse(in);
			int r = 1;

			// parse through the wiki collection, only stopping on the top 5 documents
			for (CSVRecord record : records) {
				if (top5DocIDs.contains(Integer.parseInt(record.get("id")))) {
					System.out.println("  on selected result " + r + " of " + top5DocIDs.size());
					String[] snip = new String[3]; // [0] = title, [1] and [2] = sentences
					snip[0] = record.get("title").replaceAll("_", " "); // title of the document

					// get the content minus the title
					String content = record.get("content").substring(snip[0].length()).trim();

					// get list of all the sentences
					int numOfSentences = 0;
					ArrayList<String> allSentences = new ArrayList<String>();
					ArrayList<Object2DoubleOpenHashMap<String>> sentenceTFs = new ArrayList<Object2DoubleOpenHashMap<String>>();
					Object2IntOpenHashMap<String> termDocFreq = new Object2IntOpenHashMap<String>();
					for (String term : queryWords) {
						termDocFreq.put(term, 0);
					}
					bi.setText(content);
					int lastIndex = bi.first();
					// for each sentence in the document
					while (lastIndex != BreakIterator.DONE) {
						int firstIndex = lastIndex;
						lastIndex = bi.next();
						if (lastIndex != BreakIterator.DONE) {
							String sentence = content.substring(firstIndex, lastIndex).trim();
							numOfSentences++;

							// get the frequency of index terms in the query in the sentence
							String[] words = indexing.stopAndStem(indexing.caseAndCharacters(sentence)).split(" ");
							Object2IntOpenHashMap<String> unique = new Object2IntOpenHashMap<String>();
							int maxFreq = 0;
							for (String word : words) {
								if (queryTermFreq.containsKey(word)) {
									if (!unique.containsKey(word)) {
										unique.addTo(word, 1);
									} else {
										unique.put(word, 1);
									}
									if (unique.getInt(word) > maxFreq) {
										maxFreq = unique.getInt(words);
									}
								}
							}

							// get tf for each query term of the sentence
							Object2DoubleOpenHashMap<String> tf = new Object2DoubleOpenHashMap<String>();
							for (String term : queryWords) {
								if (unique.containsKey(term)) {
									tf.put(term, (double) unique.getInt(term) / (double) maxFreq);
									termDocFreq.addTo(term, 1);
								} else {
									tf.put(term, 0.0);
								}
							}

							allSentences.add(sentence);
							sentenceTFs.add(tf);
						}
					}

					// calculate the idf and the query squared sum for the bottom of the cosine
					// similarity equation
					Object2DoubleOpenHashMap<String> idf = new Object2DoubleOpenHashMap<String>();
					double bottomQuerySum = 0;
					Object2DoubleOpenHashMap<String> queryTFIDF = new Object2DoubleOpenHashMap<String>();
					for (String term : queryWords) {
						double sentenceIDF = (Math.log((double) numOfSentences / (double) termDocFreq.getInt(term))
								/ Math.log(2.0) + 1e-10);
						idf.put(term, sentenceIDF);

						double ti = queryTF.getDouble(term) * sentenceIDF;
						queryTFIDF.put(term, ti);
						bottomQuerySum += ti * ti;
					}

					// calculate the cosine similarity for each sentence
					System.out.println("    calculate cosine similarity");
					TreeMap<Double, ArrayList<Integer>> sentenceCosineScores = new TreeMap<Double, ArrayList<Integer>>();
					for (int i = 0; i < sentenceTFs.size(); i++) {
						Object2DoubleOpenHashMap<String> sentenceTFIDF = new Object2DoubleOpenHashMap<String>();
						double topSum = 0;
						double bottomDocSum = 0;
						// get the tfxidf for each term
						for (String term : queryWords) {
							double ti = sentenceTFs.get(i).getDouble(term) * idf.getDouble(term);
							sentenceTFIDF.put(term, ti);
							topSum += sentenceTFIDF.getDouble(term) * queryTFIDF.getDouble(term);
							bottomDocSum += sentenceTFIDF.getDouble(term) * sentenceTFIDF.getDouble(term);
						}
						double bottom = Math.sqrt(bottomDocSum * bottomQuerySum); // bottom number of the cosine
																					// similarity
						double score = topSum / bottom; // cosine similarity score for the sentence
						// sentences can have the same score, so add them as a list of sentences for
						// each score
						ArrayList<Integer> sentencesWithScores = new ArrayList<Integer>();
						if (sentenceCosineScores.containsKey(score)) {
							sentencesWithScores = sentenceCosineScores.get(score);
						}
						sentencesWithScores.add(i);
						sentenceCosineScores.put(score, sentencesWithScores);
					}

					int top2 = 1;
					// get the sentences with the top 2 scores
					for (double score : sentenceCosineScores.descendingKeySet()) {
						ArrayList<Integer> sentencesWithScores = sentenceCosineScores.get(score);
						for (int sentenceID : sentencesWithScores) {
							snip[top2] = allSentences.get(sentenceID);
							top2++;
							if (top2 > 2) {
								break;
							}
						}
						if (top2 > 2) {
							break;
						}
					}

					snippets.put(Integer.parseInt(record.get("id")), snip);
					r++;
				}
				if (r > 6) {
					break;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	public static void main(String[] args) {
	}
}