<%
Cookie[] cookies = request.getCookies();
%>
<%=cookies[0].getName()%>
<%=cookies[0].getValue()%>
<%
request.getRequestDispatcher("/doc/guide/index.xhtml").forward(request, response);
%>