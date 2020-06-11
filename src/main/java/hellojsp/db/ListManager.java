package hellojsp.db;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import javax.servlet.http.HttpServletRequest;
import java.io.Writer;

import hellojsp.util.DataSet;
import hellojsp.util.Hello;
import hellojsp.util.Pager;

public class ListManager {

	public String databaseId;
	
	public String table = null;
	public String fields = "*";
	public String where = null;
	public String orderby = null;
	public String groupby = null;

	public int totalNum = 0;
	public int listNum = 10;
	public int pageNum = 1;
	public int listMode = 1;
	public int naviNum = 10;
	public int linkType = 0;
	
	public boolean debug = false;	
	public String errMsg = null;

	private String listQuery = null;
	private final ArrayList<Object> params = new ArrayList<Object>();
	private Writer out = null;
	private HttpServletRequest request = null;
	public String pageVar = "page";

	public ListManager() {

	}

	public ListManager(HttpServletRequest request) {
		setRequest(request);

	}
	
	public void setRequest(HttpServletRequest request) {
		this.request = request;
	}

	public void setDatabase(String id) {
		this.databaseId = id;
	}
	
	public int getPageNum() {

		String page = request.getParameter(pageVar);
		if(page == null || "".equals(page)) pageNum = 1;
		else if(page.matches("^[0-9]+$")) {
			try { 
				pageNum = Integer.parseInt(page);
			} catch(Exception e) {
				pageNum = 1;
			}
		} else pageNum = 1;

		return pageNum;
	}
	
    public String getDBType() {
    	DB db = new DB(databaseId);
		return db.getProduct();
    }
    
