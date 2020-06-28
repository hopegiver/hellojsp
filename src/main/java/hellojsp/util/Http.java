package hellojsp.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;

public class Http {

	private Writer out = null;
	private boolean debug = false;
	private String url = null;
	private final HashMap<String, String> headers = new HashMap<String, String>();
	private final HashMap<String, String> cookies = new HashMap<String, String>();
	private final HashMap<String, String> params = new HashMap<String, String>();
	private String encoding = Config.getEncoding();
	private String method = "GET";
	private String data = null;

	public String errMsg = "";

	public Http() { }

	public Http(String path) {
		this.url = path;
	}

	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}

	private void setError(String msg) {
		this.errMsg = msg;
		if(debug) {
			if(null != out) try { out.write("<hr>" + msg + "<hr>\n"); } catch(Exception ignored) {}
			else Hello.errorLog(msg);
		}
	}

	public void setEncoding(String enc) {
		this.encoding = enc;
	}

	public void setUrl(String path) {
		this.url = path;
	}

	public void setData(String d) {
		this.data = d;
	}

	public void setHeader(String key, String value) {
		headers.put(key, value);
	}

	public void setCookie(String key, String value) {
		cookies.put(key, value);
	}

	public void setParam(String name, String value) {
		params.put(name, value);
	}

	public String send() {
		return send(this.method);
	}

	public void send(String method, HttpListener listener) {
		this.method = method;
		new HttpAsync(this, listener).start();
	}

	public String send(String method) {
		StringBuilder buffer = new StringBuilder();
		String line;
		try {
			// Construct data
			if(data == null) {
				StringBuilder sb = new StringBuilder();
				int i = 0;
				for(String name : params.keySet()) {
					if(i > 0) { sb.append("&"); }
					sb.append(URLEncoder.encode(name, encoding)).append("=").append(URLEncoder.encode(params.get(name), encoding));
					i++;
				}
				data = sb.toString();
			}

			if("GET".equals(method) && !"".equals(data)) {
				if(url.indexOf("?") > 0) {
					this.url += "&" + data;	
				} else {
					this.url += "?" + data;
				}
			}

			setError(this.url);

			URL u = new URL(this.url);
			HttpURLConnection conn = (HttpURLConnection)u.openConnection();
			conn.setRequestMethod(method);
			conn.setUseCaches(false);
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");

			for(String key : headers.keySet()) {
				conn.setRequestProperty(key, headers.get(key));
				setError(key + ":" + headers.get(key));
			}

			StringBuilder cookie = new StringBuilder();
			for(String key : cookies.keySet()) {
				cookie.append(URLEncoder.encode(key, encoding)).append("=").append(URLEncoder.encode(cookies.get(key), encoding)).append("; ");
			}
			if(cookie.length() > 0) {
				cookie.setLength(cookie.length() - 1);
				conn.setRequestProperty("Cookie", cookie.toString());
				setError("Cookie:" + cookie.toString());
			}

			if("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method)) {
				conn.setDoOutput(true);
				if(data != null) {
					OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream(), encoding);
					setError(data);
					wr.write(data);
					wr.flush();
					wr.close();
				}
			}
			int responseCode = conn.getResponseCode();
			setError("" + responseCode);

			InputStream is = conn.getInputStream();
			BufferedReader in = new BufferedReader(new InputStreamReader(is, encoding));
			int i = 1;
			while((line = in.readLine()) != null) {
				if(i > 1000) break;
				buffer.append(line).append("\r\n");
				i++;
			}
			in.close();
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{Http.send} " + e.getMessage(), e);
		}

		return buffer.toString();
	}

	public String getUrl() {
		return this.url;
	}

}

class HttpAsync extends Thread {

	private Http http;
	private HttpListener listener;

	public HttpAsync(Http h, HttpListener l) {
		http = h;
		listener = l;
	}

	public void run() {
		try {
			String result = http.send();
			if(listener != null) listener.execute(result);
		} catch(Exception e) {
			Hello.errorLog("{HttpAsync.run} " + e.getMessage(), e);
		}
	}
}

class HttpListener {

	public void execute(String result) {

	}
}