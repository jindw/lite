/**
 * alive module
 */
var XML_SPACE_ONELINE = require('./parse-xml').XML_SPACE_ONELINE
var parseChildRemoveAttr = require('./syntax-util').parseChildRemoveAttr

var Expression=require('js-el').Expression;

var IF_TYPE=require('./template-token').IF_TYPE;
var ELSE_TYPE=require('./template-token').ELSE_TYPE;

var EL_TYPE=require('./template-token').EL_TYPE;
var XT_TYPE=require('./template-token').XT_TYPE;
var XA_TYPE=require('./template-token').XA_TYPE;

var FOR_TYPE=require('./template-token').FOR_TYPE;
var VAR_TYPE=require('./template-token').VAR_TYPE;

var CAPTURE_TYPE=require('./template-token').CAPTURE_TYPE;

//var PLUGIN_TYPE=require('./template-token').PLUGIN_TYPE;

exports['intercepton$002a'] = interceptEvent;//on*
exports.interceptLiveWidget = interceptLiveWidget;
function interceptLiveWidget(attr){
	var el = attr.ownerElement;
	var varName = attr.value;
	var oldWidgetValue = this.getAttribute(interceptLiveWidget);
	var oneline = this.getAttribute(XML_SPACE_ONELINE);
	
	this.setAttribute(XML_SPACE_ONELINE,true);
	
	el.setAttribute('data-alive-widget',varName);
	this.setAttribute(interceptLiveWidget,varName);
	var start = this.mark();
	parseChildRemoveAttr(this,attr,true);
	var result = this.reset(start)
	this.appendAll(JSON.parse(JSON.stringify(result)))
	var modelEL = processResult(result)
	//console.log(1)
	var config = {name:varName+"['']"};
	this.appendText("<script>//<![CDATA[\n");
	this.appendText('var '+varName+'=')
	this.appendEL('JSON.stringify('+modelEL+')');
	this.appendText(';\n')
	if(oldWidgetValue == null){
		this.appendText("function __update_live_widget(model,id){" +
				"document.querySelector('*[data-alive-widget=\"'+id+'\"]').innerHTML" +
				" = \nmodel[''](model)};\n")
	}
	this.appendPlugin("org.xidea.lite.parse.ClientPlugin",JSON.stringify(config));
	this.appendAll(result);
	this.appendEnd();
	this.appendText("//]]></script>")
	this.setAttribute(XML_SPACE_ONELINE,oneline);
	this.setAttribute(interceptLiveWidget,oldWidgetValue||'');
	//console.log(3)
}

var TAG_EXP = /(<[\w\-\.\:]+)|(\s+[\w\-\.\:]+(?:\s*\=\s*\'[^']*\')?)+|(\s*\/?>)/g
function processResult(result){
	trimWrapper(result);
	//console.log('$$',result)
	var refMap = vistRefs(result);
	var buf = ['{'];
	for(var n in refMap){
		buf.push(n,':',n,',')
	}
	buf[buf.length-1 || 1] = '}'
	return buf.join('');
}
function trimWrapper(result){
	var i = 0;
	var offset = 0
	outer:for(;i<result.length;i++){
		var m;
		var item = result[i];
		//console.log(i,item)
		if(typeof item == 'string'){
			while(m=TAG_EXP.exec(item)){
				//console.log(m.index,m[0])
				if(m[3]){
					offset = m.index+m[0].length;
					break outer;
				}else if(!m[0]){
					break;
				}
			}
		}
	}
	i && result.splice(0,i);
	result[0] = result[0].slice(offset);
	result.push(result.pop().replace(/<\/[\w\-\.]+>$/,''));
}
function vistRefs(result){
	var varMap = {};
	var refMap = {};
	for(var i=0;i<result.length;i++){
		var item = result[i];
		switch(item[0]){
			case CAPTURE_TYPE:
				varMap[item[1]]=true;
				break;
			case VAR_TYPE:
			case FOR_TYPE:
				varMap[item[2]]=true;
			case IF_TYPE:
			case ELSE_TYPE:
			case EL_TYPE:
			case XT_TYPE:
			case XA_TYPE:
				var el = item[1];
				if(el){
					el = new Expression(el);
					//copyTo(el.getVarMap(),varMap)
					var elRefMap = el.getVarMap();
					for(var n in elRefMap){
						if(!(n in varMap)){
							refMap[n]=true;//(refMap[n]||0)+1;
						}
					}
				}
		}
	}
	return refMap;
}
function copyTo(from,to){
	for(var n in from){
		to[n] = from[n];
	}
}
function interceptEvent(attr){
	//console.log(111111111)
	var el = attr.ownerElement;
	var attrName = attr.localName;
	var attrValue = String(attr.value).replace(/^\s+|\s+$/g,'');
	var baseValue = el.getAttribute(attrName);
	var widgetName = this.getAttribute(interceptLiveWidget);
	var buf = baseValue?[baseValue,';']:[''];
	buf.push('	with(',widgetName,'){',attrValue,'}');
	buf.push('__update_live_widget(',widgetName,',"',widgetName,'")')
	el.setAttribute(attrName,buf.join(''));
	//console.log(attr+'')
	parseChildRemoveAttr(this,attr,true);
}