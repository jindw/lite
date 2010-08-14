package org.xidea.el;

public interface Invocable {

	public abstract Object invoke(Object thiz,Object... args) throws Exception;
}