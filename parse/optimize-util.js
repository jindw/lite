/**
 * 	
	public OptimizePlugin getPlugin(List<Object> object);
	public OptimizeScope parseScope(List<Object> children, List<String> params);
	public Map<String, Set<String>> getDefCallMap();
	public void optimizeCallClosure(Map<String, Set<String>> callMap,
			Set<String> optimizedCall);
			* 
 * @see org.xidea.lite.OptimizeContext#optimize();
 */
function doOptimize(defMap,templateList){
	var pluginObjectList = [];
	var optimizeContext = [templateList,defMap,pluginObjectList];
	optimizePluginWalk(templateList, function(parentNode, index) {
		var cmd = parentNode[index];
		var config = cmd[2];
		var className = config["class"];
		try {
			var children =cmd[1];
			var plugin = new PLUGIN_TYPE_MAP[className](config, children, optimizeContext);
			pluginObjectList.push([plugin,cmd]);
			//TODO:..
			//pluginMap.put(cmd, plugin);
		} catch (e) {
			console.warn("ParsePlugin initialize failed:" + config, e);
		}
		return index;
	});
	if(pluginObjectList.length){
		for (var i=0,l=pluginObjectList.length;i<l;i++) {
			pluginObjectList[i][0].before();
		}
		// sort walk
		optimizePluginWalk(templateList, function( parentNode, index) {
			var cmd =  parentNode[index];
			for (var i=0,l=pluginObjectList.length;i<l;i++) {
				if(pluginObjectList[i][1] == cmd){
					var p = pluginObjectList[i][0];
					break;
				}
			}
			if (p != null) {
				p.optimize();
			}
			return index;
		},null);
		optimizePluginWalk(templateList, function(  parentNode, index) {
			var cmd = parentNode[index];
			var config = cmd[2];
			var className = config["class"];
			if(className in PLUGIN_TYPE_MAP){
				var children = cmd[1];
				var args = [index,1].concat(children);
				parentNode.splice.apply(parentNode,args);
				index--;
				index += children.length;
			}
			return index;
		});
	}
	var result = [];
	for(var n in defMap){
		result.push(defMap[n]);
	}
	return result.concat(templateList);
}
/**
 * callMap = {"fn":{"fn1":1,"fn2":2...}}
 * closure = {"fn1":1,"fn2":2}
 */
function optimizeCallClosure(callMap,
			closure) {
	for(var n in closure){
		if(!(n in callMap)){
			delete closure[n]
		}
	}
	var waitMap = closure;
	while (true) {
		var newClosure = {};
		for (var fn in waitMap) {
			var called = callMap[fn];
			for (var fn2 in called) {
				if ((fn2 in callMap)
						&& !(fn2 in closure) && !(fn2 in newClosure)) {
					newClosure[fn2]=1;
				}
			}
		}
		var hit  = false;
		for(var fn in newClosure){
			hit = true;
			closure[fn] = 1;
		}
		if (hit) {
			waitMap = newClosure;
		} else {
			return;
		}
	}
}

