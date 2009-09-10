package org.xidea.lite.parser.impl;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.BuildInAdvice;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.ResultContext;
import org.xidea.lite.parser.ResultItem;
import org.xidea.lite.parser.ResultTranslator;

/**
 * 接口函数不能直接相互调用，用context对象！！！
 * @author jindw
 */
public class ResultContextImpl implements ResultContext {
	private HashMap<String, String> typeIdMap = new HashMap<String, String>();
//	private HashMap<Object, String> objectIdMap = new HashMap<Object, String>();
	private int inc = 0;

	private final ArrayList<Object> result = new ArrayList<Object>();
	private ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();
	private ResultTranslator translator;
	private ParseContext context;

	public ResultContextImpl(ParseContext context) {
		this.context = context;
	}

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

	public void setResultTranslator(ResultTranslator translator) {
		this.translator = translator;
	}
	public Object parseEL(String expression) {
		return expressionFactory.parse(expression);
	}
	private Object requrieEL(Object expression){
		if(expression instanceof String){
			expression = parseEL((String)expression);
		}
		return expression;
	}

	public void append(String text) {
		if (text != null && text.length() > 0) {
			result.add(text);
		}
	}

	public void append(ResultItem item) {
		this.result.add(item);
	}

	public void append(String text, boolean encode, char quteChar) {
		if (encode) {
			text = encodeText(text, quteChar);
		}
		append(text);
	}

