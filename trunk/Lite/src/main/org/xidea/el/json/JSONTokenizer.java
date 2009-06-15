package org.xidea.el.json;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionSyntaxException;

public class JSONTokenizer {

	protected String value;
	protected int start;
	protected final int end;

	public JSONTokenizer(String value) {
		this.value = value.trim();
		if (value.startsWith("\uFEFF")) {
			value = value.substring(1);
		}
		this.end = this.value.length();
	}

	public Object parse() {
		skipComment();
		char c = value.charAt(start);
		if (c == '"') {
			return findString();
		} else if (c == '-' || c >= '0' && c <= '9') {
			return findNumber();
		} else if (c == '[') {
			return findList();
		} else if (c == '{') {
			return findMap();
		} else {
			String key = findId();
			if ("true".equals(key)) {
				return Boolean.TRUE;
			} else if ("false".equals(key)) {
				return Boolean.FALSE;
			} else if ("null".equals(key)) {
				return null;
			}
			throw new ExpressionSyntaxException("语法错误:" + value + "@" + start);
		}
	}

	private Map<String, Object> findMap() {
		start++;
		skipComment();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		if (value.charAt(start) == '}') {
			start++;
			return result;
		}
		while (true) {
			// result.add(parse());
			String key = (String) parse();
			skipComment();
			char c = value.charAt(start++);
			if (c != ':') {
				throw new ExpressionSyntaxException("错误对象语法:" + value + "@"
						+ start);
			}
			Object valueObject = parse();
			skipComment();
			c = value.charAt(start++);
			if (c == '}') {
				result.put(key, valueObject);
				return result;
			} else if (c != ',') {
				throw new ExpressionSyntaxException("错误对象语法:" + value + "@"
						+ start);
			} else {
				result.put(key, valueObject);

			}
		}
	}

	private List<Object> findList() {
		ArrayList<Object> result = new ArrayList<Object>();
		// start--;
		start++;
		skipComment();
		if (value.charAt(start) == ']') {
			start++;
			return result;
		} else {
			result.add(parse());
		}
		while (true) {
			skipComment();
			char c = value.charAt(start++);
			if (c == ']') {
				return result;
			} else if (c == ',') {
				skipComment();
				result.add(parse());
			} else {
				throw new ExpressionSyntaxException("错误数组语法:" + value + "@"
						+ start);
			}
		}
	}

	private long parseHex(int i) {
		long lvalue = 0;//
		while (i < end) {
			char c = value.charAt(i++);
			if (c >= '0' && c <= '9') {
				lvalue = (lvalue << 4) + (c - '0');
			} else if (c >= 'A' && c <= 'F') {
				lvalue = (lvalue << 4) + (c - 'A' + 10);
			} else if (c >= 'a' && c <= 'f') {
				lvalue = (lvalue << 4) + (c - 'a' + 10);
			} else {
				i--;
				break;
			}
		}
		start = i;
		return lvalue;
	}
	//还是改成JDK自己的parser？
	protected Number findNumber() {
		int i = start;// skip -;
		boolean isFloatingPoint = false;
		char c = value.charAt(i++);
		int flag;
		if (c == '-') {
			c = value.charAt(i++);
			flag = -1;
		}else{
			flag = 1;
		}
		if (c == '0') {
			if (i < end) {
				c = value.charAt(i++);
				if (c == 'x' || c == 'X') {
					return flag * parseHex(i);
				} else {
					c = '0';
					i--;
				}
			} else {
				start = i;
				return 0;
			}
		}
		long ivalue = c - '0';
		while (i < end) {
			c = value.charAt(i++);
			if (c >= '0' && c <= '9') {
				ivalue = (ivalue * 10) + (c - '0');
			} else {
				break;
			}
		}
		if (c == '.') {
			c = value.charAt(i++);
			while (c >= '0' && c <= '9') {
				isFloatingPoint = true;
				if (i < end) {
					c = value.charAt(i++);
				} else {
					break;
				}
			}
			if (!isFloatingPoint) {
				// c = '.';
				// i--;
				start = i - 2;
				return flag * ivalue;
			}
		}
		if (c == 'E' || c == 'e') {
			isFloatingPoint = true;
			c = value.charAt(i++);
			if (c == '+' || c == '-') {
				c = value.charAt(i++);
			}
			while (c >= '0' && c <= '9') {
				if (i < end) {
					c = value.charAt(i++);
				} else {
					break;
				}
			}
		} else {
			c = value.charAt(i - 1);
			if (c < '0' || c > '9') {
				i--;
			}
		}

		if (isFloatingPoint) {
			return /*flag **/ Float.parseFloat(value.substring(start, start = i));
		} else {
			start = i;
			return flag * ivalue;
		}
	}

