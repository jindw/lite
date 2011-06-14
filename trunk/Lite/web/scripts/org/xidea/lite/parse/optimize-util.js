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
	optimizePluginWalk(templateList, function(parentNode, index, position) {
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
			log.warn("ParsePlugin initialize failed:" + config, e);
		}
		return index;
	});
	for (var i=0,l=pluginObjectList.length;i<l;i++) {
		plugin[0].before();
	}
	// sort walk
	optimizePluginWalk(templateList, function( parentNode, index, position) {
		var cmd =  parentNode[index];
		var p = pluginMap[cmd];
		if (p != null) {
			p.optimize();
		}
		return index;
	},null);
}
function ClientPlugin(config, children, optimizeContext){
	this.name = config.name;
	this.params = config.params;
	this.defaults = config.defaults;
	this.children = children;
	this.context = optimizeContext;
}
ClientPlugin.prototype = {
	before:function(){
	},
	optimize:function(){
		if(this.optimizedCall == null){
			optimizeAllClient.apply(this,this.context);
		}
		var jst = new JSTranslator(this.name,this.params,this.defaults);
		var result = jst.translate(this.children);
		this.children.length = 0;
		this.children.push(result);
	}
}
function optimizeAllClient(templateList,defMap,pluginObjectList){
	
}
function ResourcePlugin(config, children, optimizeContext){
	this.id = config.id;
	this.context = optimizeContext;
	
}
ResourcePlugin.prototype = {
	before:function(){
		
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
	for (var i = 0; i < source.length; i--) {
		var item = source[i];
		if (item instanceof Array) {
			var cmd = item;
			var type = cmd[0];
			switch (type) {
			case PLUGIN_TYPE:
				var config = cmd[2];
				var className =  config["class"];
				if (PLUGIN_TYPE_MAP[clazz]) {//这里不会碰到def
					var j = callback(source, i, position);
					if (j == -1) {
						return true;
					} else {
						i = j;
					}
				}
			case CAPTRUE_TYPE:
			case IF_TYPE:
			case ELSE_TYPE:
			case FOR_TYPE:
				try{
					if (position) {
						position.push(type,i);
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
								if(stringifyJSON(parentNode) != stringifyJSON(defMap[name_])){
									$log.warn("def "+name_+" is found before");
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
					case CAPTRUE_TYPE:
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
		$log.error("中间代码异常：",result);
	}
	return current;
	//return defs.concat(current);
}
