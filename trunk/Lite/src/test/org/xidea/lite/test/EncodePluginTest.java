package org.xidea.lite.test;

import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Pattern;

import junit.framework.Assert;

import org.junit.Test;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.ValueStack;
import org.xidea.lite.EncodePlugin;

public class EncodePluginTest implements ValueStack {
	static Pattern P = Pattern.compile("[&<'\"]");
	static String data;
	EncodePlugin plugin = new EncodePlugin();
	Object value;

	{
		plugin.initialize(null, new Object[] { new Object[] { null,
				ExpressionFactory.getInstance().create("value") } });
	}

	@Test
	public void testSimple() throws IOException {

		test("123<", "123&lt;");
		test("123\"", "123&#34;");
		test("123\'", "123&#39;");
		test("123\'\"", "123&#39;&#34;");
		test("123&", "123&#38;");
		test("123&\"", "123&#38;&#34;");
	}

	@Test
	public void testEntry() throws IOException {

		test("123&\"", "123&#38;&#34;");
		test("123&xx;&", "123&xx;&#38;");
		test("123&#124;;&", "123&#124;;&#38;");
		test("123&x124;;&", "123&x124;;&#38;");
		test("123&#124;", "123&#124;");
		test("123&x124;;", "123&x124;;");
		test("123&x124", "123&#38;x124");
	}

	private void test(String source, String dest) throws IOException {
		StringWriter out = new StringWriter();
		value = source;
		plugin.execute(this, out);
		Assert.assertEquals(dest, out.toString());
		{
			String postfix = "123456";
			source += postfix;
			dest += postfix;
			out = new StringWriter();
			value = source;
			plugin.execute(this, out);
			Assert.assertEquals(dest, out.toString());
		}

	}

	public void put(Object key, Object value) {
	}

	public Object get(Object key) {
		return value;
	}

	static {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < 1204; i++) {
			char c = (char) (Math.random() * Character.MAX_VALUE);
			switch (c) {
			case '"':
			case '&':
			case '\'':
			case '<':
				break;
			default:
				buf.append(c);
			}
		}
		data = buf.toString();
	}

	public static void main(String[] args) {
		runPattern(data);
		runIndexOf(data);
		long pt = 0;
		long it = 0;
		for (int i = 0; i < 100; i++) {
			pt += runPattern(data);
			it += runIndexOf(data);
		}
		System.out.println(pt + "/" + it + '#' + (1.0 * pt / it));
	}

	static long runPattern(String data) {
		long t1 = System.nanoTime();
		P.matcher(data).find();
		long t2 = System.nanoTime();
		return t2 - t1;
	}

	static long runIndexOf(String data) {
		long t1 = System.nanoTime();
		data.indexOf('"');
		data.indexOf('\'');
		data.indexOf('&');
		data.indexOf('<');
		long t2 = System.nanoTime();
		return t2 - t1;
	}

}
