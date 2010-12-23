<%
session.setAttribute("aa","bb");
session.setAttribute("int1",123);

request.getRequestDispatcher("/example/test.tpl").forward(request, response);
%>