var inc = 1;
function ClientPlugin(config, children, optimizeContext){
	this.name = config.name;
	this.params = config.params;
	this.defaults = config.defaults;
	this.children = children;
	this.context = optimizeContext;
	this.inc = inc++;
}
ClientPlugin.prototype = {
	before:function(){
	},
	optimize:function(){
//		console.info("optimize!!!",this.optimizedCall == null,this.inc);
		if(this.optimizedCall == null){
			optimizeAllClient.apply(this,this.context);
		}
		var defMap = this.context[1]
		var result = [];
		for(var n in this.optimizedCall){
			if(defMap[n]){
				result.push(defMap[n]);
			}else{
				console.error("Defined function not found:"+n)
			}
		}
		//if(!this.first){
		//	jst.liteImpl = jst.liteImpl || 'liteImpl';
		//}
		//var result = jst.translate(result.concat(this.children));
		var jst = new JSTranslator();
		var result = jst.translate(result.concat(this.children),{name:this.name,params:this.params,defaults:this.defaults});
		this.children.length = 0;
		this.children.push(result);
	}
}
function getDefScope(data){
	var scope = data[-1];
	if(!scope){
		scope = new OptimizeScope(data[1],data[2].params);
		data[-1] = scope;
	}
	return scope;
}
function copy(source,target){
	for(var n in source){
		target[n] = source[n];
	}
}
function remove(source,target){
	for(var n in source){
		delete target[n];
	}
}
function getDefCall(data){
	var scope = getDefScope(data);
	var callMap = {}
	copy(scope.callMap,callMap);
//	if(scope.callMap['*']){
//		copy(scope.externalRefMap,callMap);
//		delete callMap['*'];
//	}
	copy(scope.externalRefMap,callMap);
	remove(scope.varMap,callMap);
	remove(scope.paramMap,callMap);
	delete callMap['*'];
	return callMap
	
}
function optimizeAllClient(templateList,defMap,pluginObjectList){
	var positionList = [];
	var cmdList = [];
	var namedClientCallMap = {};
	var pluginList = [];
	var dataList = [];
	optimizePluginWalk(templateList, function( parentNode, index, post32) {
		var cmd =  parentNode[index];
		for(var i = pluginObjectList.length;i--;){
			var po = pluginObjectList[i];
			if(po[1] == cmd && po[0] instanceof ClientPlugin){
				var p = pluginObjectList[i][0];
				positionList.push(post32);//.replace(/\u0009./g,''));
				pluginList.push(p);
				cmdList.push(cmd);
				if(p.name){
					namedClientCallMap[p.name] = getDefCall(pluginObjectList[i][1]);
				}
				break;
			}
		}
		return index;
	},[]);
	var callMap = {};
	for(var n in namedClientCallMap){
		callMap[n] = namedClientCallMap[n];
	}
	for(var n in defMap){
		if(!(n in callMap)){
			callMap[n] = getDefCall(defMap[n]);
		}
	}
	for (var i = 0, end = positionList.length; i < end; i++) {
		var plugin = pluginList[i];
		var position = positionList[i];
		var optimizedCall = getDefCall(cmdList[i]);
		optimizeCallClosure(callMap, optimizedCall);
		for(var n in optimizedCall){
			if(n in namedClientCallMap){
				delete optimizedCall[n];
			}
		}
		var isFirst = true;
		for (var j = 0; j < i; j++) {
			if (position.indexOf(positionList[j]) ==0) {
				var removeMap = pluginList[j].optimizedCall;
				isFirst = false;
				for(var n in removeMap){
					delete optimizedCall[n];
				}
			}
		}
		plugin.first = isFirst;
		plugin.optimizedCall = optimizedCall;
	}
}
function ResourcePlugin(config, children, optimizeContext){
	this.id = config.id;
	this.context = optimizeContext;
	this.children = children;
	
}
ResourcePlugin.prototype = {
	before:function(){
		var remove = [];
		var id = this.id;
		optimizePluginWalk(this.context[0],function(parentNode, index, position) {
			var cmd = parentNode[index];
			var config = cmd[2];
			if (id == config.targetId) {
				remove.push(parentNode,index)
			}
			return index;
		});
		while(remove.length){
			var index = remove.pop();
			var parentNode = remove.pop();
			var cmds = parentNode.splice(index,1);
			this.children.push(cmds[0]);
		}
	},
	optimize:function(){
		
	}
}
var PLUGIN_TYPE_MAP = {
	//"org.xidea.lite.DefinePlugin":true,
	"org.xidea.lite.parse.ClientPlugin":ClientPlugin,
	"org.xidea.lite.parse.ResourcePlugin":ResourcePlugin
}
/**
 * @see org.xidea.lite.OptimizeContext#walk(OptimizeWalker parseWalker);
 */
