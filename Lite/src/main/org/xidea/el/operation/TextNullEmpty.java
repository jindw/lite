package org.xidea.el.operation;




/**
 * 一些模板内置内部函数的集合
 * 
 * @author jindw
 */
public class TextNullEmpty implements Invocable {
	public TextNullEmpty() {
	}

	public Object invoke(Object thizz,Object... args) throws Exception {
		return args[0] == null?"":args[0];
	}

}
