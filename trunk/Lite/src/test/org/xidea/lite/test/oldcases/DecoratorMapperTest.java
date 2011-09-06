package org.xidea.lite.test.oldcases;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.File;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.xidea.lite.impl.ParseConfigImpl;
import org.xidea.lite.parse.ParseConfig;
import org.xidea.lite.parse.ParseContext;

public class DecoratorMapperTest {
	ParseConfig mapper;

	@Before
	public void setUp() throws Exception {
		File file = new File(this.getClass().getResource(
		"decorators.xml").getFile());
		mapper = new ParseConfigImpl(file.toURI(),file.toURI());
	}
/**
<lite>
    <exclude>/test1/xxx.action</exclude>
    <group layout="/test1/template.xhtml">
        <include>/test1/**</include>
    	<exclude>/test1/login.action</exclude>
    </group>
    <group layout="/test2/template.xhtml">
        <include>/test2/*.action</include>
    </group>
    <group layout="/test3/template.xhtml">
        <include>/test3/*.action</include>
    </group>
</lite>

 */
	@Test
	public void testDecoratorExist() {
		assertEquals("/test1/template.xhtml", getDecotatorPage("/test1/xxx.action"));
		assertEquals("/test1/template.xhtml", getDecotatorPage("/test1/xxxd..d.action"));
		assertEquals("/test1/template.xhtml", getDecotatorPage("/test1/login2.action"));
		assertEquals("/test2/template.xhtml", getDecotatorPage("/test2/login2.action"));
		assertEquals("/test3/template.xhtml", getDecotatorPage("/test3/login2.action"));
	}

	@Test
	public void testGetDecotatorNotExist() {
		assertNull( 
				getDecotatorPage("/test1/login.action"));
		assertNull( 
				getDecotatorPage("/test2/xxx/dd.action"));
	}
	public String getDecotatorPage(String path) {
		Map<String, String> rm = mapper.getFeatureMap(path);
		System.out.println(rm);
		return rm.get(ParseContext.FEATURE_LAYOUT);
	}

}
