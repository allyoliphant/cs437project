<%@page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@page import = "client.User" %>
<%@page import = "java.util.ArrayList" %>

<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-type" content="text/html; charset=utf-8">
    <title>IR Project</title>
    <link rel="stylesheet" href="styles/index.css" type="text/css" media="screen">
  </head>

	<body>	
		<jsp:useBean id="user" class="client.User" />		
  
  	<h1>Ally & Sammy's Wiki Search</h1>
  	
  	<form>
    	<input id="textbox_id" type="text" size="75" name="query">  
    	<button type="submit" name="search">search</button>	
  	</form>
  	
  	<script>
	  	$(window).keypress(function (e) {
	  	  if (e.key === ' ' || e.key === 'Spacebar') {
	  		  <% user.getSuggestions(request.getParameter("query")); %>
	  	  }
	  	})
  	</script>
  	
  	<c:if test = "${empty param.query}">
       <div style="margin-top: 20px;">
       	<div>disclaimer: it takes awhile to get results :|</div>
       	<div>please be patient, thank you</div>
       </div>  
    </c:if> 
  	
  	<c:if test = "${not empty param.query}">
       <c:set target="${user}" property="completedQuery" value="${param.query}"/>
       <div style="margin-top: 20px;">showing results for: <c:out value="${user.completedQuery}" /></div>
       
       <div style="max-width: 700px; margin: 20px auto 85px auto;">
	       <% ArrayList<String[]> results = user.getResults();
	            for(String[] result : results){%>                
	                <div style="text-align: left; font-weight: bold; font-size: 20px; margin-top: 20px;"><%=result[0] %></div>   
	                <hr>            
	                <div style="text-align: left; margin-top: 7px;"><%=result[1] %></div>               
	                <div style="text-align: left; margin-top: 7px;"><% if (result[2] != null) {%> <%=result[2] %> <%}%> </div>
	            <%
	            }
	        %>
       </div>   
    </c:if> 	
  	
  </body>
</html>