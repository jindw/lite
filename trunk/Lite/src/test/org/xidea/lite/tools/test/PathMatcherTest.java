package org.xidea.lite.tools.test;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Test;
import org.xidea.lite.tools.PathMatcher;

public class PathMatcherTest {

	@Test
	public void testCreateMatcher() {
		fail("Not yet implemented");
	}

	@Test
	public void testMatch() {
		match("/doc/guide",false,"/doc/guide/*.xhtml");
		match("/doc/guide/",false,"/doc/guide/*.xhtml","/doc/guide/*.html");
		match("/doc/guide/1.html",false,"/doc/guide/*.xhtml");
		match("/doc/guide/1.html",true,"/doc/guide/*.xhtml","/doc/guide/*.html");
		match("/doc/1.html",false,"/doc/guide/*.xhtml","/doc/guide/*.html");
		match("/guide/1.html",false,"/doc/guide/*.xhtml","/doc/guide/*.html");
		match("doc/guide/1.html",false,"/doc/guide/*.xhtml","/doc/guide/*.html");
		
	}

	@Test
	public void testMust() {
		must("/doc/guide/",true,"/doc/guide/**");
		must("/doc/guide/a/",true,"/doc/guide/**");
		must("/doc/guide/aa/bb/",true,"/doc/guide/**");
		must("/doc/guide/1.txt",true,"/doc/guide/**");
		must("/doc/",false,"/doc/guide/**");
		must("/",false,"/doc/guide/**");
		must("/doc/guide",false,"/doc/guide/**");
		
	}

	@Test
	public void testMaybe() {
		maybe("/doc/guide/a/",false,"/doc/guide/*.xhtml","/doc/book/*.xhtml");
		maybe("/doc/guide/a/b/",true,"/doc/guide/**.xhtml");
		maybe("/doc/book/a/b/",false,"/doc/guide/**.xhtml","/doc/book/*.xhtml");
		maybe("/doc/aa/",false,"/doc/guide/**.xhtml");
		maybe("/doc/guide/a/1.txt",false,"/doc/guide/*.xhtml","/doc/book/*.xhtml");
	}

	void match(String path,boolean result,String... pattern){
		Assert.assertEquals(result,PathMatcher.createMatcher(pattern).match(path));
	}
	void maybe(String path,boolean result,String... pattern){
		Assert.assertEquals(result,PathMatcher.createMatcher(pattern).maybe(path));
	}
	void must(String path,boolean result,String... pattern){
		Assert.assertEquals(result,PathMatcher.createMatcher(pattern).must(path));
	}
	
}
