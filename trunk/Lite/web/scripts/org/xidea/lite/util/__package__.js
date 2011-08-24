this.addScript("xml.js",['loadLiteXML','selectByXPath'
					,'findXMLAttribute','findXMLAttributeAsEL']
				,['normalizeLiteXML',"URI",'org.xidea.jsidoc.util:XMLHttpRequest'])
this.addScript('el.js','findLiteParamMap');
this.addScript('xml-normalize.js',['getLiteTagInfo','normalizeLiteXML']);


this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addScript('kv.js',["setByKey","getByKey","removeByKey"]);

this.addScript('resource.js',["URI","encodeURIComponent","decodeURIComponent"]);

this.addDependence("*",'org.xidea.jsi:$log',true);
//*
//nodejs
this.addScript("_xmlparser.js","DOMParser");
this.addDependence("xml.js",'DOMParser',true);
// */