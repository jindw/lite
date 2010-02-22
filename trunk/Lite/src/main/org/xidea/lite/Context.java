package org.xidea.lite;

import java.io.Writer;
import java.util.HashMap;

import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.impl.ValueStackImpl;

public class Context extends ValueStackImpl {
	private int readLength;
	private Context root;
	private Template template;

	public Context(Template template, Object... stack) {
		super(stack);
		this.readLength = stack.length;
		this.root = this;
		this.template = template;
	}

	private Context(Context root) {
		this(root.template, root.stack);
		this.root = root;
		this.requireWrite();
	}

	public void put(Object key, Object value) {
		requireWrite();
		ReflectUtil.setValue(stack[stack.length - 1], key, value);
	}

	/**
	 * 创建新域,需要处理局部域短路问题,倒霉,真想用栈:( 貌似有问题..添加测试吧
	 */
	public Context createScope() {
		root.requireWrite();
		return new Context(root);
	}

	/**
	 * 安需创建新HashMap
	 */
	private void requireWrite() {
		if (stack.length == readLength) {
			Object[] newStack = new Object[stack.length + 1];
			System.arraycopy(stack, 0, newStack, 0, stack.length);
			newStack[stack.length] = new HashMap<Object, Object>();
			stack = newStack;
		}
	}

	public void renderList(Object[] children, Writer out) {
		this.template.renderList(this, children, out);
	}
}