this.addScript('js-compiler.js',["liteFunction","liteTemplate"]
               ,[
                   "org.xidea.lite.impl.js:JSTranslator",
                   "org.xidea.lite.impl.*",
                   "org.xidea.lite.*",
                   "org.xidea.lite.parse.*",
                   "org.xidea.lite.util.*",
                   "org.xidea.jsi:$log"
               ]
               ,"org.xidea.jsidoc.util:XMLHttpRequest");

this.addScript('server-compiler.js',["WebCompiler","base64Encode"]
               ,[
                   "org.xidea.lite.impl.php:PHPTranslator",
                   "org.xidea.lite.parse.*",
                   "org.xidea.lite.util.*",
                   "org.xidea.jsi:$log"
               ]
               ,"org.xidea.jsidoc.util:XMLHttpRequest");
               
this.addScript("data-view.js","DataView",
				[
					"org.xidea.lite.util.stringifyJSON"
					,"org.xidea.jsidoc.util:XMLHttpRequest"
					,"base64Encode"
				])