	private String encodeText(String text, int quteChar) {
		StringWriter out = new StringWriter();
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
			case '\'':
				if (quteChar == c) {
					out.write("&#39;");
				} else {
					out.write("'");
				}
				break;
			case '"':
				if (quteChar == c) {
					out.write("&#34;");
				} else {
					out.write("\"");
				}
				break;
			default:
				out.write(c);
			}
		}
		return out.toString();
	}

	private void append(Object[] object) {
		result.add(object);
	}

	public final void appendAll(List<Object> items) {
		for (Object text : items) {
			if (text instanceof String) {
				this.append((String) text);
			} else if(text instanceof ResultItem){
				this.append((ResultItem)text);
			} else{
				this.append((Object[]) text);
			}
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

	public final void appendAttribute(String name, Object el) {
		this.append(new Object[] { Template.XML_ATTRIBUTE_TYPE, requrieEL(el), name });

	}

	public final void appendIf(Object testEL) {
		this.append(new Object[] { Template.IF_TYPE, requrieEL(testEL) });
	}

	public final void appendElse(Object testEL) {
		this.clearPreviousText();
		if (this.getType(this.result.size() - 1) != -1) {
			this.appendEnd();
		}
		this.append(new Object[] { Template.ELSE_TYPE, requrieEL(testEL) });
	}

	public final void appendEnd() {
		this.result.add(END_INSTRUCTION);
	}

	public final void appendVar(String name, Object valueEL) {
		this.append(new Object[] { Template.VAR_TYPE, requrieEL(valueEL), name });
	}

	public final void appendCaptrue(String varName) {
		this.append(new Object[] { Template.CAPTRUE_TYPE, varName });

	}

	public final void appendFor(String var, Object itemsEL, String status) {
		itemsEL = requrieEL(itemsEL);
		this.append(new Object[] { Template.FOR_TYPE, itemsEL, var });
		if (status != null && status.length() > 0) {
			this.appendVar(status, this.parseEL("for"));
		}
	}

	public final void appendEL(Object el) {
		el = requrieEL(el);
		this.append(new Object[] { Template.EL_TYPE, el });

	}

	public final void appendXmlText(Object el) {
		this.append(new Object[] { Template.XML_TEXT_TYPE, el });
	}

	public final void appendAdvice(Class<? extends Object> clazz, Object el) {
		this.append(new Object[] { Template.ADD_ON_TYPE, requrieEL(el), clazz.getName() });
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
		return optimizeResult(pops);
	}

	@SuppressWarnings("unchecked")
	public List<Object> toList() {
		appendGlobalsAddOn();

		List<Object> result2 = optimizeResult(this.result);
		ArrayList<ArrayList<Object>> stack = new ArrayList<ArrayList<Object>>();
		ArrayList<Object> current = new ArrayList<Object>();
		stack.add(current);
		int stackTop = 0;
		for (Object item : result2) {
			if (item instanceof String) {
				// System.out.println(item);
				current.add(item);
			} else if(item instanceof ResultItem){
				current.addAll(((ResultItem)item).compile());
			} else {
				Object[] cmd = (Object[]) item;

				// System.out.println(Arrays.asList(cmd));
				if (cmd.length == 0) {
					ArrayList<Object> children = stack.remove(stackTop--);
					current = stack.get(stackTop);
					((ArrayList) current.get(current.size() - 1)).set(1,
							children);
				} else {
					int type = (Integer) cmd[0];
					ArrayList<Object> cmd2 = new ArrayList<Object>(
							cmd.length + 1);
					cmd2.add(cmd[0]);
					current.add(cmd2);
					switch (type) {
					case Template.CAPTRUE_TYPE:
					case Template.IF_TYPE:
					case Template.ELSE_TYPE:
					case Template.FOR_TYPE:
					case Template.ADD_ON_TYPE:
						// case IF_STRING_IN_TYPE:
						cmd2.add(null);
						stackTop++;
						stack.add(current = new ArrayList<Object>());
					}
					for (int i = 1; i < cmd.length; i++) {
						cmd2.add(cmd[i]);
					}

				}
			}
		}
		return current;
	}

	@SuppressWarnings("unchecked")
	private void appendGlobalsAddOn() {
		appendVarAdvice(typeIdMap, "type");
		this.typeIdMap = null;
//		appendVarAdvice(objectIdMap, "value");
//		this.objectIdMap = null;
	}

	@SuppressWarnings("unchecked")
	private void appendVarAdvice(Map valueIdMap, String valueKey) {
		if (valueIdMap != null && !valueIdMap.isEmpty()) {
			HashMap<String, Object> attributeMap = new HashMap<String, Object>();
			for (Object value : valueIdMap.keySet()) {
				attributeMap.put("name", valueIdMap.get(value));
				attributeMap.put(valueKey, value);
				Object valueEL = this.parseEL(JSONEncoder.encode(attributeMap));
				this.appendAdvice(BuildInAdvice.class, valueEL);
				this.appendEnd();
			}
		}
	}
//
	public String addGlobalObject(Class<? extends Object> class1, String key) {
		String name = class1.getName();
		return addGlobalObject(typeIdMap, name, key);
	}
//
//	public String addGlobalObject(Object object, String key) {
//		return addGlobalObject(objectIdMap, object, key);
//	}
//
	@SuppressWarnings("unchecked")
	private String addGlobalObject(Map objectIdMap, Object object, String key) {
		String id = (String) (key == null ? objectIdMap.get(object) : key);
		if (id == null) {
			id = allocateId();
			objectIdMap.put(object, id);
		}
		return id;
	}

	public String allocateId() {
		String id;
		id = "__" + inc++ + "__";
		return id;
	}

	protected List<Object> optimizeResult(List<Object> result) {
		ArrayList<Object> optimizeResult = new ArrayList<Object>(result.size());
		StringBuilder buf = new StringBuilder();
		for (Object item : result) {
			if (item instanceof String) {
				buf.append(item);
			} else {
				if (buf.length() > 0) {
					optimizeResult.add(buf.toString());
					buf.setLength(0);
				}
				optimizeResult.add(item);
			}
		}
		if (buf.length() > 0) {
			optimizeResult.add(buf.toString());
		}
		return optimizeResult;
	}

	public String toCode() {
		if(translator==null){
			return JSONEncoder.encode(context.toList());
		}else{
			return translator.translate(context);
		}
	}

	public int findBeginType() {
		int begin = findBegin();
		if (begin >= 0) {
			return this.getType(begin);
		}
		return -3;// no begin
	}

	public int findBegin() {
		int depth = 0;
		int i = this.result.size();
		while (i-- > 0) {
			switch (getType(i)) {
			case Template.CAPTRUE_TYPE:
			case Template.IF_TYPE:
			case Template.ELSE_TYPE:
			case Template.FOR_TYPE:
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
			case Template.CAPTRUE_TYPE:
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


}
