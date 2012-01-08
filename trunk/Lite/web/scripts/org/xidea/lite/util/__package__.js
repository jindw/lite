this.addScript("xml.js",['loadLiteXML','selectByXPath'
					,'findXMLAttribute','findXMLAttributeAsEL']
				,['normalizeLiteXML',"URI",'XMLHttpRequest'])
this.addScript('el.js','findLiteParamMap');
this.addScript('xml-normalize.js',['getLiteTagInfo','normalizeLiteXML']);


this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addScript('xhr.js',["XMLHttpRequest"]);

this.addScript('kv.js',["setByKey","getByKey","removeByKey"]);

this.addScript('resource.js',["URI",'base64Encode','i18nHash']);

this.addScript('js-token.js',['partitionJavaScript','compressJS']);

//this.addDependence("xml.js","org.xidea.lite.nodejs:*",true);