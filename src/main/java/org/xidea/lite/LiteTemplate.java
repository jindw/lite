package org.xidea.lite;

import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionInfo;
import org.xidea.el.fn.ECMA262Impl;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONEncoder;

/**
 * 优化后的LiteTemplate实现 主要优化方向有: 1. StreamWriter 支持 2. 指令列表循环逆转
 * 
 * 简单的实现请参考:
 * 
 * #see org.xidea.lite.test.SimpleLiteTemplate
 * @author jindawei
 * 
 */
public class LiteTemplate implements Template {
	private static Log log = LogFactory.getLog(LiteTemplate.class);
	private static final int FOR_TYPE_NO_STATUS = FOR_TYPE | 0x100;
	private static final int STATIC_BCS_TYPE = -1;
	private static final int FOR_TYPE_FIRST_STATUS = FOR_TYPE | 0x200;
	private static final int PLUGIN_POS = 2;
	
	private static final String FOR_KEY = "for";
	private static final String FEATURE_CONTENT_TYPE = "contentType";//"http://www.xidea.org/lite/features/content-type";
	private static final String FEATURE_ENCODING = "encoding";//"http://www.xidea.org/lite/features/encoding";
	//public static final String FEATURE_I18N = "";"http://www.xidea.org/lite/features/i18n";

	protected ExpressionFactory expressionFactory = new ExpressionFactoryImpl();
	protected Object[] items;// transient
	protected Map<String, String> config;

	private transient  int modulePlugin = 0;
	private transient int forCount = 0;

	private ExecutorService executorService = Executors.newScheduledThreadPool(10);

	protected LiteTemplate() {
	}

	public LiteTemplate(ExecutorService executorService, List<Object> list, Map<String, String> featureMap) {
		this.executorService = executorService;
		this.config = featureMap;
		this.items = this.compile(list);
		if(modulePlugin>0){//append last
			int lastIndex = this.items.length - 1;
			Object item = this.items[lastIndex];
			if(item instanceof Object[]){
				Object[] node = (Object[])item;
				if(((Number)node[0]).intValue() == STATIC_BCS_TYPE){
					item = node[1];
				}
			}
			if(item instanceof String){
				String last = (String)item;
				String prefix = last.replaceFirst("(?:<\\/body>\\s*)?<\\/html>\\s*$","");
				String postfix = last.substring(prefix.length());
				this.items[lastIndex] = translateText(prefix);
				ArrayList<Object> items = new ArrayList<Object>(Arrays.asList(this.items));
				Object[] cmd = new Object[3];
				cmd[0] = PLUGIN_TYPE;
				cmd[PLUGIN_POS] = new ModulePlugin.Appender();
				items.add(cmd);//
				if(postfix.length()>0){
					items.add(translateText(postfix));
				}
				this.items = items.toArray();
			}


		}
	}


	public void addVar(String name, Object value){
		this.expressionFactory.addVar(name, value);
	}
	protected Expression createExpression(Object elo) {
		Expression e = expressionFactory.create(elo);
		if (e instanceof ExpressionInfo) {
			if (((ExpressionInfo) e).getVars().contains("for")) {
				forCount++;
			}
		} else {
			forCount++;
		}
		return e;
	}

