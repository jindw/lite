/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */
/**
 * 解析链对象
 */
function ParseChain(context,index){
	this.context = context;
	this.index = index>0?index:0;
}
ParseChain.prototype = {
    initialize:function(){
    	var parserList = this.context.parserList;
    	var index = this.index;
	    this.parser = parserList[index];
		this.nextChain = new ParseChain(this.context,index+1);
	    this.initialize = Function.prototype;
    },
	process:function(node){
		if(null == node){
			return null;
		}
	    this.initialize();
        var parser = this.parser;
        var nextChain = this.nextChain;
        
        if(!parser){
        	$log.error("parser is not found:",this.context.parserList.length,this.index,node)
        }
           if ((typeof node != 'string')
        	  && (node.nodeType ==null)
        	  && (node instanceof Packages.org.w3c.dom.NodeList)
        	  ) {
				var len = node.length;
				for (var i = 0; i < len; i++) {
					parser(node.item(i),this.context,nextChain)
				}
			} else{
				if(!parser.accept || parser.accept(node)){
					parser(node,this.context,nextChain)
				}else if(nextChain != null){
               		nextChain.process();
            	}
			}
			return ;
		if(!parser.accept || parser.accept(node)){
			parser(node,this.context,nextChain)
		}else if(nextChain != null){
            nextChain.process();
		}
	}
}