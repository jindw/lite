this.addScript('lite-demo-util.js',["TestCase"]
               ,[
                   "org.xidea.lite.impl.*",
                   "org.xidea.lite.parse.*",
                   "org.xidea.lite.util.*",
                   "liteFormat",
                   "org.xidea.jsi:$log"
               ]
               ,"org.xidea.jsidoc.util:XMLHttpRequest");

this.addScript('lite-formatter.js',["liteFormat"]
                ,[
                    "org.xidea.lite.*",
                    "org.xidea.lite.util.*"
                ]);
