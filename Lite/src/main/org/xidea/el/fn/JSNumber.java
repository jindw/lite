package org.xidea.el.fn;

import java.text.DecimalFormat;

import org.xidea.el.Invocable;

abstract class JSNumber extends ECMA262Impl implements Invocable {

	@SuppressWarnings("unchecked")
	public Object invoke(Object thiz, Object... args) throws Exception {
		return this.invoke((Number) thiz, args);
	}

	public abstract Object invoke(Number thiz, Object... args)
			throws Exception;

	public static final Invocable toFixed = new JSNumber() {
		@Override
		public Object invoke(Number thiz, Object... args)
				throws Exception {
			int p = getIntArg(args, 0, 0);
			DecimalFormat df = new DecimalFormat();
			df.setMinimumFractionDigits(p);
			df.setMaximumFractionDigits(p);
			df.setGroupingUsed(false);
			return df.format(thiz);
		}
	};
	//toPrecision,toExponential
}