function optimizePluginWalk(source,callback,position){
	for (var i = 0; i < source.length; i++) {
		var item = source[i];
		if (item instanceof Array) {
			var cmd = item;
			var type = cmd[0];
			switch (type) {
			case PLUGIN_TYPE:
				var config = cmd[2];
				var className =  config["class"];
				if (PLUGIN_TYPE_MAP[className]) {//这里不会碰到def
					var j = callback(source, i, position && String.fromCharCode.apply(null,position));
					if (j == -1) {
						return true;
					} else {
						i = j;
					}
				}
			case CAPTURE_TYPE:
			case IF_TYPE:
			case ELSE_TYPE:
			case FOR_TYPE:
				try{
					if (position) {
						position.push(type,i+32);
					}
					if(optimizePluginWalk(cmd[1], callback, position)){
						return true;
					}
				}finally{
					if (position) {
						position.pop();position.pop();
					}
				}

			}
		}
	}
	return false;
}
/**
 * 想当前栈顶添加数据
 * 解析和编译过程中使用
 * @public
 */
function optimizeResult(source){
    var result = [];
    var previousText;
    for(var i=0,j=0;i<source.length;i++){
    	var item = source[i];
		if ('string' == typeof item) {
			if(previousText==null){
				previousText = item;
			}else{
				previousText += item;
			}
		}else{
			if(previousText){
				result[j++] = previousText;
			}
			previousText = null;
			result[j++] = item;
		}
    }
    if(previousText){
    	result[j] = previousText;
    }
    return result;
}


/**
 * 将中间代码树形化,并将函数定义分离提出
 */
function buildTreeResult(result,defMap){
	var stack = [];//new ArrayList<ArrayList<Object>>();
	var current = [];// new ArrayList<Object>();
	stack.push(current);
	try{
		for (var i = 0;i<result.length;i++) {
		    var item = result[i];
			if ('string' == typeof item) {
				current.push(item);
			} else {
				if (item.length == 0) {//end
					var children = stack.pop();
					current = stack[stack.length-1];//向上一级列表
					var parentNode = current.pop();//最后一个是当前结束的标签
					parentNode[1]=children;
					if(parentNode[0] == PLUGIN_TYPE){
						var config = parentNode[2];
						if(config['class']== 'org.xidea.lite.DefinePlugin'){
							var name_ = config.name;
							if(name_ in defMap){
								if(JSON.stringify(parentNode) != JSON.stringify(defMap[name_])){
									console.warn("def "+name_+" is found before");
								}
							}
							defMap[name_]=parentNode;
						}else{
							current.push(parentNode);
						}
					}else{
						current.push(parentNode);
					}
					
				} else {
					var type = item[0];
					var cmd2 =[];
					cmd2.push(item[0]);
					current.push(cmd2);
					switch (type) {
					case CAPTURE_TYPE:
					case IF_TYPE:
					case ELSE_TYPE:
					case PLUGIN_TYPE:
					case FOR_TYPE:
						cmd2.push(null);
						stack.push(current = []);
					}
					for (var j = 1; j < item.length; j++) {
						cmd2.push(item[j]);
					}
	
				}
			}
		}
	}catch(e){
		console.error("中间代码异常：",result);
	}
	return current;
	//return defs.concat(current);
}

if(typeof require == 'function'){
//exports.extractStaticPrefix=extractStaticPrefix;
exports.doOptimize=doOptimize;
exports.optimizeResult=optimizeResult;
exports.buildTreeResult=buildTreeResult;
exports.PLUGIN_TYPE_MAP=PLUGIN_TYPE_MAP;
var OptimizeScope=require('./optimize-scope').OptimizeScope;
var PLUGIN_DEFINE=require('./template-token').PLUGIN_DEFINE;
var PLUGIN_TYPE=require('./template-token').PLUGIN_TYPE;
var CAPTURE_TYPE=require('./template-token').CAPTURE_TYPE;
var VAR_TYPE=require('./template-token').VAR_TYPE;

var JSTranslator=require('./js-translator').JSTranslator;

var PLUGIN_DEFINE=require('./template-token').PLUGIN_DEFINE;
var VAR_TYPE=require('./template-token').VAR_TYPE;
var ELSE_TYPE=require('./template-token').ELSE_TYPE;
var PLUGIN_TYPE=require('./template-token').PLUGIN_TYPE;
var CAPTURE_TYPE=require('./template-token').CAPTURE_TYPE;
var IF_TYPE=require('./template-token').IF_TYPE;
var FOR_TYPE=require('./template-token').FOR_TYPE;
}