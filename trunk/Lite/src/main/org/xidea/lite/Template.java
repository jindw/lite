package org.xidea.lite;

import java.io.IOException;
import java.util.Map;

public interface Template {

	// 默认值为 text/html
	public static final int EL_TYPE = 0; // [0,<el>]
	public static final int IF_TYPE = 1; // [1,[...],<test el>]
	public static final int BREAK_TYPE = 2; // [2,depth]
	public static final int XA_TYPE = 3; // [3,<value el>,'name']
	public static final int XT_TYPE = 4; // [4,<el>]
	public static final int FOR_TYPE = 5; // [5,[...],<items el>,'varName']/
	public static final int ELSE_TYPE = 6; // [6,[...],<test el>] //<test el>
	// 可为null
	public static final int PLUGIN_TYPE = 7; // [7,[...],<add on
	// el>,'<addon-class>']
	public static final int VAR_TYPE = 8; // [8,<value el>,'name']
	public static final int CAPTURE_TYPE = 9; // [9,[...],'var']

	public abstract String getEncoding();
	
	public abstract String getContentType();

	public abstract void render(Object context, Appendable out) throws IOException;

	public abstract void render(Map<String, Object> context, Object[] children,
			Appendable out);

	public abstract void addVar(String name, Object value);

}