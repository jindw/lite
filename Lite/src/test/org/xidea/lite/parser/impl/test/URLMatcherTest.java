package org.xidea.lite.parser.impl.test;


import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.impl.PathMatcher;

public class URLMatcherTest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testBeginMatch(){
		Assert.assertTrue(new PathMatcher("**").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("**/aa").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("**b/aa").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("**/bb/aa").match("/bb/aa"));
		Assert.assertTrue(!new PathMatcher("*/bb/aa").match("\\/bb/aa"));
	}
	@Test
	public void testEndMatch(){
		Assert.assertTrue(new PathMatcher("**").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("/bb**").match("/bb"));
		Assert.assertTrue(new PathMatcher("/bb/a**").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("/bb/aa**").match("/bb/aa"));
		Assert.assertTrue(!new PathMatcher("/bb/aa/**").match("/bb/aa"));
	}

	@Test
	public void test0FileMatch(){
		Assert.assertTrue(new PathMatcher("**").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("/*/bb").match("/a/bb"));
		Assert.assertTrue(!new PathMatcher("/*/bb").match("/a/a/bb"));
		Assert.assertTrue(new PathMatcher("/**/bb").match("/a/a/bb"));
		Assert.assertTrue(new PathMatcher("/**/bb").match("/bb"));
		
	}

	@Test
	public void testBothMatch(){
		Assert.assertTrue(new PathMatcher("**").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("**/bb**").match("/bb"));
		Assert.assertTrue(new PathMatcher("**/bb/a**").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("**/bb/aa**").match("/bb/aa"));
		Assert.assertTrue(new PathMatcher("**/bb/aa/**/c").match("/bb/aa/c"));
	}

}