	public void setDebug(Writer out) {
		this.debug = true;
		this.out = out;
	}
	
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}

	public void setPage(int pageNum) {
		if(pageNum < 1) pageNum = 1;
		this.pageNum = pageNum;
	}

	public void setListNum(int size) {
		this.listNum = size;
	}
	
	public void setNaviNum(int size) {
		this.naviNum = size;
	}

	public void setPageVar(String name) {
		this.pageVar = name;
	}

	public void setTable(String table) {
		this.table = table;
	}
	
	public void setFields(String fields) {
		this.fields = fields;
	}	

	public void setOrderBy(String orderby) {
		this.orderby = orderby.toLowerCase();
	}	
	
	public void setGroupBy(String groupby) {
		this.groupby = groupby;
	}	
	
	public void setWhere(String where) {
		this.where = where;
	}
	
	public void addWhere(String where) {
		addWhere(where, new Object[] {});
	}
	public void addWhere(String where, List<Object> list) {
		addWhere(where, list.toArray(new Object[0]));
	}
	public void addWhere(String where, Object[] args) {
		if(where != null && !"".equals(where)) {
			if(this.where == null) {
				this.where = where;
			} else {
				this.where = this.where + " AND " + where;
			}
			Collections.addAll(params, args);
		}
	}

	public void addSearch(String field, String keyword) {
		if(!"".equals(keyword)) addSearch(field, keyword, "=", 1);
	}

	public void addSearch(String field, String keyword, String oper) {
		int type = 1;
		if("LIKE".equals(oper.toUpperCase()) || "%LIKE%".equals(oper.toUpperCase())) type = 2;
		else if("LIKE%".equals(oper.toUpperCase())) type = 3;
		else if("%LIKE".equals(oper.toUpperCase())) type = 4;
		if(!"".equals(keyword)) addSearch(field, keyword, oper, type);
	}

	public void addSearch(String field, int keyword) {
		addSearch(field, keyword, "=", 1);
	}
	
	public void addSearch(String field, int keyword, String oper) {
		if("=".equals(oper) || "!=".equals(oper) || "<>".equals(oper) || "<".equals(oper) || ">".equals(oper) || "<=".equals(oper) || ">=".equals(oper))
			addSearch(field, keyword, oper, 1);
	}
	
	public void addSearch(String field, Object keyword, String oper, int type) {
		if(field != null && keyword != null) {
			if(type == 2) keyword = "%" + keyword + "%";
			else if(type == 3) keyword = keyword + "%";
			else if(type == 4) keyword = "%" + keyword;
			if(field.indexOf(',') == -1) {
				if(!"".equals(field)) {
					addWhere(field + " " + oper.replace("%", "") + " ?");
					params.add(keyword);
				}
			} else {
				String[] fields = field.split(",");
				ArrayList<String> v = new ArrayList<String>();
				for (String s : fields) {
					field = s.trim();
					if (!"".equals(field)) {
						v.add(s.trim() + " " + oper.replace("%", "") + " ?");
						params.add(keyword);
					}
				}
				addWhere("(" + Hello.join(" OR ", v.toArray()) + ")");
			}
		}
	}

	public void setListMode(int mode) {
		this.listMode = mode;
	}

	public int getTotalNum() {
		if(this.totalNum > 0) return this.totalNum;
		
		StringBuilder sb = new StringBuilder();
		sb.append("SELECT count(*) AS count FROM ").append(table);
		if(where != null) sb.append(" WHERE ").append(where);
		String sql = sb.toString();

		//Temporary Add
		if(groupby != null) {
			sb.append(" GROUP BY ").append(groupby);
			sql = sb.toString();
			sql = "SELECT COUNT(*) count FROM (" + sql + ") ZA";
		}

		DB db = null;
		DataSet rs;
		try {
			long stime = System.currentTimeMillis();

			db = new DB(databaseId);
			if(debug) db.setDebug(out);
			
			if(params.size() > 0) rs = db.query(sql, params.toArray());
			else rs = db.query(sql);

			if(rs != null && rs.next()) {
				this.totalNum = rs.getInt("count");	
			} else {
				this.errMsg = db.errMsg;
			}
			db.close();

			long etime = System.currentTimeMillis();
			if(debug && null != out) {
				out.write("<hr>Execution Time : " + (etime - stime) + " (1/1000 sec)<hr>");
			}
		} catch(Exception e) {
			if(db != null) this.errMsg = db.errMsg;
			Hello.errorLog("{ListManager.getTotalNum} " + e.getMessage(), e);
		} finally {
			if(db != null) db.close();
		}
		return this.totalNum;
	}

	public void setListQuery(String query) {
		this.listQuery = query;
	}

	public String getListQuery() {
		if(this.listQuery != null) return this.listQuery;

		getPageNum();

		if(listNum < 1) listNum = 10;
		if(pageNum < 1) pageNum = 1;
	

		StringBuilder sb = new StringBuilder();
		String dbType = getDBType();
		if("mssql".equals(dbType) || "db2".equals(dbType)) {
			sb.append("SELECT ZA.* FROM (");
			sb.append(" SELECT ROW_NUMBER() OVER(ORDER BY ").append(orderby).append(") AS RowNum, ").append(this.fields);
			sb.append(" FROM ").append(this.table);
			if(where != null) sb.append(" WHERE ").append(where);
			if(groupby != null) sb.append(" GROUP BY ").append(groupby);
			sb.append(") ZA WHERE ZA.RowNum BETWEEN (").append(pageNum).append(" - 1) * ").append(listNum).append(" + 1 AND ").append(pageNum * listNum).append(" ORDER BY ZA.RowNum ASC");
		} else {
			int startNum = (pageNum - 1) * listNum;

			if("oracle".equals(dbType)) {
				sb.append("SELECT ZB.* FROM (SELECT  rownum as dbo_rownum, ZA.* FROM (");
			}
			sb.append("SELECT ").append(fields).append(" FROM ").append(table);
			if(where != null) sb.append(" WHERE ").append(where);
			if(groupby != null) sb.append(" GROUP BY ").append(groupby);
			if(orderby != null) sb.append(" ORDER BY ").append(orderby);

			if("oracle".equals(dbType)) {
				sb.append(") ZA WHERE rownum  <= ").append(startNum + listNum).append(") ZB WHERE dbo_rownum > ").append(startNum);
			} else {
				sb.append(" LIMIT ").append(startNum).append(", ").append(listNum);
			}
		}
		return sb.toString();
	}


	public DataSet getDataSet() {
		if(listMode == 1) totalNum = this.getTotalNum();
		DB db = null;
		DataSet rs;
		try {
			long stime = System.currentTimeMillis();
		
			db = new DB(databaseId);
			if(debug) db.setDebug(out);

			if(params.size() > 0) rs = db.query(getListQuery(), params.toArray());
			else rs = db.query(getListQuery());
			
			if(rs == null) this.errMsg = db.errMsg;
			db.close();

			long etime = System.currentTimeMillis();
			if(debug && null != out) {
				out.write("<hr>Execution Time : " + (etime - stime) + " (1/1000 sec)<hr>");
			}
		} catch(Exception e) {
			Hello.errorLog("{ListManager.getDataSet}", e);
			if(db != null) this.errMsg = db.errMsg;
			rs = new DataSet();
		} finally {
			if(db != null) db.close();
		}

		if(rs != null && listMode == 1) {
			for(int j=0; rs.next(); j++) {
				rs.put("__ord", totalNum - (pageNum - 1) * listNum - j);
				rs.put("__asc", (pageNum - 1) * listNum + j + 1);
			}
			rs.first();
		}

		return rs;
	}
	
	public String getPaging(int linkType) {

		Pager pg = new Pager(request);
		pg.setPageVar(pageVar);
		pg.setTotalNum(totalNum);
		pg.setListNum(listNum);
		pg.setPageNum(pageNum);
		pg.setNaviNum(naviNum);
		pg.linkType = linkType;

		return pg.getPager();
	}

	public String getPaging() {
		return this.getPaging(linkType);
	}

	public DataSet getPageData() {
		Pager pg = new Pager(request);
		pg.setPageVar(pageVar);
		pg.setTotalNum(totalNum);
		pg.setListNum(listNum);
		pg.setPageNum(pageNum);
		pg.setNaviNum(naviNum);

		return pg.getPageData();
	}
}