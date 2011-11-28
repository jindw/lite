package org.xidea.el.impl;

import static org.xidea.el.impl.TokenImpl.BRACKET_BEGIN;
import static org.xidea.el.impl.TokenImpl.BRACKET_END;
import static org.xidea.el.ExpressionToken.OP_ADD;
import static org.xidea.el.ExpressionToken.OP_IN;
import static org.xidea.el.ExpressionToken.BIT_ARGS;
import static org.xidea.el.ExpressionToken.BIT_PRIORITY;
import static org.xidea.el.ExpressionToken.BIT_PRIORITY_SUB;
import static org.xidea.el.ExpressionToken.OP_GET;

import static org.xidea.el.ExpressionToken.OP_INVOKE;
import static org.xidea.el.ExpressionToken.OP_PUT;
import static org.xidea.el.ExpressionToken.OP_NEG;
import static org.xidea.el.ExpressionToken.OP_JOIN;
import static org.xidea.el.ExpressionToken.OP_POS;
import static org.xidea.el.ExpressionToken.OP_SUB;
import static org.xidea.el.ExpressionToken.OP_QUESTION;
import static org.xidea.el.ExpressionToken.OP_QUESTION_SELECT;
import static org.xidea.el.ExpressionToken.VALUE_CONSTANTS;
import static org.xidea.el.ExpressionToken.VALUE_LIST;
import static org.xidea.el.ExpressionToken.VALUE_MAP;
import static org.xidea.el.ExpressionToken.VALUE_VAR;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.LinkedList;
import java.util.List;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.json.JSONTokenizer;
/**
 * 首次遍历的时候，不支持后缀运算，单参数表达式只能前缀。
 */
public class ExpressionParser extends JSONTokenizer {
	private final static TokenImpl TOKEN_TRUE = new TokenImpl(VALUE_CONSTANTS,
			Boolean.TRUE);
	private final static TokenImpl TOKEN_FALSE = new TokenImpl(VALUE_CONSTANTS,
			Boolean.FALSE);
	private final static TokenImpl TOKEN_NULL = new TokenImpl(VALUE_CONSTANTS,
			null);


	private ParseStatus status = ParseStatus.BEGIN;
	private int previousType = Integer.MIN_VALUE;
	private Map<String, Integer> aliasMap = Collections.emptyMap();

	protected ArrayList<TokenImpl> tokens = new ArrayList<TokenImpl>();
	protected TokenImpl expression;
	private int depth;

	public ExpressionParser(String value) {
		super(value,false);
	}


