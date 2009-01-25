package org.xidea.el;

import java.util.Collections;
import java.util.Map;

import org.xidea.el.operation.Calculater;
import org.xidea.el.operation.Invocable;
import org.xidea.el.parser.ExpressionToken;
import org.xidea.el.parser.ExpressionTokenizer;

public class ResultExpressionImpl extends ExpressionImpl {

	public ResultExpressionImpl(String el) {
		this(el, new ExpressionTokenizer(el).getTokens().getData(),
				ExpressionFactoryImpl.DEFAULT_CALCULATER,
				ExpressionFactoryImpl.DEFAULT_GLOBAL_MAP);
	}

	public ResultExpressionImpl(String source, ExpressionToken[] compiled,
			Calculater calculater, Map<String, Invocable> globalMap) {
		super(source, compiled, calculater, globalMap);
	}

	public Object evaluate(Map<? extends Object, ? extends Object> context) {
		if (context == null) {
			context = Collections.emptyMap();
		}
		ValueStack stack = new ValueStack();
		evaluate(stack, expression, context);
		return stack.pop();
	}

	protected void evaluate(ValueStack stack, ExpressionToken[] tokens,
			Map<? extends Object, ? extends Object> context) {
		ExpressionToken item = null;
		int i = tokens.length;
		while (i-- > 0) {
			item = (ExpressionToken) tokens[i];
			int type = item.getType();
			if (type > 3) {
				Object arg2 = null;
				Object arg1 = null;
				int length = type & 3;
				if (length > 1) {
					arg2 = stack.pop();
					arg1 = stack.pop();
				} else{//  if (length == 1) {
					arg1 = stack.pop();
				}
				Object result = calculater.compute(item, arg1, arg2);
				if (result instanceof ExpressionToken) {
					ExpressionToken lazyToken = (ExpressionToken) result;
					if(lazyToken.getType() == ExpressionToken.VALUE_LAZY){
						evaluate(stack, (ExpressionToken[]) lazyToken.getParam(),context);
						return;
					}
				}
				stack.push(result);
			} else {
				stack.push(getTokenValue(context, item));
			}
		}
	}
}

