this.addScript("xml.js",['loadLiteXML','selectByXPath'
					,'findXMLAttribute','findXMLAttributeAsEL']
				,['normalizeLiteXML',"URI",'org.xidea.jsidoc.util:XMLHttpRequest'])
this.addScript('el.js','findLiteParamMap');
this.addScript('xml-normalize.js',['getLiteTagInfo','normalizeLiteXML']);


this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addScript('kv.js',["setByKey","getByKey","removeByKey"]);

this.addScript('resource.js',["URI",'base64Encode']);

this.addScript('js-token.js',['partitionJavaScript','compressJS']);

this.addDependence("*",'org.xidea.jsi:$log',true);

//this.addDependence("xml.js","org.xidea.lite.nodejs:*",true);