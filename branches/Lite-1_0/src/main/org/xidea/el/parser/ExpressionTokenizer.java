package org.xidea.el.parser;

import static org.xidea.el.ExpressionToken.OP_ADD;
import static org.xidea.el.ExpressionToken.OP_AND;
import static org.xidea.el.ExpressionToken.OP_GET_PROP;
import static org.xidea.el.ExpressionToken.OP_INVOKE_METHOD;
import static org.xidea.el.ExpressionToken.OP_MAP_PUSH;
import static org.xidea.el.ExpressionToken.OP_NEG;
import static org.xidea.el.ExpressionToken.OP_OR;
import static org.xidea.el.ExpressionToken.OP_PARAM_JOIN;
import static org.xidea.el.ExpressionToken.OP_POS;
import static org.xidea.el.ExpressionToken.OP_QUESTION;
import static org.xidea.el.ExpressionToken.OP_QUESTION_SELECT;
import static org.xidea.el.ExpressionToken.OP_STATIC_GET_PROP;
import static org.xidea.el.ExpressionToken.OP_SUB;
import static org.xidea.el.ExpressionToken.VALUE_CONSTANTS;
import static org.xidea.el.ExpressionToken.VALUE_LAZY;
import static org.xidea.el.ExpressionToken.VALUE_NEW_LIST;
import static org.xidea.el.ExpressionToken.VALUE_NEW_MAP;
import static org.xidea.el.ExpressionToken.VALUE_VAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.ExpressionToken;
import org.xidea.el.fn.NumberArithmetic;
import org.xidea.el.json.JSONTokenizer;

public class ExpressionTokenizer extends JSONTokenizer {

	//编译期间标记，compile time object
	static final int BRACKET_BEGIN = 0xFFFE;//([{;
	static final int BRACKET_END = 0xFFFF;//)]};
	
	private static enum Status {
		STATUS_BEGIN, STATUS_EXPRESSION, STATUS_OPERATOR
	}
	private static NumberArithmetic na = new NumberArithmetic();

	private Status status = Status.STATUS_BEGIN;
	private int previousType = Integer.MIN_VALUE;

	protected ArrayList<ExpressionToken> tokens = new ArrayList<ExpressionToken>();
	protected List<ExpressionToken> expressions;

	public ExpressionTokenizer(String value) {
		super(value);
		parseEL();
		this.expressions = right(this.tokens.iterator());
	}

	private int getPriority(int type) {
		switch (type) {
		case BRACKET_BEGIN:
		case BRACKET_END:
			return Integer.MIN_VALUE;
		default:
			return type & 30;
		}
	}

	private boolean rightEnd(ExpressionToken item, ExpressionToken privious) {
		int t1 = privious.getType();
		int t2 = item.getType();
		int p1 = getPriority(t1);
		int p2 = getPriority(t2);
		//1?1:3 + 0?5:7 ==>1
		//1?0?5:7:3 ==>7
		//1?0?5:0?11:13:3 ==>13
		if(p2 <= p1){
			if(p2 == p1){
				if(t2 == OP_QUESTION_SELECT){
					return t1 == OP_QUESTION;
				}else if(t2 == OP_QUESTION){
					return t1 != OP_QUESTION_SELECT;
				}
			}
			return true;
		}else{
			return false;
		}
	}

	// 将中序表达式转换为右序表达式
	private List<ExpressionToken> right(Iterator<ExpressionToken> tokens) {
		LinkedList<List<ExpressionToken>> rightStack = new LinkedList<List<ExpressionToken>>();
		rightStack.addFirst(new ArrayList<ExpressionToken>()); // 存储右序表达式

		LinkedList<ExpressionToken> buffer = new LinkedList<ExpressionToken>();

		while (tokens.hasNext()) {
			final ExpressionToken item = tokens.next();
			if (item.getType() > 0) {
				if (buffer.isEmpty()) {
					buffer.addFirst(item);
				} else if (item.getType() == BRACKET_BEGIN) {// ("(")
					buffer.addFirst(item);
				} else if (item.getType() == BRACKET_END) {// .equals(")"))
					while (true) {
						ExpressionToken operator = buffer.removeFirst();
						if (operator.getType() == BRACKET_BEGIN) {
							break;
						}
						addRightOperator(rightStack, operator);
					}
				} else {
					while (!buffer.isEmpty()
							&& rightEnd(item, buffer.getFirst())) {
						ExpressionToken operator = buffer.removeFirst();
						// if (operator.getType() !=
						// BRACKET_BEGIN){
						addRightOperator(rightStack, operator);
					}
					buffer.addFirst(item);
				}
			} else {// lazy begin value exp
				addRightToken(rightStack, item);
			}
		}
		while (!buffer.isEmpty()) {
			ExpressionToken operator = buffer.removeFirst();
			addRightOperator(rightStack, operator);
		}
		return rightStack.getFirst();
	}

