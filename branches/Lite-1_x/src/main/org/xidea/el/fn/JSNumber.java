package org.xidea.el.fn;

import java.text.DecimalFormat;

import org.xidea.el.Invocable;

public class JSNumber extends JSObject implements Invocable {
	@SuppressWarnings("unchecked")
	public Object toFixed(Number thiz, Object[] args) throws Exception {
		int p = ECMA262Impl.getIntArg(args, 0, 0);
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
}
