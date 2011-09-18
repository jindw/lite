<%
Cookie[] cookies = request.getCookies();
%>
<%=cookies[0].getName()%>
<%=cookies[0].getValue()%>