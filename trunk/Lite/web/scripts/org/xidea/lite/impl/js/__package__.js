this.addScript('el-translator.js',['stringifyJSEL']
                ,0
                ,["org.xidea.el:*",
                	"org.xidea.lite.util:stringifyJSON",
                	'org.xidea.jsi:$log']);
this.addScript('js-translator.js',["Translator","TranslateContext"]
                ,0
                ,['org.xidea.lite.impl:*',
                	"stringifyJSEL",
                	'org.xidea.lite.util:stringifyJSON',
                	'org.xidea.jsi:$log']);
                