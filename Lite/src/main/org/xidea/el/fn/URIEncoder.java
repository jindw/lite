package org.xidea.el.fn;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.xidea.el.Invocable;

public class URIEncoder implements Invocable {
	private static final Pattern URL_SPLIT = Pattern.compile("[\\/\\:&\\?=]");
	final boolean encode;
	private boolean split;

	URIEncoder(boolean encode, boolean split) {
		this.encode = encode;
		this.split = split;
	}

	public Object invoke(Object thiz, Object... args) throws Exception {
		// 不完全等价于 ECMA 262标准，主要是' '->+|%20
		// 编码标准参照：application/x-www-form-urlencoded
		final String text = String.valueOf(JSObject.getArg(args, 0, null));
		final String charset = String.valueOf(JSObject.getArg(args, 1,
				"utf-8"));
		return process(text, charset);
	}

	// protected Object process(final String text, String charset)
	// throws UnsupportedEncodingException {
	//
	// }
	protected Object process(final String text, String charset)
			throws UnsupportedEncodingException {
		if (split) {
			Matcher matcher = URL_SPLIT.matcher(text);
			StringBuilder buf = new StringBuilder();
			int end = 0;
			while (matcher.find()) {
				int start = matcher.start();
				if (start >= end) {
					buf
							.append(processPart(text.substring(end, start),
									charset));
				}
				buf.append(text.substring(start, end = matcher.end()));
			}
			buf.append(processPart(text.substring(end), charset));
			return buf.toString();
		} else {
			return processPart(text, charset);
		}
	}

	protected Object processPart(final String text, String charset)
			throws UnsupportedEncodingException {
		return encode ? URLEncoder.encode(text, charset) : URLDecoder.decode(
				text, charset);
	}

}