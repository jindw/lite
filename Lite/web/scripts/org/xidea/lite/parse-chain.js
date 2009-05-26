/*
 * List Template
 * License LGPL(您可以在任何地方免费使用,但请不要吝啬您对框架本身的改进)
 * http://www.xidea.org/project/lite/
 * @author jindw
 * @version $Id: template.js,v 1.4 2008/02/28 14:39:06 jindw Exp $
 */

function ParseChain(context,index){
	this.context = context;
	this.index = index || 0;
}
ParseChain.prototype = {
    initialize:function(){
	    this.parser = this.context.parseList[this.index];
		this.nextChain = new ParseChain(this.context,this.index+1);
		this.initialize = Function.prototype;
    },
	process:function(node){
	    this.initiaize();
        var parser = this.parser;
        var nextChain = this.nextChain;
		if(!parser.accept || parser.accept(node)){
			parser.apply(context,[node,this.context,nextChain])
		}else{
			if(nextChain != null){
				nextChain.process();
			}
		}
	}
}