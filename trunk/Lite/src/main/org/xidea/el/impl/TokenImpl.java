package org.xidea.el.impl;

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
	static final int OP_GET_STATIC = 0 << 12 | 0 << 8 | 0 << 6 | 8 << 2 | 0;
	static final int OP_INVOKE_WITH_STATIC_PARAM = 0 << 12 | 0 << 8 | 0 << 6
			| 8 << 2 | 1;
	// static final int OP_INVOKE_WITH_ONE_PARAM = 0<<12 | 1<<8 | 1<<6 | 8<<2 |
	// 2;

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

	public TokenImpl optimize(OperationStrategy os,
			Map<String, Object> context) {
		return optimize(os, context, new OptimizeStack(context));
	}

	private TokenImpl optimize(OperationStrategy os,
			Map<String, Object> context, ValueStack vs) {
		if (type > 0) {
			boolean childOptimized = false;
			if (type == OP_INVOKE
					|| type == TokenImpl.OP_INVOKE_WITH_STATIC_PARAM) {
				int leftType = left.getType();
				if (leftType == OP_GET || leftType == TokenImpl.OP_GET_STATIC) {
					left.left = left.left.optimize(os, context, vs);
					left.right = left.right.optimize(os, context, vs);
					if (right != null) {
						right = right.optimize(os, context, vs);
						//this.optimize();// OP_INVOKE_WITH_STATIC_PARAM
						// reoptimize
					}
					childOptimized = true;
				}
			}
			try {
				Object o = os.evaluate(this, vs);
				if (o == null || o instanceof Number
						|| o instanceof CharSequence || o instanceof Boolean) {
					return new TokenImpl(VALUE_CONSTANTS, o);
				}
			} catch (Exception e) {
			}
			if (!childOptimized) {
				if (left != null) {
					left = left.optimize(os, context, vs);
					if (right != null) {
						right = right.optimize(os, context, vs);
					}
				}
			}
			this.optimize();
		}
		return this;
	}

	private void optimize() {
		if (type == OP_GET) {
			if (right.getType() == VALUE_CONSTANTS) {
				this.type = OP_GET_STATIC;
				this.setParam(right.getParam());
			}
		} else if (type == OP_INVOKE) {
			if (right.getType() == VALUE_LIST) {
				this.type = OP_INVOKE_WITH_STATIC_PARAM;
				this.setParam(EMPTY_ARGS);
			} else {
				TokenImpl token = this.right;// op_join
				ArrayList<Object> params = null;
				while (token.type == OP_JOIN) {
					if (token.right.type == VALUE_CONSTANTS) {
						if (params == null) {
							params = new ArrayList<Object>();
						}
						params.add(0, token.right.getParam());
						token = token.left;
					} else {
						return;// fail
					}
				}
				if (token.type == VALUE_LIST) {
					this.type = OP_INVOKE_WITH_STATIC_PARAM;
					this.setParam(params.toArray());
				}

			}
		}
	}

	//
	// private boolean canOptimize(Map<String, Object> context) {
	// if (type == VALUE_CONSTANTS) {
	// return true;
	// } else if (type == VALUE_VAR) {
	// return context.containsKey(this.getParam());
	// }
	// return false;
	// // lt!=VALUE_VAR && lt!= VALUE_LIST && lt!=VALUE_MAP;
	// }

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
		if (type == OP_GET_STATIC) {
			if (index == 0) {
				return OP_GET;
			} else if (index == 1) {
				return left;
			} else if (index == 2) {
				return right;
			}
			return null;
		} else if (type == OP_INVOKE_WITH_STATIC_PARAM) {
			if (index == 0) {
				return OP_INVOKE;
			} else if (index == 1) {
				return left;
			} else if (index == 2) {
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
		if (OP_GET_STATIC == type) {
			return 3;// as OP_GET
		} else if (OP_INVOKE_WITH_STATIC_PARAM == type) {
			return 3;// as OP_INVOKE
		}
		int size = getArgCount(type) + 1;
		return hasParam() ? size + 1 : size;

	}

	private boolean hasParam() {
		switch (type) {
		case ExpressionToken.VALUE_VAR:// 0
		case ExpressionToken.VALUE_CONSTANTS:// 0
		case OP_GET_STATIC:// 1
		case OP_INVOKE_WITH_STATIC_PARAM:// 1
			// case OP_INVOKE_WITH_ONE_PARAM:// 2
		case ExpressionToken.OP_PUT:// 1
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
	private static void addToken(int type,String op){
		TOKEN_MAP.put(op, type);
		LABEL_MAP.put(type,op);
	}
	static {
//		public static final int ([\w_]+\s*)=[^/]+//(.*)
		
		addToken(VALUE_CONSTANTS ,"value");
		addToken(VALUE_VAR       ,"var");
		addToken(VALUE_LIST      ,"[]");
		addToken(VALUE_MAP       ,"{}");
		
		
		//九：（最高级别的运算符号）
		addToken(OP_GET      ,".[]");
		addToken(OP_INVOKE   ,"()");
		
		//八
		addToken(OP_NOT     ,"!");
		addToken(OP_BIT_NOT ,"~");
		addToken(OP_POS     ,"+");
		addToken(OP_NEG     ,"-");
		
		//七：
		addToken(OP_MUL ,"*");
		addToken(OP_DIV ,"/");
		addToken(OP_MOD ,"%");
		
		//六：
		//与正负符号共享了字面值
		addToken(OP_ADD ,"+");
		addToken(OP_SUB ,"-");
		
		//五:移位
		addToken(OP_LSH   ,"<<");
		addToken(OP_RSH   ,">>");
		addToken(OP_URSH   ,">>>");
		
		//四:比较
		addToken(OP_LT   ,"<");
		addToken(OP_GT   ,">");
		addToken(OP_LTEQ ,"<=");
		addToken(OP_GTEQ ,">=");
		addToken(OP_IN   ," in ");
		
		//四:等不等比较
		addToken(OP_EQ        ,"==");
		addToken(OP_NE        ,"!=");
		addToken(OP_EQ_STRICT ,"===");
		addToken(OP_NE_STRICT ,"!==");
		
		//三:按位与或
		addToken(OP_BIT_AND ,"&");
		addToken(OP_BIT_XOR ,"^");
		addToken(OP_BIT_OR  ,"|");
		//三:与或
		addToken(OP_AND ,"&&");
		addToken(OP_OR  ,"||");

		//二：
		//?;
		addToken(OP_QUESTION        ,"?");
		//:;
		addToken(OP_QUESTION_SELECT ,":");
		
		//一：
		//与Map Join 共享字面量（map join 会忽略）
		addToken(OP_JOIN   ,",");
		//与三元运算符共享字面值
		addToken(OP_PUT   ,":");
		addToken(BRACKET_BEGIN   ,"(");
		addToken(BRACKET_END   ,")");
	}

	public static boolean isPrefix(int type) {
		// TODO:
		return getArgCount(type) == 1;
	}
}
class OptimizeStack extends ValueStackImpl{
	OptimizeStack(Map<String,Object>context) {
		super(context);
	}
	@Override
	protected Object fallback(Object key) {
		throw new RuntimeException();
	}
}
