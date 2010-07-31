package org.xidea.el.json.test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

public class JSONDecoderTest {
	private int int0 = hashCode();
	private String str = ""+int0;
	private boolean b0 = 1 == (int0 & 1);
	private String[] strs = {str};
	private HashMap map;
	private List list;
	
	private JSONDecoder decoder = new JSONDecoder(false);
	@Test
	public void testDecodeObject() throws IOException {
		JSONDecoderTest test0 = new JSONDecoderTest();
		String str1 = JSONEncoder.encode(test0);
		JSONDecoderTest test1 = decoder.decode(str1,JSONDecoderTest.class);
		String str2 = JSONEncoder.encode(test1);
		System.out.println(str2);
		assertEquals(str1,str2);
		assertFalse(test1 == test0);
		
		test1.setMap((Map)JSONDecoder.decode(str1));
		String str3 = JSONEncoder.encode(test1);
		JSONDecoderTest test3 = decoder.decode(str3,JSONDecoderTest.class);
		String str4 = JSONEncoder.encode(test3);
		System.out.println(str4);
		assertEquals(str3,str4);
		
	}

	public int getInt0() {
		return int0;
	}

	public void setInt0(int int0) {
		this.int0 = int0;
	}

	public String getStr() {
		return str;
	}

	public void setStr(String str) {
		this.str = str;
	}

	public boolean isB0() {
		return b0;
	}

	public void setB0(boolean b0) {
		this.b0 = b0;
	}

	public String[] getStrs() {
		return strs;
	}

	public void setStrs(String[] strs) {
		this.strs = strs;
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = new HashMap(map);
	}

	public List getList() {
		return list;
	}

	public void setList(List list) {
		this.list = list;
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testEncodeObject() throws IOException {
		//非JSON标准,注释,多行
		assertEquals("a\nb",JSONDecoder.decode("/**/\"a\nb\""));
		assertEquals("\"a\\nb\"",JSONEncoder.encode("a\nb"));
		assertEquals(-1,JSONDecoder.decode("-1"));
		assertEquals(-1.1,JSONDecoder.decode("-1.1"));
		assertEquals(-0xFF1,JSONDecoder.decode("-0xFF1"));

		assertEquals(1,JSONDecoder.decode("1"));
		assertEquals(1.1,JSONDecoder.decode("1.1"));
		assertEquals(0xFF1,JSONDecoder.decode("0xFF1"));
		
	}

	@Test
	public void testJSEL() throws IOException {
		Object o = new ExpressionImpl("{key:'value',n:-1}").evaluate(null);
		System.out.println(o);
		assertEquals(JSONDecoder.decode("{\"key\":\"value\",\"n\":-1}"), o);
	}


}
