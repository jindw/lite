package org.xidea.el.parser;

import static org.xidea.el.parser.TokenImpl.TOKEN_MAP;
import static org.xidea.el.parser.TokenImpl.BRACKET_BEGIN;
import static org.xidea.el.parser.TokenImpl.BRACKET_END;
import static org.xidea.el.ExpressionToken.OP_ADD;
import static org.xidea.el.ExpressionToken.BIT_PARAM;
import static org.xidea.el.ExpressionToken.BIT_PRIORITY;
import static org.xidea.el.ExpressionToken.BIT_PRIORITY_SUB;
import static org.xidea.el.ExpressionToken.OP_GET_PROP;

import static org.xidea.el.ExpressionToken.OP_INVOKE_METHOD;
import static org.xidea.el.ExpressionToken.OP_MAP_PUSH;
import static org.xidea.el.ExpressionToken.OP_NEG;
import static org.xidea.el.ExpressionToken.OP_PARAM_JOIN;
import static org.xidea.el.ExpressionToken.OP_POS;
import static org.xidea.el.ExpressionToken.OP_QUESTION;
import static org.xidea.el.ExpressionToken.OP_QUESTION_SELECT;
import static org.xidea.el.ExpressionToken.OP_GET_STATIC_PROP;
import static org.xidea.el.ExpressionToken.OP_SUB;
import static org.xidea.el.ExpressionToken.VALUE_CONSTANTS;
import static org.xidea.el.ExpressionToken.VALUE_NEW_LIST;
import static org.xidea.el.ExpressionToken.VALUE_NEW_MAP;
import static org.xidea.el.ExpressionToken.VALUE_VAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.ExpressionToken;
import org.xidea.el.json.JSONTokenizer;

public class ExpressionTokenizer extends JSONTokenizer {
	private final static TokenImpl TOKEN_TRUE = new TokenImpl(VALUE_CONSTANTS,
			Boolean.TRUE);
	private final static TokenImpl TOKEN_FALSE = new TokenImpl(VALUE_CONSTANTS,
			Boolean.FALSE);
	private final static TokenImpl TOKEN_NULL = new TokenImpl(VALUE_CONSTANTS,
			null);

	private static enum Status {
		STATUS_BEGIN, STATUS_EXPRESSION, STATUS_OPERATOR
	}

	private Status status = Status.STATUS_BEGIN;
	private int previousType = Integer.MIN_VALUE;

	protected ArrayList<TokenImpl> tokens = new ArrayList<TokenImpl>();
	protected TokenImpl expression;

	public ExpressionTokenizer(String value) {
		super(value);
		parseEL();
		prepareSelect();
		LinkedList<TokenImpl> stack = new LinkedList<TokenImpl>();
		try {
			toTree(right(this.tokens.iterator()), stack);
		} catch (Exception e) {
			throw new ExpressionSyntaxException(e);
		}
		if (stack.size() != 1) {
			new ExpressionSyntaxException("表达式语法错误：" + value);
		}
		this.expression = stack.getFirst();
	}

	private void prepareSelect() {
		int p1 = tokens.size();
		while (p1-- > 0) {
			int type1 = tokens.get(p1).getType();
			if (type1 == OP_QUESTION) { // (a?b
				int pos = getSelectRange(p1, -1, -1);
				tokens.add(pos + 1, new TokenImpl(BRACKET_BEGIN, null));
				p1++;
			} else if (type1 == OP_QUESTION_SELECT) {
				int end = tokens.size();
				int pos = getSelectRange(p1, 1, end);
				tokens.add(pos, new TokenImpl(BRACKET_END, null));
			}
		}
	}

	private int getSelectRange(int p2, int inc, int end) {
		int dep = 0;
		while ((p2 += inc) != end) {
			int type2 = tokens.get(p2).getType();
			if (type2 > 0) {// op
				if (type2 == BRACKET_BEGIN) {
					dep += inc;
				} else if (type2 == BRACKET_END) {
					dep -= inc;
				} else {
					if (dep == 0 && getPriority(type2) <= getPriority(OP_QUESTION)) {
						return p2;
					}
				}
				if (dep < 0) {
					return p2;
				}
			}
		}
		return inc > 0 ? end : -1;
	}

