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
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ReflectUtil;

public class Template {
	private static Log log = LogFactory.getLog(Template.class);
	public static final int EL_TYPE = 0; // [0,<el>]
	public static final int IF_TYPE = 1; // [1,[...],<test el>]
	public static final int BREAK_TYPE = 2; // [2,depth]
	public static final int XML_ATTRIBUTE_TYPE = 3; // [3,<value el>,'name']
	public static final int XML_TEXT_TYPE = 4; // [4,<el>]
	public static final int FOR_TYPE = 5; // [5,[...],<items el>,'varName']/
	public static final int ELSE_TYPE = 6; // [6,[...],<test el>] //<test el>
	// 可为null
	public static final int ADD_ON_TYPE = 7; // [7,[...],<add on
	// el>,'<addon-class>']
	public static final int VAR_TYPE = 8; // [8,<value el>,'name']
	public static final int CAPTRUE_TYPE = 9; // [9,[...],'var']

	public static final String FOR_KEY = "for";
	protected Map<String, Object> gloabls = new HashMap<String, Object>(
			ExpressionFactoryImpl.DEFAULT_GLOBAL_MAP);

	protected ExpressionFactory expressionFactory = new ExpressionFactoryImpl(
			gloabls);

	protected Object[] items;// transient＄1�7

	protected Template() {
	}

	public Template(List<Object> list) {
		this.items = this.compile(list);
	}

