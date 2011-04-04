package org.xidea.lite.parser.impl.xml.test;

import org.junit.Assert;
import org.junit.Test;
import org.xidea.lite.impl.XMLNormalizeImpl;

public class XMLNormalizeTest {
	XMLNormalizeImpl impl = new XMLNormalizeImpl();
	@Test
	public void testUnmach(){
		assertNorm("<hr><a></a2>","<hr><a></a2>");
		assertNorm("<hr>","<hr/>");
		assertNorm("<hr><hr title=jindw selected>","<c:group xmlns:c='http://www.xidea.org/lite/core'><hr/><hr title=\"jindw\" selected=\"selected\"/></c:group>");
		assertNorm("<img src=\"'<hr>\">","<img src=\"'&lt;hr>\"/>");
		assertNorm("<img src=\"'<hr>\" title=${1 <e}>","<img src=\"'&lt;hr>\" title=\"${1 &lt;e}\"/>");
		assertNorm("<hr c:if=${1<a}></hr>","<hr c:if=\"${1&lt;a}\" xmlns:c=\"http://www.xidea.org/lite/core\"/>");
		//System.out.println(impl.normalize("<hr>"));
	}
	private void assertNorm(String source, String expect) {
		String result = impl.normalize(source,source);
		Assert.assertEquals("转换失败:"+source, expect, result);
		
	}

}
