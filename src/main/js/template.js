var asf = require('asf')
function Template(code,config){
 	//console.log(code)
	try{
    	var fn = code instanceof Function?code:eval('['+code+'][0]');
    }catch(e){
    	//console.error(config.path,require('util').inspect(e,true)+'\n\n'+(e.message +e.stack));
    	var fn = function (){throw e;};
    }
    this.fn = fn
    this.impl = asf(fn)
    this.config = config||{};
}
Template.prototype.render = function(context,out){
	var impl = this.impl;
    var out = wrapResponse(out,this);
    var path = this.config.path;
    for(var n in context){
        var v = context[n];
        if(v instanceof Promise){
            context[n] = v['catch'](function(e){
                console.error('error on render:'+path,e);
            })
        }
    }
    return new Promise(function(resolve,reject){
        try{
    		impl(context,out).then(resolve,reject);
    	}catch(e){
    	    try{
                //console.warn(this.impl+'');
                var rtv = String(e.message +e.stack);
                out.end(rtv);
    		}catch(e){
    		}
    		reject(rtv);
    	}
    })

}
Template.prototype.lazyArrived =function(resp,id,content,index,count){
	//(first?'!this.__widget_arrived&&(this.__widget_arrived=function(id,h){document.querySelector(id).innerHTML=h});':'')
	content = JSON.stringify(content).replace(/<\/script>/ig,'<\\/script>');
//	__widget_arrived = __widget_arrived || function (id,content){
//		document.querySelector("[data-lazy-widget="+id+"]").innerHTML = content;
//	}
	resp.write('<script>__widget_arrived("'+id+'",'+content+')</script>')
}
function wrapResponse(resp,tpl){
	var lazyList = [];
	var buf=[];
	var bufLen=0;
	var MAX_BUFFER = 1024;
	var size = 0
	return {
		push:function(){
		    var len = arguments.length
		    if(len){
		        for(var i = 0;i<len;i++){
                    //console.log(arguments[i])
                    var text = arguments[i];
                    if(text instanceof Function){
                        lazyList.push(text);
                    }else{
                       if(bufLen>MAX_BUFFER){
                            this.push();//flush
                        }
                        buf.push(text)
                        if(text){
                            bufLen+=text.length
                        }
                    }
                }
		    }else{//flush
		        var text = buf.join('');
		        size += text.length;
		        resp.write(text);
                buf = [];
                bufLen = 0;
            }
		},
		join:function(){//end
			var last = buf.pop();
			var matchEnd = last && last.match(/(?:<\/body>\s*)?<\/html>\s*$/i);
			if(matchEnd){
				matchEnd = matchEnd[0];
				buf.push(last.slice(0,-matchEnd.length))
			}else{
			    matchEnd = '';
			    buf.push(last);
			}
			this.push();//flush
			return new Promise(function(resolve,reject){
                doMutiLazyLoad(tpl,lazyList,resp,function(error){
                    resp.end(matchEnd);
                    size+=matchEnd.length;
                    error.length ? reject(error) : resolve(size);
                })
			})
		}
	}
}

function doMutiLazyLoad(tpl,lazyList,resp,onComplete){
	var len = lazyList.length;
	var arrivedIndex = 0;
	var errors = []
	if(len){
		for(var i = 0;i<len;i++){
			var fn = lazyList[i];
			var id = fn.name.replace(/^__widget_/g,'');//__widget_\d+__
			startModule(id,asf(fn),[]);
		}
		function startModule(id,g,result){
            function oneComplete(error){
                var content = result.join('');
                tpl.lazyArrived(resp,id,content,arrivedIndex,len)
                if(++arrivedIndex >= len){
                    onComplete(errors);
                }
            }
            function oneError(error){
                result.push(error);
                errors.push(error)
                oneComplete()
            }
			g(result).then(oneComplete,oneError);
		}
	}else{
		onComplete([]);
	}
}
exports.wrapResponse = wrapResponse;
exports.Template = Template;