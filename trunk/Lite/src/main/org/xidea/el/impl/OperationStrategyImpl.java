package org.xidea.el.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
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
import org.xidea.el.fn.ECMA262Impl;

public class OperationStrategyImpl implements OperationStrategy {
	private static final NumberArithmetic na = new NumberArithmetic();
	private static final Log log = LogFactory
			.getLog(OperationStrategyImpl.class);
	private final Map<Class<? extends Object>, Map<String, Invocable>> classMethodMap = new HashMap<Class<? extends Object>, Map<String, Invocable>>();
	private final Map<Object, Object> globalMap = new HashMap<Object, Object>();
	boolean customizable;

	public OperationStrategyImpl(boolean customizable) {
		this.customizable = customizable;
	}

	public Map<Object, Object> getGlobalMap() {
		return Collections.unmodifiableMap(globalMap);
	}

	protected void addVar(Object var, Object value) {
		this.globalMap.put(var, value);
	}

	protected void addMethod(Class<? extends Object> clazz, String name,
				Invocable invocable) {
		Map<String, Invocable> invocableMap = this.classMethodMap.get(clazz);
		if (invocableMap == null) {
			invocableMap = new HashMap<String, Invocable>();
			this.classMethodMap.put(clazz, invocableMap);
		}
		for(Map.Entry<Class<?extends Object>,Map<String,Invocable>> entry: this.classMethodMap.entrySet()){
			if(entry.getKey().isAssignableFrom(clazz)){
				entry.getValue().put(name, invocable);
			}
		}
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
				return type == ExpressionToken.OP_GTEQ
						|| type == ExpressionToken.OP_LTEQ;
			}
		} else if (arg1 instanceof Number && arg2 instanceof Number) {
			return na.compare((Number) arg1, (Number) arg2, type);
		} else if (arg1.equals(arg2)) {
			return type == ExpressionToken.OP_GTEQ
					|| type == ExpressionToken.OP_LTEQ;
		}
		arg1 = ECMA262Impl.ToPrimitive(arg1, Number.class);
		arg2 = ECMA262Impl.ToPrimitive(arg2, Number.class);
		if (arg1 instanceof String && arg2 instanceof String) {
			return na
					.compare(((String) arg1).compareTo((String) arg2), 0, type);
		}
		Number n1 = ECMA262Impl.ToNumber(arg1);
		Number n2 = ECMA262Impl.ToNumber(arg2);
		return na.compare(n1, n2, type);
	}

	protected boolean isEquals(Object arg1, Object arg2, boolean strict) {
		if (arg1 == null || arg2 == null) {
			return arg1 == arg2;
		}
		if (arg1 instanceof Number && arg2 instanceof Number) {
			return na.compare((Number) arg1, (Number) arg2,
					ExpressionToken.OP_EQ);
		} else if (arg1.equals(arg2)) {
			return true;
		}
		if (strict) {
			if (arg1 instanceof String && arg2 instanceof String) {
				return false;
			}
			if (arg1 instanceof Boolean && arg2 instanceof Boolean) {
				return false;
			}
		}
		arg1 = ECMA262Impl.ToPrimitive(arg1, Number.class);
		arg2 = ECMA262Impl.ToPrimitive(arg2, Number.class);
		if (arg1 instanceof String && arg2 instanceof String) {
			return arg1.equals(arg2);
		}
		Number n1 = ECMA262Impl.ToNumber(arg1);
		Number n2 = ECMA262Impl.ToNumber(arg2);
		return na.compare(n1, n2, ExpressionToken.OP_EQ);
	}

	@SuppressWarnings( { "unchecked", "rawtypes" })
	public Object evaluate(ExpressionToken item, Map<String, Object> vs) {
		final int type = item.getType();
		switch (type) {
		case ExpressionToken.VALUE_VAR:
			Object key = item.getParam();
			return getVar(vs, key);
		case ExpressionToken.VALUE_CONSTANTS:
			return item.getParam();
		case ExpressionToken.VALUE_LIST:
			return new ArrayList<Object>();
		case ExpressionToken.VALUE_MAP:
			return new LinkedHashMap<Object, Object>();

		case TokenImpl.OP_INVOKE_WITH_STATIC_PARAM:
		case ExpressionToken.OP_INVOKE:
			Object[] arguments;
			if (type == ExpressionToken.OP_INVOKE) {
				arguments = ((List<?>) evaluate(item.getRight(), vs)).toArray();
			} else {
				arguments = (Object[]) item.getParam();
			}
			ExpressionToken left = item.getLeft();
			int type2 = left.getType();
			Object thiz;

			if (type2 == ExpressionToken.OP_GET) {
				thiz = evaluate(left.getLeft(), vs);
				key = evaluate(left.getRight(), vs);
			} else if (type2 == TokenImpl.OP_GET_STATIC) {
				thiz = evaluate(left.getLeft(), vs);
				key = left.getRight().getParam();
			} else {
				return invoke(vs, evaluate(left, vs), arguments);
			}
			return invoke(vs, new ReferenceImpl(thiz, key), arguments);
		}
		Object arg1 = evaluate(item.getLeft(), vs);
		Object arg2 = null;
		switch (type) {
		case TokenImpl.OP_GET_STATIC:
			arg2 = item.getParam();
			return ReflectUtil.getValue(arg1, arg2);
		case ExpressionToken.OP_GET:
			arg2 = evaluate(item.getRight(), vs);
			return ReflectUtil.getValue(arg1, arg2);

			/* lazy computer elements */
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

		case ExpressionToken.OP_QUESTION_SELECT:

			ExpressionToken qtoken = (ExpressionToken) arg1;
			if (ECMA262Impl.ToBoolean(evaluate(qtoken.getLeft(), vs))) {// 取值1
				return evaluate(qtoken.getRight(), vs);
			} else {
				return evaluate(item.getRight(), vs);
			}
		case ExpressionToken.OP_QUESTION:// a?b:c -> a?:bc -- >a?b:c
			return item;
			// throw new IllegalStateException("无效表达式");
			// arg1 一定不會是refrence
			// if (arg1 == SKIP_QUESTION) {
			// return evaluate(item.getRight(), vs);
			// } else {
			// return arg1;
			// }
		}
		if ((type & ExpressionToken.BIT_ARGS) > 0) {
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
			if (p1 instanceof String || p1 instanceof Character) {
				return p1 + ECMA262Impl.ToString(p2);
			} else if (p2 instanceof String || p2 instanceof Character) {
				return ECMA262Impl.ToString(p1) + p2;
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
			return isEquals(arg1, arg2, true);
		case ExpressionToken.OP_EQ:
			return isEquals(arg1, arg2, false);

		case ExpressionToken.OP_NE:
			return !isEquals(arg1, arg2, false);

		case ExpressionToken.OP_NE_STRICT:
			return !isEquals(arg1, arg2, true);
		case ExpressionToken.OP_GT:
		case ExpressionToken.OP_GTEQ:
		case ExpressionToken.OP_LT:
		case ExpressionToken.OP_LTEQ:
			return compare(arg1, arg2, type);// 1 <= 0 //false

		case ExpressionToken.OP_JOIN:
			((List) arg1).add(arg2);
			return arg1;
		case ExpressionToken.OP_PUT:
			((Map) arg1).put(item.getParam(), arg2);
			return arg1;
		case ExpressionToken.OP_IN:
			return in(arg1, arg2);
		default:
			int a1 = ECMA262Impl.ToNumber(arg1).intValue();
			int a2 = ECMA262Impl.ToNumber(arg1).intValue();
			switch (type) {
			case ExpressionToken.OP_BIT_AND:
				return a1 & a2;
			case ExpressionToken.OP_BIT_XOR:
				return a1 ^ a2;
			case ExpressionToken.OP_BIT_OR:
				return a1 | a2;
			case ExpressionToken.OP_LSH:
				return a1 << a2;
			case ExpressionToken.OP_RSH:
				return a1 >> a2;
			case ExpressionToken.OP_URSH:
				return a1 >>> a2;

			}

			Object impl = this.globalMap.get(type);
			if (impl != null) {
				Invocable method = (Invocable) impl;
				try {
					return method.invoke(null, arg1, arg2);
				} catch (Exception e) {
					if (log.isDebugEnabled()) {
						log.debug("方法调用失败:" + arg1, e);
					}
				}
			}
			throw new RuntimeException("不支持的操作符" + item.getType());

			// case ExpressionToken.VALUE_CONSTANTS:// = -0x01;//value
			// case ExpressionToken.VALUE_VAR:// = -0x02;//var
			// case ExpressionToken.VALUE_LIST:// = -0x03;//[]
			// case ExpressionToken.VALUE_MAP:// = -0x04;//{}

			// case ExpressionToken.OP_GET:// = 0<<12 | 0<<8 | 1<<6 | 8<<2 |
			// 0;//.[]
			// case ExpressionToken.OP_INVOKE:// = 0<<12 | 0<<8 | 1<<6 | 8<<2 |
			// 1;//()
			// case ExpressionToken.OP_AND:// = 0<<12 | 1<<8 | 1<<6 | 2<<2 |
			// 0;//&&
			// case ExpressionToken.OP_OR:// = 0<<12 | 0<<8 | 1<<6 | 2<<2 |
			// 0;//||
			// case ExpressionToken.OP_QUESTION:// = 0<<12 | 0<<8 | 1<<6 | 1<<2
			// | 0;//?
			// case ExpressionToken.OP_QUESTION_SELECT:// = 0<<12 | 0<<8 | 1<<6
			// | 1<<2 | 1;//:
		}

	}

	protected boolean in(Object key, Object object) {
		int len = -1;
		Class<?> clazz = object.getClass();
		if (object instanceof List<?>) {
			len = ((List<?>) object).size();
		} else if (clazz.isArray()) {
			len = Array.getLength(object);
		}
		if (len >= 0) {
			if ("length".equals(key)) {
				return true;
			}
			Number n = ECMA262Impl.ToNumber(key);
			int i = n.intValue();
			if (i >= 0 && i <= len) {
				return i == n.floatValue();
			}
			return false;
		}
		String skey = ECMA262Impl.ToString(key);
		if (object instanceof Map<?, ?>) {
			return ((Map<?, ?>) object).containsKey(skey);
		}

		return ReflectUtil.getPropertyClass(clazz, skey) != null;
	}

	private Map<String, Invocable> requireMethodMap(
			Class<? extends Object> clazz) {
		Map<String, Invocable> methodMap = this.classMethodMap.get(clazz);
		if (methodMap == null) {
			methodMap = new HashMap<String, Invocable>();
			{
				Class<?>[] interfaces = clazz.getInterfaces();
				for (Class<?> clazz2 : interfaces) {
					Map<String, Invocable> m2 = requireMethodMap(clazz2);
					methodMap.putAll(m2);
				}
			}
			Class<? extends Object> clazz2 = clazz.getSuperclass();
			if (clazz2 != clazz) {
				if (clazz2 == Object.class && clazz.isArray()
						&& clazz != Object[].class) {
					clazz2 = Object[].class;
				}
				if (clazz2 != null) {
					Map<String, Invocable> m2 = requireMethodMap(clazz2);
					methodMap.putAll(m2);
				}
			}
		}
		return methodMap;
	}
	protected Invocable getInvocable(Object base ,String name,Object[] args){
		Map<String, Invocable> mm = requireMethodMap(base.getClass());
		Invocable invocable = mm.get(name);
		if (invocable == null && name instanceof String) {
			invocable = ExpressionFactoryImpl.getInvocable(base.getClass(), name,
					args.length);
			if (invocable == null && base instanceof Class<?>) {
				invocable = ExpressionFactoryImpl.getInvocable((Class<?>) base, name,
						args.length);
			}
		}
		return invocable;
	}

	private Object invoke(Map<String, Object> vs, Object arg1, Object[] arguments) {
		try {
			Object thiz;
			Invocable invocable = null;
			if (arg1 instanceof Reference) {
				Reference pv = (Reference) arg1;
				thiz = pv.getBase();
				Object name = pv.getName();
				invocable = getInvocable(thiz,String.valueOf(name), arguments);
				if(invocable == null){
					arg1 = pv.getValue();
				}else{
					return invocable.invoke(thiz, arguments);
				}
			}else{
				thiz = vs;
			}
			if(invocable == null){
				if (arg1 instanceof Invocable) {
					invocable = (Invocable) arg1;
				} else if ((arg1 instanceof java.lang.reflect.Method)) {
					invocable = ExpressionFactoryImpl
							.createProxy((java.lang.reflect.Method) arg1);
				}else{
					if (log.isInfoEnabled()) {
						log.info("对象不是有效函数:" + arg1);
					}
				}
			}
			return invocable.invoke(thiz, arguments);
		} catch (Exception e) {
			if (log.isInfoEnabled()) {
				log.info("方法调用失败:" + arg1, e);
			}
			return null;
		}
	}

	public Object getVar(Map<String, Object> vs, Object key) {
		Object o = vs.get(key);
		if (o == null) {
			return globalMap.get(key);
		}
		return o;
	}


};