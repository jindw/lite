package org.xidea.el.test;

import java.util.HashMap;

import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;

public class JSELTest {
    private ExpressionFactory factory = ExpressionFactory.getInstance();

    public int testAdd(int i,int j){
        return i+j;
    }
    @Test
    public void testExpression(){
        Expression el = factory.create("12 * 60 + 100");
        Object result1 = el.evaluate(null);
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
    public static void main(String[] args){
        JSELTest test = new JSELTest();
        test.testExpression();
        test.testMapContext();
        test.testFunction();
    }
}
