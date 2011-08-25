this.addScript("_xmlparser.js",["XMLP","__escapeString", "__unescapeString", "trim"])
this.addScript("_xmldom.js",["DOMParser","DOMNode","DOMNodeList"]
	,["XMLP","__escapeString", "__unescapeString", "trim"]);
this.addScript("_xmlxpath.js","XPathEvaluator",
	["DOMNode", "DOMNodeList"]
)

this.addDependence("*",'org.xidea.jsi:$log',true);