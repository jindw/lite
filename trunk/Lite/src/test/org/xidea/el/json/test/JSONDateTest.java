package org.xidea.el.json.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Collections;

import org.junit.Test;
import org.xidea.el.impl.ExpressionImpl;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;
import org.xidea.el.json.JSONTokenizer;

@SuppressWarnings( { "unused", "unchecked" })
public class JSONDateTest {
	private Date utilDate = new Date(System.currentTimeMillis());
	public Date getUtilDate() {
		return utilDate;
	}
	public void setUtilDate(Date utilDate) {
		this.utilDate = utilDate;
	}
	@Test
	public void test(){
		JSONDecoder decoder = new JSONDecoder(true);
		String result = JSONEncoder.encode(this);
		System.out.println(result);
		JSONDateTest object = decoder.decode(result, this.getClass());
		System.out.println(JSONEncoder.encode(object));
	}

}
