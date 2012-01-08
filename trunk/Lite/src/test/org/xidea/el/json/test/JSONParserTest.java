package org.xidea.el.json.test;


import org.junit.Before;
import org.junit.Test;
import org.xidea.el.json.JSONDecoder;

public class JSONParserTest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testComment(){
		doParser("/* / */{\"a\":1}");
		doParser("{\"a\":1}//111");
	}

	@Test
	public void testObject(){
		doParser("{\"a\":1}");
		doParser("{\'a\':1}//111");
	}

	public void doParser(String json){
		JSONDecoder.decode(json);
	}
}
