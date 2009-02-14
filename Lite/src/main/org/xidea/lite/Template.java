package org.xidea.lite;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionFactoryImpl;
import org.xidea.el.operation.ReflectUtil;

public class Template {
	private static Log log = LogFactory.getLog(Template.class);

	public static final int EL_TYPE = 0;// [0,'el']
	public static final int IF_TYPE = 1;// [1,[...],'test']
	public static final int BREAK_TYPE = 2;// [2,depth]
	public static final int XML_ATTRIBUTE_TYPE = 3;// [3,'value','name']
	public static final int XML_TEXT_TYPE = 4;// [4,'el']
	public static final int FOR_TYPE = 5;// [5,[...],'var','items','status']//status
	public static final int ELSE_TYPE = 6;// [6,[...],'test']//test opt?
	public static final int ADD_ON_TYPE = 7;// [7,[...],el,type]
	public static final int VAR_TYPE = 8;// [8,'value','name']
	public static final int CAPTRUE_TYPE = 9;// [9,[...],'var']

	public static final String FOR_KEY = "for";
	public static final String IF_KEY = "if";
	private Map<String, Object> gloabls = new HashMap<String, Object>(
			ExpressionFactoryImpl.DEFAULT_GLOBAL_MAP);

	private ExpressionFactory expressionFactory = new ExpressionFactoryImpl(
			gloabls);

	protected Object[] items;// transient＄1�7

	protected Template() {
	}

	public Template(List<Object> list) {
		this.items = this.compile(list);
	}

	@SuppressWarnings("unchecked")
	public void render(Object context, Writer out) throws IOException {
		Map<? extends Object, ? extends Object> contextMap;
		if (context instanceof Map) {
			contextMap = (Map<? extends Object, ? extends Object>) context;
		} else if (context == null) {
			contextMap = new HashMap<Object, Object>();
		} else {
			contextMap = ReflectUtil.map(context);
		}
		renderList(contextMap, items, out);
	}

	/**
	 * 编译模板数据,递归将元List数据转换为直接的数组，并编译el
	 * 
	 * @internal
	 */
	@SuppressWarnings("unchecked")
	protected Object[] compile(List<Object> datas) {
		// Object[] result = datas.toArray();// ;
		// reverse(result);
		List result = new ArrayList();
		for (int i = datas.size()-1; i >= 0; i--) {
			final Object item = datas.get(i);
			if (item instanceof List) {
				final List data = (List) item;
				final Object[] cmd = data.toArray();
				final int type = ((Number) cmd[0]).intValue();
				cmd[0] = type;
				// children
				switch (type) {
				case ADD_ON_TYPE:
					compileAddOns(cmd, result);
					continue;// for
				case CAPTRUE_TYPE:
				case IF_TYPE:
				case ELSE_TYPE:
				case FOR_TYPE:
					// case IF_STRING_IN_TYPE:
					cmd[1] = compile((List) cmd[1]);
					break;
				}
				switch (type) {
				case XML_ATTRIBUTE_TYPE:
					if (cmd[2] != null) {
						cmd[2] = " " + cmd[2] + "=\"";
					}
				case XML_TEXT_TYPE:
				case EL_TYPE:
					cmd[1] = createExpression(cmd[1]);
					break;
				// case VAR_TYPE:
				case IF_TYPE:
				case ELSE_TYPE:
					if (cmd[2] != null) {
						cmd[2] = createExpression(cmd[2]);
					}
					break;
				// case IF_STRING_IN_TYPE:
				// cmd[2] = createExpression(cmd[2]);
				case FOR_TYPE:
					cmd[3] = createExpression(cmd[3]);
					break;
				}
				result.add(cmd);
			}else{
				result.add(item);
			}
		}
		return result.toArray();
	}

