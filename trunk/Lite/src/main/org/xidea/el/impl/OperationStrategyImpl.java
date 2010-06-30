package org.xidea.el.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.OperationStrategy;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Invocable;
import org.xidea.el.Reference;
import org.xidea.el.ValueStack;
import org.xidea.el.fn.ECMA262Impl;

public class OperationStrategyImpl implements OperationStrategy {
	private static final Log log = LogFactory.getLog(OperationStrategyImpl.class);
	protected static final Object SKIP_QUESTION = new Object();
	private static final Object[] EMPTY_ARGS = new Object[0];
	private final Map<String, Map<String, Invocable>> methodMap = new HashMap<String, Map<String,Invocable>>();
	private final Map<String, Object> globalMap = new HashMap<String, Object>();
	private static final NumberArithmetic na = new NumberArithmetic();

	public OperationStrategyImpl(){
	}

	public void addVar(String var, Object value) {
		this.globalMap.put(var, value);
	}
	public void addMethod(Class<? extends Object> clazz, String name, Invocable invocable) {
		Map<String, Invocable> invocableMap = this.methodMap.get(name);
		if (invocableMap == null) {
			invocableMap = new HashMap<String, Invocable>();
			this.methodMap.put(name, invocableMap);
		}
		invocableMap.put(clazz.getName(), invocable);
	}

	/**
	 * @param arg1
	 * @param arg2
	 * @see <a
	 *      href="http://www.ecma-international.org/publications/standards/Ecma-262.htm">Ecma262</a>
	 * @return
	 */
	protected int compare(Object arg1, Object arg2, int validReturn) {
		if (arg1 == null) {
			if (arg2 == null) {
				return 0;
			}
		} else if (arg1 instanceof Number && arg2 instanceof Number) {
			return na.compare((Number)arg1, (Number)arg2, validReturn);
		}else if (arg1.equals(arg2)) {
			return 0;
		}
		arg1 = ECMA262Impl.ToPrimitive(arg1, Number.class);
		arg2 = ECMA262Impl.ToPrimitive(arg2, Number.class);
		if (arg1 instanceof String && arg2 instanceof String) {
			return ((String) arg1).compareTo((String) arg2);
		}
		Number n1 = ECMA262Impl.ToNumber(arg1);
		Number n2 = ECMA262Impl.ToNumber(arg2);
		return na.compare(n1, n2, validReturn);
	}


