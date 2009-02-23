<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<html>
  <head>
    <title>测试网页</title>
  </head>
  <body>
    <s:form action="HelloWorldJSP">
        <s:radio name="label1" list="#{1:1,2:2,3:3}" label="测试字符串选择"/> 
        <s:submit/>
    </s:form>
  </body>
</html>
