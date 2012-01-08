package org.xidea.lite.test.oldcases;


import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xidea.el.impl.ReflectUtil;
import org.xidea.el.json.JSONEncoder;
import org.xidea.lite.parse.ParseContext;


public class URLURITest {

	@Before
	public void setUp() throws Exception {
	}
	@Test
	public void testResourceContext() throws Exception{
		String base = "http://lh:8080/test";
		String base2 = "lite:/";
		ParseContext rc =LiteTestUtil.buildParseContext(new URI(base));
		Assert.assertEquals(base2+"test.xml", rc.createURI("test.xml").toString());
		Assert.assertEquals(base2+"test.xml", rc.createURI("./test.xml").toString());
		
		base = "http://lh:8080/test/aa/bb/";
		rc = LiteTestUtil.buildParseContext(new URI(base));
		rc.setCurrentURI(new URI(base));
		System.out.println(rc.getCurrentURI());
		Assert.assertEquals("http://lh:8080/test/aa/bb/test.xml", rc.createURI("test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../../test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../.././test.xml").toString());
		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI(".././../test.xml").toString());
//		Assert.assertEquals("http://lh:8080/test/test.xml", rc.createURI("../././../test.xml").toString());
		
	
	}
	@Test
	public void testURIRelative() throws Exception{
		System.out.println(new File(".").toURI().getPath());
		String t = "classpath:///aa/bb/cc/.././../dd";
		System.out.println(new URI(t).normalize().getPath());
		System.out.println(new URI(t).resolve("/1.xx"));
		System.out.println(new URI(t).resolve("xx:./test.x"));
	}
	@Test
	public void testURIFile(){
		File f = new File("C:/1.txt");;
		System.out.println(f.toURI());
		System.out.println(ReflectUtil.map(f.toURI()));
		System.out.println(ReflectUtil.map(new File(".").toURI()));
	}

	@Test
	public void test1() throws Exception {
		URI u = new URI("lite:///");
		System.out.println(new File("c:/x/^.x").toURI());
		System.out.println(new URI("c",null,"/x/^.x",null));
		System.out.println(u.resolve("/111.xx"));
		System.out.println(u.resolve(".111.xx#23"));
		System.out.println(u.resolve(URLEncoder.encode("^111.xx#23","UTF-8")));
		System.out.println(u.resolve("%5E111.xx"));
		test("lite:///xxx/s","lite:/xxx/s/222");
		test("lite:///xxx/s3/4","lite:/xxx/s2/222");
	}
	void test(String uri1,String uri2) throws URISyntaxException{
		URI u = new URI(uri1).relativize(new URI(uri2));
		System.out.println(uri1+"---"+uri2);
		System.out.println(u);
		System.out.println(u.getPath());
	}
	private String toRegExp(int c){
		int v = 0x10000+c;
		if(v<=0x100FF){
			return "\\x"+Integer.toHexString(v).substring(3);
		}else{
			return "\\u"+Integer.toHexString(v).substring(1);
		}
	}
	/**
	 * [\x22\x3c\x3e\x5c\x5e\x60\u1680\u180e\u202f\u205f\u3000]|[\x00-\x20]|[\x7b-\x7d]|[\x7f-\xa0]|[\u2000-\u200b]|[\u2028-\u2029]|
	 * [\x2f\x60]|[\x00-\x29]|[\x2b-\x2c]|[\x3a-\x40]|[\x5b-\x5e]|[\x7b-\uffff]|
	 * @throws Exception
	 */
	@Test
	public void testEscape() throws Exception{
//		System.out.println(Charset.availableCharsets());
		testURI();
		StringBuilder source = new StringBuilder();
		for(int i=Character.MAX_VALUE;i>=0;i--){
			try{
				String result = "http://localhost/"+(char)i+"20";
				String result2 = new URL(result).toExternalForm();
				System.out.println(result2);
//				new URI("data:text/xml,"+(char)i+"20");
//				String result = ""+(char)i;
//				String result2 = URLEncoder.encode(result, "utf-8");
				if(!result2.equals(result)){
					throw new Exception();
				}
			}catch (Exception e) {
				source.append((char)i);
				System.out.println((char)i+Integer.toString(i)+":"+Integer.toHexString(i)+":"+URLEncoder.encode(""+(char)i,"UTF-8"));//+(char)i);
				//System.out.println(Integer.toHexString(i));//+(char)i);
				//System.out.println(Integer.toHexString(i));//+(char)i);
			}
		}
		System.out.println(source);
		StringBuilder buf = new StringBuilder();
		StringBuilder starts = new StringBuilder();
		char[] cs = source.toString().toCharArray();
		int start = 0;
		int current = 0;
		int pre = 0;
		for (int i = 1; i < cs.length; i++) {
			pre = cs[i-1];
			current = cs[i];
			if(pre!=current-1){
				if(start == pre){
					starts.append(toRegExp(start));
				}else{
					buf.append("["+toRegExp(start)+"-"+toRegExp(pre)+"]|");
				}
				start = current;
			}
		}
		if(start == current){
			starts.append(toRegExp(start));
		}else{
			buf.append("["+toRegExp(start)+"-"+toRegExp(current)+"]|");
		}
		System.out.println("["+starts+"]|"+buf);
	}
	@Test
	public void testURI() throws Exception{
		System.out.println(new URI("d3.ata:/text/xml,xm$%20l+#进大为0A2Fxml").resolve("/xx#12"));
		System.out.println(new URI("http://s@,@,的(*,ss/32$4").resolve("d/-!00a0_d2.;9s:s"));
		System.out.println(JSONEncoder.encode(new File("/进大为").toURI())+new File("/进大# 为").toURI());
		
		URI url = new URI("http://localhost/进/大_-#dd/为");
		System.out.println(JSONEncoder.encode(url) +url);
		url = new URL("http://localhost/"+URLEncoder.encode("进/大/为","UTF-8")).toURI().resolve("./。a/三/阿嫂");
		System.out.println(JSONEncoder.encode(url)+url);
		System.out.println(JSONEncoder.encode(url.resolve("aaa")));
	}
}
