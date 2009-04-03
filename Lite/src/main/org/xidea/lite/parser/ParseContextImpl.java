package org.xidea.lite.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionFactoryImpl;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.BuildInAdvice;
import org.xidea.lite.Template;

public class ParseContextImpl implements ParseContext {
	private static final long serialVersionUID = 1L;
	private ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

	private HashMap<Object, Object> attributeMap = new HashMap<Object, Object>();
	private URL currentURL;
	private ArrayList<Object> result = new ArrayList<Object>();
	private HashSet<URL> resources = new HashSet<URL>();
	private HashMap<String, String> typeIdMap = new HashMap<String, String>();
	private HashMap<Object, String> objectIdMap = new HashMap<Object, String>();
	private int depth = -1;
	private int inc = 0;
	private boolean reserveSpace;
	private boolean format = false;
	private URL base;

	public ParseContextImpl(URL base) {
		this.base = base;
	}

	public int getDepth() {
		return depth;
	}

	public void setDepth(int depth) {
		this.depth = depth;
	}

	public boolean isFormat() {
		return format;
	}

	public void setFormat(boolean format) {
		this.format = format;
	}

	public boolean isReserveSpace() {
		return reserveSpace;
	}

	public void setReserveSpace(boolean keepSpace) {
		this.reserveSpace = keepSpace;
	}

	public Object optimizeEL(String expression) {
		return expressionFactory.parse(expression);
	}

	public void setAttribute(Object key, Object value) {
		this.attributeMap.put(key, value);
	}

	public Object getAttribute(Object key) {
		return this.attributeMap.get(key);
	}

	public URL getCurrentURL() {
		return currentURL;
	}

	public Set<URL> getResources() {
		return resources;
	}

	public void addResource(URL resource) {
		resources.add(resource);
	}

	public void setCurrentURL(URL currentURL) {
		if (currentURL != null) {
			resources.add(currentURL);
		}
		this.currentURL = currentURL;
	}

