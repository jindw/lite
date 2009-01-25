package org.xidea.lite.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.parser.ExpressionToken;
import org.xidea.lite.Template;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.XMLParser;

@SuppressWarnings("unchecked")
public class TemplateTimeTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test2() throws Exception {
		System.out.println(String[].class);
		System.out.println(Object[][].class);
		System.out.println(String[].class.getSuperclass());
		System.out.println(Object[][].class.getSuperclass());
		System.out.println(new String[0][0] instanceof Object[][]);

	}

	JSONEncoder encoder = new JSONEncoder() {
		protected void print(Object object, Writer out,
				Collection<Object> cached) throws IOException {
			if (object instanceof ExpressionToken[]) {
				ExpressionToken[] list = (ExpressionToken[]) object;
				out.write('[');
				int i = list.length;
				while (i-- > 0) {
					print(list[i], out, cached);
					if (i > 0) {
						out.write(',');
					}
				}
				out.write(']');
			} else if (object instanceof ExpressionToken) {
				ExpressionToken token = (ExpressionToken) object;
				int type = token.getType();
				switch (type) {
				case ExpressionToken.VALUE_CONSTANTS:
					print(token.getParam(), out, cached);
					break;
				case ExpressionToken.VALUE_VAR:
				case ExpressionToken.OP_MAP_PUSH:
				case ExpressionToken.OP_STATIC_GET_PROP:
					print(new Object[] { type, token.getParam() }, out, cached);
					break;
				default:
					print(new Object[] { type }, out, cached);
				}
			} else {
				super.print(object, out, cached);
			}
		}
	};
	@Test
	public void test() throws Exception {
		ParseContext2 xcontext = new ParseContext2();
		XMLParser parser = new XMLParser();
		parser.parse(this.getClass().getResource("asciitable.xhtml"), xcontext);
		Template xt1 = new Template1(xcontext.getResult());
		List<Object> tree = xcontext.toResultTree();
		StringWriter out = new StringWriter();
		encoder.encode(tree, out, null);
		System.out.println(out);
		org.xidea.lite.Template xt = new Template(tree);
		Template2 xt2 = new Template2(tree);
		HashMap context = new HashMap();
		context.put("data", Arrays.asList("0123456789ABCDEF".split("")));
		context.put("name", "test");
		context.put("border", "1px");
		String result1 = null;
		String result2 = null;
		String result3 = null;
		long t1 = 0;
		long t2 = 0;
		long t3 = 0;
		for (int i = 0; i < 50; i++) {
			// result2 = testXMLTemplate(xt, context);
			long m1 = System.currentTimeMillis();
			result1 = testXMLTemplate(xt2, context);

			long m2 = System.currentTimeMillis();
			result2 = testXMLTemplate(xt, context);

			long m3 = System.currentTimeMillis();
			result3 = testXMLTemplate(xt1, context);

			long m4 = System.currentTimeMillis();
			t1 += m2 - m1;
			t2 += m3 - m2;
			t3 += m4 - m3;
		}

		System.out.println(result1);
		System.out.println("====================");
		System.out.println(result2);

		System.out.println(t1);
		System.out.println(t2);
		System.out.println(t3);

		assertEquals(result1, result2);
		assertEquals(result3, result2);
	}

	public String testXMLTemplate(org.xidea.lite.Template t, Map context)
			throws Exception {
		StringWriter out = new StringWriter();
		t.render(context, out);
		out.flush();
		return out.toString();
	}

}
class ParseContext2 extends ParseContext{
	public List<Object> getResult(){
		return super.getResult();
	}
};