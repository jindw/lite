package org.xidea.lite.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.test.AutoELTest;
import org.xidea.lite.impl.ParseUtil;
import org.xidea.lite.test.oldcases.ExampleTest;
import org.xml.sax.SAXException;

public class DocumentExampleTest {
	static File webRoot = new File(new File(ExampleTest.class.getResource("/")
				.getFile()), "../../");
	@Test
	public void testAll() throws Exception{
		File dir = new File(webRoot.getCanonicalFile(),"doc/guide");
		ArrayList<String> errors = new ArrayList<String>();
		for(File file : dir.listFiles()){
			String name = file.getName();
			String status = "失败:";
			try{
				testFile(file, name);
				 status = "成功:";
			}catch (Exception e) {
				e.printStackTrace();
				errors.add(file.toString()+'\n');
				//throw e;
			}finally{
				System.out.println("文件测试"+status+file);
			}
		}
		if(errors.size()>0){
			System.out.println("错误文件有:"+errors);
			//throw new RuntimeException("失败文件:"+errors);
		}
	}

	public static void main(String[] args) throws Exception {
		File root;
		if(args.length>0){
			root = new File(args[0]);
		}else{
			root = new File(new File(AutoELTest.class.getResource("/").toURI()),"../../");
		}
		File dest = new File(root,
				"test/data/test-guide-example.json");
		Writer out = new OutputStreamWriter(new FileOutputStream(dest));
		try {
			ArrayList<Object> result = new ArrayList<Object>();
			File dir = new File(webRoot.getCanonicalFile(),"doc/guide");
			ArrayList<String> errors = new ArrayList<String>();
			for(File file : dir.listFiles()){
				String name = file.getName();
				String status = "失败:";
				try{
					List<Object> item = testFile(file, name);
					if(item != null){
						result.add(item);
					}
					 status = "成功:";
				}catch (Exception e) {
					e.printStackTrace();
					errors.add(file.toString()+'\n');
					//throw e;
				}finally{
					System.out.println("文件测试"+status+file);
				}
			}
			out.write(JSONEncoder.encode(result));
			out.flush();
		} finally {
			System.out.println("文档示例测试结果写入:"+dest);
			out.close();
		}
	}
	private static List<Object> testFile(File file, String name) throws SAXException,
			IOException, FileNotFoundException, XPathExpressionException, TransformerConfigurationException, TransformerException, TransformerFactoryConfigurationError {
		if(name.endsWith(".xhtml") && !name.startsWith("layout")){
			String xhtml = ParseUtil.loadXMLTextAndClose(new FileInputStream(file));
			xhtml = ParseUtil.normalize(xhtml, file.getAbsolutePath());
			Document doc = ParseUtil.loadXMLBySource(xhtml,file.getAbsolutePath());
			NodeList ns = ParseUtil.selectByXPath(doc, "//x:code");
			HashMap<String, String> fileMap = new HashMap<String, String>();
			HashMap<String, String> varMap = new HashMap<String, String>();
			HashMap<String, String> evalMap = new LinkedHashMap<String, String>();

			ArrayList<Object> result = new ArrayList<Object>();
			result.add(file.getName());
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
				String model = entry.getValue();
				String source = entry.getKey();
				String path = '/'+name+"@"+index++ ;
				if(varMap.containsKey(model)){
					model = varMap.get(model);
				}
				fileMap.put(path, source);
				model = model.replaceAll("\\+\\s*new\\s+Date\\(\\)", ""+System.currentTimeMillis());
				
				Map<String, String> map = LiteTest.runTemplate(fileMap, model, path,null);
				Map<String, String> outputMap = new HashMap<String, String>();
				String expect = map.get("#expect");
				outputMap.put("source", source);
				outputMap.put("model", model);
				outputMap.put("expect", expect);
				LiteTest.checkResultEqual(source, expect, map, outputMap);
				result.add(outputMap);
			}
			if(evalMap.size()>0){
				System.out.println(file+"测试成功:"+evalMap.size());
			}
			return result;
		}
		return null;
	}

}
