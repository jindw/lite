this.addScript('xml-core-parser.js','parseCoreNode'
                ,0
                ,['org.xidea.lite.impl:*','org.xidea.jsi:$log','org.xidea.lite.parse:selectNodes']);

this.addScript('xml-default-parser.js','parseXMLNode'
                ,0
                ,'org.xidea.lite.impl:*');
