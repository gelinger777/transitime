<%-- Provides schedule adherence data in JSON format. Provides for
     each route the route name, number arrivals/departures that
     are early, number late, number on time, number total, percent
     early, percent late, and percent on time. 
     Request parameters are:
       a - agency ID
       r - route ID or route short name. Can specify multiple routes. 
           Not specifying route provides data for all routes.
       beginDate - date to begin query
       numDays - number of days can do query. Limited to 31 days
       beginTime - for optionally specifying time of day for query for each day
       endTime - for optionally specifying time of day for query for each day
       allowableEarlyMinutes - how early vehicle can be and still be OK.  Decimal format OK. 
       allowableLateMinutes - how early vehicle can be and still be OK. Decimal format OK.
--%>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page import="org.transitime.reports.GenericJsonQuery" %>
<%@ page import="org.transitime.reports.SqlUtils" %>
<%
String allowableEarlyStr = request.getParameter("allowableEarly");
if (allowableEarlyStr == null || allowableEarlyStr.isEmpty())
	allowableEarlyStr = "1.0";
String allowableEarlyMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableEarlyStr) + " seconds'";

String allowableLateStr = request.getParameter("allowableLate");
if (allowableLateStr == null || allowableLateStr.isEmpty())
	allowableLateStr = "4.0";
String allowableLateMinutesStr = "'" + SqlUtils.convertMinutesToSecs(allowableLateStr) + " seconds'";
    		   
String sql =
	"SELECT " 
	+ "     COUNT(CASE WHEN scheduledtime-time > " + allowableEarlyMinutesStr + " THEN 1 ELSE null END) as early, \n"
	+ "     COUNT(CASE WHEN scheduledtime-time <= " + allowableEarlyMinutesStr + " AND time-scheduledtime <= " 
				+ allowableLateMinutesStr + " THEN 1 ELSE null END) AS ontime, \n" 
    + "     COUNT(CASE WHEN time-scheduledtime > " + allowableLateMinutesStr + " THEN 1 ELSE null END) AS late, \n" 
    + "     COUNT(*) AS total, \n"
    + "     r.name \n"
    + "FROM arrivalsdepartures ad, routes r \n"
    + "WHERE "
    // For joining in route table to get route name
    + "ad.configrev = r.configrev \n"
    + " AND ad.routeshortname = r.shortname \n"
    // Only need arrivals/departures that have a schedule time
    + " AND ad.scheduledtime IS NOT NULL \n"
    // Specifies which routes to provide data for
    + SqlUtils.routeClause(request, "ad") + "\n"
    + SqlUtils.timeRangeClause(request, "ad.time", 31) + "\n"
    // Grouping needed since want to output route name
    + " GROUP BY r.name, r.routeorder ORDER BY r.routeorder, r.name;";

// Just for debugging
System.out.println("\nFor schedule adherence query sql=\n" + sql);
    		
// Do the query and return result in JSON format    
String agencyId = request.getParameter("a");
String jsonString = GenericJsonQuery.getJsonString(agencyId, sql);
response.setContentType("application/json");
response.getWriter().write(jsonString);
%>