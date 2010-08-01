this.addScript("xml.js",['loadXML','selectNodes'
					,'getAttribute','getAttributeEL']
				,["URI",'org.xidea.jsi:$log','org.xidea.jsidoc.util:XMLHttpRequest'])
this.addScript('el.js','findParamMap')
this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addScript('kv.js',["setByKey","getByKey","removeByKey"]);

this.addScript('resource.js',["URI","encodeURIComponent","decodeURIComponent"]
                ,0
                ,['org.xidea.jsi:$log']);
