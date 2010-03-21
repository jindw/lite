//var XMLParser = $import("org.xidea.template:XMLParser");
var testTemplate = {
	testAttribute:function(){
		var template = new Template("<xml a='11'>11${1&lt;2}</xml>");
		assertEquals(template.render({}),"<xml a=\"11\">11true</xml>")
	},
	testDAttribute:function(){
		var template = new Template("<xml a='11${true}' a2='${null}' a3=\"${''}\">11${1&lt;2}</xml>");
		assertEquals(template.render({}),"<xml a=\"11true\">11true</xml>")
	},
	testIf:function(){
		var template = new Template("<c:if xmlns:c='http://www.xidea.org/ns/template' test='${true}'>1" +
		"<c:if test='${1==2}'>2</c:if>  \t<c:else>3</c:else>"+
				"</c:if>");
		assertEquals(template.render({}),"13")
	},
	testChoose:function(){
		var template = new Template("<c:choose xmlns:c='http://www.xidea.org/ns/template'>这些文字是无效的" +
		"<c:when  test='${true}'>1</c:when>"+
		"<c:when test='${true}'>2</c:when>  \t<c:otherwise>3</c:otherwise>"+
				"</c:choose>");
		assertEquals(template.render({}),"1")
		var template = new Template("<c:choose xmlns:c='http://www.xidea.org/ns/template'>1" +
		"<c:when  test='${false}'>1</c:when>"+
		"<c:when test='${false}'>2</c:when>  \t<c:otherwise>3</c:otherwise>"+
				"</c:choose>");
		assertEquals(template.render({}),"3")
	},
	testFor:function(){
		var template = new Template("<c:if test='${true}' xmlns:c='http://www.xidea.org/ns/template'>" +
		"<c:for var='item' items='${[1,2]}'>${for.index}${item}</c:for>"+
		"<c:else>skip</c:else>"+
		"-"+
		"<c:for status='status2' var='item' items='${[0]}'>${(status2.lastIndex)}${for.index}</c:for>"+
		"<c:else>skip</c:else>"+
		
		"</c:if>");
		assertEquals(template.render({}),"0112-00")
	}
}