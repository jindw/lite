package org.xidea.el.json.test;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;

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
	//com.sun.syndication.io.impl
	@Test
	public void test() throws ParseException{
		//YYYY-MM-DDThh:mm:ss.sTZD 
		DateFormat f =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
		System.out.println(f.format(f.parse("2010-08-09T19:27:25.350+0900")));
		DateFormat f2 =  new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
		System.out.println(f2.format(f.parse("2010-08-09T19:27:25.350+0900")));
		
		for(int i=0;i<0;i++){
		Date d = new Date();
		String str =f.format(d);
		System.out.println(str);
		System.out.println(f.parse(str));
		System.out.println(d);
		System.out.println(d.getTime() == f.parse(str).getTime());
		}
//		JSONDecoder decoder = new JSONDecoder(true);
//		String result = JSONEncoder.encode(this);
//		System.out.println(result);
//		JSONDateTest object = decoder.decode(result, this.getClass());
//		System.out.println(JSONEncoder.encode(object));
	}

}
