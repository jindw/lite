package org.xidea.el.impl;

import java.lang.reflect.Field;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.xidea.el.ExpressionSyntaxException;
import org.xidea.el.ExpressionToken;
import org.xidea.el.OperationStrategy;
import org.xidea.el.ValueStack;
import org.xidea.el.json.JSONEncoder;

public class TokenImpl extends AbstractList<Object> implements ExpressionToken {
	// 编译期间标记，compile time object
	static final int BRACKET_BEGIN = 0xFFFE;// ([{;
	static final int BRACKET_END = 0xFFFF;// )]};
	// 解释优化
	static final int OP_GET_STATIC_PROP = 0<<12 | 0<<8 | 0<<6 | 8<<2 | 0;
	static final int OP_INVOKE_WITH_STATIC_PARAM= 0<<12 | 0<<8 | 0<<6 | 8<<2 | 1;
//	static final int OP_INVOKE_WITH_ONE_PARAM   = 0<<12 | 1<<8 | 1<<6 | 8<<2 | 2;

	private static final Object[] EMPTY_ARGS = new Object[0];
	
	private int type;
	private TokenImpl left;
	private TokenImpl right;
	private Object param;
	String value;
	

	public TokenImpl(int type, Object param) {
		this.type = type;
		this.param = param;
	}

	public TokenImpl(String name) {
		if (TOKEN_MAP.containsKey(name)) {
			this.type = TOKEN_MAP.get(name);
		} else {
			throw new ExpressionSyntaxException("未知操作符：" + name);
		}
	}

	protected TokenImpl optimize(OperationStrategy os,Map<String, Object> context) {
		return optimize(os,context,new ValueStackImpl(context));
	}
	private TokenImpl optimize(OperationStrategy os,Map<String, Object> context,ValueStack vs) {
		if (type > 0) {
			if (left != null) {
				TokenImpl optimizedLeft = left.optimize(os, context);
				if(type == OP_INVOKE){
					if (left.getType() == OP_GET) {
						if(optimizedLeft.type != VALUE_CONSTANTS || optimizedLeft.getParam() != null){
							left = optimizedLeft;
						}
					}else{
						left = optimizedLeft;
					}
				}else{
					left = optimizedLeft;
				}
				boolean canOptimize = left.canOptimize(context);
				if (right != null) {
					right = right.optimize(os,context,vs);
					canOptimize = canOptimize && right.canOptimize(context);
				}
				if (canOptimize) {
					Object o = os.evaluate(this, vs);
					if (o == null) {

					} else if (o instanceof Number) {
						float f = ((Number) o).floatValue();
						if (Float.isNaN(f) || Float.isInfinite(f)) {// NaN
							// +-Finite不能正确系列化
							return this;
						}
					} else if (o instanceof CharSequence
							|| o instanceof Boolean) {
					} else {
						return this;
					}
					return new TokenImpl(VALUE_CONSTANTS, o);
				}
			}
		}
		if (type == OP_GET) {
			if (right.getType() == VALUE_CONSTANTS) {
				this.type = OP_GET_STATIC_PROP;
				this.setParam(right.getParam());
			}
		}else if(type == OP_INVOKE){
			if(right.getType() == VALUE_LIST){
				this.type = OP_INVOKE_WITH_STATIC_PARAM;
				this.setParam(EMPTY_ARGS);
			}else{
				TokenImpl token = this.right;//op_join
				ArrayList<Object> params = null;
				while(token.type == OP_JOIN){
					if(token.right.type == VALUE_CONSTANTS){
						if(params == null){
							params = new ArrayList<Object>();
						}
						params.add(0,token.right.getParam());
						token = token.left;
					}else{
						return this;//fail
					}
				}
				if(token.type == VALUE_LIST){
					this.type = OP_INVOKE_WITH_STATIC_PARAM;
					this.setParam(params.toArray());
				}
				
			}
		}
		return this;
	}

