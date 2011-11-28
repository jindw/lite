this.addScript('server-compiler.js',["WebCompiler"]
               ,[
                   "org.xidea.lite.impl.php:PHPTranslator",
                   "org.xidea.lite.parse.*",
                   "org.xidea.lite.util.*",
                   "org.xidea.jsi:console"
               ]
               ,"org.xidea.lite.util:XMLHttpRequest");
               
this.addScript("data-view.js","DataView",
				[
					"org.xidea.lite.util:stringifyJSON"
					,"org.xidea.lite.util:XMLHttpRequest"
					,"org.xidea.lite.util:base64Encode"
				])
