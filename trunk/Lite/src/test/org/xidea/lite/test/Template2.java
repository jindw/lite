package org.xidea.lite.test;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionFactoryImpl;
import org.xidea.lite.Template;
import org.xidea.lite.parser.CoreXMLNodeParser;

public class Template2 extends Template {
	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);

	public static final int EL_TYPE = 0;// [0,'el']
	public static final int VAR_TYPE = 1;// [1,[...],'var','value']//value
	public static final int IF_TYPE = 2;// [2,[...],'test']
	public static final int ELSE_TYPE = 3;// [3,[...],'test']//test opt?
	public static final int FOR_TYPE = 4;// [4,[...],'var','items','status']//status

	public static final int EL_TYPE_XML_TEXT = 6;// [6,'el']
	public static final int ATTRIBUTE_TYPE = 7;// [6,'value','name']//name opt?
	public static final int IF_STRING_IN_TYPE = 8;// [8,[...],'value','values']

	public static final String FOR_KEY = "for";
	public static final String IF_KEY = "if";

	private ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

	protected Object[] items;//transient＄1�7

	public Template2(List<Object> list) {
		this.items = this.compile(list);
	}

	public void render(Map<? extends Object, ? extends Object> context,
			Writer out) throws IOException {
		renderList(context, items, out);
	}

	@SuppressWarnings("unchecked")
	protected void renderList(final Map context, final Object[] children, final Writer out) {
		int index = children.length;
		while (index-->0) {
			final Object item = children[index];
			try {
				if (item instanceof String) {
					out.write((String) item);
				} else {
					final Object[] data = (Object[]) item;
					switch ((Integer) data[0]) {
					case EL_TYPE:// ":el":
						processExpression(context, data, out, false);
						break;
					case EL_TYPE_XML_TEXT:// ":el":
						processExpression(context, data, out, true);
						break;
					case VAR_TYPE:// ":set"://var
						processVar(context, data);
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
					case ATTRIBUTE_TYPE:// ":attribute":
						processAttribute(context, data, out);
						break;
					case IF_STRING_IN_TYPE://
						renderIfStringIn(context, data, out);
						break;
					}
				}
			} catch (Exception e) {// 每一个指令都拒绝异常
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}
	}

	/**
	 * 编译模板数据,递归将元List数据转换为直接的数组，并编译el
	 * 
	 * @internal
	 */
	@SuppressWarnings("unchecked")
	protected Object[] compile(List<Object> datas) {
		final Object[] result = datas.toArray();//;
		reverse(result);
		for (int i = 0; i < result.length; i++) {
			final Object item = result[i];
			if (item instanceof List) {
				final List data = (List) item;
				final Object[] cmd = data.toArray();
				final int type = (Integer) cmd[0];
				switch (type) {
				case VAR_TYPE:
					if (cmd[3] != null) {
						cmd[3] = createExpression(cmd[3]);
						break;
					}
				case IF_TYPE:
				case ELSE_TYPE:
				case FOR_TYPE:
				case IF_STRING_IN_TYPE:
					cmd[1] = compile((List) cmd[1]);
					break;
				}
				switch (type) {
				case ATTRIBUTE_TYPE:
					if (cmd[2] != null) {
						cmd[2] = " " + cmd[2] + "=\"";
					}
				case EL_TYPE_XML_TEXT:
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
				case IF_STRING_IN_TYPE:
					cmd[2] = createExpression(cmd[2]);
				case FOR_TYPE:
					cmd[3] = createExpression(cmd[3]);
					break;
				}
				result[i] = cmd;
			}
		}
		return result;
	}

	public static class ForStatus {
		int index = -1;
		final int lastIndex;

		public ForStatus(int end) {
			this.lastIndex = end - 1;
		}

		public int getIndex() {
			return index;
		}

		public int getLastIndex() {
			return lastIndex;
		}

	}

	protected Expression createExpression(Object elo) {
		return expressionFactory.createEL( elo);
	}

	@SuppressWarnings("unchecked")
	protected void printXMLAttribute(String text, Map context, Writer out,
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
		if(test == null){
			return false;
		}else if (test instanceof Boolean){
			return (Boolean)test;
		}else if (test instanceof String){
			return ((String)test).length()>0;
		}else if (test instanceof Number){
			return ((Number)test).floatValue() == 0;
		}
		return true;
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
		boolean test;
		try {
			test = toBoolean(((Expression) data[2]).evaluate(context));
			if (test) {
				renderList(context, (Object[]) data[1], out);
			}
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug(e);
			}
			test = true;
		}
		// context[2] = test;//if passed(丄1�7定要放下来，确保覆盖)
		context.put(IF_KEY, test);
	}

	@SuppressWarnings("unchecked")
	protected void renderIfStringIn(Map context, Object[] data, Writer out) {
		Boolean test = Boolean.FALSE;
		try {
			Object key = ((Expression) data[2]).evaluate(context);
			Object value = ((Expression) data[3]).evaluate(context);

			key = String.valueOf(key);
			if (value instanceof Object[]) {
				for (Object item : (Object[]) value) {
					if (item != null && key.equals(String.valueOf(item))) {
						test = Boolean.TRUE;
						break;
					}
				}
			} else if (value instanceof Collection) {
				for (Object item : (Collection<?>) value) {
					if (item != null && key.equals(String.valueOf(item))) {
						test = true;
						break;
					}
				}
			}
			if (test) {
				renderList(context, (Object[]) data[1], out);
			}
		} catch (Exception e) {
			if (log.isDebugEnabled()) {
				log.debug(e);
			}
			test = Boolean.TRUE;
		}

		// context[2] = test;//if passed(丄1�7定要放下来，确保覆盖)
		context.put(IF_KEY, test);

	}

	@SuppressWarnings("unchecked")
	protected void processElse(Map context, Object[] data, Writer out) {
		if (!toBoolean(context.get(IF_KEY))) {
			try {
				if (data[2] == null
						|| toBoolean(((Expression) data[2]).evaluate(context))) {// if
					renderList(context, (Object[]) data[1], out);
					context.put(IF_KEY, Boolean.TRUE);
				}
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
				context.put(IF_KEY, Boolean.TRUE);
			}
		}
	}

	@SuppressWarnings("unchecked")
	protected void processFor(Map context, Object[] data, Writer out) {
		final Object[] children = (Object[]) data[1];
		final String varName = (String) data[2];
		final String statusName = (String) data[4];
		final Object list = ((Expression) data[3]).evaluate(context);
		final List<Object> items;
		// alert(data.constructor)
		if (list instanceof Object[]) {
			items = Arrays.asList(list);
		} else {
			items = (List<Object>) list;
		}
		int len = items.size();
		ForStatus preiousStatus = (ForStatus) context.get(FOR_KEY);
		try {
			ForStatus forStatus = new ForStatus(len);
			context.put(FOR_KEY, forStatus);
			// context.put("for", forStatus);
			// prepareFor(this);
			if (statusName != null) {
				context.put(statusName, forStatus);
			}
			for (Object item : items) {
				forStatus.index++;
				context.put(varName, item);
				renderList(context, children, out);
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
		final String name = (String) data[2];
		if (data[3] == null) {
			StringWriter buf = new StringWriter();
			renderList(context, (Object[]) data[1], buf);
			context.put(name, buf.toString());
		} else {
			context.put(name, ((Expression) data[3]).evaluate(context));
		}
	}

	@SuppressWarnings("unchecked")
	protected void processAttribute(Map context, Object[] data, Writer out)
			throws IOException {
		if (data[2] != null) {
			Object result = ((Expression) data[1]).evaluate(context);
			if (result != null) {
				String value;
				if (result instanceof String) {
					value = (String) result;
					if (((String) result).length() == 0) {
						return;
					}
				} else {
					value = String.valueOf(result);
				}
				out.write((String) data[2]);// prefix
				printXMLAttribute(value, context, out, false);
				out.write('"');
			}
		} else {
			printXMLAttribute(String.valueOf(((Expression) data[1])
					.evaluate(context)), context, out, true);
		}

	}

}
