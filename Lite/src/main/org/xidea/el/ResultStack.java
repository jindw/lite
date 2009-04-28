package org.xidea.el;

public interface ResultStack {
	public Object get();
	public void set(Object object);
	public Object pop();
	public Object push(Object value);
}