package org.xidea.lite.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.lite.RuntimePlugin;
import org.xidea.lite.Template;
import org.xidea.lite.parse.IllegalEndException;
import org.xidea.lite.parse.OptimizeContext;
import org.xidea.lite.parse.OptimizePlugin;
import org.xidea.lite.parse.ResultContext;

/**
 * 接口函数不能直接相互调用，用context对象！！！
 * 
 * @author jindw
 */
public class ResultContextImpl implements ResultContext {
	static final Object END_INSTRUCTION = new Object[0];

	@SuppressWarnings("unused")
	private static final Log log = LogFactory.getLog(ParseContextImpl.class);

	private ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();
	private int inc = 0;

	private final ArrayList<Object> result = new ArrayList<Object>();

	public Object parseEL(String expression) {
		return expressionFactory.parse(expression);
	}

	private Object requrieEL(Object expression) {
		if (expression instanceof String) {
			expression = parseEL((String) expression);
		}
		return expression;
	}

	static Pattern VAR_PATTERN = Pattern
			.compile("^(?:(break|case|catch|const|continue|default|do|else|false|finally|for|function|if|in|instanceof|new|null|return|switch|this|throw|true|try|var|void|while|with)|[a-zA-Z_][\\w_]*)$");

	private String checkVar(String var) {
		Matcher matcher = VAR_PATTERN.matcher(var);
		if (var == null || !matcher.find() || matcher.group(1) != null) {
			throw new IllegalArgumentException("无效变量名：Lite有效变量名为(不包括括弧中的保留字)："
					+ VAR_PATTERN.pattern() + "\n当前变量名为：" + var);
		}
		return var;
	}

	public void append(String text) {
		if (text != null && text.length() > 0) {
			result.add(text);
		}
	}

	private void append(int type,Object... args) {
		Object[] item = new Object[args.length+1];
		item[0] = type;
		System.arraycopy(args, 0, item, 1, args.length);
		result.add(item);

	}

	public final void appendAll(List<Object> items) {
		for (Object text : items) {
			if (text instanceof String) {
				this.append((String) text);
				continue;
			} else if (text instanceof Collection<?>) {
				text = ((Collection<?>) text).toArray();
			}
			Object[] item = (Object[]) text;
			if(item.length>0){
				item[0] = ((Number)item[0]).intValue();
			}
			result.add(item);
		}
	}

	protected void clearPreviousText() {
		int i = result.size();
		while (i-- > 0) {
			Object item = result.get(i);
			if (item instanceof String) {
				result.remove(i);
			} else {
				break;
			}

		}
	}

	public final void appendEL(Object el) {
		el = requrieEL(el);
		this.append(Template.EL_TYPE, el);

	}

	public final void appendXT(Object el) {

		el = requrieEL(el);
		this.append(Template.XT_TYPE, el );
	}
	public final void appendXA(String name, Object el) {
		el = requrieEL(el);
		this.append( Template.XA_TYPE, el, name );

	}

	public final void appendIf(Object testEL) {
		testEL = requrieEL(testEL);
		this.append(Template.IF_TYPE, testEL );
	}

	public final void appendElse(Object testEL) {
		this.clearPreviousText();
		testEL = requrieEL(testEL);
		if (this.getType(this.result.size() - 1) != -1) {
			this.appendEnd();
		}
		this.append(Template.ELSE_TYPE, testEL );
	}

	public final int appendEnd() {
		int type = this.findBegin();
		if (type < 0) {
			throw new IllegalEndException();
		}
		this.result.add(END_INSTRUCTION);
		return type;
	}

	public final void appendVar(String name, Object el) {
		el = requrieEL(el);
		this.append(Template.VAR_TYPE, el, checkVar(name) );
	}

	public final void appendCapture(String varName) {
		this.append(Template.CAPTURE_TYPE, checkVar(varName) );

	}

	public final void appendFor(String var, Object itemsEL, String status) {
		itemsEL = requrieEL(itemsEL);
		this.append(Template.FOR_TYPE, itemsEL, var );
		if (status != null && status.length() > 0) {
			this.appendVar(checkVar(status), this.parseEL("for"));
		}
	}


	public final void appendPlugin(String pluginClazz, Map<String, Object> config) {
		try {
			Class<?> clazz = Class.forName(pluginClazz);
			if (RuntimePlugin.class.isAssignableFrom(clazz)
					|| OptimizePlugin.class.isAssignableFrom(clazz)) {
				//el = requrieEL(el);
				HashMap<String, Object> config2 = new HashMap<String, Object>(config);
				config2.put("class",pluginClazz);
				this.append(Template.PLUGIN_TYPE, config2 );
			} else {
				throw new RuntimeException("Plugin class not found(plugin ignored):"
						+ pluginClazz);
			}
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
	}

	public final int mark() {
		return result.size();
	}

	public final List<Object> reset(int mark) {
		int end = result.size();
		// 好像是关联的，所以，希望尽早解除关联
		List<Object> pops = new ArrayList<Object>(result.subList(mark, end));
		int i = end;
		while (i-- > mark) {
			result.remove(i);
		}
		return OptimizeUtil.optimizeList(pops);
	}

	
	public List<Object> toList() {
		// toMergeText
		List<Object> result2 = OptimizeUtil.optimizeList(this.result);
		// toTree
//		List<List<Object>> parsePlugins = new ArrayList<List<Object>>();
		Map<String, List<Object>> defMap = new LinkedHashMap<String,List<Object>>();
		List<Object> templateList = OptimizeUtil.toListTree(result2, defMap);
		OptimizeContext ppc = new OptimizeContextImpl(templateList,defMap);
		// 将defs 添加到中间代码的最前端
		return ppc.optimize();
	}



	public String allocateId() {
		String id;
		id = "__" + inc++ + "__";
		return id;
	}


	@SuppressWarnings("unused")
	private int findBeginType() {
		int begin = findBegin();
		if (begin >= 0) {
			return this.getType(begin);
		}
		return -3;// no begin
	}

	private int findBegin() {
		int depth = 0;
		int i = this.result.size();
		while (i-- > 0) {
			switch (getType(i)) {
			case Template.CAPTURE_TYPE:
			case Template.IF_TYPE:
			case Template.ELSE_TYPE:
			case Template.FOR_TYPE:
			case Template.PLUGIN_TYPE:
				depth--;
				break;
			case -1:
				depth++;
			}
			if (depth == -1) {
				return i;
			}
		}
		return -1;
	}

	public int getDepth() {
		int depth = 0;
		int length = this.result.size();
		for (int i = 0; i < length; i++) {
			switch (getType(i)) {
			case Template.CAPTURE_TYPE:
			case Template.IF_TYPE:
			case Template.ELSE_TYPE:
			case Template.FOR_TYPE:
				depth++;
				break;
			case -1:
				depth--;
			}
		}
		return depth;
	}

	public int getType(int offset) {
		Object item = this.result.get(offset);
		if (item instanceof Object[]) {
			Object[] ins = (Object[]) item;
			if (ins.length == 0) {
				return -1;// end token
			} else {
				return ((Number) ins[0]).intValue();
			}
		}
		return -2;// string type
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

}
