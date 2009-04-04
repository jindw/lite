this.addScript('jsi-export.js',["XMLHttpRequest","$log"]);
this.addScript('json.js',["JSON"]);

this.addScript('lite-demo-util.js',["TestCase"]
               ,[
                   "org.xidea.lite.*",
                   "JSON",
                   "liteFormat"
               ]);

this.addScript('lite-formatter.js',["liteFormat"]
                ,[
                    "org.xidea.lite.*",
                    "JSON"
                ]);
