package org.xidea.lite.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.test.oldcases.ExampleTest;
import org.xml.sax.SAXException;

public class DocumentExampleTest {
	static File webRoot = new File(new File(ExampleTest.class.getResource("/")
				.getFile()), "../../");
	@Test
	public void testAll() throws Exception{
		File dir = new File(webRoot,"doc/guide");
		ArrayList<String> errors = new ArrayList<String>();
		for(File file : dir.listFiles()){
			String name = file.getName();
			try{
				testFile(file, name);
			}catch (Exception e) {
				System.out.println(file);
				e.printStackTrace();
				errors.add(file.toString());
				//throw e;
			}
		}
		if(errors.size()>0){
			System.out.println(errors);
			throw new RuntimeException("失败文件:"+errors);
		}
	}
	private void testFile(File file, String name) throws SAXException,
			IOException, FileNotFoundException, XPathExpressionException {
		if(name.endsWith(".xhtml") && !name.startsWith("layout")){
			String xhtml = ParseUtil.loadXMLTextAndClose(new FileInputStream(file));
			xhtml = ParseUtil.normalize(xhtml, file.getAbsolutePath());
			Document doc = ParseUtil.loadXMLBySource(xhtml,file.getAbsolutePath());
			NodeList ns = ParseUtil.selectByXPath(doc, "//x:code");
			HashMap<String, String> fileMap = new HashMap<String, String>();
			HashMap<String, String> varMap = new HashMap<String, String>();
			HashMap<String, String> evalMap = new LinkedHashMap<String, String>();
			for(int i=0;i<ns.getLength();i++){
				Element el = (Element) ns.item(i);
				String varName = el.getAttribute("var");
				String alias = el.getAttribute("alias");
				String path = el.getAttribute("path");
				String model = el.getAttribute("model");
				String source = el.getTextContent().trim();
				if(el.hasAttribute("error")){
					continue;
				}
				if(path.length()>0){
					path = path.replaceFirst("^/?", "/");
					fileMap.put(path, source);
				}
				if(varName.length()>0){
					varMap.put(varName, source);
				}else if(alias.length()>0){
					varMap.put(alias, source);
				}
				if(model.length()>0){
					evalMap.put(source, model);
				}
			}
			int index = 0;
			for(Map.Entry<String, String> entry : evalMap.entrySet()){
				String m = entry.getValue();
				String s = entry.getKey();
				String path = '/'+name+"@"+index++ ;
				if(varMap.containsKey(m)){
					m = varMap.get(m);
				}
				fileMap.put(path, s);
				m = m.replaceAll("\\+\\s*new\\s+Date\\(\\)", ""+System.currentTimeMillis());
				String html = LiteTest.testTemplate(fileMap, m, path);
				//System.out.println(html);
			}
			if(evalMap.size()>0){
				System.out.println(file+"测试成功:"+evalMap.size());
			}
		}
	}

}
