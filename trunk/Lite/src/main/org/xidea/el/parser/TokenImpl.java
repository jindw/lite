package org.xidea.el.parser;

import java.util.AbstractList;
import java.util.List;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.ExpressionToken;

public class TokenImpl extends AbstractList<Object> implements ExpressionToken {
	private int type;
	private ExpressionToken left;
	private ExpressionToken right;
	private Object param;

	public TokenImpl(int type, Object param) {
		this.type = type;
		this.param = param;
	}

	public int getType() {
		return type;
	}

	public ExpressionToken getLeft() {
		return left;
	}

	public ExpressionToken getRight() {
		return right;
	}

	public Object getParam() {
		return param;
	}

	public void setLeft(ExpressionToken left) {
		this.left = left;
	}

	public void setRight(ExpressionToken right) {
		this.right = right;
	}

	public void setParam(Object param) {
		this.param = param;
	}

	public String toString() {
		switch (type) {
		case VALUE_CONSTANTS:
			return "#" + getParam();
		case VALUE_VAR:
			return "$" + getParam();
		case VALUE_NEW_LIST:
			return "[]";
		case VALUE_NEW_MAP:
			return "{}";
		default:
			for (int i = 0; i < OP_LIST.length; i += 2) {
				if (type == ((Integer) OP_LIST[i]).intValue()) {
					String text = (String) OP_LIST[i + 1];
					return text;
				}
			}
		}
		return "?" + type;
	}

	@SuppressWarnings("unchecked")
	public static ExpressionToken toToken(List<Object> tokens) {
		if (tokens == null) {
			return null;
		} else {
			int type = ((Number) tokens.get(0)).intValue();
			TokenImpl impl = new TokenImpl(type, null);
			switch (tokens.size()) {
			case 4:
				impl.setParam(tokens.get(3));
			case 3:
				impl.setRight(toToken((List<Object>) tokens.get(2)));
			case 2:
				if(type>0){
					impl.setLeft(toToken((List<Object>) tokens.get(1)));
				}else{
					impl.setParam(tokens.get(1));
				}
			case 1:
				break;
			default:
				throw new ExpressionSyntaxException("tokens 長度最大為4");
			}
			
			return impl;
		}
	}

	private static Object[] OP_LIST = { OP_ADD, "+", OP_SUB, "-", OP_MUL, "*",
			OP_DIV, "/", OP_MOD,
			"%",// +-*/%
			OP_LT, "<", OP_GT, ">", OP_LTEQ, "<=", OP_GTEQ, ">=", OP_EQ,
			"==",// relative
			OP_NOTEQ, "!=", OP_NOT, "!", OP_AND, "&&", OP_OR,
			"||",// boolean
			OP_QUESTION, "?", OP_QUESTION_SELECT,
			":",// 3op
			OP_POS, "+",
			OP_NEG,
			"-",// +-
			// BRACKET_BEGIN, "(", BRACKET_END,
			// ")", // group
			VALUE_NEW_LIST, "[", VALUE_NEW_MAP, "{", OP_MAP_PUSH, ":",
			OP_PARAM_JOIN, ",",// map list,
			OP_GET_PROP, ".",// prop
			OP_INVOKE_METHOD, "#()" // , OP_GET_GLOBAL_METHOD, "#"//method call

	};

	public static int findType(String op) {
		for (int i = 1; i < OP_LIST.length; i += 2) {
			if (op.equals(OP_LIST[i])) {
				return ((Integer) OP_LIST[i - 1]).intValue();
			}
		}
		return -1;
	}

	@Override
	public Object get(int index) {
		if (index == 0) {
			return type;
		}
		if (type > 0) {
			switch (index) {
			case 3:
				return param;
			case 1:
				return left;
			case 2:
				return right;
			}
		} else if (index == 1) {
			return param;
		}
		return null;
	}

	@Override
	public int size() {
		switch (type) {
		case ExpressionToken.VALUE_NEW_LIST:
		case ExpressionToken.VALUE_NEW_MAP:
			return 1;
		case ExpressionToken.VALUE_VAR:
		case ExpressionToken.VALUE_CONSTANTS:
			return 2;
		case ExpressionToken.OP_GET_STATIC_PROP:
		case ExpressionToken.OP_INVOKE_METHOD_WITH_STATIC_PARAM:
		case ExpressionToken.OP_MAP_PUSH:
			return 4;
		default:
			return getArgCount() + 1;
		}

	}

	private int getArgCount() {
		int c = (type & ExpressionToken.BIT_PARAM) >> 6;
		return c + 1;
	}

}
