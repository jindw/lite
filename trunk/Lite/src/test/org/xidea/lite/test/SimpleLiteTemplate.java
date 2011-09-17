package org.xidea.lite.test;

import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.fn.ECMA262Impl;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.lite.RuntimePlugin;
import org.xidea.lite.Template;

public class SimpleLiteTemplate implements Template {
	private static Log log = LogFactory.getLog(SimpleLiteTemplate.class);
	public static final String FOR_KEY = "for";

	private static final int PLUGIN_POS = 2;
	public static final String FEATURE_CONTENT_TYPE = "http://www.xidea.org/lite/features/content-type";
	public static final String FEATURE_ENCODING = "http://www.xidea.org/lite/features/encoding";

	protected ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

	protected Object[] items;// transient

	protected Map<String, String> featureMap;

	protected SimpleLiteTemplate() {
	}

	public SimpleLiteTemplate(List<Object> list, Map<String, String> featureMap) {
		this.items = this.compile(list);
		this.featureMap = featureMap;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xidea.lite.TemplateIF#render(java.lang.Object, java.io.Appendable)
	 */
	public void render(Object context, Appendable out) throws IOException {
		Map<String,Object> contextMap = expressionFactory.wrapAsContext(context);
		render(contextMap, items, out);
	}

	protected Expression createExpression(Object elo) {
		return expressionFactory.create(elo);
	}

	/**
	 * 编译模板数据,递归将元List数据转换为直接的数组，并编译el
	 * 
	 * @internal
	 */
	@SuppressWarnings( { "unchecked" })
	protected Object[] compile(List<Object> datas) {
		// Object[] result = datas.toArray();// ;
		// reverse(result);
		ArrayList<Object> result = new ArrayList<Object>();
		for (Object item: datas) {
			if (item instanceof List) {
				final List data = (List) item;
				final Object[] cmd = data.toArray();
				final int type = ((Number) cmd[0]).intValue();
				cmd[0] = type;
				switch (type) {
				case PLUGIN_TYPE:
					compilePlugin(cmd, result);
					break;// continue for
				case FOR_TYPE:
					// list表达式应该算外层循环的变量
					cmd[2] = createExpression(cmd[2]);
					cmd[1] = compile((List) cmd[1]);
					break;
				case XA_TYPE:
					if (cmd[2] != null) {
						cmd[2] = " " + cmd[2] + "=\"";
					}
				case VAR_TYPE:
				case XT_TYPE:
				case EL_TYPE:
					cmd[1] = createExpression(cmd[1]);
					break;
				// childable
				case IF_TYPE:
				case ELSE_TYPE:// cmd2 mayby null
					if (cmd[2] != null) {
						cmd[2] = createExpression(cmd[2]);
					}
				case CAPTURE_TYPE:
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

	@SuppressWarnings( { "unchecked" })
	protected void compilePlugin(final Object[] cmd, List<Object> result) {
		try {
			Map<String, Object> config = (Map<String, Object>) cmd[2];
			Class<? extends RuntimePlugin> addonType = (Class<? extends RuntimePlugin>) Class
					.forName((String) config.get("class"));
			RuntimePlugin addon = addonType.newInstance();
			ReflectUtil.setValues(addon, config);
			Object[] children = compile((List<Object>) cmd[1]);
			List<Object> list = Arrays.asList(children);
			addon.initialize(this, list.toArray());
			cmd[PLUGIN_POS] = addon;
		} catch (Exception e) {
			log.error("装载扩展失败", e);
		}
	}

	public void render(final Map<String,Object> context,
			final Object[] children, final Appendable out) {
		boolean ifpassed = false;
		for (Object item : children) {
			try {
				if (item instanceof Object[]) {
					final Object[] data = (Object[]) item;
					switch ((Integer) data[0]) {
					case EL_TYPE:// ":el":
						processExpression(context, data, out, false);
						break;
					case IF_TYPE:// ":if":
						ifpassed = processIf(context, data, out);
						break;
					case ELSE_TYPE:// ":else-if":":else":
						if (!ifpassed) {
							ifpassed = processElse(context, data, out);
						}
						break;
					case FOR_TYPE:// ":for":
						ifpassed = processFor(context, data, out, FOR_TYPE);
						break;
					case XT_TYPE:// ":el":
						processExpression(context, data, out, true);
						break;
					case XA_TYPE:// ":attribute":
						processXA(context, data, out);
						break;
					case BREAK_TYPE://
						prossesBreak(data);
					case PLUGIN_TYPE://
						prossesPlugin(context, data, out);
						break;
					case VAR_TYPE:// ":set"://var
						processVar(context, data);
						break;
					case CAPTURE_TYPE:// ":set"://var
						processCaptrue(context, data);
						break;
					}
				} else{
					out.append((String) item);
				}
			} catch (Break e) {
				if (--e.depth > 0) {
					throw e;
				}
			} catch (Exception e) {// 每一个指令都拒绝异常
				if (log.isDebugEnabled()) {
					log.debug(e);
				}
			}
		}
	}

	private void prossesPlugin(Map<String,Object> context, Object[] data, Appendable out)
			throws Exception {
		RuntimePlugin addon = (RuntimePlugin) data[PLUGIN_POS];
		addon.execute(context, out);
	}

	protected void processExpression(Map<String,Object> context, Object[] data,
			Appendable out, boolean encodeXML) throws IOException {
		Object value = ((Expression) data[1]).evaluate(context);
		if (encodeXML && value != null) {
			printXT(ECMA262Impl.ToString(value), out);
		} else {
			out.append(ECMA262Impl.ToString(value));
		}
	}

	protected boolean processIf(Map<String,Object> context, Object[] data, Appendable out) {
		if (toBoolean(((Expression) data[2]).evaluate(context))) {
			render(context, (Object[]) data[1], out);
			return true;
		} else {
			return false;
		}
	}

	protected boolean processElse(Map<String,Object> context, Object[] data, Appendable out) {
		if (data[2] == null
				|| toBoolean(((Expression) data[2]).evaluate(context))) {// if
			render(context, (Object[]) data[1], out);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * @param context
	 * @param data
	 * @param out
	 * @param type
	 *            FOR_TYPE_NO_STATUS 没有for status，不需要任何for状态操作
	 *            FOR_TYPE_FIRST_STATUS 第一个需要for变量的，需要生成状态，但是不需要恢复原有for status
	 *            FOR_TYPE 包含for变量，需要生成状态，唯一一个需要for status 恢复的条件
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected boolean processFor(Map<String,Object> context, Object[] data, Appendable out,
			final int type) {
		final Object[] children = (Object[]) data[1];
		final String varName = (String) data[3];
		ForStatus preiousStatus;
		preiousStatus = (ForStatus) context.get(FOR_KEY);
		boolean hasElement = false;
		Object list = ((Expression) data[2]).evaluate(context);
		try {// hack return 代替ifelse，减少一些判断
			if (list instanceof Map<?, ?>) {
				list = ((Map<?, ?>) list).keySet();
			}
			if (list instanceof Collection<?>) {
				Collection<Object> items = (Collection<Object>) list;
				hasElement = items.size() > 0;
				ForStatus forStatus = new ForStatus(items.size());
				context.put(FOR_KEY, forStatus);
				for (Object item : items) {
					forStatus.index++;
					context.put(varName, item);
					render(context, children, out);
				}
			} else {
				int len;
				if (list instanceof Number) {// 算是比较少见吧
					len = Math.max(((Number) list).intValue(), 0);
					list = new Object[len];
					for (int i = 0; i < len;) {
						((Object[]) list)[i] = ++i;
					}
				} else {
					len = Array.getLength(list);
				}
				hasElement = len > 0;
				ForStatus forStatus = new ForStatus(len);
				context.put(FOR_KEY, forStatus);
				while (++forStatus.index < len) {
					context.put(varName, Array.get(list, forStatus.index));
					render(context, children, out);
				}
			}
		} finally {
			context.put(FOR_KEY, preiousStatus);// for key
		}
		return hasElement;// if key
	}

	protected void processVar(Map<String,Object> context, Object[] data) {
		context.put((String)data[2], ((Expression) data[1]).evaluate(context));
	}

	protected void processCaptrue(Map<String,Object> context, Object[] data) {
		StringWriter buf = new StringWriter();
		render(context, (Object[]) data[1], buf);
		context.put((String)data[2], buf.toString());
	}

	protected void processXA(Map<String,Object> context, Object[] data, Appendable out)
			throws IOException {
		Object result = ((Expression) data[1]).evaluate(context);
		if (data[2] == null) {
			printXA(ECMA262Impl.ToString(result), out);
		} else if (result != null) {
			out.append((String) data[2]);// prefix
			printXA(ECMA262Impl.ToString(result), out);
			out.append('"');
		}

	}

	protected void prossesBreak(Object[] data) {
		throw new Break(((Number) data[1]).intValue());
	}

	protected void printXA(String text, Appendable out) throws IOException {
		for (int i = 0, len = text.length(); i < len; i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.append("&lt;");
				break;
			case '"':// 34
				out.append("&#34;");
				break;
			case '&':
				out.append("&amp;");
				break;
			default:
				out.append((char)c);
			}
		}
	}

	protected void printXT(String text, Appendable out) throws IOException {
		for (int i = 0, len = text.length(); i < len; i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.append("&lt;");
				break;
			case '&':
				out.append("&amp;");
				break;
			default:
				out.append((char)c);
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

	public String getContentType() {
		return this.featureMap.get(SimpleLiteTemplate.FEATURE_CONTENT_TYPE);
	}

	public String getEncoding() {
		return this.featureMap.get(SimpleLiteTemplate.FEATURE_ENCODING);
	}

	public void addVar(String name, Object value) {
		this.expressionFactory.addVar(name,value);
	}

}
class Break extends RuntimeException {
	private static final long serialVersionUID = 1L;
	int depth;

	protected Break(int depth) {
		this.depth = depth;
	}
}

class ForStatus {
	int index = -1;
	int lastIndex;

	ForStatus(int end) {
		this.lastIndex = end - 1;
	}

	public int getIndex() {
		return index;
	}

	public int getLastIndex() {
		return lastIndex;
	}
}