package org.xidea.el.impl.test;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.Reference;
import org.xidea.el.impl.ExpressionImpl;

public class ResultExpressionTest {
	private HashMap<Object, Object> root;
	private String text;

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		this.root = new HashMap();
		HashMap key3 = new HashMap();
		root.put("key1", 1);
		root.put("key2", new int[] { 1, 2, 3 });
		key3.put("key1", new HashMap(root));
		root.put("key3", key3);
	}

	@Test
	public void testPropertySet() {
		ExpressionImpl el = new ExpressionImpl("text");
		ResultExpressionTest context = new ResultExpressionTest();
		Reference prepare = el.prepare(context);
		String testText = "test1"+System.currentTimeMillis();
		
		prepare.setValue(testText);
		Assert.assertEquals(testText,context.getText());
	}

	@Test
	public void testPropertyGet() {
		doTest("1+key2[1]+key3.key1.key2[0]");
	}

	private void doTest(String el) {
		ExpressionImpl el1 = new ExpressionImpl(el);
		ExpressionImpl el2 = new ExpressionImpl(el);
		long t1 = 0, t2 = 0;
		Object v1 = null, v2 = null;
		int i = 1, j = 1000;
		while (i-- > 0) {
			int k = j;
			t1 -= System.nanoTime();
			while (k-- > 0) {
				v1 = el1.evaluate(root);
			}
			t1 += System.nanoTime();

			k = j;
			t2 -= System.nanoTime();
			while (k-- > 0) {
				v2 = el2.evaluate(root);
			}
			t2 += System.nanoTime();
		}
		System.out.println("rel:" + t1);
		System.out.println("wel:" + t2);
		System.out.println(v1);
		System.out.println(v2);
	}

}
