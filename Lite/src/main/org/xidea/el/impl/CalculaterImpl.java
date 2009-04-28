package org.xidea.el.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.xidea.el.Calculater;
import org.xidea.el.ExpressionToken;
import org.xidea.el.Invocable;
import org.xidea.el.Reference;
import org.xidea.el.ResultStack;

public class CalculaterImpl implements Calculater {
	private static final Log log = LogFactory.getLog(CalculaterImpl.class);
	protected static final Object SKIP_QUESTION = new Object();
	private static final Object[] EMPTY_ARGS = new Object[0];
	private Map<String, Map<String, Invocable>> methodMap;
	private NumberArithmetic arithmetic = new NumberArithmetic();

	public CalculaterImpl(){
		this(new HashMap<String, Map<String, Invocable>>());
	}
	public CalculaterImpl(Map<String, Map<String, Invocable>> methodMap){
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


	@SuppressWarnings("unchecked")
	public Object compute(ExpressionToken op,ResultStack stack) {
		final int type = op.getType();
		Object arg2 = (type & 1) == 1 ? stack.pop():null;
		Object arg1 = stack.get();
		switch (type) {
		case ExpressionToken.OP_STATIC_GET_PROP:
		    arg2 = op.getParam();
		case ExpressionToken.OP_GET_PROP:
			if (arg1 instanceof Reference) {
				return ((Reference) arg1).next(ExpressionImpl.realValue(arg2));
			} else {
				return new ReferenceImpl(arg1, ExpressionImpl.realValue(arg2));
			}
		case ExpressionToken.OP_INVOKE_METHOD:
			try {
				Object thiz;
				Object[] arguments = (arg2 instanceof List) ? ((List<?>) arg2)
						.toArray() : EMPTY_ARGS;
				Invocable invocable = null;
				if (arg1 instanceof Reference) {
					Reference pv = (Reference) arg1;
					invocable = ReferenceImpl.getInvocable(pv,methodMap, arguments);
					thiz = pv.getBase();
				} else {
					thiz = stack.getValueStack();
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
		}
		arg1 = ExpressionImpl.realValue(arg1);
		arg2 = ExpressionImpl.realValue(arg2);
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

			/* and or */
		case ExpressionToken.OP_AND:
			if (ECMA262Impl.ToBoolean(arg1)) {
				return arg2;// 进一步判断
			} else {// false
				return arg1;// //skip
			}

		case ExpressionToken.OP_OR:
			if (ECMA262Impl.ToBoolean(arg1)) {
				return arg1;
			} else {
				return arg2;
			}
		case ExpressionToken.OP_QUESTION:// a?b:c -> a?:bc -- >a?b:c
			if (ECMA262Impl.ToBoolean(arg1)) {// 取值1
				return arg2;
			} else {// 跳过 取值2
				return SKIP_QUESTION;
			}
		case ExpressionToken.OP_QUESTION_SELECT:
			if (arg1 == SKIP_QUESTION) {
				return arg2;
			} else {
				return arg1;
			}
		case ExpressionToken.OP_PARAM_JOIN:
			((List) arg1).add(arg2);
			return arg1;
		case ExpressionToken.OP_MAP_PUSH:
			((Map) arg1).put(op.getParam(), arg2);
			return arg1;
		}
		throw new RuntimeException("不支持的操作符" + op.getType());

	}
}