	@SuppressWarnings("unchecked")
	public Object evaluate(ExpressionToken item,ValueStack vs){
		final int type = item.getType();
		switch (type) {
		case ExpressionToken.VALUE_VAR:
			Object key = item.getParam();
			Object v = vs.get(key);
			if(v == null){
				return globalMap.get(key);
			}
			return v;
		case ExpressionToken.VALUE_CONSTANTS:
			return item.getParam();
		case ExpressionToken.VALUE_NEW_LIST:
			return new ArrayList<Object>();
		case ExpressionToken.VALUE_NEW_MAP:
			return new LinkedHashMap<Object, Object>();
		}
		Object arg1 = evaluate(item.getLeft(), vs);
		Object arg2 =  null;
		switch (type) {
		case ExpressionToken.OP_GET_STATIC_PROP:
		    arg2 = item.getParam();
		    if (arg1 instanceof Reference) {
				return ((Reference) arg1).next(arg2);
			} else {
				return new ReferenceImpl(arg1, arg2);
			}
		case ExpressionToken.OP_GET_PROP:
			arg2 = realValue(evaluate(item.getRight(), vs));
			if (arg1 instanceof Reference) {
				return ((Reference) arg1).next(arg2);
			} else {
				return new ReferenceImpl(arg1, arg2);
			}
		case ExpressionToken.OP_INVOKE_METHOD:
			try {
				Object thiz;
				arg2 = realValue(evaluate(item.getRight(), vs));
				Object[] arguments = (arg2 instanceof List) ? ((List<?>) arg2)
						.toArray() : EMPTY_ARGS;
				Invocable invocable = null;
				if (arg1 instanceof Reference) {
					Reference pv = (Reference) arg1;
					invocable = ReferenceImpl.getInvocable(pv,methodMap, arguments);
					thiz = pv.getBase();
				} else {
					thiz = vs;
					if (arg1 instanceof Invocable) {
						invocable = (Invocable) arg1;
					} else if ((arg1 instanceof java.lang.reflect.Method)) {
						invocable = ReferenceImpl
								.createProxy((java.lang.reflect.Method) arg1);
					}
				}
				return invocable.invoke(thiz, arguments);
			} catch (Exception e) {
				if (log.isDebugEnabled()) {
					log.debug("方法调用失败:" + arg1, e);
				}
				return null;
			}
		/* lazy computer elements*/
		/* and or */
		case ExpressionToken.OP_AND:
			arg1 = realValue(arg1);
			if (ECMA262Impl.ToBoolean(arg1)) {
				return evaluate(item.getRight(), vs);// 进一步判断
			} else {// false
				return arg1;// //skip
			}

		case ExpressionToken.OP_OR:
			arg1 = realValue(arg1);
			if (ECMA262Impl.ToBoolean(arg1)) {
				return arg1;
			} else {
				return evaluate(item.getRight(), vs);
			}
		case ExpressionToken.OP_QUESTION:// a?b:c -> a?:bc -- >a?b:c
			arg1 = realValue(arg1);
			if (ECMA262Impl.ToBoolean(arg1)) {// 取值1
				return evaluate(item.getRight(), vs);
			} else {// 跳过 取值2
				return SKIP_QUESTION;
			}
		case ExpressionToken.OP_QUESTION_SELECT:
			//arg1 一定不會是refrence
			if (arg1 == SKIP_QUESTION) {
				return evaluate(item.getRight(), vs);
			} else {
				return arg1;
			}
		}
		arg1 = realValue(arg1);
		if((type & ExpressionToken.BIT_PARAM) >0){
			arg2 = evaluate(item.getRight(), vs);
			arg2 = realValue(arg2);
		}
		switch (type) {
		case ExpressionToken.OP_NOT:
			return !ECMA262Impl.ToBoolean(arg1);
		case ExpressionToken.OP_POS:
			return ECMA262Impl.ToNumber(arg1);
		case ExpressionToken.OP_NEG:
			return na.subtract(0, ECMA262Impl.ToNumber(arg1));
			/* +-*%/ */
		case ExpressionToken.OP_ADD:
			Object p1 = ECMA262Impl.ToPrimitive(arg1, String.class);
			Object p2 = ECMA262Impl.ToPrimitive(arg2, String.class);
			if (p1 instanceof String || p2 instanceof String) {
				return String.valueOf(p1) + p2;
			} else {
				return na.add(ECMA262Impl.ToNumber(p1), ECMA262Impl
						.ToNumber(p2));
			}
		case ExpressionToken.OP_SUB:
			return na.subtract(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2));
		case ExpressionToken.OP_MUL:
			return na.multiply(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2));
		case ExpressionToken.OP_DIV:
			return na.divide(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2), true);
		case ExpressionToken.OP_MOD:
			return na.modulus(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2));

			/* boolean */
			
		case ExpressionToken.OP_EQ:
			return compare(arg1, arg2, -1) == 0;// -1 == 0 //false
		case ExpressionToken.OP_NOTEQ:
			return compare(arg1, arg2, -1) != 0;// -1 != 0 //true
		case ExpressionToken.OP_GT:
			return compare(arg1, arg2, -1) > 0;// -1 > 0 //false
		case ExpressionToken.OP_GTEQ:
			return compare(arg1, arg2, -1) >= 0;// -1 >= 0 //false
		case ExpressionToken.OP_LT:
			return compare(arg1, arg2, 1) < 0;// 1 < 0 //false
		case ExpressionToken.OP_LTEQ:
			return compare(arg1, arg2, 1) <= 0;// 1 <= 0 //false

		case ExpressionToken.OP_PARAM_JOIN:
			((List) arg1).add(arg2);
			return arg1;
		case ExpressionToken.OP_MAP_PUSH:
			((Map) arg1).put(item.getParam(), arg2);
			return arg1;
		}
		throw new RuntimeException("不支持的操作符" + item.getType());

	}
	private static final Object realValue(Object arg1){
		if(arg1 instanceof Reference){
			return  ((Reference)arg1).getValue();
		}
		return arg1;
	}

	public Object getVar(ValueStack vs,Object key) {
		Object o = vs.get(key);
		if(o == null){
			return globalMap.get(key);
		}
		return o;
	}

}
