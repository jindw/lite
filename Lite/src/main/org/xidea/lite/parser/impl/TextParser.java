package org.xidea.lite.parser.impl;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.Parser;

public class TextParser extends AbstractTextParser{
	protected class ELParser implements InstructionParser {
		private String fn = "";

		public ELParser(String fn) {
			this.fn = fn;
		}

		public ELParser() {
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
			Object el = context.optimizeEL(text);
			switch (context.getELType()) {
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
	}

	public TextParser() {
		tagParser.put("", new ELParser());
		tagParser.put("end", new InstructionParser() {
			public int parse(ParseContext context, String text, int p$) {
				context.appendEnd();
				return p$ + 4;
			}
		});
		tagParser.put("if", new ELParser("if") {
			protected void addEl(ParseContext context, String text) {
				context.appendIf(context.optimizeEL(text));
			}
		});
		tagParser.put("else", new InstructionParser() {
			public int parse(ParseContext context, String text, int p$) {
				int begin = text.indexOf('{', p$);
				if (begin > 0) {
					int end = findELEnd(text, begin);
					if (end > 0) {
						String el = text.substring(begin + 1, end);
						context.appendElse(context.optimizeEL(el));
						return end + 1;
					}
				}
				context.appendElse(null);
				return p$ + 5;// "else".length+1
			}
		});
		tagParser.put("for", new ELParser("for") {
			protected void addEl(ParseContext context, String text) {
				int p = text.indexOf(':');
				context.appendFor(text.substring(0, p).trim(), context
						.optimizeEL(text.substring(p + 1)), null);
			}
		});
		tagParser.put("var", new ELParser("var") {
			protected void addEl(ParseContext context, String text) {
				int p = text.indexOf('=');
				if (p > 0) {
					context.appendVar(text.substring(0, p).trim(), context
							.optimizeEL(text.substring(p + 1)));
				} else {
					context.appendCaptrue(text.trim());
				}
			}
		});
	}

	protected String findFN(String text, int p$) {
		int next = p$ + 1;
		for (; next < text.length()
				&& Character.isJavaIdentifierPart(text.charAt(next)); next++)
			;
		String fn = text.substring(p$ + 1, next);
		if (fn.length() == 0 || tagParser.containsKey(fn)) {
			return fn;
		} else {
			return null;
		}
	}

	/**
	 * 
	 * @param text
	 * @param elBegin
	 *            {的位置
	 * @return }的位置
	 */
	private int findELEnd(String text, int elBegin) {
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
