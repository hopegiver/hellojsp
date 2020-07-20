package hellojsp.db;

import java.io.InputStream;
import java.io.Writer;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import hellojsp.util.DataSet;
import hellojsp.util.Hello;

import javax.servlet.jsp.JspWriter;

public class DB {

	private static final HashMap<String, SqlSessionFactory> sqlSessionFactoryMap = new HashMap<String, SqlSessionFactory>();
	private static final HashMap<String, String> productMap = new HashMap<String, String>();
	
	private String databaseId = "default";
	private SqlSession _session = null;
	private int newId = 0;

	private Writer out = null;
	private boolean debug = false;
	private boolean autoCommit = true;
	private boolean insertId = false;

	public String errMsg = null;
	public String query = null;

	public DB() {

	}
	
	public DB(String id) {
		if(id != null) databaseId = id;
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
	
	public void setError(String msg) {
		this.errMsg = msg;
		if(debug) {
			try {
				if(null != out) 
					out.write("<hr>" + Hello.nl2br(msg) + "<hr>");
				else Hello.errorLog(msg);
			} catch(Exception ignored) {}
		}
	}

	public String getQuery() {
		return this.query;
	}

	public String getError() {
		return this.errMsg;
	}

	public void reloadMapper() {
		sqlSessionFactoryMap.clear();
	}

	private SqlSessionFactory getSqlSessionFactory() throws Exception {
		SqlSessionFactory sqlSessionFactory = sqlSessionFactoryMap.get(databaseId);
		if(sqlSessionFactory == null) {
			String resource = "config/mybatis-config.xml";
			InputStream inputStream = Resources.getResourceAsStream(resource);
			if("default".equals(databaseId)) {
				sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
			} else {
				sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream, databaseId);
			}
			sqlSessionFactoryMap.put(databaseId, sqlSessionFactory);
		}
		return sqlSessionFactory; 
	}
	
