package hellojsp.db;

import java.io.Writer;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Random;

import hellojsp.util.DataSet;
import hellojsp.util.Hello;

public class DataObject {
	
	public String databaseId;
	public int limit = 1000;
	public String PK = "id";
	public String dbType = "mysql";
	public String fields = "*";
	public String table = "";
	
	public String orderby = null;
	public String groupby = null;
	public String join = "";
	public String sql = "";
	public String id = "-1";
	public int newId = 0;
	public int seq = 0;
	public Hashtable<String, Object> record = new Hashtable<String, Object>();
	public Hashtable<String, String> func = new Hashtable<String, String>();
	public String errMsg = null;
	private DB db = null;
	private Writer out = null;
	private boolean debug = false;
	private boolean insertId = false;

	public DataObject() {
		
	}

	public DataObject(String table) {
		this.table = table;
	}

	public void setDebug() {
		debug = true;
		this.out = null;
	}
	public void setDebug(Writer out) {
		debug = true;
		this.out = out;
	}

	protected void setError(String msg) {
		this.errMsg = msg;
		try {
			if(debug) {
				if(null != out) out.write("<hr>" + msg + "<hr>\n");
				else Hello.errorLog(msg);
			}
		} catch(Exception ignored) {}
	}
	
	public void setDatabase(String id) {
		this.databaseId = id;
	}
	
	public void setFields(String f) {
		this.fields = f;
	}

	public void setTable(String tb) {
		this.table = tb;
	}

	public void setOrderBy(String sort) {
		this.orderby = sort;
	}

