package org.xidea.lite.parser.impl;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.lite.Template;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseContext;

public class ELParser implements InstructionParser {
	public static InstructionParser EL = new ELParser("", true);
	public static InstructionParser IF = new ELParser("if", true) {
		protected void addEl(ParseContext context, String text) {
			context.appendIf(context.parseEL(text));
		}
	};
	public static InstructionParser FOR = new ELParser("for", true) {
		protected void addEl(ParseContext context, String text) {
			int p = text.indexOf(':');
			context.appendFor(text.substring(0, p).trim(), context.parseEL(text
					.substring(p + 1)), null);
		}
	};
	public static InstructionParser ELSE = new ELParser("else", false) {
		public int parse(ParseContext context, String text, int p$) {
			int begin = text.indexOf('{', p$);
			if (begin > 0) {
				int end = findELEnd(text, begin);
				if (end > 0) {
					String el = text.substring(begin + 1, end);
					context.appendElse(context.parseEL(el));
					return end + 1;
				}
			}
			context.appendElse(null);
			return p$ + 5;
		}
	};
	public static InstructionParser CLIENT = new ELParser("client", true) {
		public int parse(ParseContext context, String text, int p$) {
			int p1 = text.indexOf('{', p$);
			int p2 = text.indexOf('}', p1);
			String id = text.substring(p1 + 1, p2);
			ParseContext clientContext = context.createClientContext(id);
			String subtext = text.substring(p2 + 1);
			clientContext.setAttribute(CLIENT, context);
			clientContext.parseText(subtext, context.getSourceType());
			return text.length();

		}
	};
	public static InstructionParser END = new ELParser("end", false) {
		public int parse(ParseContext context, String text, int p$) {
			ParseContext parentContext = (ParseContext) context
					.getAttribute(CLIENT);
			if (parentContext != null) {
				int depth = context.getDepth();
				if (depth == 0) {
					String js = (String) context.toList().get(0);
					parentContext.append("<script>/*<![CDATA[*/" + js
							+ "/*]]>*/</script>");
					String subtext = text.substring(p$ + 4);
					parentContext.parseText(subtext, parentContext
							.getSourceType());
					return text.length();
				}
			}
			context.appendEnd();
			return p$ + 4;
		}
	};
	public static InstructionParser VAR = new ELParser("var", true) {
		protected void addEl(ParseContext context, String text) {
			int p = text.indexOf('=');
			if (p > 0) {
				context.appendVar(text.substring(0, p).trim(), context
						.parseEL(text.substring(p + 1)));
			} else {
				context.appendCaptrue(text.trim());
			}
		}
	};
	private String fn = "";
	private boolean requireEL;
	private String prefix;
	private int length;

	protected ELParser(String fn, boolean requireEL) {
		this.fn = fn;
		this.prefix = '$' + fn;
		this.requireEL = requireEL;
		this.length = prefix.length();

	}

	public int findStart(ParseContext context, String text, int start) {
		int i;
		while ((i = text.indexOf(this.prefix, start)) >= start) {
			if (i < text.length()) {
				int j = i + length;
				if (j < text.length()
						&& !Character.isJavaIdentifierPart(text.charAt(j))) {
					return i;
				}
				if (j == text.length() && !this.requireEL) {
					return i;
				}

			}
			start = i + 1;
		}
		return -1;
	}

	public int parse(ParseContext context, String text, int p$) {
		int begin = text.indexOf('{', p$);
		if (begin > 0) {
			String fn2 = text.substring(p$ + 1, begin);
			if (fn2.trim().equals(fn)) {
				int end = findELEnd(text, begin);
				if (end > 0) {
					String el = text.substring(begin + 1, end);
					addEl(context, el);
					return end + 1;
				}
			}
		}
		throw new ExpressionSyntaxException(fn + ":" + text.substring(p$));
	}

	protected void addEl(ParseContext context, String text) {
		Object el = context.parseEL(text);
		switch (context.getSourceType()) {
		case Template.EL_TYPE:
			context.appendEL(el);
			break;
		case Template.XML_TEXT_TYPE:
			context.appendXmlText(el);
			break;
		case Template.XML_ATTRIBUTE_TYPE:
			context.appendAttribute(null, el);
			break;
		}
	}

	/**
	 * 
	 * @param text
	 * @param elBegin
	 *            {的位置
	 * @return }的位置
	 */
	protected int findELEnd(String text, int elBegin) {
		int length = text.length();
		if (elBegin >= length) {
			return -1;
		}
		int next = elBegin + 1;
		char stringChar = 0;
		int depth = 0;
		do {
			char c = text.charAt(next);
			switch (c) {
			case '\\':
				next++;
				break;
			case '\'':
			case '"':
				if (stringChar == c) {
					stringChar = 0;
				} else if (stringChar == 0) {
					stringChar = c;
				}
				break;
			case '{':
				if (stringChar == 0) {
					depth++;
				}
				break;
			case '}':
				if (stringChar == 0) {
					depth--;
					if (depth < 0) {
						return next;
					}
				}
			}
		} while (++next < length);
		return -1;
	}

}
