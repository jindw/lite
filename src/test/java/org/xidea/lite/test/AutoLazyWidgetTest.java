package org.xidea.lite.test;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.impl.ExpressionFactoryImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xml.sax.SAXException;

import javax.script.ScriptException;
import java.io.*;
import java.util.*;

@RunWith(Parameterized.class)
public class AutoLazyWidgetTest {
    private Map<String, String> resultMap;
    AutoSyntaxTest.TestItem currentItem;
    @Parameters(name="{index} -{0}")

    public static Collection<Object[]> getParams() {
        String[] casefiles = {
                "widget-lazy.xml"};

        return AutoSyntaxTest.getParams(casefiles);

    }
    public AutoLazyWidgetTest(AutoSyntaxTest.TestItem item) throws IOException, SAXException {
        this.currentItem = item;
        try {
            HashMap<String,Object> ctx = JSONDecoder.decode(item.model);
            item.sourceMap.put(AutoSyntaxTest.TEST_XHTML, item.source);
            this.resultMap = LiteTest.runTemplate(item.sourceMap, ctx, AutoSyntaxTest.TEST_XHTML,
                    item.expect);

            if(item.format){
                item.expect = LiteTest.normalizeXML(item.expect);
            }
        } catch (Error e) {
            e.printStackTrace();
            throw e;
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw e;
        }
    }

    @Test
    public void testJava() throws IOException {
        test("java");
    }

    //Test
    public void testPhp() throws IOException {
        test("php");
    }

//    @Test
//    public void testJS() throws IOException {
//        test("js");
//    }
    public void test(String type) throws IOException {
        String value = resultMap.get(type);
        if(!currentItem.expect.equals(value)){
            if(currentItem.format){
                value = LiteTest.normalizeXML(value);
            }else{
                value=value.replace("&quot;", "&#34;").replace("&lt;", "&#60;");
            }
        }
        Assert.assertEquals( "运行结果有误：#" +currentItem.source+"\n\n",
                currentItem.expect, value);
    }

}
