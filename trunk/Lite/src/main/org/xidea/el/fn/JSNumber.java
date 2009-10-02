package org.xidea.el.fn;

import java.text.DecimalFormat;

import org.xidea.el.Invocable;

public class JSNumber extends JSObject implements Invocable {
	public static final String MEMBERS = "toFixed,toPrecision,toExponential";
	@SuppressWarnings("unchecked")
	public Object invoke(Object thiz, Object... args) throws Exception {
		Number n = (Number) thiz;
		switch(type){
		case 0:
			return toFixed(n, args);
		case 1:
			return toPrecision(n, args);
		case 2:
			return toExponential(n, args);
		}
		return null;
	}

	public Object toFixed(Number thiz, Object[] args) throws Exception {
		int p = getIntArg(args, 0, 0);
		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(p);
		df.setMaximumFractionDigits(p);
		df.setGroupingUsed(false);
		return df.format(thiz);
	}

	private Object toExponential(Number thiz, Object[] args) {
		return null;
	}
	private Object toPrecision(Number thiz, Object[] args) {
		return null;
	}
}
