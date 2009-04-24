package org.xidea.lite.parser.impl;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.lite.Template;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseContext;

public class TextParser extends AbstractTextParser {
	public static InstructionParser EL = new ELParser("", true);
	public static InstructionParser IF = new ELParser("if", true) {
		protected void addEl(ParseContext context, String text) {
			context.appendIf(context.optimizeEL(text));
		}
	};
	public static InstructionParser ELSE = new ELParser("else", false) {
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
	};
	public static InstructionParser FOR = new ELParser("for", true) {
		protected void addEl(ParseContext context, String text) {
			int p = text.indexOf(':');
			context.appendFor(text.substring(0, p).trim(), context
					.optimizeEL(text.substring(p + 1)), null);
		}
	};
	public static InstructionParser END = new ELParser("end", false) {
		public int parse(ParseContext context, String text, int p$) {
			context.appendEnd();
			return p$ + 4;
		}
	};
	public static InstructionParser VAR = new ELParser("var", true) {
		protected void addEl(ParseContext context, String text) {
			int p = text.indexOf('=');
			if (p > 0) {
				context.appendVar(text.substring(0, p).trim(), context
						.optimizeEL(text.substring(p + 1)));
			} else {
				context.appendCaptrue(text.trim());
			}
		}
	};

	public TextParser() {
		instructionParser = new InstructionParser[] { EL, IF, ELSE, FOR, FOR,
				END, FOR };
	}

}

class ELParser implements InstructionParser {
	private String fn = "";
	private boolean requireEL;
	private String prefix;
	private int length;

	public ELParser(String fn, boolean requireEL) {
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
