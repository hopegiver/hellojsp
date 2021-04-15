package hellojsp.util;

import java.io.Writer;
import java.util.HashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspWriter;

public class Auth {

	private static String secretId = Hello.getUniqId(16);
	
	private final HttpServletRequest request;
	private final HttpServletResponse response;
	private HttpSession session;
	private HashMap<String, Object> data = new HashMap<String, Object>();
	private boolean debug = false;
	private Writer out;

	private String keyName = "HELLOJSPID";
	private String domain = null;
	private String path = "/";
	private String sameSite = null;
	private int validTime = -1;
	private int maxAge = -1;
	private boolean httpOnly = false;
	private boolean secure = false;
	private boolean isValid = false;

	public Auth(HttpServletRequest request, HttpServletResponse response) {
		this.request = request;
		this.response = response;
	}

	public Auth(HttpServletRequest request, HttpServletResponse response, HttpSession session) {
		this.request = request;
		this.response = response;
		this.session = session;
	}

	public void setDebug(Writer out) {
		this.debug = true;
		this.out = out;
	}

	public void setDebug(JspWriter out) {
		this.debug = true;
		this.out = out;
	}

	public void setDebug() {
		this.debug = true;
		this.out = null;
	}
	
	public void setError(String msg, Exception ex) {
		try {
			if(null != out && debug) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug) Hello.errorLog(msg, ex);
		} catch(Exception ignored) {}
	}

	public void setSecretId(String id) {
		if(!Auth.secretId.equals(id) && id.length() == 16) secretId = id;
	}

	public void setKeyName(String key) {
		this.keyName = key;
	}

	public void setDomain(String domain) {
		this.domain = domain;
	}

	public void setValidTime(int second) {
		this.validTime = second;
	}

	public void setMaxAge(int second) {
		this.maxAge = second;
	}

	public void setPath(String p) {
		this.path = p;
	}

	public void setSecure(boolean sc) {
		this.secure = sc;
	}

	public void setHttpOnly(boolean ho) {
		this.httpOnly = ho;
	}

	public void setSameSite(String ss) {
		this.sameSite = ss;
	}

	public boolean isValid() {
		return this.isValid;
	}

	public boolean validate() {
		String cookie = null;
		if(session == null) {
			Cookie[] cookies = request.getCookies();
			if(cookies !=null) {
				for (Cookie value : cookies) {
					if (value.getName().equals(keyName)) {
						cookie = value.getValue();
					}
				}
			}
		} else {
			cookie = (String)session.getAttribute(keyName);
		}
		if(cookie == null) return false;
		String hash = cookie.substring(0, 64);
		String info = cookie.substring(64);
		if(!hash.equals(Hello.sha256(info + secretId))) {
			destroy();
			return false;
		}

		try {
			String dataStr = new AES(secretId).decrypt(info);
			this.data = Json.decode(dataStr);
			if(validTime == -1 || System.currentTimeMillis() <= (getLong("currtime") + validTime * 1000)) {
				if(session == null) save();
				isValid = true;
				return true;
			}
		} catch(Exception e) {
			setError("{Auth.validate} info:" + info, e);
		}
		return false;
	}

	public int getInt(String key) {
		int ret = 0;
		try {
			Object val = data.get(key);
			ret = val instanceof Number ? ((Number)val).intValue() : Integer.parseInt(val.toString().trim());
		} catch(Exception e) {
			setError("{Auth.getInt} key : " + key, e);
		}
		return ret;
	}

	public long getLong(String key) {
		long ret = 0L;
		try {
			Object val = data.get(key);			
			ret = val instanceof Number ? ((Number)val).longValue() : Long.parseLong(val.toString().trim());
		} catch(Exception e) {
			setError("{Auth.getLong} key : " + key, e);
		}
		return ret;
	}
	
	public double getDouble(String key) {
		double ret = 0.0d;
		try {
			Object val = data.get(key);
			ret = val instanceof Number ? ((Number)val).doubleValue() : Double.parseDouble(val.toString().trim());
		} catch(Exception e) {
			setError("{Auth.getDouble} key : " + key, e);
		}
		return ret;
	}

	public String get(String key) {
		String ret = "";
		Object val = data.get(key);
		if(val != null) ret = val.toString();
		return ret;
	}

	public void put(String name, String value) {
		data.put(name, value);
	}

	public void put(String name, int i) {
		data.put(name, i);
	}

	public void put(String name, long i) {
		data.put(name, i);
	}
	
	public void put(String name, double d) {
		data.put(name, d);
	}
	
	public void save() {
		if(data.size() > 0) {
			data.put("currtime", System.currentTimeMillis());
			String dataStr = Json.encode(data);
			String info = new AES(secretId).encrypt(dataStr);
			String hash = Hello.sha256(info + secretId);

			if(session == null) {
				StringBuilder sb = new StringBuilder();
				sb.append(keyName);
				sb.append("=");
				sb.append(hash);
				sb.append(info);
				sb.append("; Path=");
				sb.append(path);
				if(maxAge != -1) { sb.append("; Max-Age="); sb.append(maxAge); }
				if(domain != null) { sb.append("; Domain="); sb.append(domain); }
				if(httpOnly) { sb.append("; HttpOnly"); }
				if(secure) { sb.append("; Secure"); }
				if(sameSite != null) { sb.append("; SameSite="); sb.append(sameSite); }
				response.addHeader("Set-Cookie", sb.toString());
			} else {
				session.setAttribute(keyName, hash + info);
			}
		}
	}

	public void destroy() {
		if(session == null) {
			Cookie cookie = new Cookie(keyName, "");
			cookie.setMaxAge(0);
			cookie.setPath(path);
			if(domain != null) cookie.setDomain(domain);
			response.addCookie(cookie);
		} else {
			session.removeAttribute(keyName);
		}
	}

}