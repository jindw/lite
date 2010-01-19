package org.xidea.el.operation.test;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.xidea.el.fn.ECMA262Impl;
import org.xidea.el.fn.NumberArithmetic;

public class NumberArithmeticTest {
	NumberArithmetic na = new NumberArithmetic();
	@Test
	public void testIsNaN() {
		assertEquals(true, NumberArithmetic.isNaN(Float.NaN));
		assertEquals(true, NumberArithmetic.isNaN(Double.NaN));
		assertEquals(false, NumberArithmetic.isNaN(Double.POSITIVE_INFINITY));
	}

	@Test
	public void testIsNI() {
		assertEquals(true, NumberArithmetic.isNI(Float.NEGATIVE_INFINITY));
		assertEquals(true, NumberArithmetic.isNI(Double.NEGATIVE_INFINITY));
		assertEquals(false, NumberArithmetic.isNI(Float.POSITIVE_INFINITY));
		assertEquals(false, NumberArithmetic.isNI(Double.POSITIVE_INFINITY));
		assertEquals(false, NumberArithmetic.isNI(1));
		assertEquals(false, NumberArithmetic.isNI(Double.NaN));
		assertEquals(false, NumberArithmetic.isNI(Float.NaN));
	}

	@Test
	public void testIsPI() {
		assertEquals(false, NumberArithmetic.isPI(Float.NEGATIVE_INFINITY));
		assertEquals(false, NumberArithmetic.isPI(Double.NEGATIVE_INFINITY));
		assertEquals(true, NumberArithmetic.isPI(Float.POSITIVE_INFINITY));
		assertEquals(true, NumberArithmetic.isPI(Double.POSITIVE_INFINITY));
		assertEquals(false, NumberArithmetic.isPI(1));
		assertEquals(false, NumberArithmetic.isPI(Double.NaN));
		assertEquals(false, NumberArithmetic.isPI(Float.NaN));
	
	}

	@Test
	public void testIsType() {
		// fail("Not yet implemented");
	}

	@Test
	public void testCompare() {
		System.out.println(Double.POSITIVE_INFINITY-Double.POSITIVE_INFINITY);
		System.out.println(Double.NEGATIVE_INFINITY-Double.POSITIVE_INFINITY);
		System.out.println(Double.POSITIVE_INFINITY-Double.NEGATIVE_INFINITY);
		
		assertEquals(100, na.compare(Double.NaN, Double.POSITIVE_INFINITY,100));
		assertEquals(100, na.compare(Double.POSITIVE_INFINITY, Double.NaN,100));
		

		assertEquals(0, na.compare(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY,100));
		assertEquals(0, na.compare(Double.NEGATIVE_INFINITY,Double.NEGATIVE_INFINITY, 100));
		
		assertEquals(-1, na.compare(1, Double.POSITIVE_INFINITY,100));
		assertEquals(1, na.compare(Double.POSITIVE_INFINITY,1, 100));
		
		assertEquals(1, na.compare(1, Double.NEGATIVE_INFINITY,100));
		assertEquals(-1, na.compare(Double.NEGATIVE_INFINITY,1, 100));

	}

	@Test
	public void testAdd() {
		assertEquals(2, na.add(1, 1));
		assertEquals(2.0, na.add(0.5, 1.5));
	}

	@Test
	public void testSubtract() {
		assertEquals(0, na.subtract(1, 1));
		assertEquals(-1.0, na.subtract(0.5, 1.5));
	}

	@Test
	public void testMultiply() {
		assertEquals(1, na.multiply(1, 1));
		assertEquals(0.75, na.multiply(0.5, 1.5));
	}

	@Test
	public void testDivide() {
		assertEquals(0.5, na.divide(1, 2,true));
		assertEquals(0, na.divide(1, 2,false));
		assertEquals(Float.NaN, na.divide(0, 0,false));
		assertEquals(Float.POSITIVE_INFINITY, na.divide(1, 0,false));
		assertEquals(Float.NEGATIVE_INFINITY, na.divide(-1, 0,false));
	}

	@Test
	public void testModulus() {
		assertEquals(1, na.modulus(1, 2));
		assertEquals(0, na.modulus(2, 2));
	}


	@Test
	public void testToNumber() {
		assertEquals(17, ECMA262Impl.ToNumber("0x11"));
		assertEquals(17, ECMA262Impl.ToNumber("0X11"));
		assertEquals(9, ECMA262Impl.ToNumber("011"));;
		assertEquals(-9, ECMA262Impl.ToNumber("-011"));
		assertEquals(11, ECMA262Impl.ToNumber("11"));
		assertEquals(10, ECMA262Impl.ToNumber("1E1"));
		assertEquals(10, ECMA262Impl.ToNumber("1e1"));
		assertEquals(1.1, ECMA262Impl.ToNumber("1.1"));;
		assertEquals(10, ECMA262Impl.ToNumber("+1e1"));
		assertEquals(-1.1, ECMA262Impl.ToNumber("-1.1"));
		assertEquals(1.1E1, ECMA262Impl.ToNumber("1.1E1"));
		assertEquals(1.1e-1, ECMA262Impl.ToNumber("1.1e-1"));
		assertEquals(1.1E1, ECMA262Impl.ToNumber("+1.1E1"));
		assertEquals(-1.1e-1, ECMA262Impl.ToNumber("-1.1e-1"));
	}

}
