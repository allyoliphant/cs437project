package client;

public class User {
	
	private String completedQuery;

	public User() {
		setCompletedQuery("");
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
	
	public void getResults(String query) {
		// calls method in Query class
	}
}
