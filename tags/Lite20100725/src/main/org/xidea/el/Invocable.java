package org.xidea.el;

public interface Invocable {
	public abstract Object invoke(Object thizz,Object... args) throws Exception;
}