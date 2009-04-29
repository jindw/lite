function findStatus(code){
	var vs = new VarStatus();
	doFind(code,vs);
	return vs;
}
function parseEL(el){
	return new ExpressionTokenizer(el).toTokens();
}
function VarStatus(){
	this.useReplacer = false;
	this.vars={};
	this.refs={};
	this.defs = [];
}
VarStatus.prototype.addVar = function(n){
	this.vars[n] = true;
}
VarStatus.prototype.vistEL = function(el){
	var tokens = parseEL(el)
	var result ={};
	doFindVar(tokens,result);
	for(var n in result){
		if(!this.vars[n]){
			this.refs[n] = true;
		}
	}
}
function doFindVar(tokens,result){
	for(var i=0;i<tokens.length;i++){
		var item = tokens[i];
		if(item instanceof Array){
			if(item[0] == VALUE_VAR){
				result[item[1]] = true;
			}else if(item[0] == VALUE_LAZY){
				doFindVar(item[1],result);
			}
		}
	}
}

function doFindDef(item,pvs){
	var el = window.eval('('+item[2]+')');
	pvs.addVar(el.name);
	var vs = new VarStatus();
	var args = el.arguments.slice(0);
	vs.arguments = args;
	for(var i=0;i<args.length;i++){
	    vs.vars[args[i]] = true;
	}
	vs.name = el.name;
	vs.code = item[1];
	pvs.defs.push(vs);
	doFind(item[1],vs);
	for(var n in vs.refs){
		if(!vs.vars[n]){
			vs.refs[n] = true;
			if(!pvs.vars[n]){
			    pvs.refs[n] = true;
			}
		}
	}
	pvs.useReplacer = pvs.useReplacer || vs.useReplacer;
}
function doFind(code,vs){
    for(var i=0;i<code.length;i++){
        var item = code[i];
        if(item instanceof Array){
			switch (item[0]) {
			case ADD_ON_TYPE:
				if(item[3] == '#def'){
					doFindDef(item,vs)
				}
				break;
			case XML_ATTRIBUTE_TYPE:
			case VAR_TYPE:
			case XML_TEXT_TYPE:
				vs.useReplacer = true;
			case EL_TYPE:
			    vs.vistEL(item[1]);
				break;
			case FOR_TYPE:
			case IF_TYPE:
			    vs.vistEL(item[2]);
				doFind(item[1],vs);
				break;
			case ELSE_TYPE:
				if (item[2] != null) {
			           vs.vistEL(item[2]);
				}
				doFind(item[1],vs);
				break;
			case CAPTRUE_TYPE:
				doFind(item[1],vs);
				vs.addVar(item[2]);
				break;
			case VAR_TYPE:
				vs.vistEL(item[1]);
				vs.addVar(item[2]);
				break;
			}
        }
    }
}