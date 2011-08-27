package org.xidea.lite;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionInfo;
import org.xidea.el.ValueStack;
import org.xidea.el.fn.ECMA262Impl;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ReflectUtil;

public class Template {
	private static Log log = LogFactory.getLog(Template.class);
	// 默认值为utf-8
	public static final String FEATURE_ENCODING = "http://www.xidea.org/lite/features/output-encoding";
	// 默认值为 text/html
	public static final String FEATURE_MIME_TYPE = "http://www.xidea.org/lite/features/output-mime-type";
	
	public static final int EL_TYPE = 0; // [0,<el>]
	public static final int IF_TYPE = 1; // [1,[...],<test el>]
	public static final int BREAK_TYPE = 2; // [2,depth]
	public static final int XA_TYPE = 3; // [3,<value el>,'name']
	public static final int XT_TYPE = 4; // [4,<el>]
	public static final int FOR_TYPE = 5; // [5,[...],<items el>,'varName']/
	private static final int FOR_TYPE_NO_STATUS = FOR_TYPE | 0x100;
	private static final int FOR_TYPE_FIRST_STATUS = FOR_TYPE | 0x200;

	public static final int ELSE_TYPE = 6; // [6,[...],<test el>] //<test el>
	// 可为null
	public static final int PLUGIN_TYPE = 7; // [7,[...],<add on
	// el>,'<addon-class>']
	public static final int VAR_TYPE = 8; // [8,<value el>,'name']
	public static final int CAPTURE_TYPE = 9; // [9,[...],'var']

	public static final String FOR_KEY = "for";
	private static final int PLUGIN_POS = 2;

	protected ExpressionFactory expressionFactory = new ExpressionFactoryImpl();

	private int forCount = 0;

	protected Object[] items;// transient＄1�7

	private Map<String, String> featureMap;

	protected Template() {
	}

	public Template(List<Object> list,Map<String, String> featureMap) {
		this.items = this.compile(list);
		this.featureMap = featureMap;
	}

	public String getFeature(String key){
		return featureMap.get(key);
	}
	public void render(Object context, Writer out) throws IOException {
		ValueStack contextMap;
		if (context instanceof ValueStack) {
			contextMap = (ValueStack) context;
		} else {
			contextMap = new Context(context);
		}
		renderList(contextMap, items, out);
	}

	protected Expression createExpression(Object elo) {
		Expression e = expressionFactory.create(elo);
		if (e instanceof ExpressionInfo) {
			if (((ExpressionInfo) e).getVars().contains("for")) {
				forCount++;
			}
		}
		return e;
	}

	/**
	 * 编译模板数据,递归将元List数据转换为直接的数组，并编译el
	 * 
	 * @internal
	 */
	@SuppressWarnings({ "unchecked"})
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
				case PLUGIN_TYPE:
					compilePlugin(cmd, result);
					break;// continue for
				case FOR_TYPE:
					// list表达式应该算外层循环的变量
					cmd[2] = createExpression(cmd[2]);
					int forCount0 = forCount;
					cmd[1] = compile((List) cmd[1]);

