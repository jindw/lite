package org.xidea.el.fn;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xidea.el.Invocable;

public class NumberParser implements Invocable {
	private static final Pattern INT_PARTTERN = Pattern
			.compile("^[\\+\\-]?(0x[0-9a-fA-F]+" + "|0+[0-7]*"
					+ "|[1-9][0-9]*)");
	private static final Pattern FLOAT_PARTTERN = Pattern
			.compile("^[\\+\\-]?[0-9]*(?:\\.[0-9]+)?");

	private boolean parseFloat;

	NumberParser(boolean parseFloat) {
		this.parseFloat = parseFloat;
	}

	public Object invoke(Object thiz, Object... args) throws Exception {
		String text = String.valueOf(JSObject.getArg(args, 0, null)).trim()
				.toLowerCase();
		int length = text.length();
		if (length > 0) {
			Number result = parseFloat ? parseFloat(text) : parseInt(text);
			if (result != null) {
				return result;
			}
			// return Integer.valueOf(text);
		}
		return Float.NaN;
	}

	// ECMA 262 parseInt,parseFloat不支持E[+-]?\d+
	protected Number parseFloat(String text) {
		Matcher matcher = FLOAT_PARTTERN.matcher(text);
		if (matcher.find()) {
			return Double.parseDouble(matcher.group(0));
		} else {
			return Float.NaN;
		}
	}

	protected Number parseInt(String text) {
		Matcher matcher = INT_PARTTERN.matcher(text);
		if (matcher.find()) {
			text = matcher.group(0);
			String n = matcher.group(1);
			if (n.startsWith("0x")) {
				return Long.parseLong(text.replaceFirst("0x", ""), 16);
			} else if (n.startsWith("0")) {
				return parseInt(text, 8);
			} else {
				return parseInt(text, 10);
			}
		} else {
			return parseFloat(text).intValue();
		}
	}

	private Number parseInt(String text, int readio) {
		try {
			return new Integer(Integer.parseInt(text, readio));
		} catch (NumberFormatException e) {
			return new Long(Long.parseLong(text, readio));
		}
	}

}