	public void append(String text) {
		if (text != null && text.length() > 0) {
			result.add(text);
		}
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

	public int mark() {
		return result.size();
	}

	public List<Object> reset(int mark) {
		int end = result.size();
		List<Object> pops = new ArrayList<Object>(end - mark);
		int i = mark;
		for (; i < end; i++) {
			pops.add(result.get(i));
		}
		while (i-- > mark) {
			result.remove(i);
		}
		return pops;
	}

	public void appendIndent() {
		if (this.format && !this.reserveSpace) {
			int pos = result.size() - 1;
			int depth = this.depth;
			if (depth > 0 && pos > 0) {
				char[] data = new char[depth];
				while (depth-- > 0) {
					data[depth] = '\t';
				}
				result.add("\r\n" + new String(data));
			}

		}
	}

	public void appendAll(List<Object> items) {
		for (Object text : items) {
			if (text instanceof String) {
				this.append((String) text);
			} else {

				this.append((Object[]) text);
			}

		}
	}

	public void removeLastEnd() {
		int i = result.size();
		while (i-- > 0) {
			Object item = result.get(i);
			result.remove(i);
			if (item instanceof Object[]) {
				if (((Object[]) item).length == 0) {
					break;
				}
			}
		}
	}

	@SuppressWarnings("unchecked")
	public List<Object> toResultTree() {
		List<Object> result2 = getResult();
		ArrayList<ArrayList<Object>> stack = new ArrayList<ArrayList<Object>>();
		ArrayList<Object> current = new ArrayList<Object>();
		stack.add(current);
		int stackTop = 0;
		for (Object item : result2) {
			if (item instanceof String) {
				// System.out.println(item);
				current.add(item);
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
					if (type == Template.ELSE_TYPE) {
						ArrayList<Object> children = stack.remove(stackTop--);
						current = stack.get(stackTop);
						((ArrayList) current.get(current.size() - 1)).set(1,
								children);
					}

					ArrayList<Object> cmd2 = new ArrayList<Object>(
							cmd.length + 1);
					cmd2.add(cmd[0]);
					current.add(cmd2);
					switch (type) {
					case Template.CAPTRUE_TYPE:
					case Template.IF_TYPE:
					case Template.ELSE_TYPE:
					case Template.FOR_TYPE:
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

		assert (stackTop == 0);

		HashMap<String, Object> attributeMap = new HashMap<String, Object>();
		if (!this.typeIdMap.isEmpty()) {
			Map instanceMap = toIdObject(typeIdMap);
			attributeMap.put(BuildInAdvice.INSTANCE_MAP, instanceMap);
		}
		if (!this.objectIdMap.isEmpty()) {
			Map instanceMap = toIdObject(objectIdMap);
			attributeMap.put(BuildInAdvice.OBJECT_MAP, instanceMap);
		}
		if (!attributeMap.isEmpty()) {
			current.add(Arrays.asList(new Object[] { Template.ADD_ON_TYPE,
					new ArrayList<Object>(), JSONEncoder.encode(attributeMap),
					BuildInAdvice.class.getName() }));
		}
		return current;
	}

	@SuppressWarnings("unchecked")
	private Map toIdObject(Map objectIdMap) {
		HashMap instanceMap = new HashMap<String, String>();
		for (Object key : objectIdMap.keySet()) {
			instanceMap.put(objectIdMap.get(key), key);
		}
		return instanceMap;
	}

	protected List<Object> getResult() {
		ArrayList<Object> result2 = new ArrayList<Object>(result.size());
		StringBuilder buf = new StringBuilder();
		for (Object item : result) {
			if (item instanceof String) {
				buf.append(item);
			} else {
				if (buf.length() > 0) {
					result2.add(buf.toString());
					buf.setLength(0);
				}
				result2.add((Object[]) item);
			}
		}
		if (buf.length() > 0) {
			result2.add(buf.toString());
		}
		return result2;
	}

	public String addGlobalObject(Class<? extends Object> class1, String key) {
		String name = class1.getName();
		return addGlobalObject(typeIdMap, name, key);
	}

	public String addGlobalObject(Object object, String key) {
		return addGlobalObject(objectIdMap, object, key);
	}

	@SuppressWarnings("unchecked")
	private String addGlobalObject(Map objectIdMap, Object object, String key) {
		String id = (String) (key == null ? objectIdMap.get(object) : key);
		if (id == null) {
			id = "__" + inc++ + "__";
			objectIdMap.put(object, id);
		}
		return id;
	}

	public void appendAttribute(String name, Object el) {
		this.append(new Object[] { Template.XML_ATTRIBUTE_TYPE, el, name });

	}

	public void appendIf(Object testEL) {
		this.append(new Object[] { Template.IF_TYPE, testEL });
	}

	public void appendElse(Object testEL) {
		this.append(new Object[] { Template.ELSE_TYPE, testEL });
	}

	public void appendEnd() {
		this.result.add(END_INSTRUCTION);
	}

	public void appendVar(String name, Object valueEL) {
		this.append(new Object[] { Template.VAR_TYPE, valueEL, name });
	}

	public void appendCaptrue(String varName) {
		this.append(new Object[] { Template.CAPTRUE_TYPE, varName });

	}

	public void appendFor(String var, Object itemsEL, String status) {
		this.append(new Object[] { Template.FOR_TYPE, itemsEL, var, status });
	}

	public void appendEL(Object el) {
		this.append(new Object[] { Template.EL_TYPE, el });

	}

	public void appendXmlText(Object el) {
		this.append(new Object[] { Template.XML_TEXT_TYPE, el });
	}

	public URL createURL(URL parentURL, String path) {
		try {
			if (this.base == null) {
				return new URL(parentURL, path);
			}
			if (parentURL == null) {
				if (path.startsWith("/")) {
					path = path.substring(1);
				}
				return new URL(this.base, path);
			}
			// base !=null && parentURL != null
			if (path.startsWith("/")
					&& parentURL.toExternalForm().startsWith(
							this.base.toExternalForm())) {
				return new URL(this.base, path.substring(1));
			}
			return new URL(parentURL, path);

		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public InputStream getInputStream(URL url) {
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}