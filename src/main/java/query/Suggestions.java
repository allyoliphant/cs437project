package query;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeMap;


//gets input from User class
//returns suggested queries to User class

public class Suggestions {
	private static Map<String, Set<Integer>> queryGivenID = new HashMap<String, Set<Integer>>(); 																							// suggestions
	private static Map<Integer, Set<String>> IDGivenQuery = new HashMap<Integer, Set<String>>(); 
	private static Map<String, Integer> frequency = new HashMap<String, Integer>();
	private static Set<String> candidateSet = new HashSet<String>();
	private static Set<String> scoredCandidateSet = new HashSet<String>();
	private static Set<String> emptySet = new HashSet<String>();
	private static Map<String, Double> score = new HashMap<String, Double>();;
	private static int totalFrequency = 1;
	private static int maxFrequency = 0;

	
	public static void main(String[] args) throws FileNotFoundException{

		System.out.println(getCandidates("united") + "\n");
		System.out.println(getCandidates("Boise") + "\n");
	}
	

	@SuppressWarnings("resource")
	public static Set<String> getCandidates(String inputPhrase) throws FileNotFoundException {
		File[] testFile = new File("src/main/resources/queryLogs/").listFiles();
		for(File f : testFile) {
			if(f.getName().endsWith(".txt")){
				Scanner scan = new Scanner(f);
				int ID = 0;
				String query = "";
				String time = "";
				
				scan.nextLine();
				while (scan.hasNextLine()) {
					String line = scan.nextLine();
					String delimiter = ("\t");
					Scanner lineScanner = new Scanner(line).useDelimiter(delimiter);
					ID = lineScanner.nextInt();
					query = lineScanner.next();
					time = lineScanner.next();

					// Check for the Set found with the query. If it is null, create a new HashSet
					// and add the ID to it, then put both query and new Set into the map.
					Set<Integer> newQuery = queryGivenID.get(query);
					if (newQuery == null) {
						newQuery = new HashSet<Integer>();
						newQuery.add(ID);
						queryGivenID.put(query, newQuery);
					}
					// Else: query already exists in map and has the set. Just need to add ID to the
					// already existing set
					else {
						newQuery.add(ID);
					}

					// Same as above except switched. Set found with ID. If null, create new HashSet
					// and add query to it. Then put both ID and new Set into Map
					Set<String> newID = IDGivenQuery.get(ID);
					if (newID == null) {
						newID = new HashSet<String>();
						newID.add(query);
						IDGivenQuery.put(ID, newID);
					} else {
						newID.add(query);
					}
					
					//Get Frequency for Score
					
					if(!frequency.containsKey(query)) {
						frequency.put(query, totalFrequency);
					}else {
						frequency.replace(query, frequency.get(query)+1);
					}

				}
			}
		}
		
		
		//Testing that Maps are being created correctly
		
//		System.out.println("Query given ID Map:\n");
//		for (Map.Entry<String, Set<Integer>> entry : queryGivenID.entrySet()) {
//			System.out.println(entry.getKey() + ":" + entry.getValue().toString());
//		}
//		System.out.print("\n\nID given Query Map:\n\n");
//		for (Map.Entry<Integer, Set<String>> entry : IDGivenQuery.entrySet()) {
//			System.out.println(entry.getKey() + ":" + entry.getValue().toString());
//		}

		//Start of candidacy

		if (!queryGivenID.containsKey(inputPhrase)) {
			System.out.println("There is no suggestions for: " + inputPhrase);
			return emptySet;
		}
		Set<Integer> foundID = new HashSet<Integer>();
		for (Map.Entry<String, Set<Integer>> entry : queryGivenID.entrySet()) {
			if (entry.getKey().contains(inputPhrase)) {
				foundID.addAll(entry.getValue());
			}

		}

		for (int fID : foundID) {
			Set<String> query1 = IDGivenQuery.get(fID);
			for (String fQuery : query1) {
				if (fQuery.startsWith(inputPhrase) && !fQuery.equals(inputPhrase)) {
					candidateSet.add(fQuery);
				}
			}
		}
		
		for(Map.Entry<String, Integer> mFreq : frequency.entrySet()) {
			if(mFreq.getValue() > maxFrequency) {
				maxFrequency = mFreq.getValue();
			}
		}
		
		Double frequencyOrd = (double) 0;
		for(String organizedCandidate: candidateSet) {
			double frequencyQuery = frequency.get(organizedCandidate);
			double newFrequency = (frequencyQuery/maxFrequency);
			
			double scoredFrequency = (newFrequency/(1-newFrequency));
			
			score.put(organizedCandidate, scoredFrequency);
		}
		
		int queryChangeCount;
		int totalSessionCount = 0;
		Map<Integer, Integer> changeCount = new HashMap<Integer, Integer>();
		for (Map.Entry<Integer, Set<String>> entry : IDGivenQuery.entrySet()) {
			queryChangeCount = 0;
			for(String value : entry.getValue()) {
				if(value.contains(inputPhrase)) {
					queryChangeCount++;
				}
				
			}
			if(queryChangeCount != 0) {
				changeCount.put(entry.getKey(), queryChangeCount);
				//System.out.println(queryChangeCount);
				totalSessionCount++;
			}
			
		}
//		System.out.println("Session count");
//		System.out.println(totalSessionCount);
//		System.out.println(changeCount);
	
		
		TreeMap<String, Double> sorted = new TreeMap<String, Double>();
		sorted.putAll(score);

		int i = 0;
		int finalCount = 5;
		for(Map.Entry<String, Double> entry : sorted.descendingMap().entrySet()) {
			//System.out.println(entry.getKey() + ":" + entry.getValue().toString());
			if(i++ < finalCount) {
				scoredCandidateSet.add(entry.getKey());
			}
		}
	
		

		return scoredCandidateSet;

	}

}