	private boolean canOptimize(Map<String, Object> context) {
		if(type == VALUE_CONSTANTS){
			return true;
		}else if(type == VALUE_VAR){
			return context.containsKey(this.getParam());
		}
		return false;
		// lt!=VALUE_VAR && lt!= VALUE_LIST && lt!=VALUE_MAP;
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

	public void setLeft(TokenImpl left) {
		this.left = left;
	}

	public void setRight(TokenImpl right) {
		this.right = right;
	}

	public void setParam(Object param) {
		this.param = param;
	}

	public String toString() {
		if (value != null) {
			return this.value;
		} else {
			return LABEL_MAP.get(type) + ":" + JSONEncoder.encode(this);
		}
	}

	@SuppressWarnings("unchecked")
	public static TokenImpl toToken(List<Object> tokens) {
		if (tokens == null) {
			return null;
		} else {
			int type = ((Number) tokens.get(0)).intValue();
			TokenImpl impl = new TokenImpl(type, null);

			int paramStart = getArgCount(type) + 1;
			int size = tokens.size();
			switch (Math.min(paramStart, size)) {
			case 3:
				impl.setRight(toToken((List<Object>) tokens.get(2)));
			case 2:
				impl.setLeft(toToken((List<Object>) tokens.get(1)));
			case 1:
				break;
			default:
				throw new ExpressionSyntaxException("tokens 長度最大為4");
			}
			if (paramStart < size) {
				impl.setParam(tokens.get(paramStart));
			}
			return impl;
		}
	}

	@Override
	public Object get(int index) {
		if(type == OP_GET_STATIC_PROP){
			if(index == 0){
				return OP_GET;
			}else if(index == 1){
				return left;
			}else if(index == 2){
				return right;
			}
			return null;
		}else if(type == OP_INVOKE_WITH_STATIC_PARAM){
			if(index == 0){
				return OP_INVOKE;
			}else if(index == 1){
				return left;
			}else if(index == 2){
				return right;
			}
			return null;
		}
		if (index == 0) {
			return type;
		}
		if (type > 0) {
			int paramStart = getArgCount(type) + 1;
			if (index < paramStart) {
				switch (index) {
				case 1:
					return left;
				case 2:
					return right;
				}
			}
			return param;
		} else if (index == 1) {
			return param;
		}
		return null;
	}

	@Override
	public int size() {
		if(OP_GET_STATIC_PROP == type){
			return 3;//as OP_GET
		}else if(OP_INVOKE_WITH_STATIC_PARAM == type){
			return 3;//as OP_INVOKE
		}
		int size = getArgCount(type) + 1;
		return hasParam() ? size + 1 : size;

	}

	private boolean hasParam() {
		switch (type) {
		case ExpressionToken.VALUE_VAR:// 0
		case ExpressionToken.VALUE_CONSTANTS:// 0
		case OP_GET_STATIC_PROP:// 1
		case OP_INVOKE_WITH_STATIC_PARAM:// 1
//			 case OP_INVOKE_WITH_ONE_PARAM:// 2
		case ExpressionToken.OP_PUSH:// 1
			return true;
		default:
			return false;
		}
	}

	static int getArgCount(int type) {
		if (type < 0) {
			return 0;
		}
		int c = (type & ExpressionToken.BIT_ARGS) >> 6;
		return c + 1;
	}

	/**
	 * 只能收录没有歧异的操作符
	 */
	private static final Map<String, Integer> TOKEN_MAP = new HashMap<String, Integer>();
	private static final Map<Integer, String> LABEL_MAP = new HashMap<Integer, String>();
	static {

		for (Field f : ExpressionToken.class.getFields()) {
			try {
				Integer value = (Integer) f.get(null);
				LABEL_MAP.put(value, f.getName());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		// 9
		TOKEN_MAP.put(".", OP_GET);
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
		TOKEN_MAP.put("!=", OP_NE);
		TOKEN_MAP.put("===", OP_EQ_STRICT);
		TOKEN_MAP.put("!==", OP_NE_STRICT);
		

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
		TOKEN_MAP.put(",", OP_JOIN);
		for (String key : TOKEN_MAP.keySet()) {
			LABEL_MAP.put(TOKEN_MAP.get(key), key);
		}
		LABEL_MAP.put(BRACKET_BEGIN, "(");
		LABEL_MAP.put(BRACKET_END, ")");

		// OP_PUSH
		// OP_INVOKE
	}

	public static boolean isPrefix(int type) {
		// TODO:
		return getArgCount(type) == 1;
	}
}
