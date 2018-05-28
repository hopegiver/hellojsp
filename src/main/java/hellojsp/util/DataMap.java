package hellojsp.util;

import java.util.HashMap;
import java.util.Map;

public class DataMap extends HashMap<String, Object> {

	private static final long serialVersionUID = 1L;

	public DataMap() {
		super();
	}

	public DataMap(Map<String, Object> map) {
		super(map);
	}
	
	public boolean isset(String key) {
		return containsKey(key);
	}
	
	public String[] getKeys() {
		return keySet().toArray(new String[size()]);
	}

	public String s(String key)		{ return getString(key); }
	public int i(String key)		{ return getInt(key); }
	public long l(String key)		{ return getLong(key); }
	public double d(String key)		{ return getDouble(key); }
	public boolean b(String key)	{ return getBoolean(key); }

	public String getString(String key) {
		return isset(key) ? get(key).toString() : "";
	}
	
	public int getInt(String key) {
		int ret = 0;
		try {
			Object val = get(key);
			ret = val instanceof Number ? ((Number)val).intValue() : Integer.parseInt(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataMap.getInt} key:" + key, e);
		}
		return ret;
	}

	public long getLong(String key) {
		long ret = 0L;
		try {
			Object val = get(key);			
			ret = val instanceof Number ? ((Number)val).longValue() : Long.parseLong(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataMap.getLong} key:" + key, e);
		}
		return ret;
	}

	public double getDouble(String key) {
		double ret = 0.0d;
		try {
			Object val = get(key);
			ret = val instanceof Number ? ((Number)val).doubleValue() : Double.parseDouble(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataMap.getDouble} key:" + key, e);
		}
		return ret;
	}

	public float getFloat(String key) {
		float ret = 0.0f;
		try {
			Object val = get(key);
			ret = val instanceof Number ? ((Number)val).floatValue() : Float.parseFloat(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataMap.getDouble} key:" + key, e);
		}
		return ret;
	}
	
	public boolean getBoolean(String key) {
		boolean ret = false;
		try {
			Object val = get(key);
			if(val instanceof Boolean) ret = (Boolean)val;			
			else {
				String v = getString(key).trim().toUpperCase();
				if("Y".equals(v) || "TRUE".equals(v)) ret = true;
			}
		} catch(Exception e) {
			Hello.errorLog("{DataMap.getBoolean} key:" + key, e);
		}
		return ret;
	}

}
