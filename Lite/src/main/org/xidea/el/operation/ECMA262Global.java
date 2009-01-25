package org.xidea.el.operation;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 模拟ECMA262行为，保持基本一至，但迫于简单原则，略有偷懒行为^_^
 * 
 * @author jindw
 * @see org.mozilla.javascript.NativeGlobal
 */
public class ECMA262Global implements Invocable {

	public static final int ID_DECODEURI = 1, ID_DECODEURICOMPONENT = 2,
			ID_ENCODEURI = 3, ID_ENCODEURICOMPONENT = 4, ID_ISFINITE = 7,
			ID_ISNAN = 8, ID_PARSEFLOAT = 10, ID_PARSEINT = 11;

	private static final Object[] IDMAP = { ID_DECODEURI, "decodeURI",
			ID_DECODEURICOMPONENT, "decodeURIComponent", ID_ENCODEURI,
			"encodeURI", ID_ENCODEURICOMPONENT, "encodeURIComponent",
			ID_ISFINITE, "isFinite", ID_ISNAN, "isNaN", ID_PARSEFLOAT,
			"parseFloat", ID_PARSEINT, "parseInt" };

	public static void appendTo(Map<String, Invocable> globalInvocableMap) {
		for (int i = 0; i < IDMAP.length; i += 2) {
			globalInvocableMap.put((String) IDMAP[i + 1], new ECMA262Global(
					(Integer) IDMAP[i]));
		}
	}

	private static final Pattern URL_SPLIT = Pattern
			.compile("[\\/\\:&\\?=]");
	private static final Pattern INT_PARTTERN = Pattern
			.compile("^0x[0-9a-f]+|^0+[0-7]*|^[0-9]+");
	private static final Pattern FLOAT_PARTTERN = Pattern
			.compile("^[0-9]*\\.[0-9]+");

	private final int type;

	public ECMA262Global(int type) {
		this.type = type;
	}

	private static Object getArg(Object[] args, int index, Object defaultValue) {
		if (index >= 0 && index < args.length) {
			return args[index];
		} else {
			return defaultValue;
		}
	}

	public Object invoke(Object thizz,Object... args) throws Exception {
		switch (type) {
		case ID_ISFINITE:
		case ID_ISNAN:
			Object o = getArg(args, 0, Float.NaN);
			Number number = ECMA262Util.ToNumber(o);
			if (number instanceof Float || number instanceof Double) {
				float d = number.floatValue();
				if (type == ID_ISNAN) {
					return d != d;
				} else {//ID_ISFINITE
					return d == d && !Double.isInfinite(d);
				}
			}
			return true;
		case ID_PARSEFLOAT:
		case ID_PARSEINT:
		{
			// ECMA 262 parseInt,parseFloat不支持E[+-]?\d+
			String text = String.valueOf(getArg(args, 0, null)).trim()
					.toLowerCase();
			int length = text.length();
			if (length > 0) {
				if (type == ID_PARSEFLOAT) {
					Matcher matcher = FLOAT_PARTTERN.matcher(text);
					if(matcher.find()){
						return Double.parseDouble(matcher.group());
					}
				} else {
					Matcher matcher = INT_PARTTERN.matcher(text);
					if(matcher.find()){
						text = matcher.group();
						if(text.startsWith("0x")){
							return parseInt(text,16);
						}else if(text.startsWith("0")){
							return parseInt(text,8);
						}else {
							return parseInt(text,10);
						}
					}
				}
				// return Integer.valueOf(text);
			}
			return Double.NaN;
		}
		
		case ID_ENCODEURICOMPONENT:
		case ID_DECODEURICOMPONENT:
		case ID_ENCODEURI:
		case ID_DECODEURI:
			//不完全等价于 ECMA 262标准，主要是' '->+|%20
			//编码标准参照：application/x-www-form-urlencoded
			final String text = String.valueOf(getArg(args, 0, null));
			final String encode = String.valueOf(getArg(args, 1, "utf-8"));
			if(type== ID_ENCODEURICOMPONENT || type== ID_DECODEURICOMPONENT){
				return endecode(type== ID_ENCODEURICOMPONENT,text, encode);
			}else{
				Matcher matcher = URL_SPLIT.matcher(text);
				StringBuilder buf = new StringBuilder();
				int end = 0;
				while(matcher.find()){
					int start = matcher.start();
					if(start >= end){
						buf.append(endecode(type== ID_ENCODEURI,text.substring(end,start), encode));
					}
					buf.append(text.substring(start,end = matcher.end()));
				}
				buf.append(endecode(type== ID_ENCODEURI,text.substring(end), encode));
				//return URLEncoder.encode(text,encode);
				return buf.toString();
			}

		}
		throw new UnsupportedOperationException(toString());
	}

	private String endecode(boolean encode,String text, String charset)
			throws UnsupportedEncodingException {
		return encode?
				URLEncoder.encode(text,charset):
					URLDecoder.decode(text,charset);
	}

	private Number parseInt(String text, int readio) {
		try {
			return new Integer(Integer.parseInt(text, readio));
		} catch (NumberFormatException e) {
			return new Long(Long.parseLong(text, readio));
		}
	}

	public String toString() {
		for (int i = 0; i < IDMAP.length; i += 2) {
			if ((Integer) IDMAP[i] == type) {
				return (String) IDMAP[i + 1];
			}
		}
		return null;
	}

}
