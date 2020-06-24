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
		try { if(!Config.loaded) Config.init(request); } catch(Exception ignored) {}
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
			if(str.matches("^-?[,0-9]+$")) i = Integer.parseInt(replace(str, ",", ""));
		} catch(Exception ignored) { }
		return i;
	}

	public String[] reqArr(String name) {
		return request.getParameterValues(name);
	}

	public String reqEnum(String name, String[] arr) {
		if(arr == null) return null;
		String str = request(name);
		for (String s : arr) {
			if (s.equals(str)) return s;
		}
		return arr[0];
	}

	public HashMap<String, Object> reqMap(String name) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		try {
			Enumeration e = request.getParameterNames();
			while(e.hasMoreElements()) {
				String key = e.nextElement().toString();
				if(key.matches("^(" + name + ")(.+)$")) {
					map.put(key, request(key));
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
		return "POST".equals(request.getMethod());
	}

	public void jsAlert(String msg) {
		try {
			out.write("<script>alert('" + replace(msg, "'", "\\'") + "');</script>");
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

	public String getCookie(String s) {
		Cookie[] cookie = request.getCookies();
		if(cookie == null) return "";
		try {
			for (Cookie item : cookie) {
				if (s.equals(item.getName())) {
					return URLDecoder.decode(item.getValue(), encoding);
				}
			}
		} catch(Exception ignored) {}
		return "";
	}

	public void setCookie(String name, String value) {
		try {
			Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
			if (cookieDomain != null) cookie.setDomain(cookieDomain);
			cookie.setPath("/");
			response.addCookie(cookie);
		} catch (Exception ignored) {}
	}

	public void setCookie(String name, String value, int time) {
		try {
			Cookie cookie = new Cookie(name, URLEncoder.encode(value, encoding));
			if (cookieDomain != null) cookie.setDomain(cookieDomain);
			cookie.setPath("/");
			cookie.setMaxAge(time);
			response.addCookie(cookie);
		} catch(Exception ignored) {}
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
		if(date == null) return "";
		return sdf.format(date);
	}

	public static String time(String sformat, String date) {
		if("".equals(date)) return "";
		Date d = strToDate(date.trim());
		SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		if(d == null) return "";
		return sdf.format(d);
	}

    public static String time(String sformat, Date date, String timezone) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        if(date == null) return "";
        return sdf.format(date);
    }

    public static String time(String sformat, String date, String timezone) {
    	if("".equals(date)) return "";
        Date d = strToDate(date.trim());
        SimpleDateFormat sdf = new SimpleDateFormat(sformat);
		sdf.setTimeZone(TimeZone.getTimeZone(timezone));
        if(d == null) return "";
        return sdf.format(d);
    }
    
    public static String time(String sformat, Date date, Locale locale) {
        SimpleDateFormat sdf = new SimpleDateFormat(sformat, locale);
        if(date == null) return "";
        return sdf.format(date);
    }
    
    public static String time(String sformat, String date, Locale locale) {
        Date d = strToDate(date.trim());
        SimpleDateFormat sdf = new SimpleDateFormat(sformat, locale);
        if(d == null) return "";
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
		StringBuilder sb = new StringBuilder();
        try {
			MessageDigest di = MessageDigest.getInstance(algorithm.toUpperCase());
			di.update(str.getBytes(charset));
			byte[] md5Code = di.digest();
			for (byte b : md5Code) {
				String md5Char = String.format("%02x", 0xff & (char) b);
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
		StringBuilder query = new StringBuilder();
		if(null != request.getQueryString()) {
			String[] exceptions = exception.replaceAll(" +", "").split(",");
			String[] queries = request.getQueryString().split("&");

			for (String s : queries) {
				String que = replace(s, new String[]{"<", ">", "'", "\""}, new String[]{"&lt;", "&gt;", "&#39;", "&quot;"});
				String[] attributes = que.split("=");
				if (attributes.length > 0 && inArray(attributes[0], exceptions)) continue;
				query.append("&").append(que);
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

	public void log(String msg) {
		log("debug", msg, "yyyyMMdd");
	}
	public void log(String prefix, String msg) {
		log(prefix, msg, "yyyyMMdd");
	}
	public void log(String prefix, String msg, String fmt) {
    	String logDir = Config.getLogDir();
        try {
            if(logDir == null) logDir = "/tmp";
			File log = new File(logDir);
			if(!log.exists() && !log.mkdirs()) {
				errorLog("{Hello.log}", new Exception(logDir + " is not writable."));
				return;
			}
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
			if(!log.exists() && !log.mkdirs()) {
				errorLog("{Hello.errorLog}", new Exception(logDir + " is not writable."));
				return;
			}
            if(ex != null) {
                StackTraceElement[] arr = ex.getStackTrace();
                StringBuilder sb = new StringBuilder();
				for (StackTraceElement stackTraceElement : arr) {
					if (stackTraceElement.getClassName().contains("_jsp"))
						sb.append("at ")
						.append(replace(replace(replace(stackTraceElement.getClassName(), "__jsp", ".jsp"), "_jsp", ""), "._", "/"))
						.append("\n").append(msg).append(" (").append(ex.getMessage()).append(")");
				}
				msg = sb.toString();
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
		StringBuilder buf=new StringBuilder();
		for (int i=0; i<repeat; i++) {
			buf.append(src);
		}
		return buf.toString();
	}

	public static boolean inArray(String str, String[] array) {
		for (String s : array) {
			if (s.equals(str)) return true;
		}
		return false;
	}

	public static String join(String str, Object[] array) {
		if(str != null && array != null) {
			StringBuilder sb = new StringBuilder();
			for(int i=0; i<array.length; i++) {
				sb.append(array[i].toString());
				if(i < (array.length - 1)) sb.append(str);
			}
			return sb.toString();
		}
		return "";
	}
	
	public static String join(String str, HashMap<String, Object> map) {
		StringBuilder sb = new StringBuilder();
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
			for (String s : arr) {
				String[] tmp = s.split("=>");
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
			for (String s : arr) {
				String[] tmp = s.split("=>");
				String id = tmp[0].trim();
				String value = (tmp.length > 1 ? tmp[1] : tmp[0]).trim();
				if (id.equals(item)) return value;
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
		return map.keySet().toArray(new String[0]);
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
			for (String mobileKeyWord : mobileKeyWords) {
				if (agent.contains(mobileKeyWord)) {
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
		if(ext.equals("PDF")) mime = "application/pdf";
		else if(ext.equals("PPT") || ext.equals("PPTX")) mime = "application/vnd.ms-powerpoint";
		else if(ext.equals("DOC") || ext.equals("DOCX")) mime = "application/msword";
		else if(ext.equals("XLS") || ext.equals("XLSX")) mime = "application/vnd.ms-excel";
		else if(ext.equals("HWP")) mime = "application/x-hwp";
		else if(ext.equals("PNG")) mime = "image/png";
		else if(ext.equals("GIF")) mime = "image/gif";
		else if(ext.equals("JPG") || ext.equals("JPEG")) mime = "image/jpeg";
		else if(ext.equals("MP3")) mime = "audio/mpeg";
		else if(ext.equals("MP4")) mime = "video/mp4";
		else if(ext.equals("ZIP")) mime = "application/zip";
		else if(ext.equals("TXT")) mime = "text/plain";
		else if(ext.equals("AVI")) mime = "video/x-msvideo";
		return mime;
	}

	public void download(String path, String filename) { download(path, filename, 0); }
	public void download(String path, String filename, int bandwidth) {
		File f = new File(path);
		if(f.exists()) {
			download(f, filename, bandwidth);
		} else {
			errorLog("{Hello.download} filename:" + filename, new Exception(path + " is not exists"));
		}
	}

	public void download(File f, String filename) { download(f, filename, 0); }
	public void download(File f, String filename, int bandwidth) {
		FileInputStream fis = null;
		BufferedInputStream fin = null;
		BufferedOutputStream outs = null;
		try {
			String agent = request.getHeader("user-agent");

			if(agent.contains("MSIE")) {
				filename = URLEncoder.encode(filename, "UTF-8").replaceAll("\\+", "%20");
			} else if(agent.contains("Firefox") || agent.contains("Safari")) {
				filename = new String(filename.getBytes("UTF-8"), "8859_1");
			} else if(agent.contains("Chrome") || agent.contains("Opera")) {
				StringBuilder sb = new StringBuilder();
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

			fis = new FileInputStream(f);
			fin = new BufferedInputStream(fis);
			outs = new BufferedOutputStream(response.getOutputStream());

			int read;
			int i = 1;
			bandwidth = bandwidth / 20;
			while ((read = fin.read(bbuf)) != -1) {
				outs.write(bbuf, 0, read);
				if(bandwidth > 0 && i % bandwidth == 0) Thread.sleep(100);
				i++;
			}
		} catch(Exception e) {
			errorLog("{Hello.download} path:" + f.getAbsolutePath() + ", filename:" + filename, e);
		} finally {
			if(outs != null) try { outs.close(); } catch (Exception ignored) {}
			if(fin != null) try { fin.close(); } catch (Exception ignored) {}
			if(fis != null) try { fis.close(); } catch (Exception ignored) {}
		}
	}

	public static String readFile(String path) { return readFile(path, encoding); }
	public static String readFile(String path, String encoding) {
		File f = new File(path);
		if(f.exists()) return readFile(f, encoding);
		else return "";
	}

	public static String readFile(File f) { return readFile(f, encoding); }
	public static String readFile(File f, String encoding) {
		StringBuilder sb = new StringBuilder();
		FileInputStream fin = null;
		Reader reader = null;
		BufferedReader br = null;
		try {
			fin = new FileInputStream(f);
			reader = new InputStreamReader(fin, encoding);
			br = new BufferedReader(reader);
			int c;
			while ((c = br.read()) != -1) {
				sb.append((char) c);
			}
		} catch (Exception e) {
			errorLog("{Hello.readFile} path:" + f.getAbsolutePath() + ", encoding:" + encoding, e);
		} finally {
			if(br != null) try { br.close(); } catch (Exception ignored) {}
			if(reader != null) try { reader.close(); } catch (Exception ignored) {}
			if(fin != null) try { fin.close(); } catch (Exception ignored) {}
		}
		return sb.toString();
	}

	public static void writeFile(String path, String str) {
		writeFile(path, str, encoding);
	}
	
	public static void writeFile(String path, String str, String encoding) {
		FileOutputStream fos = null;
		OutputStreamWriter osw = null;
		BufferedWriter out = null;
		try {
			fos = new FileOutputStream(path);
			osw = new OutputStreamWriter(fos, encoding);
			out = new BufferedWriter(osw);
			out.write(str);
		} catch (Exception e) {
			errorLog("{Hello.writeFile} path:" + path + ", encoding:" + encoding, e);
		} finally {
			if(out != null) try { out.close(); } catch (Exception ignored) {}
			if(osw != null) try { osw.close(); } catch (Exception ignored) {}
			if(fos != null) try { fos.close(); } catch (Exception ignored) {}
		}
	}

	public static String exec(String cmd) {
		StringBuilder output = new StringBuilder();
		Process p;
		try {
			p = Runtime.getRuntime().exec(cmd);
			p.waitFor();
			BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream(), encoding));

			String line;
			while ((line = reader.readLine())!= null) {
				output.append(line).append("\n");
			}
			if(output.length() > 0) output.setLength(output.length() - 1);
		} catch (Exception e) {
			errorLog("{Hello.exec} cmd:" + cmd, e);
		}
		return output.toString();
	}	
	
	public static void chmod(String mode, String path) {
		try {
			Runtime.getRuntime().exec("chmod " + mode + " " + path);
		} catch (Exception ignored) {}
	}

	public static void copyFile(String source, String target) {
		copyFile(new File(source), new File(target));
	}

	public static void copyFile(File source, File target) {
		if(source.isDirectory()) {
			if(!target.isDirectory() && !target.mkdirs()) {
				errorLog("{Hello.copyFile}", new Exception(target.getAbsolutePath() + " is not writable."));
				return;
			}
			String[] children  = source.list();
			if(children != null) {
				for (String child : children) {
					copyFile(new File(source, child), new File(target, child));
				}
			}
		} else {
			FileInputStream fis = null;
			FileOutputStream fos = null;
			FileChannel inChannel = null;
			FileChannel outChannel = null;
			try {
                fis = new FileInputStream(source);
                fos = new FileOutputStream(target);
                inChannel = fis.getChannel();
                outChannel = fos.getChannel();

				// magic number for Windows, 64Mb - 32Kb
				int maxCount = (64 * 1024 * 1024) - (32 * 1024);
				long size = inChannel.size(), position = 0;
				while(position < size) {
					position += inChannel.transferTo(position, maxCount, outChannel);
				}
			} catch (IOException e) {
				errorLog("{Hello.copyFile} source:" + source.getAbsolutePath() + ", target:" + target.getAbsolutePath(), e);
			} finally {
				if(inChannel != null) try { inChannel.close(); } catch (Exception ignored) {}
                if(outChannel != null) try { outChannel.close(); } catch (Exception ignored) {}
                if(fis != null) try { fis.close(); } catch (Exception ignored) {}
                if(fos != null) try { fos.close(); } catch (Exception ignored) {}
			}
		}
	}

	public static void delFile(String path) {
		delFile(path, false);
	}

	public static void delFile(String path, boolean recursive) {
		File f = new File(path);
		path = replace(path, "..", "");
		if(f.exists()) {
			if(f.isDirectory()) {
				File[] files = f.listFiles();
				if(files != null) {
					if (!recursive && files.length > 0) {
						errorLog(path + " is not empty");
						return;
					}
					for (File file : files) delFile(path + "/" + file.getName(), true);
				}
			}
			if(!f.delete()) {
				errorLog("{Hello.delFile}", new Exception(path + " is not deleted."));
			}
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

	public static String urlencode(String url) {
		try { return URLEncoder.encode(url, encoding); }
		catch (Exception ignored) { return ""; }
	}

	public static String urldecode(String url) {
		try { return URLDecoder.decode(url, encoding); }
		catch (Exception ignored) { return ""; }
	}

	public static String encode(String str) {
		try { return Base64Coder.encodeString(str); }
		catch(Exception ignored) { return ""; }
	}
	
	public static String decode(String str) {
		try { return Base64Coder.decodeString(str); }
		catch(Exception ignored) { return ""; }
	}

	public static boolean serialize(String path, Object obj) {
		return serialize(new File(path), obj);
	}

	public static boolean serialize(File file, Object obj) {
		FileOutputStream f = null;
		ObjectOutput s = null;
		boolean flag = true;
		try {
		    if(!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
				errorLog("{Hello.serialize}", new Exception(file.getParentFile().getAbsolutePath() + " is not writable."));
		    }
			f = new FileOutputStream(file);
			s = new ObjectOutputStream(f);
			s.writeObject(obj);
			s.flush();
		} catch(Exception e) {
			errorLog("{Hello.serialize} path:" + file.getAbsolutePath(), e);
			flag = false;
		} finally {
			if(s != null) try { s.close(); } catch(Exception ignored) {}
			if(f != null) try { f.close(); } catch(Exception ignored) {}
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
			if( ois != null ) try { ois.close(); } catch(Exception ignored) { }
			if( fis != null ) try { fis.close(); } catch(Exception ignored) { }
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
        int i;
        int j;
        StringBuilder buf = new StringBuilder();
		while((i = str.indexOf("<", offset)) != -1) {
			if((j = str.indexOf(">", offset)) != -1) {
				buf.append(str, offset, i);
				offset = j + 1;
			} else {
				break;
			}
		}
		buf.append(str.substring(offset));
		return replace(replace(replace(buf.toString(), "\t", ""), "\r", ""), "\n", "").trim();
    }

	public static String strpad(String input, int size, String pad) {
		int gap = size - input.getBytes().length;
		if(gap <= 0) return input;
		StringBuilder output = new StringBuilder(input);
		for(int i=0; i<gap; i++) {
			output.append(pad);
		}
		return output.toString();
	}
	public static String strrpad(String input, int size, String pad) {
		int gap = size - input.getBytes().length;
		if(gap <= 0) return input;
		StringBuilder output = new StringBuilder();
		for(int i=0; i<gap; i++) {
			output.append(pad);
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

	public void print(Object obj) {
	    try {
            out.write("<pre style='border:1px solid #ccc;margin:10px;padding:10px;font-size:12px;background-color:#f5f5f5'>");
            if (obj != null) {
                if (obj instanceof DataSet) {
                    out.write(replace(obj.toString(), "},", "},\n"));
                } else {
                    out.write(obj.toString());
                }
            } else {
                out.write("NULL");
            }
            out.write("</pre>");
        } catch(Exception ignored) {}
	}

	public void print(Object[] obj) {
	    try {
			out.write("<pre style='border:1px solid #ccc;margin:10px;padding:10px;font-size:12px;background-color:#f5f5f5'>");
            if (obj != null) {
                for (int i = 0; i < obj.length; i++) {
                    if (i > 0) out.write(", ");
                    out.write(obj[i].toString());
                }
            } else {
                out.write("NULL");
            }
            out.write("</pre>");
        } catch(Exception ignored) {}
	}

	public void p(Object obj) {
		print(obj);
	}

	public void p(Object[] obj) {
		print(obj);
	}

	public void p(String obj) {
		print(obj);
	}

	public void p(String[] obj) {
		print(obj);
	}

	public void p(int i) {
		print("" + i);
	}

	public void p(double d) {
		print("" + d);
	}

	public void p() {
	    try {
			out.write("<pre style='border:1px solid #ccc;margin:10px;padding:10px;font-size:12px;background-color:#f5f5f5'>");
            Enumeration<?> e = request.getParameterNames();
            while (e.hasMoreElements()) {
                String key = (String) e.nextElement();
                for (int i = 0; i < request.getParameterValues(key).length; i++) {
                    out.write("[" + key + "] => " + request.getParameterValues(key)[i] + "\r");
                }
            }
            out.write("</pre>");
        } catch (Exception ignored) {}
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
		return list.toArray(new String[0]);
	}

	public static String addSlashes(String str) {
		return replace(replace(replace(replace(replace(str, "\\", "\\\\"), "\"", "\\\""), "'", "\\'"), "\r\n", "\\r\\n"), "\n", "\\n");
	}

	public static String replace(String s, String sub, String with) {
		int c = 0;
		int i = s.indexOf(sub,c);
		if (i == -1) return s;

		StringBuilder buf = new StringBuilder(s.length() + with.length());
		do {
			buf.append(s, c, i);
			buf.append(with);
			c = i + sub.length();
		} while((i = s.indexOf(sub, c)) != -1);
		if(c < s.length()) {
			buf.append(s.substring(c));
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
		for (String value : sub) {
			s = replace(s, value, with);
		}
		return s;
	}

	public static String cutString(String str, int len) {
		return cutString(str, len, "...");
	}

	public static String cutString(String str, int len, String tail) {
		try  {
			byte[] by = str.getBytes("utf-8");
			if(by.length <= len) return str;
			int count = 0;
			for(int i = 0; i < len; i++) {
				if((by[i] & 0x80) == 0x80) count++;
			}
			if((by[len - 1] & 0x80) == 0x80 && (count % 2) == 1) len--;
			len = len - count / 2;
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

	public static long crc32(String str)  {
	    try {
            byte[] bytes = str.getBytes(encoding);
            Checksum checksum = new CRC32();
            checksum.update(bytes, 0, bytes.length);
            return checksum.getValue();
        } catch(Exception ignored) {
	        return 0L;
        }
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
	
	public String includePage(String url) {
		ByteArrayOutputStream buffer = null;
		OutputStreamWriter osw = null;
		String ret = "";
		try {
			buffer = new ByteArrayOutputStream();
			osw = new OutputStreamWriter(buffer, encoding);
			final PrintWriter writer = new PrintWriter(osw);
			final HttpServletResponse wrappedResponse = new HttpServletResponseWrapper(response) {
				public PrintWriter getWriter() {
					return writer;
				}
			};
			RequestDispatcher dispatcher = request.getRequestDispatcher(url);
			dispatcher.include(request, wrappedResponse);
			writer.flush();
			ret = buffer.toString();
			writer.close();
		} catch(Exception e) {
			Hello.errorLog("{Hello.includePage} url:" + url, e);
		} finally {
			if(osw != null) try { osw.close(); } catch (Exception ignored) {}
			if(buffer != null) try { buffer.close(); } catch (Exception ignored) {}
		}
		return ret;
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

	public boolean urlFilter(String... args) {
		boolean flag = false;
		String uri = request.getRequestURI();
		for(String url : args) {
			if(uri.matches("^(" + replace(url, "*", "(.*)") + ")")) {
				flag = true;
				break;
			}
		}
		return flag;
	}

	public static boolean isNumber(String str) {
		return str.matches("-?\\d+(\\.\\d+)?");
	}

	public void mail(String mailTo, String subject, String body) {
		mail(mailTo, subject, body, null);
	}

	public void mail(String mailTo, String subject, String body, String filepath) {
		try {
			Mail mail = new Mail();
			mail.setFrom(this.mailFrom);
			mail.send(mailTo, subject, body, filepath != null ? new String[] { filepath } : null);
		} catch(Exception e) {
			errorLog("{Hello.mail} to:" + mailTo + ", subject:" + subject, e);
		}
	}
	
	public void mailer(String mailTo, String subject, String body) {
		mailer(mailTo, subject, body, null);
	}
	
	public void mailer(String mailTo, String subject, String body, String filepath) {
		MailThread mt = new MailThread(mailFrom, mailTo, subject, body, filepath);
		mt.start();
	}
	
	private static class MailThread extends Thread {

		private final String mailFrom;
		private final String mailTo;
		private final String subject;
		private final String body;
		private final String filepath;

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

