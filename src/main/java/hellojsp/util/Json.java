package hellojsp.util;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class Json {

	private Object data = null;
	private Writer out = null;
	private String jstr = null;
	private boolean debug = false;

	public Json() {

	}
	
	public Json(String jstr) {
		jstr = jstr.trim();
		if(jstr.startsWith("http://") || jstr.startsWith("https://")) {
			setUrl(jstr);
		} else if(jstr.startsWith("{") || jstr.startsWith("[")) {
			setJson(jstr);
		} else if(new File(jstr).exists()) {
			setFile(jstr);
		}
	}
	
	public void setDebug(Writer out) {
		this.debug = true;
		this.out = out;
	}
	
	public void setDebug() {
		this.debug = true;
	}
	
	public void setError(String msg, Exception ex) {
		try {
			if(null != out && debug == true) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug == true) Hello.errorLog(msg, ex);
		} catch(Exception e) {}
	}

	public void setWriter(Writer out) {
		this.out = out;
	}
	
	public void setUrl(String url) {
		try { 
			Http http = new Http(url);
			if(this.debug) http.setDebug(this.out);
			setJson(http.send());
			parse();
		} catch(Exception e) {
			setError("{Json.setUrl} " + e.getMessage(), e);
		}
	}
	
	public void setJson(String jstr) {
		try {		
			this.jstr = jstr.trim();
			parse();
		} catch(Exception e) {
			setError("{Json.setJson} json:" + jstr, e);
		}
	}
	
	public void setFile(String path) {
		try {
			setJson(Hello.readFile(path));
			parse();
		} catch(Exception e) {
			setError("{Json.setFile} path:" + path, e);
		}
	}
	
	private void parse() throws Exception {
		if(this.jstr == null) {
			throw new Exception("json is null");
		}
		if(this.jstr.startsWith("{")) {
			data = new JSONObject(this.jstr);
		} else if(this.jstr.startsWith("[")) {
			data = new JSONArray(this.jstr);
		} else {
			throw new Exception("json parsing error, json:" + this.jstr);
		}
	}

	private Object get(String path) throws Exception {
		if(path.startsWith("//")) path = path.substring(2);
		String[] p = path.split("[\\.\\[\\]\\/]");

		if(data == null) parse();
		Object obj = data;
		for(int i=0; i<p.length; i++) {
			String key = p[i];
			if("".equals(key)) continue;
			if(isNumeric(key)) {
				int idx = Integer.parseInt(key);
				obj = ((JSONArray)obj).get(idx);
			} else {
				obj = ((JSONObject)obj).get(key);
			}
		}
		return obj;
	}

	public boolean isNumeric(String str) {
		for(char c : str.toCharArray()) {
			if(!Character.isDigit(c)) return false;
		}
		return true;
	}

	public String getString(String path) {
		try {
			Object obj = this.get(path);
			return obj.toString();
		} catch(Exception e) {
			setError("{Json.getString} json:" + this.jstr + ", path:" + path, e);
		}
		return "";
	}

	public int getInt(String path) {
		try {
			Object obj = this.get(path);			
			return obj instanceof Number ? ((Number)obj).intValue() : Integer.parseInt(obj.toString());
		} catch (Exception e) {
			setError("{Json.getInt} json:" + this.jstr + ", path:" + path, e);
		}
		return 0;
	}

	public double getLong(String path) {
		try {
			Object obj = this.get(path);
			return obj instanceof Number ? ((Number)obj).longValue() : Long.parseLong(obj.toString());
		} catch (Exception e) {
			setError("{Json.getDouble} json:" + this.jstr + ", path:" + path, e);
		}
		return 0.0d;
	}
	
	public double getDouble(String path) {
		try {
			Object obj = this.get(path);
			return obj instanceof Number ? ((Number)obj).doubleValue() : Double.parseDouble(obj.toString());
		} catch (Exception e) {
			setError("{Json.getDouble} json:" + this.jstr + ", path:" + path, e);
		}
		return 0.0d;
	}

	public HashMap<String, Object> getMap(String path) {
		try {
			Object obj = this.get(path);
			if(obj instanceof JSONObject) {
				JSONObject ob = (JSONObject)obj;
				HashMap<String, Object> map = new HashMap<String, Object>();
				for(Object key : ob.keySet()) map.put((String)key, ob.get((String)key));
				return map;
			}
		} catch (Exception e) {
			setError("{Json.getMap} json:" + this.jstr + ", path:" + path, e);
		}
		return null;
	}

	public ArrayList<Object> getList(String path) {
		try {
			Object obj = this.get(path);
			if(obj instanceof JSONArray) {
				JSONArray ob = (JSONArray)obj;
				ArrayList<Object> list = new ArrayList<Object>();
				for(int i = 0; i < ob.length(); i++) list.add(ob.get(i));
				return list;
			}
		} catch (Exception e) {
			setError("{Json.getList} json:" + this.jstr + ", path:" + path, e);
		}
		return null;
	}
	
	public DataSet getDataSet(String path) {
		DataSet res = new DataSet();
		try { 
			Object obj = this.get(path);
			if(obj == null) return res;
			if(obj instanceof JSONObject) {
				JSONObject ob = (JSONObject)obj;
				res.addRow();
				for(Object key : ob.keySet()) res.put((String)key, ob.get((String)key));
			} else if(obj instanceof JSONArray) {
				JSONArray arr = (JSONArray)obj;
				for(int i=0; i<arr.length(); i++) {
					JSONObject ob = (JSONObject)arr.get(i);
					res.addRow();
					for(Object key : ob.keySet()) res.put((String)key, ob.get((String)key));
				}
			}
		} catch(Exception e) {
			setError("{Json.getDataSet} json:" + this.jstr + ", path:" + path, e);
		}
		res.first();
		return res;
	}
	
	public void put(Map<String, Object> map) {
		this.data = new JSONObject(map);
	}
	
	public void put(List<Object> list) {
		this.data = new JSONArray(list);
	}
	
	public void put(String key, Object val) {
		if(data == null) data = new JSONObject();
		if(data instanceof JSONObject) {
			((JSONObject)data).put(key, val);
		} else {
			setError("{Json.put} data:" + data.toString(), new Exception("data is not JSONObject"));
		}
	}

	public void clear() {
		data = null;
	}
	
	public String toString() {
		return data != null ? data.toString() : "";
	}

	public void print() throws Exception {
		if(data != null) out.write(data.toString());
	}

	public void error(int code, String message) throws Exception {
		print(code, message, null);
	}
	
	public void success(String message, Object data) throws Exception {
		print(0, message, data);
	}

	public void print(int code, String message, Object obj) throws Exception {
		JSONObject ret = new JSONObject();
		ret.put("error", code);
		ret.put("message", message);
		if(obj != null) {
			if(data instanceof Map) ret.put("data", new JSONObject((Map<?,?>)obj));
			else if(data instanceof List) ret.put("data", new JSONArray((List<?>)obj));
			else ret.put("data", obj.toString());
		} else if(data != null) {
			ret.put("data", this.data);
		}
		out.write(ret.toString());
	}

	public static String encode(Map<?,?> map) throws Exception {
		return new JSONObject(map).toString();
	}
	
	public static String encode(List<?> list) throws Exception {
		return new JSONArray(list).toString();
	}

	public static HashMap<String, Object> decode(String str) throws Exception {
		return decode(new JSONObject(str));
	}
	
	public static HashMap<String, Object> decode(JSONObject arr) throws Exception {
		HashMap<String, Object> map = new HashMap<String, Object>();
		for(String key : arr.keySet()) {
			if(arr.get(key) instanceof JSONObject) {
				map.put(key, decode((JSONObject)arr.get(key)));
			} else if(arr.get(key) instanceof JSONArray) {
				map.put(key, decodeArray((JSONArray)arr.get(key)));
			} else {
				map.put(key, arr.get(key));
			}
		}
		return map;
	}
	
	public static ArrayList<Object> decodeArray(String str) throws Exception {
		return decodeArray(new JSONArray(str));
	}
	
	public static ArrayList<Object> decodeArray(JSONArray arr) throws Exception {
		ArrayList<Object> list = new ArrayList<Object>();
		for(int i=0; i<arr.length(); i++) {
			if(arr.get(i) instanceof JSONArray) {
				list.add(decodeArray((JSONArray)arr.get(i)));
			} else if(arr.get(i) instanceof JSONObject) {
				list.add(decode((JSONObject)arr.get(i)));
			} else {
				list.add(arr.get(i));
			}
		}
		return list;
	}	
}