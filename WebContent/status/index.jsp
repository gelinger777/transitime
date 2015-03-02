<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%
String agencyId = request.getParameter("a");
if (agencyId == null || agencyId.isEmpty()) {
    response.getWriter().write("You must specify agency in query string (e.g. ?a=mbta)");
    return;
}
%>
<html>
<head>
  <script src="http://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js"></script> 

  <!-- Load in JQuery UI javascript and css to set general look and feel -->
  <script src="/api/jquery-ui/jquery-ui.js"></script>
  <link rel="stylesheet" href="/api/jquery-ui/jquery-ui.css">

  <link rel="stylesheet" href="/api/css/general.css">
  
  <!-- Load in general transitime javascript library -->
  <script src="/api/javascript/transitime.js"></script>
    
<meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1">
<title>Status Pages</title>
</head>
<body>
<%@include file="/template/header.jsp" %>
<div id="mainDiv">
<div id="title">Status Reports</div>
<ul>
  <li><a href="activeBlocks.jsp?a=<%= agencyId %>"
    title="Shows how many block assignments are currently active and if they have assigned vehicles">
      Active Blocks</a></li>
  <li><a href="serverStatus.jsp?a=<%= agencyId %>"
    title="Shows how well system is running, including the AVL feed">
      Server Status</a></li>
  <li><a href="dbDiskSpace.jsp?a=<%= agencyId %>"
    title="Shows how much disk space is being used by the database.">
      Database Disk Space Utilization</a></li>
</ul>
</div>
</body>
</html>