	public ExpressionToken getResult() {
		return expression;
	}

	private void toTree(Iterator<TokenImpl> tokens, LinkedList<TokenImpl> stack) {
		while (tokens.hasNext()) {
			final TokenImpl item = tokens.next();
			int type = item.getType();
			switch (type) {
			case VALUE_CONSTANTS:
			case VALUE_VAR:
			case VALUE_NEW_LIST:
			case VALUE_NEW_MAP:
				stack.addFirst(item);
				break;
			default:// OP
				if ((type & BIT_PARAM) > 0) {// 两个操作数
					TokenImpl arg2 = stack.removeFirst();
					TokenImpl arg1 = stack.removeFirst();
					item.setLeft(arg1);
					item.setRight(arg2);
					stack.addFirst(item);
				} else {// 一个操作树
					TokenImpl arg1 = stack.removeFirst();
					item.setLeft(arg1);
					stack.addFirst(item);
				}
			}
		}
	}

	// 将中序表达式转换为右序表达式
	private Iterator<TokenImpl> right(Iterator<TokenImpl> tokens) {
		LinkedList<List<TokenImpl>> rightStack = new LinkedList<List<TokenImpl>>();
		rightStack.addFirst(new ArrayList<TokenImpl>()); // 存储右序表达式

		LinkedList<TokenImpl> buffer = new LinkedList<TokenImpl>();

		while (tokens.hasNext()) {
			final TokenImpl item = tokens.next();
			if (item.getType() > 0) {
				if (buffer.isEmpty()) {
					buffer.addFirst(item);
				} else if (item.getType() == BRACKET_BEGIN) {// ("(")
					buffer.addFirst(item);
				} else if (item.getType() == BRACKET_END) {// .equals(")"))
					while (true) {
						TokenImpl operator = buffer.removeFirst();
						if (operator.getType() == BRACKET_BEGIN) {
							break;
						}
						addRightToken(rightStack, operator);
					}
				} else {
					while (!buffer.isEmpty()
							&& rightEnd(item, buffer.getFirst())) {
						TokenImpl operator = buffer.removeFirst();
						// if (operator.getType() !=
						// BRACKET_BEGIN){
						addRightToken(rightStack, operator);
					}
					buffer.addFirst(item);
				}
			} else {// lazy begin value exp
				addRightToken(rightStack, item);
			}
		}
		while (!buffer.isEmpty()) {
			TokenImpl operator = buffer.removeFirst();
			addRightToken(rightStack, operator);
		}
		return rightStack.getFirst().iterator();
	}

	private void addRightToken(LinkedList<List<TokenImpl>> rightStack,
			TokenImpl token) {
		List<TokenImpl> list = rightStack.getFirst();
		if (token.getType() == OP_GET_PROP) {
			int last = list.size() - 1;
			if (last >= 0) {
				TokenImpl previous = list.get(last);
				if (previous.getType() == VALUE_CONSTANTS) {
					list.remove(last);
					token = new TokenImpl(OP_GET_STATIC_PROP, previous
							.getParam());
				}
			}
		}
		list.add(token);
	}

	protected int getPriority(int type) {
		switch (type) {
		case BRACKET_BEGIN:
		case BRACKET_END:
			return Integer.MIN_VALUE;
		default:
			return (type & BIT_PRIORITY) << 4 | (type & BIT_PRIORITY_SUB) >> 12;
		}
	}

	private boolean rightEnd(TokenImpl item, TokenImpl privious) {
		int t1 = privious.getType();
		int t2 = item.getType();
		int p1 = getPriority(t1);
		int p2 = getPriority(t2);
		// 1+2*2
		// (a?b:c) == > (a?b):c
		// (a?b:ca:cb:cc) => (a?b):((ca?cb):cc)
		// 1?1:3 + 0?5:7 ==>1 //1?1:(3 + 0?5:7 )
		// 1?0?5:7:3 ==>7 //1?(0?5:7):3
		// 1?0?5:0?11:13:3 ==>13 //1?((0?5:0)?11:13):3
		if (p2 <= p1) {
			// if(p2 == p1){
			// if(t2 == OP_QUESTION_SELECT){
			// return true;//t1 == OP_QUESTION;
			// }else if(t2 == OP_QUESTION){
			// return false;//t1 == OP_QUESTION_SELECT;
			// }
			// }
			return true;
		} else {
			return false;
		}
	}

