package org.xidea.el.parser;


import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.ExpressionToken;
import org.xidea.el.json.JSONEncoder;

public class TokenImpl extends AbstractList<Object> implements ExpressionToken {
	// 编译期间标记，compile time object
	static final int BRACKET_BEGIN = 0xFFFE;// ([{;
	static final int BRACKET_END = 0xFFFF;// )]};
	
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
		return LABEL_MAP.get(type) + ":" + JSONEncoder.encode(this);
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
				if (type > 0) {
					impl.setLeft(toToken((List<Object>) tokens.get(1)));
				} else {
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

	static final Map<String, Integer> TOKEN_MAP = new HashMap<String, Integer>();
	static final Map<Integer,String> LABEL_MAP = new HashMap<Integer,String>();
	static {

		for (Field f : ExpressionToken.class.getFields()) {
			try {
				Integer value = (Integer)f.get(null);
				LABEL_MAP.put(value, f.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 9
		TOKEN_MAP.put(".", OP_GET_PROP);
		// 8
		TOKEN_MAP.put("!", OP_NOT);
		TOKEN_MAP.put("^", OP_BIT_NOT);
		// TOKEN_MAP.put("+",OP_POS);
		// TOKEN_MAP.put("-",OP_NEG);
		// 7
		TOKEN_MAP.put("*", OP_MUL);
		TOKEN_MAP.put("/", OP_DIV);
		TOKEN_MAP.put("%", OP_MOD);
		// 6
		// TOKEN_MAP.put("+",OP_ADD);
		// TOKEN_MAP.put("-",OP_SUB);
		// 5
		TOKEN_MAP.put("<", OP_LT);
		TOKEN_MAP.put(">", OP_GT);
		TOKEN_MAP.put("<=", OP_LTEQ);
		TOKEN_MAP.put(">=", OP_GTEQ);
		TOKEN_MAP.put("==", OP_EQ);
		TOKEN_MAP.put("!=", OP_NOTEQ);

		// 4
		TOKEN_MAP.put("&", OP_BIT_AND);
		TOKEN_MAP.put("^", OP_BIT_XOR);
		TOKEN_MAP.put("|", OP_BIT_OR);
		// 3
		TOKEN_MAP.put("&&", OP_AND);
		TOKEN_MAP.put("||", OP_OR);
		// 2
		TOKEN_MAP.put("?", OP_QUESTION);
		TOKEN_MAP.put(":", OP_QUESTION_SELECT);// map 中的：被直接skip了
		// 1
		TOKEN_MAP.put(",", OP_PARAM_JOIN);
		for(String key : TOKEN_MAP.keySet()){
			LABEL_MAP.put(TOKEN_MAP.get(key), key);
		}
		LABEL_MAP.put(BRACKET_BEGIN, "(");
		LABEL_MAP.put(BRACKET_END, ")");

		// OP_MAP_PUSH
		// OP_INVOKE_METHOD
	}
}
