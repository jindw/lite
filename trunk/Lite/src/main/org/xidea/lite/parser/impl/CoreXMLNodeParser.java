package org.xidea.lite.parser.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.DefinePlugin;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.NodeParser;
import org.xml.sax.SAXException;

public class CoreXMLNodeParser implements NodeParser<Node> {
	private static Log log = LogFactory.getLog(CoreXMLNodeParser.class);
	private static Object FIRST_NODE = new Object();
	private static ClientParser clientParser = new ClientParser();
	private static ExtensionParser extensionParser = new ExtensionParser();

	public void parse(Node node, ParseContext context, ParseChain chain) {
		if (node instanceof Element) {
			Element el = (Element) node;
			String parent = getExtends(el);
			if (parent.length() > 0) {
				node = (Element) findExtends(el, context);
			}
			String prefix = el.getPrefix();
			String namespaceURI = el.getNamespaceURI();
			if (namespaceURI != null
					&& ParseUtil.isCoreNS(prefix, namespaceURI)) {
				String name = el.getLocalName();
				if ("include".equals(name)) {
					parseIncludeTag(el, context);
				} else if ("client".equals(name)) {
					clientParser.parse(el, context);
				} else if ("block".equals(name) || "group".equals(name)
						|| "context".equals(name)) {
					parseBlockTag(el, context);
				} else if ("json".equals(name)) {
				} else if ("macro".equals(name) || "def".equals(name)) {
					parseMacroTag(el, context);
				} else if ("choose".equals(name)) {
					parseChooseTag(el, context);
				} else if ("elseif".equals(name) || "else-if".equals(name)
						|| "elif".equals(name)) {
					parseElseTag(el, context, true);
				} else if ("else".equals(name)) {
					parseElseTag(el, context, false);
				} else if ("if".equals(name)) {
					parseIfTag(el, context);
				} else if ("out".equals(name)) {
					parseOutTag(el, context);
				} else if ("for".equals(name) || "forEach".equals(name)
						|| "for-each".equals(name)) {
					parseForTag(el, context);
				} else if ("var".equals(name)) {
					parseVarTag(el, context);
				} else if ("comment".equals(name)) {
				} else if ("extension".equals(name) || "extention".equals(name)
						|| "ext".equals(name)) {
					extensionParser.parse(el, context, chain);
				} else {
					log.error("未知核心标记" + name);
					chain.process(el);
				}
			} else {
				chain.process(el);
			}
		} else if (node instanceof Document) {
			boolean isFirst = context.getAttribute(FIRST_NODE) == null;
			if (isFirst) {
				context.setAttribute(FIRST_NODE, true);
				node = findExtends((Document) node, context);
			}
			chain.process(node);
		}
	}

