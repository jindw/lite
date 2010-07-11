this.addScript('config-parser.js',['parseConfig','buildURIMatcher']
				,0
				,'org.xidea.lite.util:loadXML');
this.addScript('config.js','ParseConfig'
				,0
				,["Core",'ExtensionParser','Extension','parseDefaultXMLNode','parseText']);
					"",
this.addScript('extension-parser.js','ExtensionParser'
				,0
				,["org.xidea.el:findELEnd",
					"Core",'Extension','parseDefaultXMLNode','parseText']);
this.addScript('extension.js','Extension'
				,0
				,[
					'org.xidea.lite.util:removeByKey','org.xidea.lite.util:getByKey','org.xidea.lite.util:setByKey']);



           
this.addScript('parse-context.js','ParseContext'
                ,['ResultContext','ParseConfig','org.xidea.lite.util:loadXML','org.xidea.lite.util:selectNodes']
                ,['buildTopChain','org.xidea.lite.util:URI']);

this.addScript('result-context.js','ResultContext'
                ,0
                ,['org.xidea.el.ExpressionTokenizer','org.xidea.lite.util:stringifyJSON','org.xidea.lite.util:getByKey','org.xidea.lite.util:setByKey']);
     
this.addScript('parse-chain.js','buildTopChain');

this.addScript('xml-default-parser.js','parseDefaultXMLNode'
                ,0
                ,'org.xidea.lite.impl:*');
this.addScript('xml-core-parser.js','Core'
                ,0
                ,['org.xidea.lite.impl:*','org.xidea.lite.util:selectNodes','org.xidea.lite.util:URI']);

this.addScript('text-parser.js',['parseText']
                ,0
                ,['org.xidea.el:ExpressionTokenizer']);

this.addDependence("*",'org.xidea.lite.impl:*',true);
this.addDependence("*",'org.xidea.jsi:$log',true);


/*
  UserDefinedParser
  ExtensionParser
  TextParser
    ELParser
    ExtensionSeeker

*/