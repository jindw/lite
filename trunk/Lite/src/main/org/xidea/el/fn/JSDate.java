//package org.xidea.el.fn;
//
//import java.util.Date;
//
//import org.xidea.el.Invocable;
//
//import sun.util.resources.CalendarData;
//
///**
//getUTCDate  
//Returns the day (date) of the month in the specified date according to universal time.  
//getUTCDay  
//Returns the day of the week in the specified date according to universal time.  
//getUTCFullYear  
//Returns the year in the specified date according to universal time.  
//getUTCHours  
//Returns the hours in the specified date according to universal time.  
//getUTCMilliseconds  
//Returns the milliseconds in the specified date according to universal time.  
//getUTCMinutes 
//Returns the minutes in the specified date according to universal time.  
//getUTCMonth  
//Returns the month according in the specified date according to universal time.  
//getUTCSeconds  
//Returns the seconds in the specified date according to universal time.  
//
//
//
//
//setDate
//Sets the day of the month for a specified date according to local time.  
//setFullYear  
//Sets the full year for a specified date according to local time.  
//setHours
//Sets the hours for a specified date according to local time.  
//setMilliseconds  
//Sets the milliseconds for a specified date according to local time.  
//setMinutes
//Sets the minutes for a specified date according to local time.  
//setMonth
//Sets the month for a specified date according to local time.  
//setSeconds  
//Sets the seconds for a specified date according to local time.  
//setTime  
//Sets the value of a Date object according to local time.  
//setUTCDate  
//Sets the day of the month for a specified date according to universal time.  
//setUTCFullYear  
//Sets the full year for a specified date according to universal time.  
//setUTCHours  
//Sets the hour for a specified date according to universal time.  
//setUTCMilliseconds  
//Sets the milliseconds for a specified date according to universal time.  
//setUTCMinutes  
//Sets the minutes for a specified date according to universal time.  
//setUTCMonth  
//Sets the month for a specified date according to universal time.  
//setUTCSeconds  
//Sets the seconds for a specified date according to universal time.  
//setYear
//Sets the year for a specified date according to local time.  
//
//
//
//
//toGMTString
//Converts a date to a string, using the Internet GMT conventions.  
//toLocaleString
//Converts a date to a string, using the current locale's conventions.  
//toLocaleDateString
//Returns the "date" portion of the Date as a string, using the current locale's conventions.  
//toLocaleTimeString
//Returns the "time" portion of the Date as a string, using the current locale's conventions.  
//toSource
//Returns an object literal representing the specified Date object; you can use this value to create a new object. Overrides the Object.toSource method.  
//toString
//Returns a string representing the specified Date object. Overrides the Object.toString method.  
//toUTCString  
//Converts a date to a string, using the universal time convention.  
//UTC
//Returns the number of milliseconds in a Date object since January 1, 1970, 00:00:00, universal time.  
//valueOf
// */
//public class JSDate implements Invocable {
//	int type;
//	public Object invoke(Object thiz, Object... args) throws Exception {
//		return null;
//	}
//	public Object get(Date date){
//		return date.getDate();
//	}
//	//
//	/**
//	 * get*,getUTC*
//	 * @see java.util.Date
//	 * @see java.sql.Date
//	 * getDate()
//	 * Returns the day of the month for the specified date according to local time.  
//	 * getDay()
//	 * Returns the day of the week for the specified date according to local time.  
//	 * getYear
//	 * Returns the year in the specified date according to local time.  
//	 * getFullYear ()
//	 * Returns the year of the specified date according to local time.  
//	 * getHours()
//	 * Returns the hour in the specified date according to local time.  
//	 * getMilliseconds()
//	 * Returns the milliseconds in the specified date according to local time.  
//	 * getMinutes()
//	 * Returns the minutes in the specified date according to local time.  
//	 * getMonth()
//	 * Returns the month in the specified date according to local time.  
//	 * getSeconds()
//	 * Returns the seconds in the specified date according to local time.
//	 */
//	/**
//	 * parse
//	 * Returns the number of milliseconds in a date string since January 1, 1970, 00:00:00, local time.    
//	 * getTime()
//	 * Returns the numeric value corresponding to the time for the specified date according to local time.  
//	 * getTimezoneOffset
//Returns the time-zone offset in minutes for the current locale.  
//	 */
//}