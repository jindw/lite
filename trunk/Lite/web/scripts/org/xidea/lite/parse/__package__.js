this.addScript('parse-context.js','ParseContext'
                ,['loadXML','selectNodes','ParseChain']
                ,['parseEL','org.xidea.lite.util:stringifyJSON','org.xidea.lite.util:URI']);
this.addScript('parse-chain.js','ParseChain');
this.addScript('text-parser.js',['parseText','parseEL']
                ,0
                ,['org.xidea.el:ExpressionTokenizer','org.xidea.jsi:$log','org.xidea.el:findELEnd']);

this.addScript('xml-context.js',['loadXML','selectNodes']
                ,0
                ,['org.xidea.jsi:$log','org.xidea.jsidoc.util:XMLHttpRequest']);


this.addDependence("*",'org.xidea.lite.impl:*',true);

/*
  UserDefinedParser
  ExtensionParser
  TextParser
    ELParser
    ExtensionSeeker

*/