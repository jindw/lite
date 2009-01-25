package org.xidea.el.json;

public class JSONDecoder {
	public static Object decode(String value) {
		return new JSONTokenizer(value).parse();
	}
}
