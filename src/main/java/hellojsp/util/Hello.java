package hellojsp.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;
import java.util.zip.CRC32;
import java.util.zip.Checksum;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;

public class Hello {

	public String cookieDomain = null;
	public static String encoding = Config.getEncoding();
	public String mailFrom = "Hello";

	private HttpServletRequest request;
	private HttpServletResponse response;
	private HttpSession session;
	private Writer out;

	public Hello() {}
	
	public Hello(HttpServletRequest request, HttpServletResponse response, Writer out) {
		this.setRequest(request);
		this.setResponse(response);
		this.setWriter(out);
	}
	
	public void setRequest(HttpServletRequest request) {
		this.request = request;
		this.session = request.getSession();
		try { if(!Config.loaded) Config.init(request); } catch(Exception e) {}
	}
	
	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	
	public void setWriter(Writer out) {
		this.out = out;
	}

	public void setMailFrom(String from) {
		this.mailFrom = from;
	}

	public String request(String name) {
		return request(name, "");
	}

	public String request(String name, String str) {
		String value = request.getParameter(name);
		if(value == null) {
			return str;
		} else {
			return replace(replace(value.replace('\'', '`'), "<", "&lt;"), ">", "&gt;");
		}
	}

	public int reqInt(String name) {
		return reqInt(name, 0);
	}

	public int reqInt(String name, int i) {
		String str = request(name, "" + i);
		try {
			if(str.matches("^-?[\\,0-9]+$")) i = Integer.parseInt(replace(str, ",", ""));
		} catch(Exception e) { }
		return i;
	}

	public String[] reqArr(String name) {
		return request.getParameterValues(name);
	}

	public String reqEnum(String name, String[] arr) {
		if(arr == null) return null;
		String str = request(name);
		for(int i=0; i<arr.length; i++) {
			if(arr[i].equals(str)) return arr[i];
		}
		return arr[0];
	}

