package org.xidea.el.json.test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.junit.Test;
import org.xidea.el.json.JSONDecoder;
import org.xidea.el.json.JSONEncoder;

@SuppressWarnings( { "unused", "unchecked" })
public class JSONDateTest {
	private final static String PATTERN= "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
	private final static String SAMPLE = "1900-01-01T00:00:00.000";//"+08:00"
	private final static int DATE_LENGTH = 10;
	private final static int DATE_TIME_LENGTH = SAMPLE.length();

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
		String t = JSONDecoder.decode(JSONEncoder.encode(new Date()));
		JSONDecoder de = new JSONDecoder(true);
		System.out.println(t);
		{
			long n1 = System.nanoTime();
			for (int i = 0; i < 100; i++) {
				new JSONDecoder(false).transform(t,Date.class);
			}
			System.out.println((System.nanoTime()-n1)/1000000d);
			System.out.println(JSONEncoder.encode(new JSONDecoder(false).transform(t,Date.class)));
		}
		{
			long n1 = System.nanoTime();
			for (int i = 0; i < 100; i++) {
				parse(t);
			}
			System.out.println((System.nanoTime()-n1)/1000000d);
			System.out.println(JSONEncoder.encode(parse(t)));
		}
	}
	/**
	 * <pre>
     * Year:YYYY (eg 1997)
     * Year and month:YYYY-MM (eg 1997-07)
     * Complete date:YYYY-MM-DD (eg 1997-07-16)
     * Complete date plus hours and minutes:
     *    YYYY-MM-DDThh:mmTZD (eg 1997-07-16T19:20+01:00)
     * Complete date plus hours, minutes and seconds:
     *    YYYY-MM-DDThh:mm:ssTZD (eg 1997-07-16T19:20:30+01:00)
     * Complete date plus hours, minutes, seconds and a decimal fraction of a second
     *    YYYY-MM-DDThh:mm:ss.sTZD (eg 1997-07-16T19:20:30.45+01:00)
     * </pre>
	 * @param source
	 * @return
	 * @throws ParseException 
	 */
	protected Date parse(String source) throws ParseException{
	    // if sDate has time on it, it injects 'GTM' before de TZ displacement to
        // allow the SimpleDateFormat parser to parse it properly
		final int len = source.length();
		boolean noZone =false;
		if(len <= DATE_LENGTH){
			source = source+SAMPLE.substring(len);//+"+0000";
			noZone = true;
			//return new SimpleDateFormat(PATTERN.substring(0,DATE_TIME_LENGTH)).parse(source);
		}else{
			//标准化TimeZone
            if ('Z' == source.charAt(len-1)) {
            	source = source.substring(0,len-1)+"+0000";
            }else{
            	if(source.charAt(len-3) == ':'){
            		source = new StringBuilder(source).delete(len-3,len-2).toString();
            	}
            }
            //标准化时间信息
            if(source.length() != DATE_TIME_LENGTH+5){
            	final int len2 = source.length();
            	final int t = source.indexOf('T');
            	final int offset = DATE_LENGTH - t;
            	final int zp = len2 - 5;
            	//add timezone
            	final char c = source.charAt(zp);
            	if(c == '+' || c == '-'){
            		source = source.substring(0,zp) + SAMPLE.substring(zp + offset)+source.substring(zp);
            	}else{
            		noZone = true;
            		source = source+ SAMPLE.substring(len2 + offset);
            	}
                
            }
        }
//        ParsePosition p = new ParsePosition(0);
        return new SimpleDateFormat(noZone?PATTERN.substring(0,DATE_TIME_LENGTH):PATTERN).parse(source);
//        if(p.getIndex()!=source.length()){
//        	throws new Runtime
//        }
//        	
//        return result;
	}
}
