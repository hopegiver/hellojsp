package hellojsp.util;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;

public class DataSet extends ArrayList<HashMap<String, Object>> {

	private static final long serialVersionUID = 1L;
	private int idx = -1;
	public String[] columns;
	public int[] types;
	public int sortType = -1;
	
	public DataSet() {}
	
	public DataSet(List<?> list) {
		for (Object o : list) this.addRow((Map<?, ?>) o);
		this.first();
	}
	
	public DataSet(ResultSet rs) {
		try {
			ResultSetMetaData meta = rs.getMetaData();
			int columnCount = meta.getColumnCount();
			while (rs.next()) {
				this.addRow();
				for (int i = 1; i <= columnCount; i++) {
					String column = meta.getColumnLabel(i).toLowerCase();
					try {
						if (meta.getColumnType(i) == java.sql.Types.CLOB) {
							this.put(column, rs.getString(i));
						} else if (meta.getColumnType(i) == java.sql.Types.DATE) {
							this.put(column, rs.getTimestamp(i));
						} else {
							this.put(column, rs.getObject(i));
						}
					} catch (Exception e) {
						this.put(column, "");
						Hello.errorLog("{DataSet.resultset} " + e.getMessage(), e);
					}
				}
			}
			rs.close();
			this.first();
		} catch (Exception ignored) {}
	}
	
	public DataSet(DataSet ds) {
		ds.first();
		while(ds.next()) {
			this.addRow(ds.getRow());
		}
		this.first();
		this.columns = ds.columns;
		this.types = ds.types;
		this.sortType = ds.sortType;
	}

	public boolean next() {
		if(this.size() <= (idx + 1)) return false;
		idx = idx + 1;
		return true;
	}

	public boolean move(int id) {
		if(id > -1 && id >= this.size()) return false;
		idx = id;
		return true;
	}

	public int getIndex() {
		return idx;
	}

	public void addRow() {
		this.add(new HashMap<String, Object>());
		idx++;
	}

	public void addRow(HashMap<String, Object> map) {
		if(map != null) {
			this.add(map);
			idx++;
		}
	}

	public int addRow(Hashtable<String, Object> map) {
		if(map != null) {
			this.add(new HashMap<String, Object>(map));
			idx++;
		}
		return idx;
	}
	
	public void addRow(Map<?,?> map) {
		if(map != null) {
			this.addRow();
			for(Object key : map.keySet()) this.put(key.toString(),  map.get(key));
		}
	}

	public boolean updateRow(HashMap<String, Object> data) {
		if(data == null) return false;
		if(idx > -1) {
			HashMap<String, Object> map = get(idx);
			if(map == null) return false;
			for(String key : map.keySet()) {
				if(data.containsKey(key)) {
					this.put(key, data.get(key));
				}
			}
			return true;
		} else {
			return false;
		}
	}

	public boolean updateRow(int id, HashMap<String, Object> data) {
		if(!move(id)) return false;
		return updateRow(data);
	}

	public boolean prev() {
		idx = idx - 1;
		if(idx < 0) {
			idx = 0;
			return false;
		} else {
			return true;
		}
	}

	public boolean first() {
		idx = -1;
		return true;
	}

	public boolean last() {
		idx = this.size() - 1;
		return true;
	}

	public void put(String name, int i) {
		this.put(name, new Integer(i));
	}

	public void put(String name, double d) {
		this.put(name, new Double(d));
	}

	public void put(String name, boolean b) {
		this.put(name, Boolean.valueOf(b));
	}

	public void put(String name, String value) {
		get(idx).put(name, value == null ? "" : value);
	}

	public void put(String name, Object value) {
		get(idx).put(name, value == null ? "" : value);
	}

	public Object get(String name) {
		if(idx < 0) return null;
		Object ret = null;
		try {
			HashMap<String,Object> map = get(idx);
			if(map != null && map.containsKey(name)) {
				ret = map.get(name);
			}
		} catch(Exception e) {
			Hello.errorLog("{DataSet.get} name : " + name + " - " + e.getMessage(), e);
		}
		return ret;
	}

	public String getString(String name) {
		if(idx < 0) return "";
		String ret = "";
		try {
			HashMap<String, Object> map = get(idx);
			if(map != null && map.containsKey(name)) {
				ret = map.get(name).toString();
			}
		} catch(Exception e) {
			Hello.errorLog("{DataSet.getString} name : " + name + " - " + e.getMessage(), e);
		}

		return ret;
	}
	
	public int getInt(String name) {
		int ret = 0;
		try {
			Object val = get(name);
			ret = val instanceof Number ? ((Number)val).intValue() : Integer.parseInt(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataSet.getInt} name : " + name + " - " + e.getMessage(), e);
		}
		return ret;
	}