	public HashMap<String, Object> reqMap(String name) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		try {
			Enumeration<String> e = request.getParameterNames();
			while(e.hasMoreElements()) {
				String key = e.nextElement();
				if(key.matches("^(" + name + ")(.+)$")) {
					map.put(key, request.getParameter(key));
				}
			}
		} catch(Exception ex) {
			errorLog("{Hello.reqMap} name:" + name, ex);
		}
		return map;
	}
	
	public static int parseInt(String str) {
		if(str != null && str.matches("^-?[0-9]+$")) return Integer.parseInt(str);
		return 0;
	}

	public static long parseLong(String str) {
		if(str != null && str.matches("^-?[0-9]+$")) return Long.parseLong(str);
		return 0L;
	}

	public static double parseDouble(String str) {
		if(str != null && str.matches("^-?[0-9]+$")) return Integer.parseInt(str) * 1.0;
		else if(str != null && str.matches("^-?[0-9]+\\.[0-9]+$")) return Double.parseDouble(str);
		return 0.0d;
	}

	public void redirect(String url) {
		try {
			response.sendRedirect(url);
		} catch(Exception e) {
			errorLog("{Hello.redirect} url:" + url, e);
			jsReplace(url);
		}
	}

	public boolean isPost() {
		if("POST".equals(request.getMethod())) {
			return true;
		} else {
			return false;
		}
	}

	public void jsAlert(String msg) {
		try {
			out.write("<script>alert('" + replace(msg, "\'", "\\\'") + "');</script>");
		} catch(Exception e) {
			errorLog("{Hello.jsAlert} msg:" + msg, e);
		}
	}

	public void jsError(String msg) {
		try {
			out.write("<script>alert('" + msg + "');history.go(-1)</script>");
		} catch(Exception e) {
			errorLog("{Hello.jsError} msg:" + msg, e);
		}
	}

	public void jsError(String msg, String target) {
		try {
			out.write("<script>alert('" + msg + "');" + target + ".location.href = " + target + ".location.href;</script>");
		} catch(Exception e) {
			errorLog("{Hello.jsError} msg" + msg + ", target:" + target, e);
		}
	}

	public void js(String str) {
		try {
			out.write("<script type=\"text/javascript\">");
			out.write(str);
			out.write("</script>");
		} catch(Exception e) {
			errorLog("{Hello.js} str:" + str, e);
		}
	}

	public void jsErrClose(String msg) {
		jsErrClose(msg, null);
	}

	public void jsErrClose(String msg, String tgt) {
		try {
			if(tgt == null) tgt = "window";
			out.write("<script>alert('" + msg + "');" + tgt + ".close()</script>");
		} catch(Exception e) {
			errorLog("{Hello.jsErrClose} msg:" + msg +", target:" + tgt, e);
		}
	}

	public void jsReplace(String url) {
		jsReplace(url, "window");
	}

	public void jsReplace(String url, String target) {
		try {
			out.write("<script>"+ target +".location.replace('" + url + "');</script>");
		} catch(Exception e) {
			errorLog("{Hello.jsReplace} url:" + url + ", target:" + target, e);
		}
	}

	public String getCookie(String s) throws Exception {
		Cookie[] cookie = request.getCookies();
		if(cookie == null) return "";
		for(int i = 0; i < cookie.length; i++) {
			if(s.equals(cookie[i].getName())) {
				String value = URLDecoder.decode(cookie[i].getValue(), encoding);
				return value;
			}
		}
		return "";
	}

	public void setCookie(String name, String value) throws Exception {
		Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
		if(cookieDomain != null) cookie.setDomain(cookieDomain);
		cookie.setPath("/");
		response.addCookie(cookie);
	}

	public void setCookie(String name, String value, int time) throws Exception {
		Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
		if(cookieDomain != null) cookie.setDomain(cookieDomain);
		cookie.setPath("/");
		cookie.setMaxAge(time);
		response.addCookie(cookie);
	}

	public void delCookie(String name) {
		Cookie cookie = new Cookie(name, "");
		if(cookieDomain != null) cookie.setDomain(cookieDomain);
		cookie.setPath("/");
		cookie.setMaxAge(0);
		response.addCookie(cookie);
	}

	public String getSession(String s) {
		Object obj = session.getAttribute(s);
		if(obj == null) return "";
		return (String)obj;
	}

	public void setSession(String name, String value) {
		session.setAttribute(name, value);
	}

	public static String time() {
		return time("yyyyMMddHHmmss");
	}

	public static String time(String sformat) {
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		return sdf.format((new GregorianCalendar()).getTime());
	}

	public static String time(String sformat, Date date) {
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		if(sdf == null || date == null) return "";
		return sdf.format(date);
	}

	public static String time(String sformat, String date) {
		if("".equals(date)) return "";
		Date d = strToDate(date.trim());
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		if(sdf == null || d == null) return "";
		return sdf.format(d);
	}

    public static String time(String sformat, Date date, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        if(sdf == null || date == null) return "";
        return sdf.format(date);
    }

    public static String time(String sformat, String date, String timezone) {
    	if("".equals(date)) return "";
        Date d = strToDate(date.trim());
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        if(sdf == null || d == null) return "";
        return sdf.format(d);
    }
    
    public static String time(String sformat, Date date, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat, locale);
        if(sdf == null || date == null) return "";
        return sdf.format(date);
    }
    
    public static String time(String sformat, String date, Locale locale) {
        Date d = strToDate(date.trim());
        SimpleDateFormat sdf = new SimpleDateFormat(sformat, locale);
        if(sdf == null || d == null) return "";
        return sdf.format(d);
    }

	public static int diffDate(String type, String sdate, String edate) {
		int ret = 0;
		try {
			Date d1 = strToDate(sdate.trim());
			Date d2 = strToDate(edate.trim());
			if(d1 == null || d2 == null) throw new Exception("date is null");

			long diff =	d2.getTime() - d1.getTime();
			type = type.toUpperCase();
			if("D".equals(type)) ret = (int)(diff / (long)(1000 * 3600 * 24));
			else if("H".equals(type)) ret = (int)(diff / (long)(1000 * 3600));
			else if("I".equals(type)) ret = (int)(diff / (long)(1000 * 60));
			else if("S".equals(type)) ret = (int)(diff / (long)1000);
			else if("Y".equals(type) || "M".equals(type)) {
				Calendar startCalendar = new GregorianCalendar();
				startCalendar.setTime(d1);
				Calendar endCalendar = new GregorianCalendar();
				endCalendar.setTime(d2);

				ret = endCalendar.get(Calendar.YEAR) - startCalendar.get(Calendar.YEAR);
				if("M".equals(type)) {
					ret = ret * 12 + endCalendar.get(Calendar.MONTH) - startCalendar.get(Calendar.MONTH);
				}
			}
		} catch(Exception e) {
			errorLog("{Hello.diffDate} type:" + type + ", sdate:" + sdate + ", edate:" + edate, e);
		}
		return ret;
	}

	public static Date addDate(String type, int amount) {
		return addDate(type, amount, new Date());
	}

	public static Date addDate(String type, int amount, String d) {
		return addDate(type, amount, strToDate(d));
	}

	public static Date addDate(String type, int amount, Date d) {
		if(d == null) return null;
		try {
			Calendar cal = Calendar.getInstance();
			cal.setTime(d);
			type = type.toUpperCase();
			if("Y".equals(type)) cal.add(Calendar.YEAR, amount);
			else if("M".equals(type)) cal.add(Calendar.MONTH, amount);
			else if("W".equals(type)) cal.add(Calendar.WEEK_OF_YEAR, amount);
			else if("D".equals(type)) cal.add(Calendar.DAY_OF_MONTH, amount);
			else if("H".equals(type)) cal.add(Calendar.HOUR_OF_DAY, amount);
			else if("I".equals(type)) cal.add(Calendar.MINUTE, amount);
			else if("S".equals(type)) cal.add(Calendar.SECOND, amount);
			return cal.getTime();
		} catch(Exception e) {
			errorLog("{Hello.addDate} type:" + type + ", date:" + d.toString(), e);
		}
		return null;
	}

	public static String addDate(String type, int amount, String d, String format) {
		return addDate(type, amount, strToDate(d), format);
	}

	public static String addDate(String type, int amount, Date d, String format) {
		return time(format, addDate(type, amount, d));
	}

	public static Date strToDate(String format, String source, Locale loc) {
		if(source == null || "".equals(source)) return null;

		SimpleDateFormat sdf = new SimpleDateFormat(format, loc);
		Date d = null;
		try {
			d = sdf.parse(source);
		} catch (Exception e) {
			errorLog("{Hello.strToDate} format:" + format + ", date:" + source + ", locale:" + loc, e) ;
		}
		return d;
	}

	public static Date strToDate(String format, String source) {
		if(source == null || "".equals(source)) return null;

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = sdf.parse(source);
		} catch (Exception e) {
			errorLog("{Hello.strToDate} format:" + format + ", date:" + source, e) ;
		}
		return d;
	}

	public static Date strToDate(String source) {
		if(source == null || "".equals(source)) return null;

		String format = "yyyyMMddHHmmss";
		if(source.matches("^[0-9]{8}$")) format = "yyyyMMdd";
		else if(source.matches("^[0-9]{14}$")) format = "yyyyMMddHHmmss";
		else if(source.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2}$")) format = "yyyy-MM-dd";
		else if(source.matches("^[0-9]{4}-[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}$")) format = "yyyy-MM-dd HH:mm:ss";

		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date d = null;
		try {
			d = sdf.parse(source);
		} catch (Exception e) {
			errorLog("{Hello.strToDate} date:" + source, e) ;
		}
		return d;
	}

	public static double getPercent(int cnt, int total) {
		if(total == 0) return 0.0d;
		return java.lang.Math.round(((double)cnt / (double)total) * 100);
	}

	public static String md5(String str) { return encrypt(str, "MD5"); }
	public static String sha1(String str) { return encrypt(str, "SHA-1"); }
	public static String sha256(String str) { return encrypt(str, "SHA-256"); }

	public static String encrypt(String str, String algorithm) { return encrypt(str, algorithm, encoding); }
	public static String encrypt(String str, String algorithm, String charset) {
		StringBuffer sb = new StringBuffer();
        try {
			MessageDigest di = MessageDigest.getInstance(algorithm.toUpperCase());
			di.update(new String(str).getBytes(charset));
			byte[] md5Code = di.digest();
			for (int i=0;i<md5Code.length;i++) {
				String md5Char = String.format("%02x", 0xff&(char)md5Code[i]);
				sb.append(md5Char);
			}
		} catch (Exception e) {
			errorLog("{Hello.encrypt} str:" + str + ", algorithm:" + algorithm, e);
		}
        return sb.toString();
    }

	public static String getFileExt(String filename) {
		int i = filename.lastIndexOf(".");
		if(i == -1) return "";
		else return filename.substring(i+1);
	}

	public String getQueryString(String exception) {
		String query = "";
		if(null != request.getQueryString()) {
			String[] exceptions = exception.replaceAll(" +", "").split("\\,");
			String[] queries = request.getQueryString().split("\\&");

			for(int i=0; i<queries.length; i++) {
				String que = replace(queries[i], new String[] { "<", ">", "'", "\"" }, new String[] { "&lt;", "&gt;", "&#39;", "&quot;" });
				String[] attributes = que.split("\\=");
				if(attributes.length > 0 && inArray(attributes[0], exceptions)) continue;
				query += "&" + que;
			}
		}
		return query.length() > 0 ? query.substring(1) : "";
	}
	public String getQueryString() {
		return getQueryString("");
	}
	public String qs(String exception) { return getQueryString(exception); }
	public String qs() { return getQueryString(""); }

	public String getThisURI() {
		String uri = request.getRequestURI();
		String query = request.getQueryString();

		return query == null ? uri : (uri + "?" + query);
	}
	public String getThisURL() {
		String url = request.getRequestURL().toString();
		String query = request.getQueryString();

		return query == null ? url : (url + "?" + query);
	}

	public void log(String msg) throws Exception {
		log("debug", msg, "yyyyMMdd");
	}
	public void log(String prefix, String msg) throws Exception {
		log(prefix, msg, "yyyyMMdd");
	}
	public void log(String prefix, String msg, String fmt) throws Exception {
    	String logDir = Config.getLogDir();
        try {
            if(logDir == null) logDir = "/tmp";
			File log = new File(logDir);
			if(!log.exists()) log.mkdirs();
			FileWriter logger = new FileWriter(logDir + "/" + prefix + "_" + time(fmt) + ".log", true);
			logger.write("["+time("yyyy-MM-dd HH:mm:ss")+"] "+request.getRemoteAddr()+" : "+getThisURI()+"\n"+msg+"\n");
			logger.close();
		} catch(Exception e) {
            e.printStackTrace(System.out);
		}
	}

    public static void errorLog(String msg) {
        errorLog(msg, null);
    }

    public static void errorLog(String msg, Exception ex) {
    	String logDir = Config.getLogDir();
        try {
            if(logDir == null) logDir = "/tmp";
			File log = new File(logDir);
			if(!log.exists()) log.mkdirs();
            if(ex != null) {
                StackTraceElement[] arr = ex.getStackTrace();
                for(int i=0; i<arr.length; i++) {
                    if(arr[i].getClassName().indexOf("_jsp") != -1)
                        msg = "at " + replace(replace(replace(arr[i].getClassName(), "__jsp", ".jsp"), "_jsp", ""), "._", "/")
                        + "\n" +  msg + " (" + ex.getMessage() + ")";
                }
				ex.printStackTrace(System.out);
            }
            FileWriter logger = new FileWriter(logDir + "/error_" + time("yyyyMMdd") + ".log", true);
            logger.write("["+time("yyyy-MM-dd HH:mm:ss")+"] "+ msg +"\n");
            logger.close();
        } catch(Exception e) {
            e.printStackTrace(System.out);
        }
    }

	// Get Unique ID
	public static String getUniqId() {
		return getUniqId(10);
	}
	public static String getUniqId(int size) {
		String chars = "abcdefghijklmonpqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
		Random r = new Random();
		char[] buf = new char[size];
		for (int i = 0; i < buf.length; i++) {
			buf[i] = chars.charAt(r.nextInt(chars.length()));
		}
		return new String(buf);
	}

	public static String repeatString(String src, int repeat) {
		StringBuffer buf=new StringBuffer();
		for (int i=0; i<repeat; i++) {
			buf.append(src);
		}
		return buf.toString();
	}

	public static boolean inArray(String str, String[] array) {
		for(int i=0; i<array.length; i++) {
			if(array[i].equals(str)) return true;
		}
		return false;
	}

	public static String join(String str, Object[] array) {
		if(str != null && array != null) {
			StringBuffer sb = new StringBuffer();
			for(int i=0; i<array.length; i++) {
				sb.append(array[i].toString());
				if(i < (array.length - 1)) sb.append(str);
			}
			return sb.toString();
		}
		return "";
	}
	
	public static String join(String str, HashMap<String, Object> map) {
		StringBuffer sb = new StringBuffer();
		int size = map.size(), i = 0;
		for(String key : map.keySet()) {
			String value = map.get(key) != null ? map.get(key).toString() : "";
			sb.append(value);
			if(i < (size - 1)) sb.append(str);
			i++;
		}
		return sb.toString();
	}


	public static DataSet arr2loop(String[] arr) {
		return arr2loop(arr, false);
	}

	public static DataSet arr2loop(String[] arr, boolean empty) {
		DataSet result = new DataSet();
		if(null != arr) {
			for(int i=0; i<arr.length; i++) {
				String[] tmp = arr[i].split("=>");
				String key = tmp[0].trim();
				String value = (tmp.length > 1 ? tmp[1] : (empty ? "" : tmp[0])).trim();
				result.addRow();
				result.put("key", key);
				result.put("value", value);
			}
		}
		result.first();
		return result;
	}

	public static DataSet arr2loop(HashMap<String, Object> map) {
		DataSet result = new DataSet();
		for(String key : map.keySet()) {
			String value = map.get(key) != null ? map.get(key).toString() : "";
			result.addRow();
			result.put("key", key);
			result.put("value", value);
		}
		result.first();
		return result;
	}

	public static String getItem(String item, String[] arr) {
		if(null != arr) {
			for(int i=0; i<arr.length; i++) {
				String[] tmp = arr[i].split("=>");
				String id = tmp[0].trim();
				String value = (tmp.length > 1 ? tmp[1] : tmp[0]).trim();
				if(id.equals(item)) return value;
			}
		}
		return "";
	}

	public static String getItem(String item, HashMap<String, Object> map) {
		for(String key : map.keySet()) {
			String value = map.get(key) != null ? map.get(key).toString() : "";
			if(key.equals(item)) return value;
		}
		return "";
	}

	public static String[] getKeys(Map<String, ?> map) {
		return map.keySet().toArray(new String[map.size()]);
	}

	public static String[] getKeys(String[] arr) {
		String[] data = new String[arr.length];
		for(int i=0; i<arr.length; i++) {
			String[] tmp = arr[i].split("=>");
			String id = tmp[0].trim();
			data[i] = id;
		}
		return data;
	}

	public boolean isMobile() {
		String agent = request.getHeader("user-agent");
		boolean isMobile = false;
		if(null != agent) {
			String[] mobileKeyWords = {
				"iPhone", "iPod", "iPad"
				, "BlackBerry", "Android", "Windows CE"
				, "LG", "MOT", "SAMSUNG", "SonyEricsson"
			};
			for(int i=0; i<mobileKeyWords.length; i++) {
				if(agent.indexOf(mobileKeyWords[i]) != -1) {
					isMobile = true;
					break;
				}
			}
		}
		return isMobile;
	}

	public static String getMimeType(String filename) {
		String ext = getFileExt(filename).toUpperCase();
		String mime = "application/octet-stream;";
		if(ext.equals("PDF")) {
			mime = "application/pdf";
		} else if(ext.equals("PPT") || ext.equals("PPTX")) {
			mime = "application/vnd.ms-powerpoint";
		} else if(ext.equals("DOC") || ext.equals("DOCX")) {
			mime = "application/msword";
		} else if(ext.equals("XLS") || ext.equals("XLSX")) {
			mime = "application/vnd.ms-excel";
		} else if(ext.equals("HWP")) {
			mime = "application/x-hwp";
		} else if(ext.equals("PNG")) {
			mime = "image/png";
		} else if(ext.equals("GIF")) {
			mime = "image/gif";
		} else if(ext.equals("JPG") || ext.equals("JPEG")) {
			mime = "image/jpeg";
		} else if(ext.equals("MP3")) {
			mime = "audio/mpeg";
		} else if(ext.equals("MP4")) {
			mime = "video/mp4";
		} else if(ext.equals("ZIP")) {
			mime = "application/zip";
		} else if(ext.equals("TXT")) {
			mime = "text/plain";
		} else if(ext.equals("AVI")) {
			mime = "video/x-msvideo";
		}
		return mime;
	}

	public void download(String path, String filename) throws Exception {
		download(path, filename, 0);
	}

	public void download(String path, String filename, int bandwidth) throws Exception {

		File f = new File(path);
		if(f.exists()){

			try {
				String agent = request.getHeader("user-agent");

				if(agent.indexOf("MSIE") != -1) {
					filename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
				} else if(agent.indexOf("Firefox") != -1 || agent.indexOf("Safari") != -1) {
					filename = new String(filename.getBytes("UTF-8"), "8859_1");
				} else if(agent.indexOf("Chrome") != -1 || agent.indexOf("Opera") != -1) {
					StringBuffer sb = new StringBuffer();
					for (int i=0; i<filename.length(); i++) {
						char c = filename.charAt(i);
						if (c > '~') {
							sb.append(URLEncoder.encode("" + c, "UTF-8"));
						} else {
							sb.append(c);
						}
					}
					filename = sb.toString();
				} else {
					filename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
				}

				if(isMobile()) {
					response.setContentType( getMimeType(filename) );
				} else {
					response.setContentType( "application/octet-stream;" );
				}
				response.setContentLength( (int)f.length() );
				response.setHeader( "Content-Disposition", "attachment; filename=\"" + filename + "\"" );

				byte[] bbuf = new byte[2048];

				BufferedInputStream fin = new BufferedInputStream(new FileInputStream(f));
				BufferedOutputStream outs = new BufferedOutputStream(response.getOutputStream());

				int read = 0;
				int i = 1;
				bandwidth = bandwidth / 20;
				while ((read = fin.read(bbuf)) != -1) {
					outs.write(bbuf, 0, read);
					if(bandwidth > 0 && i % bandwidth == 0) Thread.sleep(100);
					i++;
				}

				outs.close();
				fin.close();

			} catch(Exception e) {
				errorLog("{Hello.download} path:" + path + ", filename:" + filename, e);
				response.setContentType("text/html");
				out.write("File Download Error : " + e.getMessage());
			}
		} else {
			response.setContentType("text/html");
			out.write("File Not Found : " + path);
		}

	}

	public static String readFile(String path) throws Exception {
		return readFile(path, encoding);
	}

	public static String readFile(String path, String encoding) throws Exception {
		File f = new File(path);
		if(f.exists()) {

			FileInputStream fin = new FileInputStream(f);
			Reader reader = new InputStreamReader(fin, encoding);
			BufferedReader br = new BufferedReader(reader);

			StringBuffer sb = new StringBuffer();
			int c = 0;
			while((c = br.read()) != -1) {
				sb.append((char)c);
			}
			br.close();
			reader.close();
			fin.close();

			return sb.toString();
		} else {
			return "";
		}
	}

	public static void writeFile(String path, String str) throws Exception {
		writeFile(path, str, encoding);
	}
	
	public static void writeFile(String path, String str, String encoding) throws Exception {
		Writer out = new BufferedWriter(new OutputStreamWriter(
    		new FileOutputStream(path), encoding));
    	try {
        	out.write(str);
        } finally {
            out.close();
        }
	}

	public static String exec(String cmd) {
		StringBuffer output = new StringBuffer();
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

			String line = "";			
			while ((line = reader.readLine())!= null) {
				output.append(line + "\n");
			}
		} catch (Exception e) {
			errorLog("{Hello.exec} cmd:" + cmd, e);
		}
		return output.toString();
	}	
	
	public static void chmod(String mode, String path) throws Exception {
		Runtime.getRuntime().exec("chmod " + mode + " " + path);
	}

	public static void copyFile(String source, String target) throws Exception {
		copyFile(new File(source), new File(target));
	}

	public static void copyFile(File source, File target) throws Exception {
		if(source.isDirectory()) {
			if(!target.isDirectory()){
				target.mkdirs();
			}
			String[] children  = source.list();
			for(int i=0; i<children.length; i++){
				copyFile(new File(source, children[i]),new File(target, children[i]));
			}
		} else {
			FileInputStream fis = new FileInputStream(source);
			FileOutputStream fos = new FileOutputStream(target);
			FileChannel inChannel = fis.getChannel();
			FileChannel outChannel = fos.getChannel();
			try {
				// magic number for Windows, 64Mb - 32Kb
				int maxCount = (64 * 1024 * 1024) - (32 * 1024);
				long size = inChannel.size(), position = 0;
				while(position < size) {
					position += inChannel.transferTo(position, maxCount, outChannel);
				}
			} catch (IOException e) {
				errorLog("{Hello.copyFile} source:" + source.getAbsolutePath() + ", target:" + target.getAbsolutePath(), e);
				throw e;
			} finally {
				if(inChannel != null) inChannel.close();
				if(outChannel != null) outChannel.close();
				if(fis != null) fis.close();
				if(fos != null) fos.close();
			}
		}
	}

	public static void delFile(String path) throws Exception {
		delFile(path, false);
	}
	
	public static void delFile(String path, boolean recursive) throws Exception {
		File f = new File(path);
		path = replace(path, "..", "");
		if(f.exists()) {
			if(f.isDirectory()) {
				File[] files = f.listFiles();
				if(recursive == false && files.length > 0) {
					errorLog(path + " is not empty");
					return;
				}
				for(int i=0; i<files.length; i++) delFile(path + "/" + files[i].getName(), recursive);
			} 
			f.delete();
		} else {
			errorLog(path + " is not found", new Exception("file not found"));
		}
	}

	public static int getRandInt(int start, int count) {
		Random r = new Random();
		return start + r.nextInt(count);
	}

	public static int getUnixTime() {
		Date d = new Date();
		return (int)(d.getTime() / 1000);
	}

	public static int getUnixTime(String date) {
		Date d = strToDate(date);
		if(d == null) return 0;
		return (int)(d.getTime() / 1000);
	}

	public static String urlencode(String url) throws Exception {
		return URLEncoder.encode(url, encoding);
	}

	public static String urldecode(String url) throws Exception {
		return URLDecoder.decode(url, encoding);
	}

	public static String encode(String str) throws Exception {
		try { return Base64Coder.encodeString(str); }
		catch(Exception e) { return ""; }
	}
	
	public static String decode(String str) throws Exception {
		try { return Base64Coder.decodeString(str); }
		catch(Exception e) { return ""; }
	}

	public static boolean serialize(String path, Object obj) {
		return serialize(new File(path), obj);
	}

	public static boolean serialize(File file, Object obj) {
		FileOutputStream f = null;
		ObjectOutput s = null;
		boolean flag = true;
		try {
		    if(!file.getParentFile().isDirectory()) {
		    	file.getParentFile().mkdirs();
		    }
			f = new FileOutputStream(file);
			s = new ObjectOutputStream(f);
			s.writeObject(obj);
			s.flush();
		} catch(Exception e) {
			errorLog("{Hello.serialize} path:" + file.getAbsolutePath(), e);
			flag = false;
		} finally {
			if( s != null ) try { s.close(); } catch(Exception e) { }
			if( f != null ) try { f.close(); } catch(Exception e) { }
		}
		return flag;
	}

	public static Object unserialize(String path) {
		return unserialize(new File(path));
	}

	public static Object unserialize(File file) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		Object obj = null;
		try {
			fis = new FileInputStream(file);
			ois = new ObjectInputStream(fis);
			obj = ois.readObject();
		} catch(Exception e) {
			errorLog("{Hello.unserialize} path:" + file.getAbsolutePath(), e);
		} finally {
			if( ois != null ) try { ois.close(); } catch(Exception e) { }
			if( fis != null ) try { fis.close(); } catch(Exception e) { }
		}
		return obj;
	}

	public static String nl2br(String str) {
		return replace(str, new String[] { "\r\n", "\r", "\n" }, "<br />");
	}
	
	public static String htmlentities(String src) {
		return replace(replace(replace(src, "&", "&amp;"), "<", "&lt;"), ">", "&gt;");
	}

	public static String htt(String str) {
		return nl2br(htmlentities(str));
	}
	
    public static String stripTags(String str) {
        int offset = 0;
        int i = 0;
        int j = 0;
        StringBuffer buf = new StringBuffer();
        synchronized(buf) {
            while((i = str.indexOf("<", offset)) != -1) {
                if((j = str.indexOf(">", offset)) != -1) {
                    buf.append(str.substring(offset, i));
                    offset = j + 1;
                } else {
                    break;
                }
            }
            buf.append(str.substring(offset));
            return replace(replace(replace(buf.toString(), "\t", ""), "\r", ""), "\n", "").trim();
        }
    }

	public static String strpad(String input, int size, String pad) {
		int gap = size - input.getBytes().length;
		if(gap <= 0) return input;
		String output = input;
		for(int i=0; i<gap; i++) {
			output += pad;
		}
		return output;
	}
	public static String strrpad(String input, int size, String pad) {
		int gap = size - input.getBytes().length;
		if(gap <= 0) return input;
		String output = "";
		for(int i=0; i<gap; i++) {
			output += pad;
		}
		return output + input;
	}

	public static String getFileSize(long size) {
		if(size >= 1024 * 1024 * 1024) {
			return (size / (1024 * 1024 * 1024)) + "GB";
		} else if(size >= 1024 * 1024) {
			return (size / (1024 * 1024)) + "MB";
		} else if(size >= 1024) {
			return (size / 1024) + "KB";
		} else {
			return size + "B";
		}
	}

	public static double round(double size, int i) {
		double sub = java.lang.Math.pow(10, i);
		return java.lang.Math.round(size * sub) / sub;
	}

	public static String numberFormat(int n) {
		DecimalFormat df = new DecimalFormat("#,###");
		return df.format(n);
	}

	public static String numberFormat(double n, int i) {
		String format = "#,##0";
		if(i > 0) {
			format += "." + strpad("", i, "0");
			n += Double.parseDouble("0." + strpad("", i, "0") + 1); //round fix
		}
		DecimalFormat df = new DecimalFormat(format);
		return df.format(n);
	}
	public static String nf(int n) { return numberFormat(n); }
	public static String nf(double n, int i) { return numberFormat(n, i); }

	public void p(Object obj) throws Exception {
		out.write("<div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
		if(obj != null) {
			if(obj instanceof DataSet) {
				out.write(replace(obj.toString(), new String[] {"[", "{", ",","}","]","="}, new String[] {"[<BLOCKQUOTE>", "{<BLOCKQUOTE>", ",<BR>","</BLOCKQUOTE>}","</BLOCKQUOTE>]"," => "}));
			} else  {
				out.write(obj.toString());
			}
		} else {
			out.write("NULL");
		}
		out.write("</div>");
	}

	public void p(Object[] obj) throws Exception {
		out.write("<div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
		if(obj != null) {
			for(int i=0; i<obj.length; i++) {
				if(i > 0) out.write(", ");
				out.write(obj[i].toString());
			}
		} else {
			out.write("NULL");
		}
		out.write("</div>");
	}

	public void p(int i) throws Exception {
		p("" + i);
	}

	public void p() throws Exception {
		out.write("<div style='border:3px solid lightgreen;margin-bottom:5px;padding:10px;font-size:12px;'>");
		out.write("<pre style='text-align:left;font-size:9pt;'>");
		Enumeration<?> e = request.getParameterNames();
		while(e.hasMoreElements()) {
			String key = (String)e.nextElement();
			for(int i=0; i<request.getParameterValues(key).length; i++) {
				out.write("[" + key + "] => " + request.getParameterValues(key)[i] + "\r");
			}
		}
		out.write("</pre>");
		out.write("</div>");
	}

	public static String dirname(String path) {
		File f = new File(path);
		return f.getParent();
	}

	public static String[] split(String p, String str, int length) {
		String[] arr = str.split(p);
		String[] result = new String[length];
		for(int i=0; i<length; i++) {
			if(i < arr.length) {
				result[i] = arr[i];
			} else {
				result[i] = "";
			}
		}
		return result;
	}

	public static String[] split(String delim, String str) {
		ArrayList<String> list = new ArrayList<String>();
		int offset = 0;
		int len = delim.length();
		while(true) {
			int pos = str.indexOf(delim, offset);
			if(pos == -1) {
				list.add(str.substring(offset));
				break;
			} else {
				list.add(str.substring(offset, pos));
				offset = pos + len;
			}
		}
		return list.toArray(new String[list.size()]);
	}

	public static String addSlashes(String str) {
		return replace(replace(replace(replace(str, "\\", "\\\\"), "\"", "\\\""), "\'", "\\\'"), "\r\n", "\\r\\n");
	}

	public static String replace(String s, String sub, String with) {
		int c = 0;
		int i = s.indexOf(sub,c);
		if (i == -1) return s;

		StringBuffer buf = new StringBuffer(s.length() + with.length());
		do {
			buf.append(s.substring(c, i));
			buf.append(with);
			c = i + sub.length();
		} while((i = s.indexOf(sub, c)) != -1);
		if(c < s.length()) {
			buf.append(s.substring(c, s.length()));
		}
		return buf.toString();
	}
	
	public static String replace(String s, String[] sub, String[] with) {
		if(sub.length != with.length) return s;
		for(int i=0; i<sub.length; i++) {
			s = replace(s, sub[i], with[i]);
		}
		return s;
	}
	
	public static String replace(String s, String[] sub, String with) {
		for(int i=0; i<sub.length; i++) {
			s = replace(s, sub[i], with);
		}
		return s;
	}

	public static String cutString(String str, int len) throws Exception {
		return cutString(str, len, "...");
	}

	public static String cutString(String str, int len, String tail) throws Exception {
		try  {
			byte[] by = str.getBytes("utf-8");
			if(by.length <= len) return str;
			int count = 0;
			for(int i = 0; i < len; i++) {
				if((by[i] & 0x80) == 0x80) count++;
			}
			if((by[len - 1] & 0x80) == 0x80 && (count % 2) == 1) len--;
			len = len - (int)(count / 2);
			return str.substring(0, len) + tail;
		} catch(Exception e) {
			errorLog("{Hello.cutString} str:" + str + ", length:" + len, e);
			return "";
		}
	}
	
	public boolean eq(String s1, String s2) {
		if(null == s1 || null == s2) return false;
		return s1.equals(s2);
	}

	public static long crc32(String str) throws Exception {
		byte bytes[] = str.getBytes(encoding);
		Checksum checksum = new CRC32();
		checksum.update(bytes,0,bytes.length);
		return checksum.getValue();
	}

	public String getWebUrl() {
		String scheme = request.getScheme();
		int port = request.getServerPort();
		String url = scheme + "://" + request.getServerName();
		if("https".equals(scheme)) {
			url += port != 443 ? ":" + port : "";
		} else {
			url += port != 80 ? ":" + port : "";
		}
		return url;
	}
	
	public String includePage(String url) throws Exception {
		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		try {
			final PrintWriter writer = new PrintWriter(new OutputStreamWriter(buffer));
			final HttpServletResponse wrappedResponse = new HttpServletResponseWrapper(response) {
				public PrintWriter getWriter() {
					return writer;
				}
			};
			RequestDispatcher dispatcher = request.getRequestDispatcher(url);
			dispatcher.include(request, wrappedResponse);
			writer.flush();
		} catch(Exception e) {
			Hello.errorLog("{Hello.includePage} url:" + url, e);
		}
		return buffer.toString();
	}

	public String getRemoteAddr() {
        String ip = request.getHeader("X-Forwarded-For");
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if(ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
	}

	public void mail(String mailTo, String subject, String body) throws Exception {
		mail(mailTo, subject, body, null);
	}

	public void mail(String mailTo, String subject, String body, String filepath) throws Exception {
		try {
			Mail mail = new Mail();
			mail.setFrom(this.mailFrom);
			mail.send(mailTo, subject, body, filepath != null ? new String[] { filepath } : null);
		} catch(Exception e) {
			errorLog("{Hello.mail} to:" + mailTo + ", subject:" + subject, e);
		}
	}
	
	public void mailer(String mailTo, String subject, String body) throws Exception {
		mailer(mailTo, subject, body, null);
	}
	
	public void mailer(String mailTo, String subject, String body, String filepath) throws Exception {
		MailThread mt = new MailThread(mailFrom, mailTo, subject, body, filepath);
		mt.start();
	}
	
	private static class MailThread extends Thread {

		private String mailFrom;
		private String mailTo;
		private String subject;
		private String body;
		private String filepath;

		public MailThread(String mailFrom, String mailTo, String subject, String body, String filepath) {
			this.mailFrom = mailFrom;
			this.mailTo = mailTo;
			this.subject = subject;
			this.body = body;
			this.filepath = filepath;
		}

		public void run() {
			try {
				Mail mail = new Mail();
				mail.setFrom(mailFrom);
				mail.send(mailTo, subject, body, filepath);
			} catch(Exception e) {
				Hello.errorLog("{MailThread.run} to:" + mailTo + ", subject:" + subject, e);
			}
		}
	}	
}

