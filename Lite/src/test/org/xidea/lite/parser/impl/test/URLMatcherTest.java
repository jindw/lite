package org.xidea.lite.parser.impl.test;


import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.parser.impl.URLMatcher;

public class URLMatcherTest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testBeginMatch(){
		Assert.assertTrue(URLMatcher.createMatcher("**").match("/bb/aa"));
		Assert.assertTrue(URLMatcher.createMatcher("**/aa").match("/bb/aa"));
		Assert.assertTrue(URLMatcher.createMatcher("**b/aa").match("/bb/aa"));
		Assert.assertTrue(URLMatcher.createMatcher("**/bb/aa").match("/bb/aa"));
		Assert.assertTrue(!URLMatcher.createMatcher("**//bb/aa").match("/bb/aa"));
	}
	@Test
	public void testEndMatch(){
		Assert.assertTrue(URLMatcher.createMatcher("**").match("/bb/aa"));
		Assert.assertTrue(URLMatcher.createMatcher("/bb**").match("/bb"));
		Assert.assertTrue(URLMatcher.createMatcher("/bb/a**").match("/bb/aa"));
		Assert.assertTrue(URLMatcher.createMatcher("/bb/aa**").match("/bb/aa"));
		Assert.assertTrue(!URLMatcher.createMatcher("/bb/aa/**").match("/bb/aa"));
	}

	@Test
	public void testBothMatch(){
		Assert.assertTrue(URLMatcher.createMatcher("**").match("/bb/aa"));
		Assert.assertTrue(URLMatcher.createMatcher("**/bb**").match("/bb"));
		Assert.assertTrue(URLMatcher.createMatcher("**/bb/a**").match("/bb/aa"));
		Assert.assertTrue(URLMatcher.createMatcher("**/bb/aa**").match("/bb/aa"));
		Assert.assertTrue(!URLMatcher.createMatcher("**/bb/aa/**").match("/bb/aa"));
	}

}
