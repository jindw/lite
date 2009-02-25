<%@ page contentType="text/html; charset=UTF-8" %>
<%@ taglib uri="/struts-tags" prefix="s" %>
<html xmlns='http://www.w3.org/1999/xhtml'>
  <head>
    <title>测试网页</title>
  </head>
  <body>
    <s:form action="HelloWorldJSP" theme="simple">
      <h3>测试Boolean類型屬性</h3>
      <div>
        <s:radio name="test" list="#{true:'真',false:'假'}"/> 
      </div>
      <div>
        <s:submit/>
      </div>
    </s:form>
  </body>
</html>
