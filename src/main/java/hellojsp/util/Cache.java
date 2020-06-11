package hellojsp.util;

import java.io.File;
import java.io.Writer;

public class Cache {

	private int timeout = 300; //second
	private final String cacheDir;

	private Writer out = null;
	private boolean debug = false;

	public Cache() {
		cacheDir = Config.get("cacheDir", Config.getDataDir() + "/cache");
		File dir = new File(cacheDir);
		if(!dir.exists() && !dir.mkdirs()) {
			Hello.errorLog("{Cache}", new Exception(cacheDir + " is not writable."));
		}
	}

	public void setDebug() {
		this.out = null;
		this.debug = true;
	}
	
	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}

	public void setError(String msg, Exception ex) {
		try {
			if(null != out && debug) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug) Hello.errorLog(msg, ex);
		} catch(Exception ignored) {}
	}

	public void setTimeout(int t) {
		this.timeout = t;
	}

	public Object get(String key) {
		File f = new File(getCachePath(key));
		if(!f.exists()) {
			setError("{Cache.get} key:" + key, new Exception(key + " NOT EXISTS"));
			return null;
		}
		if(System.currentTimeMillis() - f.lastModified() < (timeout * 1000)) {
			return Hello.unserialize(f);
		} else {
			return null;
		}
	}

	public String getString(String key) {
		return (String)get(key);
	}

	public DataMap getDataMap(String key) {
		return (DataMap)get(key);
	}

	public DataSet getDataSet(String key) {
		return (DataSet)get(key);
	}

	public boolean print(Writer out, String key) {
		String data = getString(key);
		if(data != null) {
			try { out.write(data); } catch(Exception ignored) {}
			return true;
		}
		return false;
	}

	public void save(String key, Object data) {
		Hello.serialize(getCachePath(key), data);
	}

	public void savePrint(String key, Object data, Writer out) {
		save(key, data);
		try { out.write(data.toString()); } catch(Exception ignored) {}
	}

	private String getCachePath(String key) {
		return cacheDir + "/" + Hello.md5(key);
	}
}