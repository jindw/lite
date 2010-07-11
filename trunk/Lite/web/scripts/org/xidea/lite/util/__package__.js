this.addScript('json.js',["stringifyJSON","parseJSON"]);

this.addScript('kv.js',["setByKey","getByKey","removeByKey"]);

this.addScript('resource.js',["URI","buildURIMatcher",'loadXML','selectNodes','NodeList']
                ,0
                ,['org.xidea.jsi:$log','org.xidea.jsidoc.util:XMLHttpRequest']);
