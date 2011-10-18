this.addScript('browser-compiler.js',["liteWrapImpl"]
               ,[
                   "org.xidea.lite.impl.js:JSTranslator",
                   "org.xidea.lite.parse.*",
                   "org.xidea.lite.util.*",
                   "org.xidea.jsi:$log"
               ]);

this.addScript('js-translator.js',["JSTranslator",'GLOBAL_DEF_MAP',"GLOBAL_VAR_MAP"]
                ,'org.xidea.lite.impl:*'
                ,["org.xidea.el:stringifyJSEL",
                	'org.xidea.lite.util:stringifyJSON',
                	'org.xidea.jsi:$log']);