	public long getLong(String name) {
		long ret = 0L;
		try {
			Object val = get(name);			
			ret = val instanceof Number ? ((Number)val).longValue() : Long.parseLong(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataSet.getLong} name : " + name + " - " + e.getMessage(), e);
		}
		return ret;
	}

	public double getDouble(String name) {
		double ret = 0.0d;
		try {
			Object val = get(name);
			ret = val instanceof Number ? ((Number)val).doubleValue() : Double.parseDouble(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataSet.getDouble} name : " + name + " - " + e.getMessage(), e);
		}
		return ret;
	}

	public float getFloat(String name) {
		float ret = 0.0f;
		try {
			Object val = get(name);
			ret = val instanceof Number ? ((Number)val).floatValue() : Float.parseFloat(val.toString().trim());
		} catch(Exception e) {
			Hello.errorLog("{DataSet.getDouble} name : " + name + " - " + e.getMessage(), e);
		}
		return ret;
	}
	
	public boolean getBoolean(String name) {
		boolean ret = false;
		try {
			Object val = get(name);
			if(val instanceof Boolean) ret = (Boolean)val;			
			else {
				String v = getString(name).trim().toUpperCase();
				if("Y".equals(v) || "TRUE".equals(v)) ret = true;
			}
		} catch(Exception e) {
			Hello.errorLog("{DataSet.getBoolean} name : " + name + " - " + e.getMessage(), e);
		}
		return ret;
	}

	public String s(String name) { return getString(name); }
	public int i(String name) { return getInt(name); }
	public long l(String name) { return getLong(name); }
	public double d(String name) { return getDouble(name); }
	public boolean b(String name) { return getBoolean(name); }

	public Date getDate(String name) {
		Date ret = null;
		try {
			ret = (Date)(get(idx).get(name));
		} catch(Exception e) {
			Hello.errorLog("{DataSet.getDate} name : " + name + " - " + e.getMessage(), e);
		}
		return ret;
	}

	public ArrayList<HashMap<String, Object>> getRows() {
		return this;
	}

	public HashMap<String, Object> getRow(int id) {
		if(move(id)) return getRow();
		else return null;
	}

	public HashMap<String, Object> getRow() {
		if(idx > -1) {
			return get(idx);
		} else {
			return null;
		}
	}

	public String[] getColumns() {
		return columns;
	}

	public String[] getKeys() {
		if(idx > -1) {
			HashMap<String, Object> map = get(idx);
			if(map != null) 
				return map.keySet().toArray(new String[0]);
		}
		return new String[] {};
	}

	public boolean isColumn(String key) {
		if(columns != null) {
			return Hello.inArray(key, columns);
		} else {
			return false;
		}
	}

	public boolean isKey(String key) {
		if(idx > -1) {
			HashMap<String, Object> map = get(idx);
			if(map == null) return false;
			return map.containsKey(key);
		} else {
			return false;
		}	
	}

	public HashMap<String, Object> find(String key, String value) {
		this.first();
		while(this.next()) {
			if(getString(key).equals(value)) {
				this.first();
				return getRow();
			}
		}
		this.first();
		return new HashMap<String, Object>();
	}

	public String find(String key, String value, String retKey) {
		this.first();
		while(this.next()) {
			if(getString(key).equals(value)) {
				this.first();
				return getString(retKey);
			}
		}
		this.first();
		return "";
	}

	public DataSet search(String key, String value, String op) {
		DataSet list = new DataSet();
		this.first();
		while(this.next()) {
			boolean flag;
			if("%".equals(op)) flag = getString(key).contains(value);
			else if("!%".equals(op)) flag = !getString(key).contains(value);
			else if("!".equals(op)) flag = !getString(key).equals(value);
			else if("^".equals(op)) flag = getString(key).matches(value);
			else flag = getString(key).equals(value);
			if(flag) list.addRow(getRow());
		}
		this.first();
		list.first();
		return list;
	}
	
	public DataSet search(String key, String value) {
		return search(key, value, "=");
	}

	public String toJson() {
		return new JSONArray(this).toString();
	}
	public String serialize() { return toJson(); }
	
	public void fromJson(String str) {
		JSONArray arr = new JSONArray(str);
		this.removeAll();
		for(int i=0; i<arr.length(); i++) {
			HashMap<String, Object> map = new HashMap<String, Object>();
			JSONObject obj = (JSONObject)arr.get(i);
			for(String key : obj.keySet()) {
				map.put(key, obj.get(key));
			}
			this.addRow(map);
		}
	}
	public void unserialize(String str) { fromJson(str); }

	private void removeAll() {
		this.clear();
		this.idx = -1;
	}

}