	protected void parseEL() {
		skipSpace(0);
		while (start < end) {
			char c = value.charAt(start);
			if (c == '"' || c == '\'') {
				String text = findString();
				addKeyOrObject(text, false);
			} else if (c >= '0' && c <= '9') {
				Number number = findNumber();
				addKeyOrObject(number, false);
			} else if (Character.isJavaIdentifierStart(c)) {
				String id = findId();
				if ("true".equals(id)) {
					addToken(TOKEN_TRUE);
				} else if ("false".equals(id)) {
					addToken(TOKEN_FALSE);
				} else if ("null".equals(id)) {
					addToken(TOKEN_NULL);
				} else {
					skipSpace(0);
					if (previousType == OP_GET_PROP) {
						addToken(new TokenImpl(VALUE_CONSTANTS, id));
					} else {
						addKeyOrObject(id, true);
					}
				}
			} else {
				String op = findOperator();
				// if (value.startsWith(op, start))
				if (op != null) {
					parseOperator(op);
				} else {
					this.reportError("未知操作符:");
				}
			}
			skipSpace(0);
		}
	}

	private String findOperator() {// optimize json ,:[{}]
		char c = value.charAt(start);
		int end = start + 1;
		char next = value.length() > end ? value.charAt(end) : 0;
		switch (c) {
		case ',':// optimize for json
		case ':':// 3op,map key
		case '[':// list
		case ']':
		case '{':// map
		case '}':
		case '(':// quote
		case ')':
		case '.':// prop
		case '?':// 3op
		case '+':// 5op
		case '-':
		case '~':
		case '^':
		case '*':
		case '/':
		case '%':
			break;
		case '=':// ==
			if (next == '=') {
				end++;
				if (value.length() > end && value.charAt(end) == '=') {
					this.reportError("不支持=== 和!==操作符，请使用==,!=");
				}
			} else {
				this.reportError("不支持赋值操作:");
			}
			break;
		case '!':// !,!=
			if (next == '=') {
				end++;
				if (value.length() > end && value.charAt(end) == '=') {
					this.reportError("不支持=== 和!==操作符，请使用==,!=");
				}
			}
			break;
		case '>':// >,>=
		case '<':// <,<=
			if (next == '=') {
				end++;
			}
			break;
		case '&':// && / &
		case '|':// || /|
			if ((c == next)) {
				end++;
			}
			break;
		default:
			return null;
		}

		return value.substring(start, start = end);
	}

	private void reportError(String msg) {
		throw new ExpressionSyntaxException(msg + "\n@" + start + "\n"
				+ value.substring(start) + "\n----\n" + value);
	}

	/**
	 * 碰見:和,的時候，就需要檢查是否事map的間隔符號了
	 * 
	 * @return
	 */
	private boolean isMapMethod() {
		int i = tokens.size() - 1;
		int depth = 0;
		for (; i >= 0; i--) {
			TokenImpl token = tokens.get(i);
			int type = token.getType();
			if (depth == 0) {
				if (type == OP_MAP_PUSH || type == VALUE_NEW_MAP) {// (
					// <#newMap>
					// <#push>
					return true;
				} else if (type == OP_PARAM_JOIN) {// (
					// <#newList>
					// <#param_join>
					return false;
				}
			}
			if (type == BRACKET_BEGIN) {
				depth--;
			} else if (type == BRACKET_END) {
				depth++;
			}
		}
		return false;
	}

