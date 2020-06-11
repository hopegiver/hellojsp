package hellojsp.util;

import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;

public class Config {
	
	public static boolean loaded = false;
	private static String docRoot;
	private static String tplRoot;
	private static String dataDir;
	private static String logDir;
	private static String encoding = "UTF-8";
	private static final HashMap<String, String> data = new HashMap<String, String>();

	public static void init(HttpServletRequest req) {
		init(req, null);
	}
	
	public static void init(HttpServletRequest req, String configPath) {
		docRoot = req.getSession().getServletContext().getRealPath("/").replace('\\', '/');
		if(docRoot.endsWith("/")) docRoot = docRoot.substring(0, docRoot.length() - 1);
		load(configPath);
	}
	
	public static void load() {
		load(null);
	}

	public static void load(String configPath) {
		if(configPath == null) configPath = "/WEB-INF/classes/config/hellojsp-config.xml";
		try {
			parse(docRoot + configPath);
		} catch(Exception e) {
			Hello.errorLog("{Config.load} configPath:" + configPath, e);
		}
		
		tplRoot = get("tplRoot", docRoot + "/WEB-INF/tpl");
		logDir = get("logDir", docRoot + "/WEB-INF/log");
		dataDir = get("dataDir", docRoot + "/data");
		encoding = get("encoding", "UTF-8");
		
		loaded = true;
	}

	private static void parse(String path) {
		XML xml = new XML(path);
		DataSet rs = xml.getDataSet("//config/env");
		if(rs.next()) {
			HashMap<String, Object> row = rs.getRow();
			for(String key : row.keySet()) {
				data.put(key, rs.getString(key));
			}
		}
	}
	
	public static String getDocRoot() {
		return docRoot;
	}

	public static String getTplRoot() {
		return tplRoot;
	}

	public static String getDataDir() {
		return dataDir;
	}
	
	public static String getLogDir() {
		return logDir;
	}

	public static String getEncoding() {
		return encoding;
	}

	public static void put(String key, String value) {
		data.put(key, value);
	}

	public static String get(String key, String defaultValue) {
		String ret = data.get(key);
		if(ret == null) return defaultValue;
		else return ret;
	}
	
	public static String get(String key) {
		return get(key, null);
	}
	
	public static int getInt(String key, int defaultValue) {
		String ret = data.get(key);
		if(ret == null) return defaultValue;
		else {
			try { return Integer.parseInt(ret); } catch(Exception ignored) { }
			return 0;
		}
	}
	
	public static int getInt(String key) {
		return getInt(key, 0);
	}

}