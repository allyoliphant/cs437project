package query;

import index.Indexing;

import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Sets;

// gets input from User class
// returns docs to User
public class Query {

	private Indexing indexing;
	private String processedQuery;
	// keeps track of the number of times w appears in d
	// also acts as a global set of candidate resources
	private Int2ObjectOpenHashMap<Object2IntOpenHashMap<String>> docFreq = new Int2ObjectOpenHashMap<Object2IntOpenHashMap<String>>();
	// keeps track of the number of documents in DC in which w appears at least once
	private Object2IntOpenHashMap<String> collectFreq = new Object2IntOpenHashMap<String>();	
	private IntOpenHashSet resources = new IntOpenHashSet();
	private List<Map.Entry<Integer, Double>> top5 = new ArrayList<Map.Entry<Integer, Double>>();

	public Query(String q) {
		indexing = new Indexing();

		System.out.println("processing the query....");
		processedQuery = indexing.stopAndStem(indexing.caseAndCharacters(q)).trim();
	}

	/**
	 * Given query q, create a set of docs comprised of all the docs in the
	 * collection that contain each of the terms in q. If there are less than 50
	 * docs, then consider resources in the collection that contain n-1 terms in q.
	 */
	public void identifyCandidateResources() {
		System.out.println("getting the index....");
		Object2ObjectOpenHashMap<String, Int2IntOpenHashMap> index = indexing.getIndex();

		String[] terms = processedQuery.split("\\s+");

		System.out.println("finding the candidates....");
		Set<Integer> cr = new HashSet<Integer>();
		Int2IntOpenHashMap allDocs = new Int2IntOpenHashMap(); // docIDs and number of terms from query they contain
		for (String term : terms) {
			if (term.length() > 0 && !collectFreq.containsKey(term) && index.containsKey(term)) {
				Int2IntOpenHashMap docs = index.get(term);
				System.out.println("  term '" + term + "' in index with " + docs.size() + " docs");
				collectFreq.put(term, docs.size());

				Set<Integer> termDocs = new HashSet<Integer>();
				for (int id : docs.keySet()) {
					termDocs.add(id);
					if (allDocs.containsKey(id)) {
						allDocs.addTo(id, 1);
					} else {
						allDocs.put(id, 1);
					}
				}

				if (term.compareTo(terms[0]) != 0) {
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

		// set of candidate resources is too small
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

		for (int doc : cr) {
			Object2IntOpenHashMap<String> termFreq = new Object2IntOpenHashMap<String>();
			for (String term : collectFreq.keySet()) {
				if (index.get(term).containsKey(doc)) {
					termFreq.put(term, index.get(term).get(doc));
				}
			}
			docFreq.put(doc, termFreq);
			resources.add(doc);
		}
	}

	/**
	 * Compute for each document in CR its corresponding relevance score with
	 * respect to q. Then sort by score (highest to lowest) and keep the top 5.
	 */
	public void relevanceRanking() {
		System.out.println("calculating relevance ranking....");
		Int2IntOpenHashMap maxDocFreq = indexing.getMaxDocFreq(resources);
		HashMap<Integer, Double> relevanceScore = new HashMap<Integer, Double>();

		for (int doc : resources) {
			Object2IntOpenHashMap<String> freqInDoc = docFreq.get(doc);
			double score = 0.0;
			for (String word : collectFreq.keySet()) {
				if (freqInDoc.containsKey(word)) {
					if (maxDocFreq.get(doc) != 0 && collectFreq.getInt(word) != 0) {
						double tf = (double) freqInDoc.getInt(word) / (double) maxDocFreq.get(doc);
						double idf = (Math.log(1662756 / collectFreq.getInt(word)) / Math.log(2) + 1e-10);
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
		while (r < 5 && r < list.size()) {
			top5.add(list.get(r));
			System.out.println("  " + (r + 1) + ". " + list.get(r).getValue());
			r++;
		}
	}

	public static void main(String[] args) {
		Query q = new Query("cheapest united airline flights sdfghj");
		q.identifyCandidateResources();
		q.relevanceRanking();

	}

}
