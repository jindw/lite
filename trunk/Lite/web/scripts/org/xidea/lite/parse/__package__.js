this.addScript('result-context.js','ResultContext'
                ,0
                ,['org.xidea.lite.util:URI']);
                
this.addScript('xml-default-parser.js','parseXMLNode'
                ,0
                ,'org.xidea.lite.impl:*');
this.addScript('parse-context.js','ParseContext'
                ,['ResultContext']
                ,['ParseChain',
                	'org.xidea.lite.util:loadXML','org.xidea.lite.util:selectNodes',
                	'org.xidea.lite.util:stringifyJSON']);


this.addScript('parse-chain.js','ParseChain'
				,0
				,'org.xidea.jsi:$log');
				
this.addScript('xml-core-parser.js','Core'
                ,0
                ,['org.xidea.lite.impl:*','org.xidea.jsi:$log','org.xidea.lite.parse:selectNodes']);



this.addScript('text-parser.js',['parseText']
                ,0
                ,['org.xidea.el:ExpressionTokenizer','org.xidea.jsi:$log','org.xidea.el:findELEnd']);

this.addDependence("*",'org.xidea.lite.impl:*',true);

/*
  UserDefinedParser
  ExtensionParser
  TextParser
    ELParser
    ExtensionSeeker

*/