package org.xidea.lite;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
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
	public static final int VAR_TYPE = 1;// [1,'value','name']
	public static final int IF_TYPE = 2;// [2,[...],'test']
	public static final int ELSE_TYPE = 3;// [3,[...],'test']//test opt?
	public static final int FOR_TYPE = 4;// [4,[...],'var','items','status']//status
	public static final int BREAK_TYPE = 5;// [1,depth]

	public static final int EL_XML_TEXT_TYPE = 6;// [6,'el']
	public static final int ATTRIBUTE_TYPE = 7;// [7,'value','name']
	public static final int CAPTRUE_TYPE = 8;// [1,[...],'var']
	public static final int ADD_ONS_TYPE =9;// [1,[...],'var']

	public static final String FOR_KEY = "for";
	public static final String IF_KEY = "if";
	private Map<String, Object> gloabls = new HashMap<String, Object>(ExpressionFactoryImpl.DEFAULT_GLOBAL_MAP);

	private ExpressionFactory expressionFactory = new ExpressionFactoryImpl(gloabls);

	protected Object[] items;// transient＄1�7

	protected Template() {
	}

	public Template(List<Object> list) {
		this.items = this.compile(list);
	}

	@SuppressWarnings("unchecked")
	public void render(Object context,
			Writer out) throws IOException {
		Map<? extends Object, ? extends Object> contextMap;
		if(context instanceof Map){
			contextMap = (Map<? extends Object, ? extends Object>) context;
		}else if(context == null){
			contextMap = new HashMap<Object, Object>();
		}else{
			contextMap = ReflectUtil.map(context);
		}
		renderList(contextMap, items, out);
	}

	@SuppressWarnings("unchecked")
	protected void renderList(final Map context, final Object[] children,
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
					case EL_XML_TEXT_TYPE:// ":el":
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
					case ATTRIBUTE_TYPE:// ":attribute":
						processAttribute(context, data, out);
						break;
					case BREAK_TYPE://
						prossesBreak(data);
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

	protected void reverse(Object[] result) {
		int begin = 0;
		int end = result.length - 1;
		while (begin < end) {
			Object item = result[begin];
			result[begin] = result[end];
			result[end--] = item;
			begin++;
		}
	}

	/**
	 * 编译模板数据,递归将元List数据转换为直接的数组，并编译el
	 * 
	 * @internal
	 */
	@SuppressWarnings("unchecked")
	protected Object[] compile(List<Object> datas) {
		Object[] result = datas.toArray();// ;
		reverse(result);
		for (int i = 0; i < result.length; i++) {
			final Object item = result[i];
			if (item instanceof List) {
				final List data = (List) item;
				final Object[] cmd = data.toArray();
				final int type =((Number) cmd[0]).intValue();
				cmd[0] = type;
				// children
				switch (type) {
				case ADD_ONS_TYPE:
					this.compileAddOns(createExpression(cmd[1]));
					break;
				case CAPTRUE_TYPE:
				case IF_TYPE:
				case ELSE_TYPE:
				case FOR_TYPE:
					// case IF_STRING_IN_TYPE:
					cmd[1] = compile((List) cmd[1]);
					break;
				}
				switch (type) {
				case ATTRIBUTE_TYPE:
					if (cmd[2] != null) {
						cmd[2] = " " + cmd[2] + "=\"";
					}
				case EL_XML_TEXT_TYPE:
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
				result[i] = cmd;
			}
		}
		return result;
	}

	private void compileAddOns(Expression createExpression) {
		@SuppressWarnings("unchecked")
		Map<String, String> addOnMap = (Map<String, String>) createExpression.evaluate(null);
		for(Map.Entry<String, String> entry : addOnMap.entrySet()){
			String key = entry.getKey();
			try {
				Object value = Class.forName(entry.getValue()).newInstance();
				gloabls.put(key, value);
			} catch (Exception e) {
				log.error("无法装载扩展："+entry.getValue(),e);
			}
		}
	}

	protected Expression createExpression(Object elo) {
		return expressionFactory.createEL(elo);
	}

	@SuppressWarnings("unchecked")
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
		if(data[2] == null){
			printXMLAttribute(String.valueOf(result), out, true);
		}else if (result != null) {
			out.write((String) data[2]);// prefix
			printXMLAttribute(String.valueOf(result), out, false);
			out.write('"');
		}

	}

	protected void prossesBreak(Object[] data) {
		throw new Break(((Number) data[1]).intValue());
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
