package org.xidea.el.operation;

public interface Invocable {
	public abstract Object invoke(Object thizz,Object... args) throws Exception;
}