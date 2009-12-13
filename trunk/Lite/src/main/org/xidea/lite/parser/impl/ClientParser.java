package org.xidea.lite.parser.impl;

import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.el.Expression;
import org.xidea.lite.parser.NodeParser;
import org.xidea.lite.parser.ParseChain;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.TextParser;

public class ClientParser extends ELParser implements NodeParser<Element>,
		TextParser {
	static Pattern SCRIPT_END_PATTERN = Pattern.compile("</script>",
			Pattern.CASE_INSENSITIVE);

	public ClientParser() {
		super("client", true);
	}

	public Expression create(Object el) {
		throw new UnsupportedOperationException();
	}

	public Object parse(String expression) {
		return expression;
	}

	public int parse(final String text, final int p$, ParseContext context) {
		int p1 = text.indexOf('{', p$);
		int p2 = text.indexOf('}', p1);
		String id = text.substring(p1 + 1, p2).trim();

		ClientEnd ce = new ClientEnd();

		parse(id, text.substring(p2 + 1),  context,ce);
		return p2 + 1 + ce.end;
	}

	public void parse(String id, final String text,
			ParseContext context) {
		parse(id, text, context, null);
	}
	private void parse(String id, final String text,
			ParseContext context,TextParser ce) {
		JSProxy proxy = JSProxy.newProxy();
		ParseContext clientContext = new ParseContextImpl(context, proxy
				.createJSTranslator(id));
		if(ce!=null){
			clientContext.addTextParser(ce);
		}
		clientContext.parse(text);
		compileJS(proxy, context, clientContext, true);
	}

	private class ClientEnd extends ELParser {
		private int end;

		protected ClientEnd() {
			super("end", false);
		}

		public int findStart(String text, int start, int other$start) {
			return super.findStart(text, start, other$start);
		}

		public int parse(String text, int p$, ParseContext context) {
			int depth = context.getDepth();
			if (depth == 0) {
				end = p$ + 4;
				return text.length();
			} else {
				context.appendEnd();
			}
			return p$ + 4;
		}

		public int getPriority() {
			return 1000;
		}

		public String toString() {
			return "Client End EL:";
		}
	};

	public void parse(Element el, ParseContext context, ParseChain chain) {
		if ("client".equals(el.getLocalName())
				&& ParseUtil.isCoreNS(el.getPrefix(), el.getNamespaceURI())) {
			parse(el, context);
		} else {
			chain.process(el);
		}
	}

	void parse(Element el, ParseContext context) {
		Node next = el.getFirstChild();
		if (next != null) {
			// new Java6JSBuilder();
			String id = ParseUtil.getAttributeOrNull(el, "id", "name");
			JSProxy proxy = JSProxy.newProxy();
			ParseContext clientContext = new ParseContextImpl(context, proxy
					.createJSTranslator(id));
			// 前端直接压缩吧？反正保留那些空白也没有调试价值
			do {
				clientContext.parse(next);
			} while ((next = next.getNextSibling()) != null);
			//System.out.println(clientContext.toList());
			compileJS(proxy, context, clientContext, needScript(el));

		}
	}

	private void compileJS(JSProxy proxy, ParseContext context,
			ParseContext clientContext, boolean needScript) {
		String js = clientContext.toCode();
		if (clientContext.isCompress()) {
			js = proxy.compress(js);
		}
		js = SCRIPT_END_PATTERN.matcher(js).replaceAll("<\\\\/script>");
		if (needScript) {
			context.append("<!--//--><script>//<![CDATA[\n" + js
					+ "//]]></script>\n");
		} else {
			context.append("//<![CDATA[\n" + js + "//]]>\n");
		}
	}

	private boolean needScript(Element el) {
		return true;
	}

}