	@SuppressWarnings("unchecked")
	protected void compileAddOns(final Object[] cmd, List<Object> result) {
		try {
			cmd[1] = compile((List<Object>) cmd[1]);
			Expression el = (Expression) (cmd[2] = createExpression(cmd[2]));
			Class<? extends Object> addOnType = Class.forName(String
					.valueOf(cmd[3]));
			cmd[3] = addOnType;
			if (CompileAdvice.class.isAssignableFrom(addOnType)) {
				((CompileAdvice) addOnType.newInstance()).execute(gloabls, el,
						result);
			}
			if (RuntimeAdvice.class.isAssignableFrom(addOnType)) {
				result.add(cmd);
			}
		} catch (Exception e) {
			log.error("装载扩展失败", e);
		}
	}

	protected void renderList(
			final Map<? extends Object, ? extends Object> context,
			final Object[] children, final Writer out) {
		int index = children.length;
		// for (final Object item : children) {
		while (index-- > 0) {
			final Object item = children[index];
			try {
				if (item instanceof String) {
					out.write((String) item);
				} else {
					final Object[] data = (Object[]) item;
					switch (((Number) data[0]).intValue()) {
					case EL_TYPE:// ":el":
						processExpression(context, data, out, false);
						break;
					case XML_TEXT_TYPE:// ":el":
						processExpression(context, data, out, true);
						break;
					case VAR_TYPE:// ":set"://var
						processVar(context, data);
						break;
					case CAPTRUE_TYPE:// ":set"://var
						processCaptrue(context, data);
						break;
					case IF_TYPE:// ":if":
						processIf(context, data, out);
						break;
					case ELSE_TYPE:// ":else-if":":else":
						processElse(context, data, out);
						break;
					case FOR_TYPE:// ":for":
						processFor(context, data, out);
						break;
					case XML_ATTRIBUTE_TYPE:// ":attribute":
						processAttribute(context, data, out);
						break;
					case BREAK_TYPE://
						prossesBreak(data);
					case ADD_ON_TYPE://
						prossesAddons(context, data, out);
						break;
					}
				}
			} catch (Break e) {
				if (e.isValid()) {
					throw e;
				}
			} catch (Exception e) {// 每一个指令都拒绝异常
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void prossesAddons(Map<? extends Object, ? extends Object> context,
			Object[] data, Writer out) throws Exception {
		Map<String, Object> attributeMap = (Map) ((Expression) data[1])
				.evaluate(context);
		RuntimeAdvice tag = (RuntimeAdvice) ((Class) data[2]).newInstance();
		for (String key : attributeMap.keySet()) {
			ReflectUtil.setValue(tag, key, attributeMap.get(key));
		}
		tag.execute(context, out);
	}

	@SuppressWarnings("unchecked")
	protected void processExpression(Map context, Object[] data, Writer out,
			boolean encodeXML) throws IOException {
		Object value = ((Expression) data[1]).evaluate(context);
		if (encodeXML && value != null) {
			printXMLText(String.valueOf(value), out);
		} else {
			out.write(String.valueOf(value));
		}
	}

	@SuppressWarnings("unchecked")
	protected void processIf(Map context, Object[] data, Writer out) {
		Boolean test = Boolean.TRUE;
		try {
			if (toBoolean(((Expression) data[2]).evaluate(context))) {
				renderList(context, (Object[]) data[1], out);
			} else {
				test = Boolean.FALSE;
			}
		} finally {
			context.put(IF_KEY, test);
		}

	}

	@SuppressWarnings("unchecked")
	protected void processElse(Map context, Object[] data, Writer out) {
		if (!toBoolean(context.get(IF_KEY))) {
			Boolean test = Boolean.TRUE;
			try {
				if (data[2] == null
						|| toBoolean(((Expression) data[2]).evaluate(context))) {// if
					renderList(context, (Object[]) data[1], out);
				} else {
					test = Boolean.FALSE;
				}
			} finally {
				context.put(IF_KEY, test);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void processFor(Map context, Object[] data, Writer out) {
		Object list = ((Expression) data[3]).evaluate(context);
		final Object[] children = (Object[]) data[1];
		final String varName = (String) data[2];
		final String statusName = (String) data[4];
		final ForStatus preiousStatus = (ForStatus) context.get(FOR_KEY);
		int len = 0;
		ForStatus forStatus = new ForStatus(preiousStatus == null ? 0
				: preiousStatus.getDepth() + 1);
		try {
			context.put(FOR_KEY, forStatus);
			if (statusName != null) {
				context.put(statusName, forStatus);
			}
			if (list instanceof CharSequence) {
				list = ((CharSequence) list).toString().toCharArray();
			}
			if (list.getClass().isArray()) {// list!=null && 让他抛吧，外面有检查
				len = Array.getLength(list);
				forStatus.setSize(len);
				while (++forStatus.index < len) {
					context.put(varName, Array.get(list, forStatus.index));
					renderList(context, children, out);
				}
			} else if (list instanceof Number) {
				len = ((Number) list).intValue();
				while (++forStatus.index < len) {
					context.put(varName, forStatus.index + 1);
					renderList(context, children, out);
				}
			} else if (list instanceof Collection) {
				Collection<Object> items = (Collection<Object>) list;
				len = items.size();
				forStatus.setSize(len);
				for (Object item : items) {
					forStatus.index++;
					context.put(varName, item);
					renderList(context, children, out);
				}
			}
			if (statusName != null) {
				context.put(statusName, preiousStatus);
			}
		} finally {
			// context.put("for", preiousStatus);
			context.put(FOR_KEY, preiousStatus);// for key
			context.put(IF_KEY, len > 0);// if key
		}
	}

	@SuppressWarnings("unchecked")
	protected void processVar(Map context, Object[] data) {
		context.put(data[2], ((Expression) data[1]).evaluate(context));
	}

	@SuppressWarnings("unchecked")
	protected void processCaptrue(Map context, Object[] data) {
		StringWriter buf = new StringWriter();
		renderList(context, (Object[]) data[1], buf);
		context.put(data[2], buf.toString());
	}

	@SuppressWarnings("unchecked")
	protected void processAttribute(Map context, Object[] data, Writer out)
			throws IOException {
		Object result = ((Expression) data[1]).evaluate(context);
		if (data[2] == null) {
			printXMLAttribute(String.valueOf(result), out, true);
		} else if (result != null) {
			out.write((String) data[2]);// prefix
			printXMLAttribute(String.valueOf(result), out, false);
			out.write('"');
		}

	}

	protected void prossesBreak(Object[] data) {
		throw new Break(((Number) data[1]).intValue());
	}

	protected Expression createExpression(Object elo) {
		return expressionFactory.createEL(elo);
	}

	protected void printXMLAttribute(String text, Writer out,
			boolean escapeSingleChar) throws IOException {
		for (int i = 0; i < text.length(); i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.write("&lt;");
				break;
			case '>':
				out.write("&gt;");
				break;
			case '&':
				out.write("&amp;");
				break;
			case '"':// 34
				out.write("&#34;");
				break;
			case '\'':// 39
				if (escapeSingleChar) {
					out.write("&#39;");
				}
				break;
			default:
				out.write(c);
			}
		}
	}

	protected void printXMLText(String text, Writer out) throws IOException {
		for (int i = 0; i < text.length(); i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.write("&lt;");
				break;
			case '>':
				out.write("&gt;");
				break;
			case '&':
				out.write("&amp;");
				break;
			default:
				out.write(c);
			}
		}
	}

	protected boolean toBoolean(Object test) {
		if (test == null) {
			return false;
		} else if (test instanceof Boolean) {
			return (Boolean) test;
		} else if (test instanceof String) {
			return ((String) test).length() > 0;
		} else if (test instanceof Number) {
			return ((Number) test).floatValue() != 0;
		}
		return true;
	}

	@SuppressWarnings("serial")
	protected static class Break extends RuntimeException {
		private int depth;

		protected Break(int depth) {
			this.depth = depth;
		}

		protected boolean isValid() {
			return --depth > 0;
		}

	}

	public static class ForStatus {
		private int index = -1;
		private int lastIndex;
		private final int depth;

		ForStatus(int depth) {
			this.depth = depth;
		}

		private void setSize(int end) {
			this.lastIndex = end - 1;
		}

		public int getIndex() {
			return index;
		}

		public int getDepth() {
			return depth;
		}

		public int getLastIndex() {
			return lastIndex;
		}

	}
}
