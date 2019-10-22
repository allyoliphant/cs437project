package client;

import java.util.ArrayList;
import java.util.List;

import query.Query;

public class User {
	
	public String completedQuery;
	public String suggestionQuery;
	private Query q;

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
		System.out.println(query);
	}
	
	public ArrayList<String[]> getResults() {
		q = new Query(completedQuery);
		return q.getResults();		
	}
	
}
