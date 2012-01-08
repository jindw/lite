package org.xidea.el.json.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.util.Collections;

import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

@SuppressWarnings({"unused","unchecked"})
public class JSONTest {

	static String getText(String file) {
		try {
			InputStreamReader in = new InputStreamReader(JSONTest.class
					.getResourceAsStream(file), "utf8");
			char[] buf = new char[1024];
			StringWriter out = new StringWriter();
			int len;
			while ((len = in.read(buf)) >= 0) {
				out.write(buf, 0, len);
			}
			return out.toString();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	@Test
	public void testComment() throws Exception{
		String text = "{//ddd\n\"aa\":1}";
		//JSONReader reader = new JSONReader();
		//Object o1 = reader.read(text);//這裡有死循環
		//new JSONTokener(text).nextValue();
		ExpressionImpl el = new ExpressionImpl(text);
		Object o1 = el.evaluate(Collections.EMPTY_MAP);
		Object o2 = new ExpressionImpl("{\"aa\":1}//ddd\n\n    ").evaluate(Collections.EMPTY_MAP);
		Object o3 = new ExpressionImpl("{\"aa\":1}//ddd").evaluate(Collections.EMPTY_MAP);
		Object o4 = new ExpressionImpl("{/**/\"aa\":1//dds,,+d\n}//ddd\n\n    ").evaluate(Collections.EMPTY_MAP);
		Object o5 = new ExpressionImpl("{//d...999!!!d\n\"aa\":/*____-----*/1}//ddd").evaluate(Collections.EMPTY_MAP);
		

		assertEquals(o1, o2);
		assertEquals(o3, o2);
		assertEquals(o4, o3);
		assertEquals(o4, o5);
	}

	@Test
	public void testTime() throws Exception {
		Object object = JSONDecoder.decode("{\"a\":[],\"d\":[1,23,\"123\",[1,\"dddd\"],[]]}");
		System.out.println(JSONEncoder.encode(object));		
		String[] tests = new String[]{"test-number.json","test-array.json","test.json"};
		for(int i=0;i<tests.length;i++){
			String file = tests[i];
			long jsc = 0;
			long jsr = 0;
			long elc = 0;
			long elr = 0;
			Object jso = null;
			Object elo = null;

			String json = getText(file);
			for (int j = 0; j < 10; j++) {
				long t1 = System.currentTimeMillis();

				//JSONReader reader = new JSONReader();
				//JSONTokener reader = new JSONTokener(json);
				long t2 = System.currentTimeMillis();
				jso = JSONDecoder.decode(json);
				//jso = reader.nextValue();

				long t3 = System.currentTimeMillis();
				ExpressionImpl el = new ExpressionImpl(json);
				long t4 = System.currentTimeMillis();
				elo = el.evaluate(Collections.EMPTY_MAP);
				long t5 = System.currentTimeMillis();
				jsc += (t2 - t1);
				jsr += (t3 - t2);

				elc += (t4 - t3);
				elr += (t5 - t4);
			}
			System.out.println("File:"+file);
			System.out.println("jsc-elc:" + jsc + "-" + elc);
			System.out.println("jsr-elr:" + jsr + "-" + elr);
			System.out.println("js-el:" + (jsc + jsr) + "-" + (elc + elr));
			JSONWriter writer = new JSONWriter();
			//assertEquals(writer.write(jso), writer.write(elo));
		}
	}

}