	public void setAliasMap(Map<String, Integer> aliasMap) {
		this.aliasMap = aliasMap;
	}
//	this.aliasMap = aliasMap;
	public TokenImpl parseEL() {
		skipSpace(0);
		while (start < end) {
			char c = toLower(value.charAt(start));
			if (c == '"' || c == '\'') {
				String text = findString();
				addKeyOrObject(text, false);
			} else if (c >= '0' && c <= '9') {
				Number number = findNumber();
				addKeyOrObject(number, false);
			} else if (Character.isJavaIdentifierStart(c)) {
				String id = findId();
				addId(id);
			} else {
				String op = findOperator();
				addOperator(op);
			}
			skipSpace(0);
		}
		

		if(depth!=0){
			this.fail("表达式括弧不匹配");
		}
		prepareSelect();
		LinkedList<TokenImpl> stack = new LinkedList<TokenImpl>();
		try {
			toTree(right(this.tokens), stack);
		} catch (Exception e) {
			this.fail("逆波兰式树型化异常");
			//throw new ExpressionSyntaxException("逆波兰式树型化异常",e);
		}
		if (stack.size() != 1) {
			this.fail("表达式语法错误");
		}
		this.expression = stack.getFirst();
		expression.value = this.value;
		return expression;
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

	private void toTree(List<TokenImpl> tokens, LinkedList<TokenImpl> stack) {
		for (final TokenImpl item : tokens) {
			int type = item.getType();
			switch (type) {
			case VALUE_CONSTANTS:
			case VALUE_VAR:
			case VALUE_LIST:
			case VALUE_MAP:
				stack.addFirst(item);
				break;
			default:// OP
				if ((type & BIT_ARGS) > 0) {// 两个操作数
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
	private List<TokenImpl> right(List<TokenImpl> tokens) {
		LinkedList<List<TokenImpl>> rightStack = new LinkedList<List<TokenImpl>>();
		rightStack.addFirst(new ArrayList<TokenImpl>()); // 存储右序表达式

		LinkedList<TokenImpl> buffer = new LinkedList<TokenImpl>();

		for (final TokenImpl item:tokens) {
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
		return rightStack.getFirst();
	}

	private void addRightToken(LinkedList<List<TokenImpl>> rightStack,
			TokenImpl token) {
		List<TokenImpl> list = rightStack.getFirst();
		list.add(token);
	}

	protected int getPriority(int type) {
		switch (type) {
		case BRACKET_BEGIN:
		case BRACKET_END:
			return Integer.MIN_VALUE;
		default:
			return (type & BIT_PRIORITY) << 4 | (type & BIT_PRIORITY_SUB) >> 8;
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
		// 1    ?     0?5:0 ? 11 : 13    :    3 ==>13 //1?((0?5:0)?11:13):3
		if(p1 == p2){
			if(TokenImpl.isPrefix(t2)){
				return false;
			}
		}
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

	private void addToken(TokenImpl token) {
		int type = token.getType();
		//TODO: 疑问注释：invoke 在invoke解析时处理纠正歧异
		if (type == BRACKET_BEGIN || type < 0) {
			replacePrevious();
		}
		if(type == VALUE_VAR){
			Object id = token.getParam();
			Integer op = aliasMap.get(id);
			if(op == null &&  "in".equals(id)){
				op = OP_IN;
			}
			if(op!=null){
				int c = TokenImpl.getArgCount(op);
				if(c == 2 && status == ParseStatus.EXPRESSION
						||c == 1 &&status != ParseStatus.EXPRESSION){
					token = new TokenImpl(op,null);
				}
			}
		}
		
		switch (token.getType()) {
		case BRACKET_BEGIN:
			depth++;
			status = ParseStatus.BEGIN;
			break;
		case BRACKET_END:
			depth--;
			if(depth<0){
				fail("括弧异常");
			}
		case VALUE_CONSTANTS:
		case VALUE_VAR:
		case VALUE_LIST:
		case VALUE_MAP:
			status = ParseStatus.EXPRESSION;
			break;
		default:
			status = ParseStatus.OPERATOR;
			break;
		}
		// previousType2 = previousType;
		previousType = type;
		tokens.add(token);
	}

	private void replacePrevious() {
		int last = tokens.size()-1;
		if(previousType == VALUE_VAR && last>=0){
			TokenImpl lt = tokens.get(last);
			Integer op = aliasMap.get(lt.getParam());
			if(op!=null){
				tokens.set(last, new TokenImpl(op,null));
				status = ParseStatus.OPERATOR;
				previousType = op;
			}
		}
	}
	private void addId(String id) {
		if ("true".equals(id)) {
			addToken(TOKEN_TRUE);
		} else if ("false".equals(id)) {
			addToken(TOKEN_FALSE);
		} else if ("null".equals(id)) {
			addToken(TOKEN_NULL);
		} else {
			skipSpace(0);
			if (previousType == OP_GET) {
				addToken(new TokenImpl(VALUE_CONSTANTS, id));
			} else {
				addKeyOrObject(id, true);
			}
		}
	}

	private String findOperator() {// optimize json ,:[{}]
		int end = start + 1;
		final char c = toLower(value.charAt(start));
		final char next = value.length() > end ? 
				toLower(value.charAt(end))
				: 0;
		
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
			if (next != '=') {
				this.fail("不支持赋值操作:");
			}
		case '!':// !,!=
			if (next == '=') {
				end++;
				if (value.length() > end && toLower(value.charAt(end)) == '=') {
					end++;
					//this.fail("不支持=== 和!==操作符，请使用==,!=");
				}
			}
			break;
		case '>':// >,>=
		case '<':// <,<=
			if (next == '=') {
				end++;
			}else if(next == c){
				end ++;
				if (value.length() > end && toLower(value.charAt(end)) == c) {
					end++;
				}
				
			}
			break;
		case '&':// && / &
		case '|':// || /|
			if ((c == next)) {
				end++;
			}
			break;
		default:
//			String v = this.aliasMap.get(""+c+next);
//			if(v!=null){
//				return v;
//			}
//			return this.aliasMap.get(""+c);
			return null;
		}

		return toLower(value.substring(start, start = end));
	}


	private String toLower(String c) {
		char[] cs = c.toCharArray();
		for (int i = 0; i < cs.length; i++) {
			cs[i] = toLower(cs[i]);
		}
		return new String(cs);
		
	}

	private void fail(String msg) {
		throw new ExpressionSyntaxException(msg + "\n@" + start + "\n"
				+ value.substring(start) + "\n----\n" + value);
	}

	private void addOperator(String op) {
		if (op == null) {
			this.fail("未知操作符:");
		}
		if (op.length() == 1) {
			switch (op.charAt(0)) {
			case '(':
				replacePrevious();
				if (status == ParseStatus.EXPRESSION) {
					addToken(new TokenImpl(OP_INVOKE, null));
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
				if (status == ParseStatus.EXPRESSION) {// getProperty
					addToken(new TokenImpl(OP_GET, null));
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
						status == ParseStatus.EXPRESSION ? OP_ADD : OP_POS,
						null));
				break;
			case '-':
				addToken(new TokenImpl(
						status == ParseStatus.EXPRESSION ? OP_SUB : OP_NEG,
						null));
				break;
			case ':':
				addToken(new TokenImpl(OP_QUESTION_SELECT,null));// map : is skipped
				break;
			case ',':// :(object_setter is skipped,',' should
				// be skip)
				if (isMapMethod()) {
					status = ParseStatus.OPERATOR;
				}else{
					addToken(new TokenImpl(OP_JOIN, null));
				}
				break;
			case '/':
				char next = value.charAt(start);
				if (next == '/' || next == '*') {
					start--;
					skipComment();
					break;
				} else if (this.status != ParseStatus.EXPRESSION) {
					int end = findRegExp(this.value, this.start);
					if (end > 0) {
						String regexp = this.value.substring(this.start - 1,
								end);
						Map<String, Object> value = new HashMap<String, Object>();
//						int p = regexp.lastIndexOf('/');
//						String source = regexp.substring(1,p);
						value.put("class", "RegExp");
						value.put("literal", regexp);
//						value.put("source", source);
//						value.put("multiline", regexp.indexOf('m',p));
//						value.put("global", regexp.indexOf('g',p));
//						value.put("ignoreCase", regexp.indexOf('i',p));
						this.addToken(new TokenImpl(VALUE_CONSTANTS, value));
						this.start = end;
						break;
					}
				}
				addToken(new TokenImpl(op));
				break;
			default:
				addToken(new TokenImpl(op));
			}
		} else {
			addToken(new TokenImpl(op));
		}
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
				if (type == OP_PUT || type == VALUE_MAP) {// (
					// <#newMap>
					// <#push>
					return true;
				} else if (type == OP_JOIN) {// (
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

	private void addKeyOrObject(Object object, boolean isVar) {
		if (skipSpace(':') && isMapMethod()) {// object key
			addToken(new TokenImpl(OP_PUT, object));
			this.start++;// skip :
		} else if (isVar) {
			addToken(new TokenImpl(VALUE_VAR, object));
		} else {
			addToken(new TokenImpl(VALUE_CONSTANTS, object));
		}
	}

	private void addList() {
		addToken(new TokenImpl(BRACKET_BEGIN, null));
		addToken(new TokenImpl(VALUE_LIST, null));
		if (!skipSpace(']')) {
			addToken(new TokenImpl(OP_JOIN, null));
		}
	}

	private void addMap() {
		addToken(new TokenImpl(BRACKET_BEGIN, null));
		addToken(new TokenImpl(VALUE_MAP, null));
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
enum ParseStatus {
	BEGIN, EXPRESSION, OPERATOR
}

