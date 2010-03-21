package org.xidea.lite.parser.impl.test;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.parser.impl.ELParser;

public class ELParserTest extends ELParser{

	public ELParserTest() {
		super("", true);
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testFindELEnd() {
		test("${/>/}}", 1,-2);
		test("${/>/}/", 1,-2);
		test("${/1/}}", 1,-2);
		test("${/1/+/1/}/}", 1,-3);
		test("${/1/}", 1,-1);
		test("${/12\"}/.test(1)}", 1,-1);
		test("${1/2}/}", 1,-3);
		test("${/12\\\"}/.test(1)}", 1,-1);
		test("${1}", 1,-1);
	}

	private void test(String text,int elBegin,int expextEnd) {
		expextEnd = text.length()+expextEnd;
		int end = this.findELEnd(text,elBegin);
		assertEquals(expextEnd, end);
	}

}
