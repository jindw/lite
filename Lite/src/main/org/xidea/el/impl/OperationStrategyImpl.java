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
	protected boolean compare(Object arg1, Object arg2, int type) {
		if (arg1 == null) {
			if (arg2 == null) {
				return type == ExpressionToken.OP_GTEQ || type == ExpressionToken.OP_LTEQ;
			}
		} else if (arg1 instanceof Number && arg2 instanceof Number) {
			return na.compare((Number)arg1, (Number)arg2, type);
		}else if (arg1.equals(arg2)) {
			return type == ExpressionToken.OP_GTEQ || type == ExpressionToken.OP_LTEQ;
		}
		arg1 = ECMA262Impl.ToPrimitive(arg1, Number.class);
		arg2 = ECMA262Impl.ToPrimitive(arg2, Number.class);
		if (arg1 instanceof String && arg2 instanceof String) {
			return na.compare(((String) arg1).compareTo((String) arg2),0,type);
		}
		Number n1 = ECMA262Impl.ToNumber(arg1);
		Number n2 = ECMA262Impl.ToNumber(arg2);
		return na.compare(n1, n2, type);
	}

	protected boolean isEquals(Object arg1, Object arg2, boolean strict) {
		if(arg1 == null || arg2 == null){
			return arg1 == arg2;
		}
		if (arg1.equals(arg2)) {
			return true;
		}else if (arg1 instanceof Number && arg2 instanceof Number) {
			return na.compare((Number)arg1, (Number)arg2, ExpressionToken.OP_EQ);
		}
		if(strict){
			return false;
		}else if (arg1 instanceof String && arg2 instanceof String) {
			return false;
		}
		arg1 = ECMA262Impl.ToPrimitive(arg1, Number.class);
		arg2 = ECMA262Impl.ToPrimitive(arg2, Number.class);
		if (arg1 instanceof String && arg2 instanceof String) {
			return arg1.equals(arg2);
		}
		Number n1 = ECMA262Impl.ToNumber(arg1);
		Number n2 = ECMA262Impl.ToNumber(arg2);
		return na.compare(n1, n2,  ExpressionToken.OP_EQ);
	}

	@SuppressWarnings({ "unchecked" })
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
		case ExpressionToken.VALUE_LIST:
			return new ArrayList<Object>();
		case ExpressionToken.VALUE_MAP:
			return new LinkedHashMap<Object, Object>();

		case TokenImpl.OP_INVOKE_WITH_STATIC_PARAM:
			
		case ExpressionToken.OP_INVOKE:
			Object[] arguments;
			if(type == ExpressionToken.OP_INVOKE){
				arguments =((List<?>)  evaluate(item.getRight(), vs)).toArray();
			}else{
				arguments = (Object[])item.getParam();
			}
			ExpressionToken left = item.getLeft();
			int type2 = left.getType();
			Object thiz;
			
			if(type2 == ExpressionToken.OP_GET){
				thiz = evaluate(left.getLeft(),vs);
				key = evaluate(left.getRight(),vs);
			}else if(type2 == TokenImpl.OP_GET_STATIC_PROP){
				thiz = evaluate(left.getLeft(),vs);
				key = left.getRight().getParam();
			}else{
				return invoke(vs, evaluate(left, vs), arguments);
			}
			return invoke(vs, new ReferenceImpl(thiz,key), arguments);
		}
		Object arg1 = evaluate(item.getLeft(), vs);
		Object arg2 =  null;
		switch (type) {
		case TokenImpl.OP_GET_STATIC_PROP:
		    arg2 = item.getParam();
		    return ReflectUtil.getValue(arg1, arg2);
		case ExpressionToken.OP_GET:
			arg2 = evaluate(item.getRight(), vs);
		    return ReflectUtil.getValue(arg1, arg2);

		/* lazy computer elements*/
		/* and or */
		case ExpressionToken.OP_AND:
			if (ECMA262Impl.ToBoolean(arg1)) {
				return evaluate(item.getRight(), vs);// 进一步判断
			} else {// false
				return arg1;// //skip
			}

		case ExpressionToken.OP_OR:
			if (ECMA262Impl.ToBoolean(arg1)) {
				return arg1;
			} else {
				return evaluate(item.getRight(), vs);
			}
		case ExpressionToken.OP_QUESTION:// a?b:c -> a?:bc -- >a?b:c
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
		if((type & ExpressionToken.BIT_ARGS) >0){
			arg2 = evaluate(item.getRight(), vs);
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

		case ExpressionToken.OP_EQ_STRICT:
			return isEquals(arg1,arg2,true);
		case ExpressionToken.OP_EQ:
			return isEquals(arg1,arg2,false);

		case ExpressionToken.OP_NE:
			return !isEquals(arg1,arg2,false);

		case ExpressionToken.OP_NE_STRICT:
			return !isEquals(arg1,arg2,true);
		case ExpressionToken.OP_GT:
		case ExpressionToken.OP_GTEQ:
		case ExpressionToken.OP_LT:
		case ExpressionToken.OP_LTEQ:
			return compare(arg1, arg2, type);// 1 <= 0 //false

		case ExpressionToken.OP_JOIN:
			((List) arg1).add(arg2);
			return arg1;
		case ExpressionToken.OP_PUSH:
			((Map) arg1).put(item.getParam(), arg2);
			return arg1;
		}
		throw new RuntimeException("不支持的操作符" + item.getType());

	}

	private Object invoke(ValueStack vs, Object arg1, Object[] arguments) {
		try {
			Object thiz;
			Invocable invocable = null;
			if (arg1 instanceof Reference) {
				Reference pv = (Reference) arg1;
				invocable = ReferenceImpl.createInvocable(pv,methodMap, arguments);
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
	}

	public Object getVar(ValueStack vs,Object key) {
		Object o = vs.get(key);
		if(o == null){
			return globalMap.get(key);
		}
		return o;
	}

}