	private Node findExtends(Node root, ParseContext context) {
		try {
			LinkedList<Node> docs = new LinkedList<Node>();
			while (true) {
				docs.addFirst(root);
				Element el = root instanceof Document ? ((Document) root)
						.getDocumentElement() : (Element) root;
				String parent = getExtends(el);
				if (parent.length() > 0) {
					root = loadXML(context, parent);
				} else {
					break;
				}
			}
			for (Node doc : docs) {
				DocumentFragment nodes = context.selectNodes(doc, "//c:block");
				if (nodes.hasChildNodes()) {
					Node child = nodes.getFirstChild();
					do {
						String id = ParseUtil.getAttributeOrNull(
								(Element) child, "id", "name");
						context.setAttribute("#" + id, child);
					} while ((child = child.getNextSibling()) != null);
				}
			}
			String path = (String) context.getAttribute(ParseContext.PATH);
			if (docs.size() == 1) {
				String decoratorPath = context.getDecotatorPage(path);
				if (decoratorPath != null && !decoratorPath.equals(path)) {
					context.setAttribute("#page", root);
					context.setAttribute("#content", root);
					context.setAttribute("#main", root);
					root = context.loadXML(context.createURI(path, null));
				}
			}
			return root;

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private String getExtends(Element el) {
		String parent = el.getAttributeNS(ParseUtil.CORE_URI, "extends");
		return parent;
	}

	/**
	 * <c:def name="test(arg1,arg2)"> .... </c:def>
	 * 
	 * @param el
	 * @param context
	 */
	protected void parseMacroTag(Element el, ParseContext context) {
		String name = el.getAttribute("name");
		String exp = createMacro(name);
		context.appendPlugin(DefinePlugin.class, context.parseEL(exp));

		if (el.hasChildNodes()) {
			ParseUtil.parseChild(el.getFirstChild(), context);
		}

		context.appendEnd();

	}

	static String createMacro(String name) {
		String[] args = name.trim().split("[^\\w]+");
		name = args[0];
		StringBuilder buf = new StringBuilder("{\"name\":\"");
		buf.append(name);
		buf.append("\",params:[");
		for (int i = 1; i < args.length; i++) {
			if (i > 1) {
				buf.append(',');
			}
			buf.append('"');
			buf.append(args[i]);
			buf.append('"');
		}
		buf.append("]}");
		return buf.toString();
	}

	protected void parseIncludeTag(final Element el, ParseContext context) {
		String var = ParseUtil.getAttributeOrNull(el, "var");
		String path = ParseUtil.getAttributeOrNull(el, "path");
		String xpath = ParseUtil.getAttributeOrNull(el, "xpath");
		String xslt = ParseUtil.getAttributeOrNull(el, "xslt");
		String name = ParseUtil.getAttributeOrNull(el, "name");
		Node doc = el.getOwnerDocument();
		final URI parentURI = context.getCurrentURI();
		try {
			if (name != null) {
				Node cachedNode = XMLContextImpl.toDocumentFragment(el, el
						.getChildNodes());
				context.setAttribute("#" + name, cachedNode);
			}
			if (var != null) {
				context.appendCaptrue(var);
				context.parse(el.getChildNodes());
				context.appendEnd();
			}
			if (path != null) {
				doc = loadXML(context, path);
			}

			if (xpath != null) {
				doc = context.selectNodes(doc, xpath);
			}
			if (xslt != null) {
				URI currentURI = context.getCurrentURI();
				Node xsltNode = null;
				if (xslt.startsWith("#")) {
					xsltNode = ((Node) context.getAttribute(xslt));
				} else {
					xsltNode = context.loadXML(context.createURI(xslt,
							currentURI));
				}
				doc = context.transform(doc, xsltNode);
				context.setCurrentURI(currentURI);
			}
			context.parse(doc);
		} catch (Exception e) {
			log.warn(e);
		} finally {
			context.setCurrentURI(parentURI);
		}
	}

	private Node loadXML(ParseContext context, String path)
			throws SAXException, IOException {
		Node doc;
		if (path.startsWith("#")) {
			doc = (Node) context.getAttribute(path);
			if (doc == null) {
				log.error("没找到相关命名节点：" + context.getCurrentURI() + path);
				throw new RuntimeException();
			}

			String uri;
			if (doc instanceof Document) {
				uri = ((Document) doc).getDocumentURI();
			} else {
				uri = doc.getOwnerDocument().getDocumentURI();
			}
			if (uri != null) {
				context.setCurrentURI(context.createURI(uri, null));
			}
		} else {
			doc = context.loadXML(context.createURI(path, context
					.getCurrentURI()));
		}
		return doc;
	}

	protected Node parseIfTag(Element el, ParseContext context) {
		Object test = ParseUtil.getAttributeEL(context, el, "test");
		context.appendIf(test);
		ParseUtil.parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseElseTag(Element el, ParseContext context,
			boolean requiredTest) {
		Object test = ParseUtil.getAttributeEL(context, el, "test");
		if (requiredTest) {
			if (test == null) {
				throw new IllegalStateException("不能有多个连续无条件Else");
			}
		}
		context.appendElse(test);
		ParseUtil.parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseElseTag(Element el, ParseContext context) {
		if (((Element) el).hasAttribute("test")) {
			Object test = ParseUtil.getAttributeEL(context, el, "test");
			context.appendElse(test);
		} else {
			context.appendElse(null);
		}
		ParseUtil.parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseChooseTag(final Element el2, ParseContext context) {
		boolean first = true;
		String whenTag = "when";
		String elseTag = "otherwise";
		Node next = el2.getFirstChild();
		while (next != null) {
			if (next instanceof Element) {
				// ignore namespace check
				if (whenTag.equals(next.getLocalName())) {
					if (first) {
						first = false;
						parseIfTag((Element) next, context);
					} else {
						parseElseTag((Element) next, context, true);
					}
				} else if (next.getLocalName().equals(elseTag)) {
					parseElseTag((Element) next, context, false);
				} else {
					throw new RuntimeException("choose 只接受when，otherwise 节点");
				}
			}
			next = next.getNextSibling();
		}
		return null;
	}

	protected Node parseForTag(Element el, ParseContext context) {
		Object items = ParseUtil.getAttributeEL(context, el, "list", "items",
				"values", "value");
		String var = ParseUtil.getAttributeOrNull(el, "var", "name", "id");
		String status = ParseUtil.getAttributeOrNull(el, "status");
		context.appendFor(var, items, status);
		ParseUtil.parseChild(el.getFirstChild(), context);
		context.appendEnd();
		return null;
	}

	protected Node parseVarTag(Element el, ParseContext context) {
		String name = ParseUtil.getAttributeOrNull(el, "name", "id");
		String value = ParseUtil.getAttributeOrNull(el, "value");
		if (value == null) {
			context.appendCaptrue(name);
			ParseUtil.parseChild(el.getFirstChild(), context);
			context.appendEnd();
		} else {
			List<Object> temp = context.parseText(value, Template.EL_TYPE);
			if (temp.size() == 1) {
				Object item = temp.get(0);
				if (item instanceof Object[]) {// EL_TYPE
					context.appendVar(name, ((Object[]) item)[1]);
				} else {
					context.appendVar(name, context.parseEL(JSONEncoder
							.encode(item)));
				}
			} else {
				context.appendCaptrue(name);
				context.appendAll(temp);
				context.appendEnd();
			}
		}
		return null;
	}

	protected void parseBlockTag(Element el, ParseContext context) {
		if (el.hasAttribute("output")) {
			String output = el.getAttribute("output");
			if ("false".equals(output)) {
				return;
			}
		}
		String key = ParseUtil.getAttributeOrNull(el, "id", "name", "key");
		Node n = key == null?null:(Node) context.getAttribute("#" + key);
		ParseUtil.parseChild((n == null ? el : n).getFirstChild(), context);
	}

	protected void parseJSONTag(final Element el, ParseContext context) {
		String var = ParseUtil.getAttributeOrNull(el, "var");
		String file = ParseUtil.getAttributeOrNull(el, "file");
		String encoding = ParseUtil.getAttributeOrNull(el, "encoding",
				"charset");
		String content = ParseUtil.getAttributeOrNull(el, "content");
		if (file != null) {
			try {
				URI uri = context.createURI(file, null);
				InputStream in = context.openStream(uri);
				InputStreamReader reader = new InputStreamReader(in,
						encoding == null ? "utf-8" : encoding);
				StringBuilder sbuf = new StringBuilder();
				char[] cbuf = new char[1024];
				int c;
				while ((c = reader.read(cbuf)) >= 0) {
					sbuf.append(cbuf, 0, c);
				}
				content = sbuf.toString();
			} catch (Exception e) {
				if (log.isWarnEnabled()) {
					log.warn("json文件读取失败", e);
				}
			}
		}
		if (content == null) {
			// Node next = node.getFirstChild();
			// context.append(new Object[] { VAR_TYPE, var, null });
			// if (next != null) {
			// do {
			// this.parser.parseNode(next, context);
			// } while ((next = next.getNextSibling()) != null);
			// }
			// context.appendEnd();
			content = el.getTextContent();
		}
		context.appendVar(var, context.parseEL(content));
	}

	protected void parseOutTag(Element el, ParseContext context) {
		String value = ParseUtil.getAttributeOrNull(el, "value");
		context.appendAll(context.parseText(value, Template.EL_TYPE));
	}
}
