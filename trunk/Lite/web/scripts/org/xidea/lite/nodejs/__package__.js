this.addScript("_xmlxpath.js","XPathEvaluator",
	["DOMNode", "DOMNodeList"]
)
this.addScript("jsi-lite-compiler.js","LiteCompiler",
	["DOMParser","XPathEvaluator"],
	["org.xidea.lite:Template",
	 "org.xidea.lite.parse:extractStaticPrefix",
	 "org.xidea.lite.parse:ParseConfig",
	 "org.xidea.lite.parse:parseConfig",
	 "org.xidea.lite.parse:ParseContext",
	 "org.xidea.lite.impl.js:JSTranslator",
	 "org.xidea.lite.util:normalizeLiteXML",
	 "DOMParser"]);
