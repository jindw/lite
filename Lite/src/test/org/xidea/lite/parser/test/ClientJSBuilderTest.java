package org.xidea.lite.parser.test;

import static org.junit.Assert.*;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.JScrollBar;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.Expression;
import org.xidea.el.ExpressionFactory;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parser.Java6JSBuilder;
import org.xidea.lite.parser.ParseContext;
import org.xidea.lite.parser.ParseContextImpl;
import org.xidea.lite.parser.XMLParser;

public class ClientJSBuilderTest {
	private ExpressionFactory clientExpressionFactory = new ExpressionFactory() {
		public Expression create(Object el) {
			throw new UnsupportedOperationException();
		}

		public Object parse(String expression) {
			return expression;
		}

	};
	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testBuildJS() {
		URL url = this.getClass().getResource("format-test.xhtml");
		ParseContext context2 = new ParseContextImpl(url);
		// 前端直接压缩吧？反正保留那些空白也没有调试价值
		// context2.setCompress(context.isCompress());
		context2.setCompress(true);
		context2.setExpressionFactory(clientExpressionFactory);
		List<Object> liteCode = new XMLParser().parse(url,context2);
		String result = new Java6JSBuilder().buildJS("test", liteCode);
		System.out.println("==JS Code==");
		System.out.println(result);
		boolean isError = Pattern.compile("[\r\n]alert",Pattern.MULTILINE).matcher(result).find();
		Assert.assertTrue("生成失败"+result,!isError);
	}
	@Test
	public void testClient() {
		List<Object> clientLiteCode = new XMLParser().parse(this.getClass()
				.getResource("asciitable-client.xhtml"));
		System.out.println("==JS Code==");
		System.out.println(clientLiteCode);
		String result = (String)clientLiteCode.get(0);
		boolean isError = Pattern.compile("[\r\n]alert",Pattern.MULTILINE).matcher(result).find();
		Assert.assertTrue("生成失败"+result,!isError);
	}

}
