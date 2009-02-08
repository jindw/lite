package org.xidea.lite.parser;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.xidea.el.ExpressionFactory;
import org.xidea.el.ExpressionFactoryImpl;
import org.xidea.lite.Template;

public class TextParser implements Parser {
	private ExpressionFactory expressionFactory = ExpressionFactoryImpl
			.getInstance();

	public void setExpressionFactory(ExpressionFactory expressionFactory) {
		this.expressionFactory = expressionFactory;
	}

	public List<Object> parse(Object text, ParseContext context) {
		return parseText((String) text, Template.EL_TYPE, (char) 0);
	}

	protected InputStream getInputStream(URL url) throws IOException {
		return url.openStream();
	}

	protected String encodeText(String text, int quteChar) {
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
			case '"':
				if (quteChar == c) {
					out.write("&#39;");
					break;
				} else if (quteChar == c) {
					out.write("&#34;");
					break;
				}
			default:
				out.write(c);
			}
		}
		return out.toString();
	}

	/**
	 * 解析指定文本
	 * 
	 * @public
	 * @abstract
	 * @return <Array> result
	 */
	public List<Object> parseText(final String text, final int defaultElType,
			final int quteChar) {
		int i = 0;
		int start = 0;
		int length = text.length();
		ArrayList<Object> result = new ArrayList<Object>();
		do {
			final int p$ = text.indexOf('$', start);
			if (p$ < 0) {
				continue;
			} else if (p$ > 0 && text.charAt(p$ - 1) == '\\') {
				int pre = p$ - 1;
				while (pre-- > 0 && text.charAt(pre) == '\\')
					;
				int count = p$ - pre;
				result.add(text.substring(start, p$ - count % 2));
				start = p$;
				if ((count & 1) == 0) {// escape
					continue;
				}
			}
			String fn = findFN(text, p$);
			// final int p1 = text.indexOf('{', p$);
			if (fn != null) {
				start = parseInstruction(result, text, fn, defaultElType,
						start, p$);
			}

		} while (++i < length);
		if (start < length) {
			result.add(text.substring(start));
		}
		return encodeResult(result, defaultElType, quteChar);
	}

	protected List<Object> encodeResult(ArrayList<Object> result,
			int defaultElType, int quteChar) {
		int i;
		if (defaultElType != Template.EL_TYPE) {
			i = result.size();
			while (i-- > 0) {
				Object item = result.get(i);
				if (item instanceof String) {
					if (((String) item).length() == 0) {
						result.remove(i);
					} else {
						result.set(i, encodeText((String) item, quteChar));
					}
				}
			}
		}
		return result;
	}

	protected String findFN(String text, int p$) {
		int next = p$ + 1;
		for (; next < text.length()
				&& Character.isJavaIdentifierPart(text.charAt(next)); next++)
			;
		String fn = text.substring(p$ + 1, next);
		if ("end".equals(fn) || next < text.length()
				&& text.charAt(next) == '{') {
			return fn;
		} else {
			return null;
		}
	}

	protected int parseInstruction(List<Object> result, String text, String fn,
			int defaultElType, int start, final int p$) {
		if (start < p$) {
			result.add(text.substring(start, p$));
			start = p$;
		}
		if ("end".equals(fn)) {
			result.add(END_INSTRUCTION);
			return p$ + fn.length();
		} else {
			int elBegin = p$ + fn.length() + 1;
			int elEnd = findELEnd(text, elBegin);
			if (elEnd > 0) {
				try {
					Object el = optimizeEL(text.substring(elBegin, elEnd));
					result.add(new Object[] { defaultElType, el });
					return elEnd + 1;
				} catch (Exception e) {
				}
			}
			result.add(text.substring(start, start + 1));
			return start + 1;
		}
	}

	public Object optimizeEL(String expression) {
		return expressionFactory.optimizeEL(expression);
	}

	protected int findELEnd(String text, int p) {
		int length = text.length();
		if (p >= length) {
			return -1;
		}
		int next = p;
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
					if (depth == 0) {
						return next;
					}
				}
			}
		} while (++next < length);
		return -1;
	}

}
