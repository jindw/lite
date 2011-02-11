this.addScript('el-translator.js',['stringifyJSEL']
                ,0
                ,['org.xidea.el:getTokenParamIndex','org.xidea.el:getTokenParam','org.xidea.el:getPriority',"org.xidea.el:findTokenText","org.xidea.lite.util:stringifyJSON",'org.xidea.jsi:$log']);
this.addScript('js-translator.js',["Translator","TranslateContext"]
                ,0
                ,["stringifyJSEL",'org.xidea.lite.impl:LiteStatus','org.xidea.lite.util:stringifyJSON','org.xidea.jsi:$log']);
                