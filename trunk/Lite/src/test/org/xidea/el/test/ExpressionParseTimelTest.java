package org.xidea.el.test;


import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;

import org.xidea.el.ExpressionToken;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.parser.ExpressionTokenizer;

public class ExpressionParseTimelTest {
	private ExpressionFactoryImpl factory = new ExpressionFactoryImpl();

	public static void main(String[] args) throws Exception {
		new ExpressionParseTimelTest().testELTime();
	}
	public void testELTime() throws IOException {
		test("\"123\"");
		test("123444");
		test("sss[123444]");
		test("aaa[1][2][\"123\"]");
		test("aaa.bb.cc.dd");
		test("aaa+ddd(1)+asd.cc(1,2,3)");
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

	private void test(String el) throws IOException {
		ExpressionTokenizer tokener = new ExpressionTokenizer(el);
		ExpressionToken[] els = tokener.getTokens().getData();
		StringWriter out = new StringWriter();
		encoder.encode(els, out, null);
		String jsonel = out.toString();
		int count = 1000,count2;
		long t1 = 0, t2 = 0;
		while (count-- > 0) {
			long m1 = System.currentTimeMillis();
			count2 = 100;
			while (count2-- > 0) {
				factory.create(el);
			}
			long m2 = System.currentTimeMillis();
			count2 = 100;
			while (count2-- > 0) {
				factory.create(JSONDecoder.decode(jsonel));
			}
			long m3 = System.currentTimeMillis();
			t1 += m2 - m1;
			t2 = m3 - m2;
		}
		System.out.println(el);
		System.out.println(jsonel);
		System.out.println("el:"+t1);
		System.out.println("jsonel:"+t2);
		System.out.println();

	}

}
