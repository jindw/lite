package org.xidea.el.fn;

import java.text.DecimalFormat;

import org.xidea.el.Invocable;

class JSNumber extends JSObject implements Invocable {
	public Object toFixed(Number thiz, Object[] args) throws Exception {
		int p = JSObject.getIntArg(args, 0, 0);
		DecimalFormat df = new DecimalFormat();
		df.setMinimumFractionDigits(p);
		df.setMaximumFractionDigits(p);
		df.setGroupingUsed(false);
		if(thiz.doubleValue() == 0.0){
			thiz = 0.0;
		}
		return df.format(thiz);
	}

	public Object toExponential(Number thiz, Object[] args) {
		return null;
	}
	public Object toPrecision(Number thiz, Object[] args) {
		return null;
	}
	public Object toString(Number thiz, Object[] args) {
		int radix = JSObject.getIntArg(args, 0, 10);
		return ECMA262Impl.toString(thiz,radix);
	}
}
