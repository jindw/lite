package org.xidea.lite.impl;

import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xidea.el.Expression;
import org.xidea.jsi.impl.RuntimeSupport;
import org.xidea.lite.parse.NodeParser;
import org.xidea.lite.parse.ParseChain;
import org.xidea.lite.parse.ParseContext;
import org.xidea.lite.parse.TextParser;

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
		ParseContext clientContext = new ParseContextImpl(context);
		if(ce!=null){
			clientContext.addTextParser(ce);
		}
		clientContext.parse(text);
		parseClient(context,id, clientContext, true);
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

			ParseContext clientContext = new ParseContextImpl(context);
			// 前端直接压缩吧？反正保留那些空白也没有调试价值
			do {
				clientContext.parse(next);
			} while ((next = next.getNextSibling()) != null);
			//System.out.println(clientContext.toList());
			parseClient(context,id, clientContext, needScript(el));

		}
	}

	RuntimeSupport proxy = (RuntimeSupport) RuntimeSupport.create();
	private void parseClient(ParseContext context,String id, 
			ParseContext clientContext, boolean needScript) {

		proxy.eval("$import('org.xidea.lite.impl:Translator')");
		Object ts = proxy.eval("new Translator('"+ id + "')");
		String code = (String)proxy.invoke(ts, "translate", clientContext);
		//

//		if (clientContext.isCompress()) {
//			js = proxy.compress(js);
//		}
		code = SCRIPT_END_PATTERN.matcher(code).replaceAll("<\\\\/script>");
		if (needScript) {
			context.append("<!--//--><script>//<![CDATA[\n" + code
					+ "//]]></script>\n");
		} else {
			context.append("//<![CDATA[\n" + code + "//]]>\n");
		}
	}

	private boolean needScript(Element el) {
		return true;
	}

}
