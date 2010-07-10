package org.xidea.lite.parser.impl.test;


import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.impl.URIMatcher;

public class URLMatcherTest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testBeginMatch(){
		Assert.assertTrue(URIMatcher.createMatcher("**").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("**/aa").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("**b/aa").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("**/bb/aa").match("/bb/aa"));
		Assert.assertTrue(!URIMatcher.createMatcher("*/bb/aa").match("\\/bb/aa"));
	}
	@Test
	public void testEndMatch(){
		Assert.assertTrue(URIMatcher.createMatcher("**").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("/bb**").match("/bb"));
		Assert.assertTrue(URIMatcher.createMatcher("/bb/a**").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("/bb/aa**").match("/bb/aa"));
		Assert.assertTrue(!URIMatcher.createMatcher("/bb/aa/**").match("/bb/aa"));
	}

	@Test
	public void test0FileMatch(){
		Assert.assertTrue(URIMatcher.createMatcher("**").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("/*/bb").match("/a/bb"));
		Assert.assertTrue(!URIMatcher.createMatcher("/*/bb").match("/a/a/bb"));
		Assert.assertTrue(URIMatcher.createMatcher("/**/bb").match("/a/a/bb"));
		Assert.assertTrue(URIMatcher.createMatcher("/**/bb").match("/bb"));
		
	}

	@Test
	public void testBothMatch(){
		Assert.assertTrue(URIMatcher.createMatcher("**").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("**/bb**").match("/bb"));
		Assert.assertTrue(URIMatcher.createMatcher("**/bb/a**").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("**/bb/aa**").match("/bb/aa"));
		Assert.assertTrue(URIMatcher.createMatcher("**/bb/aa/**/c").match("/bb/aa/c"));
	}

}
