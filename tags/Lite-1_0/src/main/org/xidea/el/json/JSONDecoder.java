package org.xidea.el.json;

public class JSONDecoder {
	@SuppressWarnings("unchecked")
	public static <T> T decode(String value) {
		return (T)new JSONTokenizer(value).parse();
	}
}
