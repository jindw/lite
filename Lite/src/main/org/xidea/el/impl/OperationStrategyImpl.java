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
import org.xidea.el.fn.NumberArithmetic;

public class OperationStrategyImpl implements OperationStrategy {
	private static final Log log = LogFactory.getLog(OperationStrategyImpl.class);
	protected static final Object SKIP_QUESTION = new Object();
	private static final Object[] EMPTY_ARGS = new Object[0];
	private Map<String, Map<String, Invocable>> methodMap;
	private NumberArithmetic arithmetic = new NumberArithmetic();

	public OperationStrategyImpl(){
		Map<String, Map<String, Invocable>> methodMap = new HashMap<String, Map<String, Invocable>>();
		this.methodMap = methodMap;
		ECMA262Impl.setup(this);
	}
	public OperationStrategyImpl(Map<String, Map<String, Invocable>> methodMap){
		this.methodMap = methodMap;
	}
	
	public void addMethod(Class<? extends Object> clazz, String name, Invocable invocable) {
		Map<String, Invocable> invocableMap = this.methodMap.get(name);
		if (invocableMap == null) {
			invocableMap = new HashMap<String, Invocable>();
			this.methodMap.put(name, invocableMap);
		}
		invocableMap.put(clazz.getName(), invocable);
	}

	protected boolean compare(int type, Object arg1, Object arg2) {
		switch (type) {
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
		}
		throw new RuntimeException("怎么可能？？？");
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
		} else if (arg1.equals(arg2)) {
			return 0;
		}
		arg1 = ECMA262Impl.ToPrimitive(arg1, Number.class);
		arg2 = ECMA262Impl.ToPrimitive(arg2, Number.class);
		if (arg1 instanceof String && arg2 instanceof String) {
			return ((String) arg1).compareTo((String) arg2);
		}
		Number n1 = ECMA262Impl.ToNumber(arg1);
		Number n2 = ECMA262Impl.ToNumber(arg2);
		return arithmetic.compare(n1, n2, validReturn);
	}


	protected Object getValue(ValueStack context, ExpressionToken item) {
		switch (item.getType()) {
		case ExpressionToken.VALUE_VAR:
			String value = (String) item.getParam();
			return context.get(value);
		case ExpressionToken.VALUE_CONSTANTS:
			return item.getParam();
		case ExpressionToken.VALUE_NEW_LIST:
			return new ArrayList<Object>();
		case ExpressionToken.VALUE_NEW_MAP:
			return new LinkedHashMap<Object, Object>();
		}
		throw new IllegalArgumentException("unknow token:"+Integer.toBinaryString(item.getType()));
	}
	@SuppressWarnings("unchecked")
	public Object evaluate(ExpressionToken op,ValueStack vs){
		final int type = op.getType();
		if(type<0){
			return getValue(vs, op);
		}

		Object arg1 = evaluate(op.getLeft(), vs);
		Object arg2 =  null;
		switch (type) {
		case ExpressionToken.OP_GET_STATIC_PROP:
		    arg2 = op.getParam();
		    if (arg1 instanceof Reference) {
				return ((Reference) arg1).next(arg2);
			} else {
				return new ReferenceImpl(arg1, arg2);
			}
		case ExpressionToken.OP_GET_PROP:
			arg2 = realValue(evaluate(op.getRight(), vs));
			if (arg1 instanceof Reference) {
				return ((Reference) arg1).next(arg2);
			} else {
				return new ReferenceImpl(arg1, arg2);
			}
		case ExpressionToken.OP_INVOKE_METHOD:
			try {
				Object thiz;
				arg2 = realValue(evaluate(op.getRight(), vs));
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
				return evaluate(op.getRight(), vs);// 进一步判断
			} else {// false
				return arg1;// //skip
			}

		case ExpressionToken.OP_OR:
			arg1 = realValue(arg1);
			if (ECMA262Impl.ToBoolean(arg1)) {
				return arg1;
			} else {
				return evaluate(op.getRight(), vs);
			}
		case ExpressionToken.OP_QUESTION:// a?b:c -> a?:bc -- >a?b:c
			arg1 = realValue(arg1);
			if (ECMA262Impl.ToBoolean(arg1)) {// 取值1
				return evaluate(op.getRight(), vs);
			} else {// 跳过 取值2
				return SKIP_QUESTION;
			}
		case ExpressionToken.OP_QUESTION_SELECT:
			//arg1 一定不會是refrence
			if (arg1 == SKIP_QUESTION) {
				return evaluate(op.getRight(), vs);
			} else {
				return arg1;
			}
		}
		arg1 = realValue(arg1);
		if((type & ExpressionToken.BIT_PARAM) >0){
			arg2 = evaluate(op.getRight(), vs);
			arg2 = realValue(arg2);
		}
		switch (type) {
		case ExpressionToken.OP_NOT:
			return !ECMA262Impl.ToBoolean(arg1);
		case ExpressionToken.OP_POS:
			return ECMA262Impl.ToNumber(arg1);
		case ExpressionToken.OP_NEG:
			return arithmetic.subtract(0, ECMA262Impl.ToNumber(arg1));
			/* +-*%/ */
		case ExpressionToken.OP_ADD:
			Object p1 = ECMA262Impl.ToPrimitive(arg1, String.class);
			Object p2 = ECMA262Impl.ToPrimitive(arg2, String.class);
			if (p1 instanceof String || p2 instanceof String) {
				return String.valueOf(p1) + p2;
			} else {
				return arithmetic.add(ECMA262Impl.ToNumber(p1), ECMA262Impl
						.ToNumber(p2));
			}
		case ExpressionToken.OP_SUB:
			return arithmetic.subtract(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2));
		case ExpressionToken.OP_MUL:
			return arithmetic.multiply(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2));
		case ExpressionToken.OP_DIV:
			return arithmetic.divide(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2), true);
		case ExpressionToken.OP_MOD:
			return arithmetic.modulus(ECMA262Impl.ToNumber(arg1), ECMA262Impl
					.ToNumber(arg2));

			/* boolean */
		case ExpressionToken.OP_GT:
		case ExpressionToken.OP_GTEQ:
		case ExpressionToken.OP_NOTEQ:
		case ExpressionToken.OP_EQ:
		case ExpressionToken.OP_LT:
		case ExpressionToken.OP_LTEQ:
			return compare(type, arg1, arg2);

		case ExpressionToken.OP_PARAM_JOIN:
			((List) arg1).add(arg2);
			return arg1;
		case ExpressionToken.OP_MAP_PUSH:
			((Map) arg1).put(op.getParam(), arg2);
			return arg1;
		}
		throw new RuntimeException("不支持的操作符" + op.getType());

	}
	private static final Object realValue(Object arg1){
		if(arg1 instanceof Reference){
			return  ((Reference)arg1).getValue();
		}
		return arg1;
	}
}
