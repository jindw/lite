this.addScript("_xmlparser.js",["XMLP","__escapeString", "__unescapeString", "trim"])
this.addScript("_xmldom.js",["DOMParser","DOMNode","DOMNodeList"]
	,["XMLP","__escapeString", "__unescapeString", "trim"]);
this.addScript("_xmlxpath.js","XPathEvaluator",
	["DOMNode", "DOMNodeList"]
)
this.addScript("template-compiler.js","TemplateCompiler",
	["DOMParser","XPathEvaluator"],
	["org.xidea.lite:Template",
	 "org.xidea.lite.parse:ParseConfig",
	 "org.xidea.lite.parse:parseConfig",
	 "org.xidea.lite.parse:ParseContext",
	 "org.xidea.lite.impl.js:JSTranslator",
	 "org.xidea.lite.util:normalizeLiteXML",
	 "DOMParser"]);
this.addDependence("*",'org.xidea.jsi:console',true);