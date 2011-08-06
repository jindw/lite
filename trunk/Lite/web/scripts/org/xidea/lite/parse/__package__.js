this.addScript('config-parser.js',['parseConfig']
				,0
				,['org.xidea.lite.util:stringifyJSON','org.xidea.lite.util:loadXML',"org.xidea.lite.util:getAttribute"]);
this.addScript('config.js','ParseConfig'
				,0
				,['org.xidea.lite.util:URI']);
this.addScript('extension-parser.js','ExtensionParser'
				,0
				,['Extension','Core','HTML','HTML_EXT','org.xidea.lite.util:getOwnerElement']);
this.addScript('extension.js','Extension'
				,0
				,[
					'org.xidea.lite.util:removeByKey','org.xidea.lite.util:getByKey','org.xidea.lite.util:setByKey']);


           
this.addScript('parse-context.js','ParseContext'
                ,['ResultContext','ParseConfig','org.xidea.lite.util:URI','org.xidea.lite.util:loadXML','org.xidea.lite.util:selectByXPath']
                ,['buildTopChain','ExtensionParser','Extension','parseDefaultXMLNode','parseText','org.xidea.lite.util:getByKey','org.xidea.lite.util:setByKey']);

this.addScript('result-context.js','ResultContext'
                ,0
                ,['org.xidea.el.ExpressionTokenizer'
                	,'buildTreeResult'
                	,'optimizeResult'
                	,'doOptimize'
                	]);
     
this.addScript('parse-chain.js','buildTopChain');

this.addScript('xml-default-parser.js',['parseDefaultXMLNode','XML_SPACE_TRIM']);
this.addScript('html-parser.js',['HTML','HTML_EXT']
                ,'Core'
                ,['XML_SPACE_TRIM',"org.xidea.el:findELEnd",'parseChildRemoveAttr','org.xidea.lite.util:*']);
this.addScript('xml-core-parser.js',['Core','parseChildRemoveAttr']
                ,0
                ,["org.xidea.lite.impl.js:JSTranslator","org.xidea.el:findELEnd",'org.xidea.lite.parse:ParseContext','org.xidea.lite.util:*']);

this.addScript('text-parser.js',['parseText']
                ,0
                ,["org.xidea.el:findELEnd",'org.xidea.el:ExpressionTokenizer']);

this.addScript('optimize-scope.js','OptimizeScope',
				'org.xidea.el.*');

this.addScript('optimize-util.js',['doOptimize','optimizeResult','buildTreeResult','PLUGIN_TYPE_MAP']
				,'org.xidea.el.*'
				,['OptimizeScope'
					,"org.xidea.lite.util.stringifyJSON"
					,"org.xidea.lite.impl.js:JSTranslator"
					]);
this.addDependence("*",'org.xidea.lite.impl:*',true);
this.addDependence("*",'org.xidea.jsi:$log',false);


/*
  UserDefinedParser
  ExtensionParser
  TextParser
    ELParser
    ExtensionSeeker

*/