	public SqlSession getSqlSession() {
		if(!autoCommit && this._session != null) return _session;
		SqlSession session = null;
		try {
			SqlSessionFactory sqlSessionFactory = getSqlSessionFactory();
			session = sqlSessionFactory.openSession();
			if(!autoCommit) _session = session;
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.getSqlSession} " + e.getMessage(), e);
		}
		return session;
	}
	
	public Connection getConnection() {
		return getSqlSession().getConnection();
	}

	public void close() {
		if(_session != null) _session.close();
	}

	public void begin() {
		this.autoCommit = false;
		this.errMsg = null;
		if(_session != null) {
			_session.close();
			_session = null;
		}
	}

	public void commit() {
		if(errMsg == null) _session.commit();
		else _session.rollback();
		_session.close();
		_session = null;
		autoCommit = true;
	}
	
	public String getProduct() {
		String product = productMap.get(this.databaseId);
		if(product == null) {
			
			SqlSession session = getSqlSession();
			try {
				Connection conn = session.getConnection();
				product = conn.getMetaData().getDatabaseProductName().toLowerCase();
				if(product.startsWith("microsoft")) product = "mssql";
				productMap.put(this.databaseId, product);
			} catch(Exception e) {
				setError(e.getMessage());
				Hello.errorLog("{DB.getProduct} " + e.getMessage(), e);
			} finally {
				session.close();
			}
		}
		return product;		
	}
	
	public String getDBType() {
		return getProduct();
	}
	
	public DataSet select(String statement, Object parameters) {
		SqlSession session = getSqlSession();
		DataSet ret = new DataSet();
		try {
			List<?> list = session.selectList(statement, parameters);
			ret = new DataSet(list);
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.select} " + e.getMessage(), e);
		} finally {
			if(autoCommit) session.close();
		}
		return ret;
	}
	
	public int insert(String statement, Object parameters) {
		if(!autoCommit && errMsg != null) return 0;
		SqlSession session = getSqlSession();
		int ret = 0;
		try {
			ret = session.insert(statement, parameters);
			if(autoCommit) session.commit();
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.insert} " + e.getMessage(), e);
		} finally {
			if(autoCommit) session.close();
		}
		return ret;
	}

	public int update(String statement, Object parameters) {
		if(!autoCommit && errMsg != null) return 0;
		SqlSession session = getSqlSession();
		int ret = 0;
		try {
			ret = session.update(statement, parameters);
			if(autoCommit) session.commit();
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.update} " + e.getMessage(), e);
		} finally {
			if(autoCommit) session.close();
		}
		return ret;
	}

	public int delete(String statement, Object parameters) {
		if(!autoCommit && errMsg != null) return 0;
		SqlSession session = getSqlSession();
		int ret = 0;
		try {
			ret = session.delete(statement, parameters);
			if(autoCommit) session.commit();
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.delete} " + e.getMessage(), e);
		} finally {
			if(autoCommit) session.close();
		}
		return ret;
	}

	public DataSet query(String query) {
		return query(query, null);
	}
	
	public DataSet query(String query, Object[] args) {
		SqlSession session = getSqlSession();
		Connection conn = session.getConnection();
		DataSet records = new DataSet();
		if(conn == null) return records;

		this.query = query;
		ResultSet rs = null;
		PreparedStatement pstmt = null;
		try {
			setError(query);
			if(args != null) setError(Arrays.toString(args));
			pstmt = conn.prepareStatement(query);
			if(args != null) {
				for(int i=0; i<args.length; i++) pstmt.setObject(i+1, args[i]);
			}
			rs = pstmt.executeQuery();
			records = new DataSet(rs);
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.query} " + query + " => " + e.getMessage() + "\n" + Arrays.toString(args), e);
		} finally {
			if(rs != null) try { rs.close(); } catch(Exception ignored) {}
			if(pstmt != null) try { pstmt.close(); } catch(Exception ignored) {}
			if(autoCommit) {
				session.close();
			}
		}

		return records;
	}
	
	public int execute(String query) {
		return execute(query, null);
	}
	
	public int execute(String query, Object[] args) {
		SqlSession session = getSqlSession();
		Connection conn = session.getConnection();
		if(conn == null) return -1;

		this.query = query;
		PreparedStatement pstmt = null;
		int ret = -1;
		try {
			setError(query);
			setError(Arrays.toString(args));
			if(insertId) {
				pstmt = conn.prepareStatement(query, PreparedStatement.RETURN_GENERATED_KEYS);
			} else {
				pstmt = conn.prepareStatement(query);
			}
			if(args != null) {
				for(int i=0; i<args.length; i++) pstmt.setObject(i+1, args[i]);
			}
			ret = pstmt.executeUpdate();
			if(insertId && ret == 1) {
				ResultSet rs = pstmt.getGeneratedKeys();
				if (rs != null && rs.next()) {
					try { newId = rs.getInt(1); } catch(Exception ignored) {} finally { rs.close(); }
				}
			}
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.execute} " + query + " => " + e.getMessage() + "\n" + Arrays.toString(args), e);
		} finally {
			if(pstmt != null) try { pstmt.close(); } catch(Exception ignored) {}
			if(autoCommit) {
				session.close();
			}
		}

		return ret;
	}

	public DataSet call(String sql, Object[] inArgs) {
		return call(sql, inArgs, new String[] {});
	}
	
	public DataSet call(String sql, Object[] inArgs, String[] outTypes) {
		DataSet ret = new DataSet();
		SqlSession session = getSqlSession();
		Connection conn = session.getConnection();
		if(conn == null) return ret;

		this.query = sql;
		CallableStatement stmt = null;
		try {
			setError(query);
			stmt = conn.prepareCall(query);
			for(int i=0; i<inArgs.length; i++) stmt.setObject(i+1, inArgs[i]);
			for(int j=0; j<outTypes.length; j++) {
				if("VARCHAR".equals(outTypes[j])) stmt.registerOutParameter(inArgs.length + j + 1, java.sql.Types.VARCHAR);
				else if("INTEGER".equals(outTypes[j])) stmt.registerOutParameter(inArgs.length + j + 1, java.sql.Types.INTEGER);
				else if("DOUBLE".equals(outTypes[j])) stmt.registerOutParameter(inArgs.length + j + 1, java.sql.Types.DOUBLE);
				else if("DECIMAL".equals(outTypes[j])) stmt.registerOutParameter(inArgs.length + j + 1, java.sql.Types.DECIMAL);
				else if("DATE".equals(outTypes[j])) stmt.registerOutParameter(inArgs.length + j + 1, java.sql.Types.DATE);
				else stmt.registerOutParameter(inArgs.length + j + 1, java.sql.Types.OTHER);
			}
			stmt.execute();
			if(outTypes.length > 0) {
				ret.addRow();
				for(int k=0; k<outTypes.length; k++) 
					ret.put("out" + (k + 1), stmt.getObject(inArgs.length + k + 1));
				ret.first();
			}
		} catch(Exception e) {
			setError(e.getMessage());
			Hello.errorLog("{DB.call} " + query + " => " + e.getMessage(), e);
		} finally {
			if(stmt != null) try { stmt.close(); } catch(Exception ignored) {}
			session.close();
		}
		return ret;
	}
	public void setInsertId() {
		this.insertId = true;
	}
	public int getInsertId() {
		return newId;
	}
}