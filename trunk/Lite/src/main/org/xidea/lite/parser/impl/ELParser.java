package org.xidea.lite.parser.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.ExpressionSyntaxException;
import org.xidea.lite.Template;
import org.xidea.lite.parser.InstructionParser;
import org.xidea.lite.parser.ParseContext;

public class ELParser implements InstructionParser {
	private static final Log log = LogFactory.getLog(ELParser.class);
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
		public int parse(String text, int p$, ParseContext context) {
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
		public int parse(String text, int p$, ParseContext context) {
			int p1 = text.indexOf('{', p$);
			int p2 = text.indexOf('}', p1);
			String id = text.substring(p1 + 1, p2);
			ParseContext clientContext = new ParseContextImpl(context,id,JSTranslator.getInstance());
			String subtext = text.substring(p2 + 1);
			clientContext.setAttribute(CLIENT, context);
			clientContext.appendAll(clientContext.parseText(subtext, context.getTextType()));
			return text.length();

		}
	};
	public static InstructionParser END = new ELParser("end", false) {
		public int parse(String text, int p$, ParseContext context) {
			ParseContext parentContext = (ParseContext) context
					.getAttribute(CLIENT);
			if (parentContext != null) {
				int depth = context.getDepth();
				if (depth == 0) {
					String js = (String) context.toList().get(0);
					parentContext.append("<script>/*<![CDATA[*/" + js
							+ "/*]]>*/</script>");
					String subtext = text.substring(p$ + 4);
					parentContext.appendAll(parentContext.parseText(subtext, parentContext
							.getTextType()));
					
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

	public int findStart(String text, int start,int other$start) {
		int i;
		while ((i = text.indexOf(this.prefix, start)) >= start && i<=other$start) {
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

	public int parse(String text, int p$, ParseContext context) {
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
		switch (context.getTextType()) {
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
	 * @param elQuteBegin
	 *            {的位置
	 * @return }的位置
	 */
	protected int findELEnd(String text, final int elQuteBegin) {
		int length = text.length();
		int next = elQuteBegin + 1;
		if (next >= length) {
			return -1;
		}
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
				break;
			case '/':// 如果是正则，需要跳过正则
				boolean isRegExp = isRegExp(text, elQuteBegin, next);
				if (isRegExp) {
					int end = findRegExpEnd(text, next);
					if(end >0){
						next = end;
					}else{
						log.error("无效状态");
					}
				}
			}
		} while (++next < length);
		return -1;
	}

	private static boolean isRegExp(String text, final int elQuteBegin,
			int regExpStart) {
		for (int i = regExpStart-1; i > elQuteBegin; i--) {
			char pc = text.charAt(i);
			if (!Character.isWhitespace(pc)) {
				if (Character.isJavaIdentifierPart(pc)) {
					return false;// 有效id后，不可能是正则
				} else {
					switch (pc) {
					case ']':// 伪有效id后，不可能是正则
					case ')':
					case '}':
						return false;
						// case '{'
						// case '[':
						// case '(':
						// 伪开头，不可能是除号，是正则
						// isRegExp = true;
						// break;
						// +-*/ 非后缀运算符后，一定是正则，非运算符
					default:
						return true;
					}
				}
			}
		}
		// 开头出现时，是正则
		return true;
	}

	private static int findRegExpEnd(String text, int regExpStart) {
		int length = text.length();
		int depth = 0;
		for (regExpStart++; regExpStart < length; regExpStart++) {
			char rc = text.charAt(regExpStart);
			if (rc == '[') {
				depth = 1;
			} else if (rc == ']') {
				depth = 0;
			} else if (rc == '\\') {
				regExpStart++;
			} else if (depth == 0 && rc == '/') {
				while (regExpStart < length) {
					rc = text.charAt(regExpStart++);
					switch (rc) {
					case 'g':
					case 'i':
					case 'm':
						break;
					default:
						return regExpStart - 1;
					}
				}

			}
		}
		return -1;
	}

}
