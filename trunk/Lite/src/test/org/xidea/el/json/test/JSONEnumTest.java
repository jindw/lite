package org.xidea.el.json.test;

import org.junit.Test;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

public class JSONEnumTest {
	public A e = A.b;
	public static enum A{
		a,b
	}
	
	@Test
	public void testEnum(){
		String source = JSONEncoder.encode(this);
		System.out.println(source);
		JSONDecoder d = new JSONDecoder(false);
		JSONEnumTest e = d.decode(source, JSONEnumTest.class);
		System.out.println(e.e);
		
		source = "{\"e\":\"a\"}";
		 e = d.decode(source, JSONEnumTest.class);
		System.out.println(e.e);
		source = "{\"e\":2}";
		 e = d.decode(source, JSONEnumTest.class);
		System.out.println(e.e);
	}

}
