package client;

import java.util.ArrayList;
import java.util.List;

import query.Query;

public class User {
	
	private String completedQuery;
	private Query q;
	
	private List<String[]> results;

	public User() {
	}

	public void setCompletedQuery(String q) {
		this.completedQuery = q;
		System.out.println("set query to: " + completedQuery);
	}

	public String getCompletedQuery() {
		return this.completedQuery;
	}
	
	public void getSuggestions(String query) {
		// calls method in Suggestions class
	}
	
	public ArrayList<String[]> getResults() {
		q = new Query(completedQuery);
		return q.getResults();		
	}
}