	public void addJoin(String tb, String type, String cond) {
		this.join += " " + type + " JOIN " + tb + " ON " + cond;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public String getQuery() {
		return this.sql;
	}

	public void item(String name, Object obj) {
		if(obj == null) {
			record.put(name, "");
	    } else if(obj instanceof String) {
			record.put(name, Hello.replace((String)obj, "`", "'"));
		} else if(obj instanceof Date) {
			record.put(name, new java.sql.Timestamp(((Date)obj).getTime()));
		} else {
			record.put(name, obj);
		}
	}

	public void item(String name, int obj) {
		record.put(name, obj);
	}

	public void item(String name, long obj) {
		record.put(name, obj);
	}

	public void item(String name, double obj) {
		record.put(name, obj);
	}

	public void item(String name, String value, String fc) {
		record.put(name, Hello.replace(value, "`", "'"));
		func.put(name, fc);
	}


	public void item(HashMap<String, Object> obj, String exceptions) {
		String[] arr = exceptions.split(",");
		for(String key : obj.keySet()) {
			if(Hello.inArray(key, arr)) continue;
			this.item(key, null != obj.get(key) ? obj.get(key) : "");
		}
	}

	public void clear() {
		record.clear();
	}

	public DataSet get(int i) {
		this.id = "" + i;
		return find(this.PK + " = " + i);
	}

	public DataSet get(String id) {
		this.id = id;
		return find(this.PK + " = '" + id + "'");
	}

	public int getOneInt(String query) {
		return getOneInt(query, null);
	}
	public int getOneInt(String query, Object[] args) {
		String str = getOne(query, args);
		if(str.matches("^-?[0-9]+$")) {
			return Integer.parseInt(str);
		}
		return 0;
	}

	public String getOne(String query) {
		return getOne(query, null);
	}
	public String getOne(String query, Object[] args) {
		DataSet info = this.selectLimit(query, args, 1);
		if(info.next()) {
			for(String key : info.getRow().keySet()) {
				if(key.length() > 0 && "_".equals(key.substring(0, 1))) continue;
				return info.getString(key);
			}
		}
		return "";
	}

	public DataSet find(String where) {
		return find(where, this.fields, this.orderby);
	}
	public DataSet find(String where, String fields) {
		return find(where, fields, this.orderby);
	}
	public DataSet find(String where, String fields, String sort) {
		String sql = "SELECT " + fields + " FROM " + this.table + this.join;
		if(where != null && !"".equals(where)) sql = sql + " WHERE " + where;
		if(sort != null && !"".equals(sort)) sql = sql + " ORDER BY " + sort;
		return query(sql);
	}

	public DataSet find(String where, Object[] args) {
		return find(where, args, this.fields);
	}
	public DataSet find(String where, Object[] args, int limit) {
		return find(where, args, this.fields, limit);
	}
	public DataSet find(String where, Object[] args, String fields) {
		String sql = "SELECT " + fields + " FROM " + this.table + this.join;
		if(where != null && !"".equals(where)) sql = sql + " WHERE " + where;
		return query(sql, args);
	}
	public DataSet find(String where, Object[] args, String fields, int limit) {
		String sql = "SELECT " + fields + " FROM " + this.table + this.join;
		if(where != null && !"".equals(where)) sql = sql + " WHERE " + where;
		return selectLimit(sql, args, limit);
	}

	public DataSet find(String where, String fields, int limit) {
		return find(where, fields, this.orderby, limit);
	}
	public DataSet find(String where, String fields, String sort, int limit) {
		String sql = "SELECT " + fields + " FROM " + this.table + this.join;
		if(where != null && !"".equals(where)) sql = sql + " WHERE " + where;
		if(sort != null && !"".equals(sort)) sql = sql + " ORDER BY " + sort;
		return selectLimit(sql, limit);
	}

    public String getDBType() {
    	if(db == null) db = new DB(databaseId);
    	return db.getProduct();
    }

	public DataSet select(String statement, Object parameters) {
		DataSet rs = new DataSet();
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);
			rs = db.select(statement, parameters);

			long etime = System.currentTimeMillis();
			setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");

		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.selectLimit} " + e.getMessage(), e);
		}
		return rs;
	}
	public DataSet selectLimit(String sql, int limit) {
		return selectLimit(sql, null, limit);
	}

	public DataSet selectLimit(String sql, Object[] args, int limit) {
		DataSet rs = new DataSet();
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);

			sql = sql.trim();
			String dbType = getDBType();
			if("oracle".equals(dbType)) {
				sql = "SELECT * FROM (" + sql + ") WHERE rownum  <= " + limit;
			} else if("mssql".equals(dbType)) {
				sql = sql.replaceAll("(?i)^(SELECT)", "SELECT TOP(" + limit + ")");
			} else if("db2".equals(dbType)) {
				sql += " FETCH FIRST " + limit + " ROWS ONLY";
			} else {
				sql += " LIMIT " + limit;
			}
			rs = db.query(sql, args);
			
			long etime = System.currentTimeMillis();
			setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			
		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.selectLimit} " + e.getMessage(), e);
		}
		return rs;
	}

	public DataSet selectRandom(String sql, int limit) {
		return selectRandom(sql, null, limit);
	}

	public DataSet selectRandom(String sql, Object[] args, int limit) {
		DataSet rs = new DataSet();
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);

			sql = sql.trim();
			String dbType = getDBType();
			if("oracle".equals(dbType)) {
				sql = "SELECT * FROM (" + sql + " ORDER BY dbms_random.value) WHERE rownum  <= " + limit;
			} else if("mssql".equals(dbType)) {
				sql = sql.replaceAll("(?i)^(SELECT)", "SELECT TOP(" + limit + ")") + " ORDER BY NEWID()";
			} else if("db2".equals(dbType)) {
				sql = sql.replaceAll("(?i)^(SELECT)", "SELECT RAND() as IDXX, ") + " ORDER BY IDXX FETCH FIRST " + limit + " ROWS ONLY";
			} else {
				sql += " ORDER BY RAND() LIMIT " + limit; 
			}
			rs = db.query(sql, args);
			
			long etime = System.currentTimeMillis();
			setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			
		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.selectRandom} " + e.getMessage(), e);
		}
		return rs;
	}
	
	public int findCount(String where) {	
		return findCount(where, null);
	}
	public int findCount(String where, Object[] args) {
		DataSet rs = find(where, args, " COUNT(*) AS count ");
		if(rs == null || !rs.next()) {
			return 0;
		} else {
			return rs.getInt("count");
		}
	}

	public boolean insert() {

		int max = record.size();
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();

		sb.append("INSERT INTO ").append(this.table).append(" (");
		int k = 0;
		Object[] args = new Object[max];
		for(String key : record.keySet()) {
			sb.append(key);
			if(k < (max - 1)) sb.append(",");

			if(func.containsKey(key)) { sb2.append(func.get(key)); } 
			else sb2.append("?");

			args[k] = record.get(key);			
			if(k++ < (max - 1)) sb2.append(",");
		}
		sb.append(") VALUES (");
		sb2.append(")");
	
		int ret = execute(sb.toString() + sb2.toString(), args);
		
		return ret > 0;
	}

	public int insert(boolean withId) {
		if(withId) return insertWithId();
		else return 0;
	}

	public int insertWithId() {
		setInsertId();
		insert();
		return newId;
	}
	
	public boolean replace() {
		int max = record.size();
		StringBuilder sb = new StringBuilder();
		StringBuilder sb2 = new StringBuilder();

		sb.append("REPLACE INTO ").append(this.table).append(" (");
		int k = 0;
		Object[] args = new Object[max];
		for(String key : record.keySet()) {
			sb.append(key);
			args[k] = record.get(key);
			if(func.containsKey(key)) { sb2.append(func.get(key)); } 
			else { sb2.append("?"); }
			if(k++ < (max - 1)) {
				sb.append(",");
				sb2.append(",");
			}
		}
		sb.append(") VALUES (");
		sb2.append(")");
		String sql = sb.toString() + sb2.toString();

		int ret = execute(sql, args);
		return ret > 0;
	}

	public boolean update() {
		return update(this.PK + " = '" + id + "'");
	}
	public boolean update(String where) {
		int max = record.size();
		StringBuilder sb = new StringBuilder();

		sb.append("UPDATE ").append(this.table).append(" SET ");
		int k = 0;
		Object[] args = new Object[max];
		for(String key : record.keySet()) {
			if(func.containsKey(key)) {
				sb.append(key).append("=").append(func.get(key));
			} else {
				sb.append(key).append("=?");
			}
			args[k] = record.get(key);
			if(k++ < (max - 1)) sb.append(",");
		}
		sb.append(" WHERE ").append(where);
		
		int ret = execute(sb.toString(), args);
		return ret > -1;
	}

	public boolean delete() {
		return delete(this.PK + " = '" + this.id + "'");
	}

	public boolean delete(int id) {
		return delete(this.PK + " = " + id);
	}

	public boolean delete(String where) {
		String sql = "DELETE FROM " + this.table + " WHERE " + where;

		int ret = execute(sql);
		return ret > -1;
	}
 
	public void setInsertId() {
		this.insertId = true;
	}
	
	public int getInsertId() {
		return newId;
	}

	public DataSet query(String sql) {
		DataSet rs = null;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);
			rs = db.query(sql);

			if(rs == null) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}

		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.query} " + e.getMessage(), e);
		}
		return rs;
	}

	public DataSet query(String sql, List<Object> list) {
		return query(sql, list.toArray(new Object[0]));
	}
	public DataSet query(String sql, Object[] args) {
		DataSet rs = null;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);
			rs = db.query(sql, args);

			if(rs == null) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}

		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.query} " + e.getMessage(), e);
		}
		return rs;
	}

	public DataSet query(String sql, int limit) {
		return this.selectLimit(sql, limit);
	}

	public DataSet query(String sql, Object[] args, int limit) {
		return this.selectLimit(sql, args, limit);
	}

	public int execute(String sql) {
		int ret = -1;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);
			if(insertId) db.setInsertId();
			
			ret = db.execute(sql);

			if(insertId) newId = db.getInsertId();
			if(ret == -1) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}
		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.execute} " + e.getMessage(), e);
		}
		return ret;
	}

	public int execute(String sql, List<Object> list) {
		return execute(sql, list.toArray(new Object[0]));
	}
	public int execute(String sql, Object[] args) {
		int ret = -1;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);
			if(insertId) db.setInsertId();
			
			ret = db.execute(sql, args);
			
			if(insertId) newId = db.getInsertId();
			if(ret == -1) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}
		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.execute} " + e.getMessage(), e);
		}
		return ret;
	}

	public DataSet call(String sql, Object[] args, String[] types) {
		DataSet rs = null;
		this.sql = sql;
		try {
			long stime = System.currentTimeMillis();

			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);
			rs = db.call(sql, args, types);

			if(rs == null) this.errMsg = db.errMsg;
			else {
				long etime = System.currentTimeMillis();
				setError("Execution Time : " + (etime - stime) + " (1/1000 sec)");
			}

		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{DataObject.query} " + e.getMessage(), e);
		}
		return rs;
	}

	public void startTrans() {
		try {
			if(db == null) db = new DB(databaseId);
			if(debug) db.setDebug(out);
			db.begin();
		} catch(Exception ignored) {}
	}

	public void startTransWith(DataObject... daos) {
		startTrans();
		for(DataObject dao : daos) {
			if(dao != null) dao.setDB(db);
		}
	}

	public void endTrans() {
		try {
			if(db != null) db.commit();
		} catch(Exception ignored) {}
	}

	public DB getDB() {
		if(db == null) db = new DB(databaseId);
		return db;
	}

	public void setDB(DB d) {
		if(d != null) db = d;
	}

	public void connect(DataObject dao) {
		db = dao.getDB();
	}

	public String getErrMsg() {
		return this.errMsg;
	}

	public long getNextId() {
		return System.currentTimeMillis() * 1000 + (new Random()).nextInt(999);		
	}

	public String getNextId(String prefix) {
		return prefix + getNextId();
	}

}