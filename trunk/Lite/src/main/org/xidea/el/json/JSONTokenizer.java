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
	protected boolean strict = false;

	public JSONTokenizer(String source, boolean strict) {
		this.value = source.trim();
		if (value.startsWith("\uFEFF")) {
			value = value.substring(1);
		}
		this.end = this.value.length();
		this.strict = strict;
	}

	protected Object parse() {
		skipComment();
		char c = toLower(value.charAt(start));
		switch (c) {
		case '"':
		case '\'':
			return findString();
		case '[':
			return findList();
		case '{':
			return findMap();
		default:
			if (c >= '0' && c <= '9') {
				return findNumber();
			} else if (c == '-') {
				if (strict) {
					return findNumber();
				} else {
					int start = this.start;
					skipComment();
					c = value.charAt(this.start);
					if (c >= 0 || c <= 9) {
						this.start = start;
						return findNumber();
					} else {
						String key = findId();
						if ("Infinity".equals(key)) {
							return Double.NEGATIVE_INFINITY;
						} else {
							throw buildError(key + " is  not a valid number!!");
						}
					}
				}
			}
			String key = findId();
			if ("true".equals(key)) {
				return Boolean.TRUE;
			} else if ("false".equals(key)) {
				return Boolean.FALSE;
			} else if ("null".equals(key)) {
				return null;
			} else if (!strict) {
				if ("NaN".equals(key)) {
					return Double.NaN;
				} else if ("Infinit".equals(key)) {
					return Double.POSITIVE_INFINITY;
				}

			}
			throw buildError("");
		}
	}

	protected ExpressionSyntaxException buildError(String msg) {
		return new ExpressionSyntaxException("语法错误:" + msg + "\n" + value + "@"
				+ start);
	}

	/*
	 * 0xfee0+0x21-0xfee0+0x7e \uff01-\uff5e ！ - ～ ! - ~
	 */
	protected char toLower(char c) {
		if (c >= 0xff01 && c <= 0xff5e) {
			c -= 0xfee0;
		}
		return c;
	}

	protected Map<String, Object> findMap() {
		start++;
		skipComment();
		LinkedHashMap<String, Object> result = new LinkedHashMap<String, Object>();
		if (value.charAt(start) == '}') {
			start++;
			return result;
		}
		while (true) {
			// result.add(parse());
			char c = value.charAt(start);
			final String key;
			if (c == '"') {
				key = findString();
			} else {
				if (c == '\'') {
					key = findString();
				} else {
					if (strict) {
						throw buildError("JSON 标准Object Key 必须为标准JSON 字符串,如:{\"key\":\"value\"}");
					}
					key = findId();
				}
			}
			skipComment();
			c = value.charAt(start++);
			if (c != ':') {
				throw buildError("无效对象语法");
			}
			Object valueObject = parse();
			skipComment();
			c = value.charAt(start++);
			if (c == ',') {
				result.put(key, valueObject);
				skipComment();
			} else if (c == '}') {
				result.put(key, valueObject);
				return result;
			} else {
				throw buildError("无效对象语法");
			}
		}
	}

	protected List<Object> findList() {
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
				result.add(parse());
			} else {
				throw buildError("无效数组语法:");
			}
		}
	}

	private long parseHex() {
		long lvalue = 0;//
		while (start < end) {
			char c = value.charAt(start++);
			if (c >= '0' && c <= '9') {
				lvalue = (lvalue << 4) + (c - '0');
			} else if (c >= 'A' && c <= 'F') {
				lvalue = (lvalue << 4) + (c - 'A' + 10);
			} else if (c >= 'a' && c <= 'f') {
				lvalue = (lvalue << 4) + (c - 'a' + 10);
			} else {
				start--;
				break;
			}
		}
		return lvalue;
	}

	private int parseOctal() {
		int lvalue = 0;//
		while (start < end) {
			char c = value.charAt(start++);
			if (c >= '0' && c < '8') {
				lvalue = (lvalue << 3) + (c - '0');
			} else {
				start--;
				break;
			}
		}
		return lvalue;
	}

	private void seekDecimal() {
		while (start < end) {
			char c = value.charAt(start++);
			if (c >= '0' && c <= '9') {
			} else {
				start--;
				break;
			}
		}
	}

	private void seekNegative() {
		char c = value.charAt(start++);
		if (c == '-' || c == '+') {
		} else {
			start--;
		}

	}

	private Number parseZero(boolean neg) {
		if (start < end) {
			char c = value.charAt(start++);
			if (c == 'x' || c == 'X') {
				if (strict) {
					throw buildError("JSON未定义16进制数字");
				}
				long value = parseHex();
				if (neg) {
					value = -value;
				}
				return value;
			} else if (c > '0' && c <= '7') {
				if (strict) {
					throw buildError("JSON未定义8进制数字");
				}
				start--;
				int value = parseOctal();
				if (neg) {
					value = -value;
				}
				return value;
			} else if (c == '.') {
				start--;
				return parseFloat(start - 1);
			} else {
				start--;
				return 0;
			}
		} else {
			return 0;
		}
	}

	/**
	 * 当前值为 . 或者 E，e
	 * 
	 * @param begin
	 * @return
	 */
	private Number parseFloat(final int begin) {
		boolean isFloatingPoint = false;
		char next = value.charAt(start);
		if (next == '.') {
			start++;
			int p = start;
			seekDecimal();
			if (start == p) {// 复位
				start--;
				String ns = value.substring(begin, start);
				return Long.parseLong(ns);
			} else {
				isFloatingPoint = true;
				if (start < end) {
					next = value.charAt(start);
				} else {
					next = 0;
				}
			}
		}
		if (next == 'E' || next == 'e') {
			start++;
			isFloatingPoint = true;
			seekNegative();
			seekDecimal();
		}
		String ns = value.substring(begin, start);
		// System.out.println(ns);
		if (isFloatingPoint) {
			return Double.parseDouble(ns);
		} else {
			return Long.parseLong(ns);
		}
	}

	// 还是改成JDK自己的parser？
	protected Number findNumber() {
		// 10进制优化
		final int begin = start;
		boolean nag = false;
		char c = value.charAt(start++);
		if (c == '+') {
			c = value.charAt(start++);
		} else if (c == '-') {
			nag = true;
			c = value.charAt(start++);
		}

		if (c == '0') {
			return parseZero(nag);
		} else {
			long ivalue = c - '0';
			while (start < end) {
				c = value.charAt(start++);
				if (c >= '0' && c <= '9') {
					ivalue = ivalue * 10 + (c - '0');
				} else {
					if (c == '.' || c == 'E') {
						start--;
						return parseFloat(begin);
					} else {
						start--;
						break;
					}
				}
			}
			return nag ? -ivalue : ivalue;
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
		throw buildError("无效id");

	}

	/**
	 * {@link Decompiler#printSourceString
	 */
	protected String findString() {
		char quoteChar = value.charAt(start++);
		if (strict && quoteChar == '\'') {
			throw buildError("JSON标准 字符串应该是双引号\"...\")");
		}
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
				case '/':
					buf.append('/');
					break;
				case '\'':
					buf.append('\'');
					break;
				case '\"':
					buf.append('"');
					break;
				case 'u':
					buf.append((char) Integer.parseInt(value.substring(start,
							start + 4), 16));
					start += 4;
					break;
				case 'x':
					buf.append((char) Integer.parseInt(value.substring(start,
							start + 2), 16));
					start += 2;
					break;
				default:
					if (strict) {
						throw buildError("发现JSON 标准未定义转义字符");
					}
					buf.append(c);
					buf.append(c2);
				}
				break;
			case '"':
			case '\'':
				if (c == quoteChar) {
					return (buf.toString());
				}
				buf.append(c);
				break;
			case '\r':
			case '\n':
				// if(strict){
				throw buildError("JSON 标准字符串不能换行");
				// }
			default:
				buf.append(c);

			}
		}
		throw buildError("未结束字符串");
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
				if (strict) {
					throw buildError("JSON 标准未定义注释");
				}
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
							} else {
								cend++;
							}
						} else {
							throw buildError("未結束注釋");
						}
					}
				}
			} else {
				break;
			}
		}
	}

}
