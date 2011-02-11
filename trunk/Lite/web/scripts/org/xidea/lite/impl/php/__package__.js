this.addScript('php-el-translator.js',['PHPELTranslator','stringifyPHP']
                ,["org.xidea.el:ELTranslator"]
                ,['org.xidea.jsi:$log']);
this.addScript('php-translator.js',["PHPTranslator"]
                ,["org.xidea.lite.impl:TranslateContext"]
                ,["PHPELTranslator","stringifyPHP",'org.xidea.jsi:$log']);

                
