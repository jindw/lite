//token 定义
this.addScript('template-token.js',["PLUGIN_DEFINE",'VAR_TYPE','XA_TYPE','ELSE_TYPE','PLUGIN_TYPE','CAPTURE_TYPE','IF_TYPE','EL_TYPE','BREAK_TYPE','XT_TYPE','FOR_TYPE']);

//基本认为是没用的。
this.addScript('template-impl.js',["TemplateImpl"]
				,'template-token.js'
                ,["org.xidea.lite.Template","org.xidea.lite.util:selectByXPath",'org.xidea.lite.impl.js:JSTranslator','org.xidea.lite.parse:ParseContext','org.xidea.el:evaluate']);

this.addScript('translate-context.js','TranslateContext'
                ,0
                ,["org.xidea.lite.parse.OptimizeScope",
                	"org.xidea.el:*",
                	"org.xidea.lite.util:stringifyJSON"]);
                
this.addDependence("*",'*',true);