this.addScript("xml.js",['loadXML','selectByXPath','getOwnerElement'
					,'getAttribute','getAttributeEL']
				,['normalizeXML',"URI",'org.xidea.jsidoc.util:XMLHttpRequest'])
this.addScript('el.js','findParamMap');
this.addScript('xml-normalize.js','normalizeXML');


this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addScript('kv.js',["setByKey","getByKey","removeByKey"]);

this.addScript('resource.js',["URI","encodeURIComponent","decodeURIComponent"]);

this.addDependence("*",'org.xidea.jsi:$log',true);