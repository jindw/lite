package org.xidea.lite.test.oldcases;

import java.util.HashMap;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.test.ELTest;

public class JSELTest {
    private ExpressionFactory factory = ExpressionFactoryImpl.getInstance();

    
    private final double EPS6 = 0.000001;

     @Test
    public void testMapContext2() {
        Expression el = factory.create("var1 +2 * var2 + null");

        Object result21 = el.evaluate("var1", 123, "var2", 456, "obj2",
 this);
       System.out.println(String.format("result21 = %s", result21));
        Assert.assertEquals(1035.0, ((Number) result21).doubleValue(), EPS6);
    }
    
    public int testAdd(int i,int j){
        return i+j;
    }
    @Test
    public void testExpression(){
        Expression el = factory.create("12 * 60 + 100");
        Object result1 = el.evaluate();
        System.out.println("result1");
        System.out.println(result1);
    }
    @Test
    public void testMapContext(){
        Expression el = factory.create("var1 +2 * var2 + obj.testAdd(1,2)");

        Object result21 = el.evaluate("var1",123,
                                      "var2",456,
                                      "obj",this);
        System.out.println("result21:");
        System.out.println(result21);
        
        //通过方式传递变量
        HashMap<String, Object> context = new HashMap<String, Object>();
        context.put("var1",111);
        context.put("var2",222);
        context.put("obj",this); //设置对象

        Object result22 = el.evaluate(context);
        System.out.println("result22:");
        System.out.println(result22);
    }
    @Test
    public void testFunction(){
        Expression el = factory.create("100 + testAdd(1,2)");
        Object result3 = el.evaluate(this);
        System.out.println("result3");
        System.out.println(result3);
    }
	@Test
	public void testNumber(){
		System.out.println(Double.NaN);
		System.out.println(Double.NEGATIVE_INFINITY);
		ELTest.testEL("{\"abc\":null}","'abc'*123",false);
	}

	@Test
	public void testIn(){
		String expression = "srcPort in [1,2,30]";  
		Expression expressionInstance = factory.create(expression); 
		HashMap variables = new HashMap();  
		variables.put("srcPort", 2);  
		System.out.println(expressionInstance.evaluate(variables));  
	}
	@Test
	public void testArray(){
		HashMap<String, Object> context = new HashMap<String, Object>();
		//context.put("array", "1,2,3,4".split(","));
		context.put("array", new String[]{"1","2","3","4"});
		ELTest.testEL(context,"array.slice(1,2)",false);
	}
	
    public static void main(String[] args){
        JSELTest test = new JSELTest();
        test.testExpression();
        test.testMapContext();
        test.testFunction();
    }
}