	public void render(Object context, Writer out) throws IOException {
		Context contextMap;
		if (context == null) {
			contextMap = new Context(this, gloabls);
		} else {
			if(context  instanceof Object[]){
				Object[] values = new Object[((Object[])context).length+1];
				System.arraycopy(context, 0, values, 1, values.length-1);
				values[0] = gloabls;
				contextMap = new Context(this, values);
			}else{
				contextMap = new Context(this, gloabls, context);
			}
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
		ArrayList<Object> result = new ArrayList<Object>();
		for (int i = datas.size() - 1; i >= 0; i--) {
			final Object item = datas.get(i);
			if (item instanceof List) {
				final List data = (List) item;
				final Object[] cmd = data.toArray();
				final int type = ((Number) cmd[0]).intValue();
				cmd[0] = type;

				switch (type) {
				case ADD_ON_TYPE:
					compilePlugin(cmd, result);
					break;// continue for
				case XML_ATTRIBUTE_TYPE:
					if (cmd[2] != null) {
						cmd[2] = " " + cmd[2] + "=\"";
					}
				case VAR_TYPE:
				case XML_TEXT_TYPE:
				case EL_TYPE:
					cmd[1] = createExpression(cmd[1]);
					break;
				case IF_TYPE:
				case FOR_TYPE:
				case ELSE_TYPE:
					if (cmd[2] != null) {
						cmd[2] = createExpression(cmd[2]);
					}
				case CAPTRUE_TYPE:
					// children
					cmd[1] = compile((List) cmd[1]);
					break;
				}
				result.add(cmd);
			} else {
				result.add(item);
			}
		}
		return result.toArray();
	}

	@SuppressWarnings("unchecked")
	protected void compilePlugin(final Object[] cmd, List<Object> result) {
		try {
			Object[] children = compile((List<Object>) cmd[1]);
			Expression el = createExpression(cmd[2]);
			Class<Plugin> addonType = (Class<Plugin>) Class.forName((String)cmd[3]);
			Plugin addon = addonType.newInstance();
			ReflectUtil.setValues(addon, (Map) el.evaluate(null));
			addon.initialize(children);
			cmd[3] = addon;
		} catch (Exception e) {
			log.error("装载扩展失败", e);
		}
	}

	protected void renderList(final Context context, final Object[] children,
			final Writer out) {
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
					case IF_TYPE:// ":if":
						processIf(context, data, out);
						break;
					case ELSE_TYPE:// ":else-if":":else":
						processElse(context, data, out);
						break;
					case FOR_TYPE:// ":for":
						processFor(context, data, out);
						break;
					case XML_TEXT_TYPE:// ":el":
						processExpression(context, data, out, true);
						break;
					case XML_ATTRIBUTE_TYPE:// ":attribute":
						processAttribute(context, data, out);
						break;
					case BREAK_TYPE://
						prossesBreak(data);
					case ADD_ON_TYPE://
						prossesPlugin(context, data, out);
						break;
					case VAR_TYPE:// ":set"://var
						processVar(context, data);
						break;
					case CAPTRUE_TYPE:// ":set"://var
						processCaptrue(context, data);
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
	private void prossesPlugin(Context context, Object[] data, Writer out)
			throws Exception {
		Plugin addon = (Plugin)data[3];
		addon.execute(context,out);
	}

	protected void processExpression(Context context, Object[] data,
			Writer out, boolean encodeXML) throws IOException {
		Object value = ((Expression) data[1]).evaluate(context);
		if (encodeXML && value != null) {
			printXMLText(String.valueOf(value), out);
		} else {
			out.write(String.valueOf(value));
		}
	}

	protected void processIf(Context context, Object[] data, Writer out) {
		Boolean test = Boolean.TRUE;
		try {
			if (toBoolean(((Expression) data[2]).evaluate(context))) {
				renderList(context, (Object[]) data[1], out);
			} else {
				test = Boolean.FALSE;
			}
		} finally {
			context.ifStatus = test;
		}

	}

	protected void processElse(Context context, Object[] data, Writer out) {
		if (!context.ifStatus) {
			Boolean test = Boolean.TRUE;
			try {
				if (data[2] == null
						|| toBoolean(((Expression) data[2]).evaluate(context))) {// if
					renderList(context, (Object[]) data[1], out);
				} else {
					test = Boolean.FALSE;
				}
			} finally {
				context.ifStatus = test;
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void processFor(Context context, Object[] data, Writer out) {
		final Object[] children = (Object[]) data[1];
		final String varName = (String) data[3];
		final ForStatus preiousStatus = (ForStatus) context.get(FOR_KEY);
		int len = 0;
		Object list = ((Expression) data[2]).evaluate(context);
		try {// hack return 代替ifelse，减少一些判断

			if (list instanceof Map) {
				list = ((Map) list).entrySet();
			}
			if (list instanceof Collection) {
				Collection<Object> items = (Collection<Object>) list;
				len = items.size();
				ForStatus forStatus = new ForStatus(len);
				context.put(FOR_KEY, forStatus);
				for (Object item : items) {
					forStatus.index++;
					context.put(varName, item);
					renderList(context, children, out);
				}
			} else {
				if (list instanceof Number) {// 算是比较少见吧
					len = ((Number) list).intValue();
					list = null;
				} else {
					if (list instanceof CharSequence) {
						list = ((CharSequence) list).toString().toCharArray();
					}
					if (list.getClass().isArray()) {// list!=null && 让他抛吧，外面有检查
						len = Array.getLength(list);
					}
				}
				ForStatus forStatus = new ForStatus(len);
				context.put(FOR_KEY, forStatus);
				while (++forStatus.index < len) {
					if (list != null) {
						context.put(varName, Array.get(list, forStatus.index));
					}
					renderList(context, children, out);
				}
			}
		} finally {
			// context.put("for", preiousStatus);
			context.put(FOR_KEY, preiousStatus);// for key
			context.ifStatus = len > 0;// if key
		}
	}

	protected void processVar(Context context, Object[] data) {
		context.put(data[2], ((Expression) data[1]).evaluate(context));
	}

	protected void processCaptrue(Context context, Object[] data) {
		StringWriter buf = new StringWriter();
		renderList(context, (Object[]) data[1], buf);
		context.put(data[2], buf.toString());
	}

	protected void processAttribute(Context context, Object[] data, Writer out)
			throws IOException {
		Object result = ((Expression) data[1]).evaluate(context);
		if (data[2] == null) {
			printXMLAttribute(String.valueOf(result), out, false);
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
		return expressionFactory.create(elo);
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
					break;
				}
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

		private ForStatus(int end) {
			this.lastIndex = end - 1;
		}

		public int getIndex() {
			return index;
		}

		public int getLastIndex() {
			return lastIndex;
		}

	}
}
