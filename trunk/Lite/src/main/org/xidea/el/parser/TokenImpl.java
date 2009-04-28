package org.xidea.el.parser;

import org.xidea.el.ExpressionToken;

public class TokenImpl implements ExpressionToken {

	private final static ExpressionToken TOKEN_TRUE = new TokenImpl(
			VALUE_CONSTANTS, Boolean.TRUE);
	private final static ExpressionToken TOKEN_FALSE = new TokenImpl(
			VALUE_CONSTANTS, Boolean.FALSE);
	private final static ExpressionToken TOKEN_NULL = new TokenImpl(
			VALUE_CONSTANTS, null);

	protected static ExpressionToken getConstainsToken(String key) {
		if ("true".equals(key)) {
			return TOKEN_TRUE;
		} else if ("false".equals(key)) {
			return TOKEN_FALSE;
		} else if ("null".equals(key)) {
			return TOKEN_NULL;
		}
		return null;
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
			OP_POS, "+", OP_NEG,
			"-",// +-
			BRACKET_BEGIN, "(", BRACKET_END,
			")", // group
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

	private int type;
	private Object param;

	public TokenImpl(int type, Object param) {
		this.type = type;
		this.param = param;
	}

	public int getType() {
		return type;
	}

	public Object getParam() {
		return param;
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
		case VALUE_LAZY:
			return "[" + getParam()+"]";
		case VALUE_NEW_LIST:
			return "[]";
		case VALUE_NEW_MAP:
			return "{}" ;
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

}