	protected String findId() {
		int p = start;
		if (Character.isJavaIdentifierPart(value.charAt(p++))) {
			while (p < end) {
				if (!Character.isJavaIdentifierPart(value.charAt(p))) {
					break;
				}
				p++;
			}
			return (value.substring(start, start = p));
		}
		throw new ExpressionSyntaxException("无效id");

	}

	/**
	 * {@link Decompiler#printSourceString
	 */
	protected String findString() {
		char quoteChar = value.charAt(start++);
		StringBuilder buf = new StringBuilder();
		while (start < end) {
			char c = value.charAt(start++);
			switch (c) {
			case '\\':
				char c2 = value.charAt(start++);
				switch (c2) {
				case 'b':
					buf.append('\b');
					break;
				case 'f':
					buf.append('\f');
					break;
				case 'n':
					buf.append('\n');
					break;
				case 'r':
					buf.append('\r');
					break;
				case 't':
					buf.append('\t');
					break;
				case 'v':
					buf.append(0xb);
					break; // Java lacks \v.
				case ' ':
					buf.append(' ');
					break;
				case '\\':
					buf.append('\\');
					break;
				case '\'':
					buf.append('\'');
					break;
				case '\"':
					buf.append('"');
					break;
				case 'u':
					buf.append((char) Integer.parseInt(value.substring(
							start + 1, start + 5), 16));
					start += 4;
					break;
				case 'x':
					buf.append((char) Integer.parseInt(value.substring(
							start + 1, start + 3), 16));
					start += 2;
					break;
				}
				break;
			case '"':
			case '\'':
				if (c == quoteChar) {
					return (buf.toString());
				}
			default:
				buf.append(c);

			}
		}
		throw new ExpressionSyntaxException("未结束字符串:" + value + "@" + start);
	}

	protected void skipComment() {
		while (true) {
			while (start < end) {
				if (!Character.isWhitespace(value.charAt(start))) {
					break;
				}
				start++;
			}
			if (start < end && value.charAt(start) == '/') {
				start++;
				char next = value.charAt(start++);
				if (next == '/') {
					int end1 = this.value.indexOf('\n', start);
					int end2 = this.value.indexOf('\r', start);
					int cend = Math.min(end1, end2);
					if (cend < 0) {
						cend = Math.max(end1, end2);
					}
					if (cend > 0) {
						start = cend;
					} else {
						start = this.end;
					}
				} else if (next == '*') {
					int cend = start + 1;
					while (true) {
						cend = this.value.indexOf('/', cend);
						if (cend > 0) {
							if (this.value.charAt(cend - 1) == '*') {
								start = cend + 1;
								break;
							}
						} else {
							throw new ExpressionSyntaxException("未結束注釋:"
									+ value + "@" + start);
						}
					}
				}
			} else {
				break;
			}
		}
	}

	protected boolean skipSpace(int nextChar) {
		while (start < end) {
			if (!Character.isWhitespace(value.charAt(start))) {
				break;
			}
			start++;
		}
		if (nextChar > 0 && start < end) {
			int next = value.charAt(start);
			if (nextChar == next) {
				return true;
			}
		}
		return false;
	}
}
