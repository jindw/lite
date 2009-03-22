package org.xidea.lite.parser;


import org.xidea.el.operation.Invocable;


/**
 * 一些模板内置内部函数的集合
 * 
 * @author jindw
 */
public class HTMLNullEmptyText implements Invocable {
	public HTMLNullEmptyText() {
	}

	public Object invoke(Object thizz,Object... args) throws Exception {
		return args[0] == null?"":args[0];
	}

}