					//log.info("\n" + forCount0 + "/" + forCount + cmd[3]);
					if (forCount == forCount0) {
						//log.info("no_status");
						cmd[0] = FOR_TYPE_NO_STATUS;
					} else if (forCount0 == 0) {
						//log.info("first_status");
						cmd[0] = FOR_TYPE_FIRST_STATUS;// may not first(no for
														// status after)
					} else {
						//log.info("full_status");
					}
					forCount = forCount0;
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
				String cs = (String)item;//长字符串切片
				result.add(cs.toCharArray());
			}
		}
		return result.toArray();
	}

	@SuppressWarnings({"unchecked" })
	protected void compilePlugin(final Object[] cmd, List<Object> result) {
		try {
			int forCount0 = forCount;
			Map<String, Object> config = (Map<String, Object>) cmd[2];
			Class<? extends RuntimePlugin> addonType = (Class<? extends RuntimePlugin>) Class
					.forName((String) config.get("class"));
			RuntimePlugin addon = addonType.newInstance();
			ReflectUtil.setValues(addon, config);
			Object[] children = compile((List<Object>) cmd[1]);
			if (addonType == DefinePlugin.class) {// definePlugin no status care
				forCount = forCount0;
			}
			addon.initialize(this, children);
			cmd[PLUGIN_POS] = addon;
		} catch (Exception e) {
			log.error("装载扩展失败", e);
		}
	}

	protected void renderList(final ValueStack context,
			final Object[] children, final Writer out) {
		int index = children.length;
		boolean ifpassed = false;
		// for (final Object item : children) {
		while (index-- > 0) {
			final Object item = children[index];
			try {
				if (item instanceof char[]) {
					out.write((char[]) item);
				} else {
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
					case FOR_TYPE_FIRST_STATUS:
						ifpassed = processFor(context, data, out,
								FOR_TYPE_FIRST_STATUS);
						break;
					case FOR_TYPE_NO_STATUS:
						ifpassed = processFor(context, data, out,
								FOR_TYPE_NO_STATUS);
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

	private void prossesPlugin(ValueStack context, Object[] data, Writer out)
			throws Exception {
		RuntimePlugin addon = (RuntimePlugin) data[PLUGIN_POS];
		addon.execute(context, out);
	}

	protected void processExpression(ValueStack context, Object[] data,
			Writer out, boolean encodeXML) throws IOException {
		Object value = ((Expression) data[1]).evaluate(context);
		if (encodeXML && value != null) {
			printXT(ECMA262Impl.ToString(value), out);
		} else {
			out.write(ECMA262Impl.ToString(value));
		}
	}

	protected boolean processIf(ValueStack context, Object[] data, Writer out) {
		if (toBoolean(((Expression) data[2]).evaluate(context))) {
			renderList(context, (Object[]) data[1], out);
			return true;
		} else {
			return false;
		}
	}

	protected boolean processElse(ValueStack context, Object[] data, Writer out) {
		if (data[2] == null
				|| toBoolean(((Expression) data[2]).evaluate(context))) {// if
			renderList(context, (Object[]) data[1], out);
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
	protected boolean processFor(ValueStack context, Object[] data, Writer out,
			final int type) {
		final Object[] children = (Object[]) data[1];
		final String varName = (String) data[3];
		ForStatus preiousStatus;
		if (type == FOR_TYPE) {
			preiousStatus = (ForStatus) context.get(FOR_KEY);
		} else {
			preiousStatus = null;
		}
		boolean hasElement = false;
		Object list = ((Expression) data[2]).evaluate(context);
		try {// hack return 代替ifelse，减少一些判断
			if (list instanceof Map<?, ?>) {
				list = ((Map<?, ?>) list).keySet();
			}
			if (list instanceof Collection<?>) {
				Collection<Object> items = (Collection<Object>) list;
				hasElement = items.size() > 0;
				if (type == FOR_TYPE_NO_STATUS) {
					for (Object item : items) {
						context.put(varName, item);
						renderList(context, children, out);
					}
				} else {
					ForStatus forStatus = new ForStatus(items.size());
					context.put(FOR_KEY, forStatus);
					for (Object item : items) {
						forStatus.index++;
						context.put(varName, item);
						renderList(context, children, out);
					}
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
				if (type == FOR_TYPE_NO_STATUS) {
					for (int i = 0; i < len; i++) {
						context.put(varName, Array.get(list, i));
						renderList(context, children, out);
					}
				} else {
					ForStatus forStatus = new ForStatus(len);
					context.put(FOR_KEY, forStatus);
					while (++forStatus.index < len) {
						context.put(varName, Array.get(list, forStatus.index));
						renderList(context, children, out);
					}
				}
			}
		} finally {
			// context.put("for", preiousStatus);
			if (type == FOR_TYPE) {
				context.put(FOR_KEY, preiousStatus);// for key
			}
		}
		return hasElement;// if key
	}

	protected void processVar(ValueStack context, Object[] data) {
		context.put(data[2], ((Expression) data[1]).evaluate(context));
	}

	protected void processCaptrue(ValueStack context, Object[] data) {
		StringWriter buf = new StringWriter();
		renderList(context, (Object[]) data[1], buf);
		context.put(data[2], buf.toString());
	}

	protected void processXA(ValueStack context, Object[] data,
			Writer out) throws IOException {
		Object result = ((Expression) data[1]).evaluate(context);
		if (data[2] == null) {
			printXA(ECMA262Impl.ToString(result), out);
		} else if (result != null) {
			out.write((String) data[2]);// prefix
			printXA(ECMA262Impl.ToString(result), out);
			out.write('"');
		}

	}

	protected void prossesBreak(Object[] data) {
		throw new Break(((Number) data[1]).intValue());
	}

	protected void printXA(String text, Writer out)
			throws IOException {
		for (int i = 0,len=text.length(); i < len; i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.write("&lt;");
				break;
			case '"':// 34
				out.write("&#34;");
				break;
			case '&':
				out.write("&amp;");
				break;
			default:
				out.write(c);
			}
		}
	}

	protected void printXT(String text, Writer out) throws IOException {
		for (int i = 0,len=text.length(); i < len; i++) {
			int c = text.charAt(i);
			switch (c) {
			case '<':
				out.write("&lt;");
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