	private void addRightOperator(LinkedList<List<ExpressionToken>> rightStack,
			ExpressionToken operator) {
		switch (operator.getType()) {
		case OP_OR:
		case OP_AND:
		case OP_QUESTION:
		case OP_QUESTION_SELECT:
			List<ExpressionToken> children = rightStack.removeFirst();
			List<ExpressionToken> list = rightStack.getFirst();
			if (children.size() == 1) {
				list.set(list.size() - 1, children.get(0));
				break;
			}
			if (children.size() == 2) {
				ExpressionToken t1 = children.get(0);
				ExpressionToken t2 = children.get(1);
				if (t2.getType() == VALUE_CONSTANTS
						&& t2.getParam() instanceof Number) {
					if (t1.getType() == OP_POS) {
						list.set(list.size() - 1, t2);
						break;
					} else if (t1.getType() == OP_NEG) {
						t2 = new TokenImpl(OP_NEG, na.subtract(0,(Number)t2.getParam()));
						list.set(list.size() - 1, t2);
						break;
					}
				}
			}
			ExpressionToken token = (ExpressionToken) list.get(list.size() - 1);
			((TokenImpl) token).setParam(reverseArray(children));

		}
		addRightToken(rightStack, operator);
	}

	private void addRightToken(LinkedList<List<ExpressionToken>> rightStack,
			ExpressionToken token) {
		List<ExpressionToken> list = rightStack.getFirst();
		if (token.getType() == OP_GET_PROP) {
			int last = list.size() - 1;
			if (last >= 0) {
				ExpressionToken previous = list.get(last);
				if (previous.getType() == VALUE_CONSTANTS) {
					list.remove(last);
					token = new TokenImpl(OP_STATIC_GET_PROP, previous
							.getParam());
				}
			}
		} else if (token.getType() == VALUE_LAZY) {
			rightStack.addFirst(new ArrayList<ExpressionToken>());
		}
		list.add(token);
	}

	private ExpressionToken[] reverseArray(List<ExpressionToken> list) {
		ExpressionToken[] expression = new ExpressionToken[list.size()];
		int i = expression.length - 1;
		for (ExpressionToken expressionToken : list) {
			expression[i--] = expressionToken;
		}
		return expression;
	}

	public Tokens getTokens() {
		return new Tokens(reverseArray(expressions));
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
				ExpressionToken constains = TokenImpl.getConstainsToken(id);
				if (constains == null) {
					skipSpace(0);
					if (previousType == OP_GET_PROP) {
						addToken(new TokenImpl(VALUE_CONSTANTS, id));
					} else {
						addKeyOrObject(id, true);
					}
				} else {
					addToken(constains);
				}
			} else {
				String op = findOperator();
				// if (value.startsWith(op, start))
				if (op != null) {
					parseOperator(op);
				} else {
					throw new ExpressionSyntaxException("语法错误:" + value + "@"
							+ start);
				}
			}
			skipSpace(0);
		}
	}

	private String findOperator() {// optimize json ,:[{}]
		switch (value.charAt(start)) {
		case '!':// !,!=
		case '>':// >,>=
		case '<':// <,<=
			if (value.charAt(start + 1) == '=') {
				return value.substring(start, start += 2);
			}
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
		case '*':
		case '/':
		case '%':
			return value.substring(start, start += 1);

		case '=':// ==
		case '&':// &&
		case '|':// ||
			assert (value.charAt(start) == value.charAt(start + 1));
			return value.substring(start, start += 2);
		}
		return null;
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
			ExpressionToken token = tokens.get(i);
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
				// addToken(OperatorToken.getToken(SKIP_AND));
				break;
			case '-':
				addToken(new TokenImpl(
						status == Status.STATUS_EXPRESSION ? OP_SUB : OP_NEG,
						null));
				// addToken(OperatorToken.getToken(SKIP_AND));
				break;
			case '?':// ?:
				addToken(new TokenImpl(OP_QUESTION, null));
				// addToken(OperatorToken.getToken(SKIP_QUESTION));
				addToken(new TokenImpl(VALUE_LAZY, null));
				break;
			case ':':// :(object_setter is skiped)
				addToken(new TokenImpl(OP_QUESTION_SELECT, null));
				addToken(new TokenImpl(VALUE_LAZY, null));
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
				}
			default:
				addToken(new TokenImpl(TokenImpl.findType(op), null));
			}
		} else if (op.equals("||")) { // ||
			addToken(new TokenImpl(OP_OR, null));
			addToken(new TokenImpl(VALUE_LAZY, null));
			// addToken(LazyToken.LAZY_TOKEN_END);
		} else if (op.equals("&&")) {// &&
			addToken(new TokenImpl(OP_AND, null));
			addToken(new TokenImpl(VALUE_LAZY, null));
			// addToken(OperatorToken.getToken(SKIP_AND));
		} else {
			addToken(new TokenImpl(TokenImpl.findType(op), null));
		}

	}

	private void addToken(ExpressionToken token) {
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
}