	/**
     * internal method
	 * 编译模板数据,递归将元List数据转换为直接的数组，并编译el
     * @param datas 模板中间数组代码
     * @return 编译优化结果
	 *
	 */
	@SuppressWarnings( { "unchecked", "rawtypes" })
	protected Object[] compile(List<Object> datas) {
		// Object[] result = datas.toArray();// ;
		// reverse(result);
		ArrayList<Object[]> result = new ArrayList<Object[]>();
		for (final Object item : datas) {// 逆序很危险.
			if (item instanceof List) {
				final List data = (List) item;
				final Object[] cmd = data.toArray();
				final int type = ((Number) cmd[0]).intValue();
				cmd[0] = type;
				switch (type) {
				case PLUGIN_TYPE:
					if(compilePlugin(cmd, result)){
						continue;// continue is skip
					}else{
						break;
					}
				case FOR_TYPE:
					// list表达式应该算外层循环的变量
					cmd[2] = createExpression(cmd[2]);
					int forCount0 = forCount;
					cmd[1] = compile((List) cmd[1]);

					if (forCount == forCount0) {
						cmd[0] = FOR_TYPE_NO_STATUS;
					} else if (forCount0 == 0) {// for count0 不等价于 父循环的for 命中次数
						cmd[0] = FOR_TYPE;// _FIRST_STATUS;// may not first(no
						// 不能草率决定first_status,first_status 必须完整解析之后才能优化
					} else {
						// log.info("full_status");
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
				result.add(translateText((String) item));
			}
		}
		return result.toArray();
	}

	private Object[] translateText( String item) {
		String cs = item;// 长字符串切片
		try {
            return new Object[]{STATIC_BCS_TYPE,cs,cs.toCharArray(),cs.getBytes(this.getEncoding())};
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
	}

	/**
	 * "org.xidea.lite.ModulePlugin"
	 * "org.xidea.lite.DefinePlugin"
	 * "org.xidea.lite.DatePlugin"
	 * @param cmd 插件代码
	 * @param result 当前结果
	 * @return skip it？  DefinePugin or Process Error (should be skipped and remove from instruction)
	 */
	@SuppressWarnings( { "unchecked" })
	protected boolean compilePlugin(final Object[] cmd, List<Object[]> result) {
		try {
			int forCount0 = forCount;
			Map<String, Object> config = (Map<String, Object>) cmd[2];
			Class<? extends RuntimePlugin> addonType = (Class<? extends RuntimePlugin>) Class
					.forName((String) config.get("class"));
			RuntimePlugin addon = addonType.newInstance();
			ReflectUtil.setValues(addon, config);
			Object[] children = compile((List<Object>) cmd[1]);
			addon.initialize(this, children);
			if (addonType == ModulePlugin.class) {// definePlugin no status care
				((ModulePlugin)addon).setExecutorService(executorService);
				this.modulePlugin++;
				cmd[PLUGIN_POS] = addon;
				return false;
			}else if (addonType == DefinePlugin.class) {// definePlugin no status care
				forCount = forCount0;
				return true;
			}else{
				cmd[PLUGIN_POS] = addon;
				return false;
			}

		} catch (Exception e) {
			log.error("装载扩展失败", e);
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.xidea.lite.TemplateIF#render(java.lang.Object,
	 * java.io.Appendable)
	 */
	public void render(Object context, Appendable out) throws IOException {
		Map<String, Object> contextMap = FutureWaitStack.wrap(expressionFactory.wrapAsContext(context));
		render(contextMap, items, out);
        if(out instanceof Flushable){
            ((Flushable)out).flush();
        }
	}
	public void render(final Map<String,Object> context,
			final Object[] children, final Appendable out) {
		boolean ifpassed = false;
		for (final Object item : children) {
			try {
				final Object[] data = (Object[]) item;
				switch ((Integer) data[0]) {
				case STATIC_BCS_TYPE:
					if(out instanceof Writer){
						((Writer)out).write((char[])data[2]);
					//}else if(out instanceof OutputStream){
					//	((OutputStream)out).write((byte[])data[3]);
					}else {
						/* @see java.lang.StringBuilder#append(String)*/
						out.append((String)data[1]);
					}
					break;
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
			} catch (Break e) {
				if (--e.depth > 0) {
					throw e;
				}
			} catch (Exception e) {// 每一个指令都拒绝异常
				if (log.isInfoEnabled()) {
					log.info("模板渲染异常",e);
				}
				//e.printStackTrace();
			}
		}
	}

	private void prossesPlugin(Map<String,Object> context, Object[] data, Appendable out)
			throws Exception {
		try {
			RuntimePlugin addon = (RuntimePlugin) data[PLUGIN_POS];
			addon.execute(context, out);
		}catch (Exception e){
			System.out.println(JSONEncoder.encode(data));
			e.printStackTrace();;
		}
	}

	protected void processExpression(Map<String,Object> context, Object[] data,
			Appendable out, boolean encodeXML) throws IOException {
		Object value = ((Expression) data[1]).evaluate(context);
		if (encodeXML) {
			printXT(ECMA262Impl.ToString(value), out);
		} else {
			out.append(ECMA262Impl.ToString(value));
		}
	}

	protected boolean processIf(Map<String,Object> context, Object[] data,
			Appendable out) {
		if (toBoolean(((Expression) data[2]).evaluate(context))) {
			render(context, (Object[]) data[1], out);
			return true;
		} else {
			return false;
		}
	}

	protected boolean processElse(Map<String,Object> context, Object[] data,
			Appendable out) {
		if (data[2] == null
				|| toBoolean(((Expression) data[2]).evaluate(context))) {// if
			render(context, (Object[]) data[1], out);
			return true;
		} else {
			return false;
		}
	}

	/*
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
	protected boolean processFor(Map<String,Object> context, Object[] data,
			Appendable out, final int type) {
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
						render(context, children, out);
					}
				} else {// first status and other_status
					ForStatus forStatus = new ForStatus(items.size());
					context.put(FOR_KEY, forStatus);
					for (Object item : items) {
						forStatus.index++;
						context.put(varName, item);
						render(context, children, out);
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
						render(context, children, out);
					}
				} else {
					ForStatus forStatus = new ForStatus(len);
					context.put(FOR_KEY, forStatus);
					while (++forStatus.index < len) {
						context.put(varName, Array.get(list, forStatus.index));
						render(context, children, out);
					}
				}
			}
		} finally {
			if (type == FOR_TYPE) {
				context.put(FOR_KEY, preiousStatus);// for key
			}
		}
		return hasElement;// if key
	}

	protected void processVar(Map<String,Object> context, Object[] data) {
		context.put((String)data[2], ((Expression) data[1]).evaluate(context));
	}

	protected void processCaptrue(Map<String,Object> context, Object[] data) {
		StringBuilder buf = new StringBuilder();
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
				out.append((char) c);
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
				out.append((char) c);
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
		return this.config.get(FEATURE_CONTENT_TYPE);
	}

	public String getEncoding() {
		String encoding  = this.config.get(FEATURE_ENCODING);
		return encoding == null?"utf-8":encoding;
	}
    static class Break extends RuntimeException {
        private static final long serialVersionUID = 1L;
        int depth;

        protected Break(int depth) {
            this.depth = depth;
        }
    }

    static class ForStatus {
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
}

