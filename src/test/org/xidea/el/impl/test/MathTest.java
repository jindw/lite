package org.xidea.el.impl.test;

import static org.junit.Assert.assertEquals;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;

public class MathTest {
	ExpressionFactory factory = new ExpressionFactoryImpl();
	ScriptEngine se = new ScriptEngineManager().getEngineByExtension("js");

	@Test
	public void testMaxMin() throws Exception {
		test("Math.max(1.2,2,4,1/0,199)");
		test("Math.min(1.2,2,4,1,2)");
		test("Math.max(1.2,2.1,-4,1,2)");
		test("Math.min(1.2,2,4,-1/0,199)");
	}

	@Test
	public void testConstant() throws Exception {
		test("Math.E");
		test("Math.PI");
		test("Math.LN10");
		test("Math.LN2");
		test("Math.LOG2E");
		test("Math.LOG10E");
		test("Math.SQRT1_2");
		test("Math.SQRT2");
	}

	@Test
	public void testRandom() throws Exception {
		for (int i = 0; i < 100; i++) {
			test("Math.random()>0");
			test("Math.random()<1");
			test("Math.random()!=Math.random()");
		}
	}


	@Test
	public void testAngle() throws Exception {
		for (int i = 0; i < 10; i++) {
			double value1 = i * 10 * (Math.random() - 0.5);
			// 15.8.2.1 abs(x)
			test("Math.abs(" + value1 + ")");
			// 15.8.2.2 acos(x)
			test("Math.acos(" + value1 + ")");
			// 15.8.2.3 asin(x)
			test("Math.asin(" + value1 + ")");
			// 15.8.2.4 atan(x)
			test("Math.atan(" + value1 + ")");
			// 15.8.2.6 ceil(x)
			test("Math.ceil(" + value1 + ")");
			// 15.8.2.7 cos(x)
			test("Math.cos(" + value1 + ")");
			// 15.8.2.9 floor(x)
			test("Math.floor(" + value1 + ")");
			// 15.8.2.10 log(x)
			test("Math.log(" + value1 + ")");
			// 15.8.2.16 sin(x)
			test("Math.sin(" + value1 + ")");
			// 15.8.2.17 sqrt(x)
			test("Math.sqrt(" + value1 + ")");
			// 15.8.2.18 tan(x)
			test("Math.tan(" + value1 + ")");
		}
	}

	@Test
	public void testPowExp() throws Exception {
		for (int i = 0; i < 10; i++) {
			double value1 = i * 10 * (Math.random() - 0.5);
			double value2 = i * 10 * (Math.random() - 0.5);
			// 15.8.2.13 pow(x, y)
			testLike("Math.pow(" + value1 + "," + value2 + ")",0.01);
			// 15.8.2.8 exp(x)
			testLike("Math.exp(" + value1 + ")",0.01);
		}
	}

	private void testLike(String exp, double max) throws ScriptException {
		Expression el = factory.create(exp);
		Number jsv = (Number) se.eval(exp);
		Number elv = (Number) el.evaluate(); 
		
		if(Double.isNaN(jsv.doubleValue()) != Double.isNaN(elv.doubleValue())){
			Assert.fail("误差太大："+exp+"\n"+jsv+"\n"+elv);
		}
		max *= Math.max(jsv.doubleValue(),elv.doubleValue());
		double offset = Math.abs(jsv.doubleValue()-elv.doubleValue());
		if(offset>max){
			Assert.fail("误差太大了："+exp+"\n"+jsv+"\n"+elv);
		}
		
	}
	private void test(String exp) throws ScriptException {
		Expression el = factory.create(exp);
		Object jsv = se.eval(exp);
		Object elv = el.evaluate(); 
		//System.out.println(exp +":"+jsv);
		//System.out.println(":"+elv);
		// System.out.println(jsv.getClass());
		assertEquals(jsv, elv);
	}

}