	private void parseOperator(String op) {
		if (op.length() == 1) {
			switch (op.charAt(0)) {
			case '(':
				if (status == Status.STATUS_EXPRESSION) {
					addToken(new TokenImpl(OP_INVOKE_METHOD, null));
					if (skipSpace(')')) {
						addToken(new TokenImpl(VALUE_CONSTANTS,
								Collections.EMPTY_LIST));
						start++;
					} else {
						addList();
					}

				} else {
					addToken(new TokenImpl(BRACKET_BEGIN, null));
				}
				break;
			case '[':
				if (status == Status.STATUS_EXPRESSION) {// getProperty
					addToken(new TokenImpl(OP_GET_PROP, null));
					addToken(new TokenImpl(BRACKET_BEGIN, null));
				} else {// list
					addList();
				}
				break;
			case '{':
				addMap();
				break;
			case '}':
			case ']':
			case ')':
				addToken(new TokenImpl(BRACKET_END, null));
				break;
			case '+'://
				addToken(new TokenImpl(
						status == Status.STATUS_EXPRESSION ? OP_ADD : OP_POS,
						null));
				break;
			case '-':
				addToken(new TokenImpl(
						status == Status.STATUS_EXPRESSION ? OP_SUB : OP_NEG,
						null));
				break;
			case ',':// :(object_setter is skiped,',' should
				// be skip)
				if (!isMapMethod()) {
					addToken(new TokenImpl(OP_PARAM_JOIN, null));

				}
				break;
			case '/':
				char next = value.charAt(start);
				if (next == '/' || next == '*') {
					start--;
					skipComment();
					break;
				} else if (this.status != Status.STATUS_EXPRESSION) {
					int end = findRegExp(this.value, this.start);
					if (end > 0) {
						String regexp = this.value.substring(this.start - 1,
								end);
						HashMap<String, String> value = new HashMap<String, String>();
						value.put("class", "RegExp");
						value.put("source", regexp);
						this.addToken(new TokenImpl(VALUE_CONSTANTS, value));
						this.start = end;
						break;
					}
				}
				addToken(new TokenImpl(TOKEN_MAP.get(op), null));
				break;
			default:
				addToken(new TokenImpl(TOKEN_MAP.get(op), null));
			}
		} else {
			addToken(new TokenImpl(TOKEN_MAP.get(op), null));
		}

	}

	private void addToken(TokenImpl token) {
		switch (token.getType()) {
		case BRACKET_BEGIN:
			status = Status.STATUS_BEGIN;
			break;
		case VALUE_CONSTANTS:
		case VALUE_VAR:
		case BRACKET_END:
			status = Status.STATUS_EXPRESSION;
			break;
		default:
			status = Status.STATUS_OPERATOR;
			break;
		}
		// previousType2 = previousType;
		previousType = token.getType();
		tokens.add(token);
	}

	private void addKeyOrObject(Object object, boolean isVar) {
		if (skipSpace(':') && isMapMethod()) {// object key
			addToken(new TokenImpl(OP_MAP_PUSH, object));
			this.start++;// skip :
		} else if (isVar) {
			addToken(new TokenImpl(VALUE_VAR, object));
		} else {
			addToken(new TokenImpl(VALUE_CONSTANTS, object));
		}
	}

	private void addList() {
		addToken(new TokenImpl(BRACKET_BEGIN, null));
		addToken(new TokenImpl(VALUE_NEW_LIST, null));
		if (!skipSpace(']')) {
			addToken(new TokenImpl(OP_PARAM_JOIN, null));
		}
	}

	private void addMap() {
		addToken(new TokenImpl(BRACKET_BEGIN, null));
		addToken(new TokenImpl(VALUE_NEW_MAP, null));
	}

	int findRegExp(String text, int start) {
		int depth = 0;
		int end = text.length();
		char c;
		while (start < end) {
			c = text.charAt(start++);
			if (c == '[') {
				depth = 1;
			} else if (c == ']') {
				depth = 0;
			} else if (c == '\\') {
				start++;
			} else if (depth == 0 && c == '/') {
				while (start < end) {
					c = text.charAt(start++);
					switch (c) {
					case 'g':
					case 'i':
					case 'm':
						break;
					default:
						return start - 1;
					}
				}

			}
		}
		return -1;
	}

}
