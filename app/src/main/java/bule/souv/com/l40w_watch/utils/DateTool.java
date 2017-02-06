package bule.souv.com.l40w_watch.utils;

import android.text.format.Time;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Random;

/**
 *
 * @class DateTool
 * @author cally
 * @date 2012-11-23 上午12:31:20
 * @description 时间工具类
 *
 */
public class DateTool {

	public static final String TAG = DateTool.class.getSimpleName();
	/**
	 * 年月（六位）
	 */
	public static final int YYYY_MM=-1;
	/**
	 * 年月日（八位）
	 */
	public static final int YYYY_MM_DD = 0;
	/**
	 * 年月日时分（十二位）
	 */
	public static final int YYYY_MM_DD_HH_MM = 1;
	/**
	 * 年月日时分秒（十四位）
	 */
	public static final int YYYY_MM_DD_HH_MM_SS = 2;
	/**
	 * 年月日格式（八位）
	 */
	public static final int YYYY_MM_DD_STYPE = 3;

	/**
	 * 年月日时分秒毫秒（十四位）
	 */
	public static final int YYYY_MM_DD_HH_MM_SS_SSS = 5;

	/**
	 *
	 * @description 时间格式转换
	 * @author cally
	 * @date 2012-11-23 上午12:40:50
	 *
	 * @param type
	 *            转换类型
	 * @param date
	 *            需转换的时间
	 * @return
	 */
	public static String formatDate(int type, Date date) {
		String format = "";
		if (date == null) {
			return "";
		}
		if (type == YYYY_MM_DD) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			format = dateFormat.format(date);
		} else if (type == YYYY_MM_DD_HH_MM) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmm");
			format = dateFormat.format(date);
		} else if (type == YYYY_MM_DD_HH_MM_SS) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			format = dateFormat.format(date);
		} else if (type == YYYY_MM_DD_STYPE) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			format = dateFormat.format(date);
		}else if(type==YYYY_MM){
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
			format=dateFormat.format(date);
		}else if(type==YYYY_MM_DD_HH_MM_SS_SSS){
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmssSSS");
			format = dateFormat.format(date);
		}
		return format;
	}

	/**
	 *
	 * @description 时间格式转换
	 * @author cally
	 * @date 2012-11-23 上午12:40:50
	 *
	 * @param type
	 *            转换类型
	 * @param date
	 *            需转换的时间
	 * @return
	 */
	public static String parseDate(int type, Long date) {
		String format = "";
		if (date == null) {
			return "";
		}
		if (type == YYYY_MM_DD) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
			format = dateFormat.format(date);
		} else if (type == YYYY_MM_DD_HH_MM) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");
			format = dateFormat.format(date);
		} else if (type == YYYY_MM_DD_HH_MM_SS) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
			format = dateFormat.format(date);
		} else if (type == YYYY_MM_DD_STYPE) {
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
			format = dateFormat.format(date);
		}else if(type==YYYY_MM){
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMM");
			format=dateFormat.format(date);
		}
		return format;
	}


	/**
	 *
	 * @description 解析Date
	 * @author cally
	 * @date 2012-12-14 下午3:30:20
	 *
	 * @param str
	 * @return
	 */
	public static String parseDate(String str) {
		if (str == null || "".equals(str) || str.length() < 12) {
			return "";
		}
		return str.substring(0, 4) + "-" + str.substring(4, 6) + "-"
				+ str.substring(6, 8) + " " + str.substring(8, 10) + ":"
				+ str.substring(10, 12);
	}

	/**
	 *
	 * @description 解析Date
	 * @author cally
	 * @date 2012-12-14 下午3:30:20
	 *
	 * @param str
	 * @return
	 */
	public static String parseDate8(String str) {
		if (str == null || "".equals(str) || str.length() < 8) {
			return "";
		}
		return str.substring(0, 4) + "-" + str.substring(4, 6) + "-"
				+ str.substring(6, 8);
	}

	/**
	 *
	 * @description 当前时间+随机数
	 * @author cally
	 * @date 2012-12-4 下午7:34:06
	 *
	 * @return
	 */
	public static String getTmName() {
		SimpleDateFormat simpleFormat = new SimpleDateFormat("MMddHHmmsss");
		String generationfileName = simpleFormat.format(new Date())
				+ new Random().nextInt(1000);
		return generationfileName;
	}
	/**
	 *获取月初
	 */
	public static String yueChu(Date date){
		if(date==null){
			return null;
		}
//		SimpleDateFormat format1 = new SimpleDateFormat("yyyy-MM");
		return formatDate(YYYY_MM,date)+"01000000";/*format1.format(date);*/
	}


	/***
	 * 添加一天
	 * @param year
	 * @param monthOfYear
	 * @param dayOfMonth
	 * @return
	 */
	public static String addOneDay(int year,
								   int monthOfYear, int dayOfMonth){
		Calendar c=Calendar.getInstance();
		if(year==0&&monthOfYear==0&&dayOfMonth==0){
			c.setTime(new Date());
			year=c.get(Calendar.YEAR);
			monthOfYear=c.get(Calendar.MONTH);
			dayOfMonth=c.get(Calendar.DAY_OF_MONTH);
		}else{
			c.set(Calendar.YEAR, year);
			c.set(Calendar.MONTH, monthOfYear);
		}
		int maxDay= c.getActualMaximum(Calendar.DAY_OF_MONTH);
		if(dayOfMonth<maxDay){
			dayOfMonth+=1;
		}else if(dayOfMonth==maxDay){
			if(monthOfYear==11){
				year+=1;
				monthOfYear=0;
				dayOfMonth=1;
			}else{
				monthOfYear+=1;
				dayOfMonth=1;
			}
		}
		c.set(year, monthOfYear, dayOfMonth);
		return formatDate(YYYY_MM_DD, c.getTime());
	}

	/**
	 * @descriptoin	获取当前时间
	 * @author	dc
	 * @param
	 * @date 2016/1/14 11:09
	 * @return 当前时间
	 */
	public static String getSystemTime(){
		SimpleDateFormat    formatter    =   new    SimpleDateFormat    ("yyyy-MM-dd HH:mm:ss");
		Date    curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
		String    str    =    formatter.format(curDate);
		return str;
	}

	public static String getSystemTimeType1(){
		SimpleDateFormat    formatter    =   new    SimpleDateFormat    ("yyyy-MM-dd HH:mm");
		Date    curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
		String    str    =    formatter.format(curDate);
		return str;
	}

	/**
	 * 获取系统时间   24小时制
	 * @return
	 */
	public static String getSystemTimeType(){
		//通过SimpleDateFormat获取24小时制时间
		Time localTime = new Time("Asia/Hong_Kong");
		localTime.setToNow();
		return localTime.format("%Y-%m-%d %H:%M");
	}

	/**
	 * @descriptoin	比较两个时间的差
	 * @author	dc
	 * @param currentTime  当前时间   time  需要比较的时间
	 * @date 2016/1/14 11:27
	 * @return 时间差
	 */
	public static long compareTime(String currentTime,String time){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date d1 = null;
		Date d2 = null;
		try {
			d1 = sdf.parse(currentTime);
			d2 = sdf.parse(time);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		long diff = d1.getTime() - d2.getTime();//这样得到的差值是微秒级别
		long days = diff / (1000 * 60 * 60 * 24);

		return diff;
	}

	/**
	 * 时间戳转换成日期格式字符串
	 * @param seconds 精确到秒的字符串
	 * @param formatStr
	 * @return
	 */
	public static String timeStamp2Date(String seconds) {
		if(seconds == null || seconds.isEmpty() || seconds.equals("null")){
			return "";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return sdf.format(new Date(Long.valueOf(seconds+"000")));
	}
	/**
	 * 时间戳转换成日期格式字符串
	 * @param seconds 精确到秒的字符串
	 * @param formatStr
	 * @return
	 */
	public static String timeStamp2Datetype(String seconds) {
		if(seconds == null || seconds.isEmpty() || seconds.equals("null")){
			return "";
		}
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		return sdf.format(new Date(Long.valueOf(seconds+"000")));
	}

	/**
	 * 取得当前时间戳（精确到秒）
	 * @return
	 */
	public static String timeStamp(){
		long time = System.currentTimeMillis();
		String t = String.valueOf(time/1000);
		return t;
	}

	/**
	 * @descriptoin	获取当前unix时间戳
	 * @author	dc
	 * @date 2017/1/11 9:28
	 */
	public static long unixTimeStamp(){
		long time = getTimesmorning();
		return time;
	}

	//获得当天0点时间
	public static int getTimesmorning(){
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return (int) (cal.getTimeInMillis()/1000);
	}

	/**
	 * 判断当前日期是星期几<br>
	 * <br>
	 * @param pTime 修要判断的时间<br>
	 * @return dayForWeek 判断结果<br>
	 * @Exception 发生异常<br>
	 */
	public static int dayForWeek(String pTime) throws Exception {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		Calendar c = Calendar.getInstance();
		c.setTime(format.parse(pTime));
		int dayForWeek = 0;
		if(c.get(Calendar.DAY_OF_WEEK) == 1){
			dayForWeek = 7;
		}else{
			dayForWeek = c.get(Calendar.DAY_OF_WEEK) - 1;
		}

		return dayForWeek;
	}

	// strTime要转换的String类型的时间
	// formatType时间格式
	// strTime的时间格式和formatType的时间格式必须相同
	public static long stringToLong(String strTime, String formatType)
			throws ParseException {
		Date date = stringToDate(strTime, formatType); // String类型转成date类型
		if (date == null) {
			return 0;
		} else {
			long currentTime = dateToLong(date); // date类型转成long类型
			return currentTime;
		}
	}

	// strTime要转换的string类型的时间，formatType要转换的格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日
	// HH时mm分ss秒，
	// strTime的时间格式必须要与formatType的时间格式相同
	public static Date stringToDate(String strTime, String formatType)
			throws ParseException {
		SimpleDateFormat formatter = new SimpleDateFormat(formatType);
		Date date = null;
		date = formatter.parse(strTime);
		return date;
	}

	// formatType格式为yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
	// data Date类型的时间
	public static String dateToString(Date data, String formatType) {
		return new SimpleDateFormat(formatType).format(data);
	}

	// date要转换的date类型的时间
	public static long dateToLong(Date date) {
		return date.getTime();
	}

	// currentTime要转换的long类型的时间
	// formatType要转换的string类型的时间格式
	public static String longToString(long currentTime, String formatType)
			throws ParseException {
		Date date = longToDate(currentTime, formatType); // long类型转成Date类型
		String strTime = dateToString(date, formatType); // date类型转成String
		return strTime;
	}


	// currentTime要转换的long类型的时间
	// formatType要转换的时间格式yyyy-MM-dd HH:mm:ss//yyyy年MM月dd日 HH时mm分ss秒
	public static Date longToDate(long currentTime, String formatType)
			throws ParseException {
		Date dateOld = new Date(currentTime); // 根据long类型的毫秒数生命一个date类型的时间
		String sDateTime = dateToString(dateOld, formatType); // 把date类型的时间转换为string
		Date date = stringToDate(sDateTime, formatType); // 把String类型转换为Date类型
		return date;
	}


	/**
	 * 获取指定网站的日期时间
	 *
	 * @param webUrl
	 * @return
	 * @author SHANHY
	 * @date   2015年11月27日
	 */
	public static String getWebsiteDatetime(String webUrl){
		try {
			URL url = new URL(webUrl);// 取得资源对象
			URLConnection uc = url.openConnection();// 生成连接对象
			uc.connect();// 发出连接
			long ld = uc.getDate();// 读取网站日期时间
			Date date = new Date(ld);// 转换为标准时间对象
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINESE);// 输出北京时间
			return sdf.format(date);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}


	/*
	* 将时间戳转换为时间
	*/
	public static String stampToDate(String s){
		String res;
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		long lt = new Long(s);
		Date date = new Date(lt);
		res = simpleDateFormat.format(date);
		return res;
	}

	/**
	 * unix时间戳转化为时间
	 * @param timestampString
	 * @return
	 */
	public static String TimeStamp2Date(String timestampString){
		Long timestamp = Long.parseLong(timestampString)*1000;
		String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date(timestamp));
		return date;
	}

	/**
	 * 判断时间是否今天
	 * @param currentTime 需要判断的时间
	 */
	public static boolean isOnTheToDay(String currentTime){
		String formatType = "yyyy-MM-dd HH:mm:ss";
		try {
			Date date = stringToDate(currentTime, formatType); // 把String类型转换为Date类型
			int year = date.getYear();
			int month = date.getMonth()+1;
			int day = date.getDay();

			Date    curDate    =   new    Date(System.currentTimeMillis());//获取当前时间
			int curYear = curDate.getYear();
			int curMonth = curDate.getMonth()+1;
			int curDay = curDate.getDay();
			if(year == curYear && month == curMonth && day == curDay){
				return true;
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return false;
	}



	public static void main(String args[]){

		System.out.print(getWebsiteDatetime("http://www.taobao.com"));
	}

}