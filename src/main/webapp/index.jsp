<%@page language="java" contentType="text/html; charset=utf-8" pageEncoding="utf-8" %>
<%@taglib uri="http://java.sun.com/jstl/core" prefix="c" %>
<%@page import = "client.User" %>

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
    	<input type="text" size="75" name="query">  
    	<button type="submit" name="search">search</button>	
  	</form>
  	
  	<c:if test = "${not empty param.query}">
       <c:set target="${user}" property="completedQuery" value="${param.query}"/>
       <div style="margin-top: 20px;">showing results for: <c:out value="${user.completedQuery}" /></div>
    </c:if>  	
  	
  </body>
</html>