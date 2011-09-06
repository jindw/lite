package org.xidea.lite.test.oldcases;

import java.util.regex.Pattern;

public class TestMain {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		String key = "xmlns:h";
		int p = key.indexOf(':');
		String text = "<ss s <h:client ";
		String regexp = "[<\\s]" + key.substring(p + 1)
		+ "\\:";
		System.out.println(Pattern.compile(regexp).matcher(text).find());
		System.out.println(regexp+text);
	}

}
