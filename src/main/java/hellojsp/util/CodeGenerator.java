package hellojsp.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.util.Vector;

import hellojsp.db.DB;

public class CodeGenerator {
	public String schema;
	public String table;
	public String directory = "";
	public String prefix = "";
	public String title = "";
	public String mid = "MenuId";
	public boolean overwrite = false;
	public String user = "";
	public String[] types;
	public String srcDir = "src";
	public String resultDir;
	public DataSet result;
	public DataSet info;
	public DataSet columns;
	public Vector<String> primaries;
	private Template p;

	public CodeGenerator(String schema, String table, String[] types) {
		this.schema = schema;
		this.table = table;
		this.types = types;
	}

	public CodeGenerator(String table, String[] types) {
		this.table = table;
		this.types = types;
	}

	public void setUtility(Template p) { this.p = p; }
	public void setResultDir(String path) { this.resultDir = path; }
	public void setDirectoryName(String name) { this.directory = name; }
	public void setPrefixName(String name) { this.prefix = name; }
	public void setTitleName(String name) { this.title = name; }
	public void setMenuId(String id) { this.mid = id; }
	public void setOverWrite(boolean overwrite) { this.overwrite = overwrite; }
	public void setUser(String user) { this.user = user; }
	public void setSrcDir(String dir) { this.srcDir = dir; }

	public DataSet get() throws Exception {

		getMetaInfo();

		result = new DataSet();
		if(info.next()) {
			info.put("prefix", "".equals(prefix) ? getSingularName(table.toLowerCase()) : prefix);
			info.put("directory", "".equals(directory) ? info.getString("prefix") : directory);
			info.put("daoName", getDaoName(table));
			info.put("className", getClassName(table));
			info.put("mid", mid);
			info.put("pk", Hello.join(", ", primaries.toArray()));
			if("".equals(info.getString("table_comment"))) info.put("table_comment", info.getString("table_name"));
			if(!"".equals(title)) info.put("table_comment", title);
			info.put("table_title", info.getString("table_comment").replaceAll("°ü¸®$", ""));
			
			info.put("is_enctype", false);
			info.put("is_editor", false);
			DataSet primaryList = Hello.arr2loop((String[])primaries.toArray(new String[primaries.size()]));
			columns.first();
			while(columns.next()) {
				if("true".equals(columns.getString("is_file"))) info.put("is_enctype", true);
				if("true".equals(columns.getString("is_textarea"))) info.put("is_editor", true);
				primaryList.first();
				while(primaryList.next()) {
					if(columns.getString("column_name").equals(primaryList.getString("id"))) primaryList.put("is_num", columns.getString("is_num"));
				}
			}

			p.setVar("tbl", info);
			p.setLoop("primaries", primaryList);
			p.setLoop("list", columns);
			boolean isSpring = types.length == 7;

			for(int i=0; i<types.length; i++) {
				String filename = "";
				if(types[i].equals("init.jsp")) filename = types[i];
				else if(types[i].equals("dao.java")) filename = getClassName(table) + "Dao.java";
				else if(types[i].equals("Controller.java")) filename = getClassName(table) + "Controller.java";
				else if(types[i].equals("Service.java")) filename = getClassName(table) + "Service.java";
				else if(types[i].equals("ServiceImpl.java")) filename = getClassName(table) + "ServiceImpl.java";
				else if(types[i].equals("Dao.java")) filename = getClassName(table) + "Dao.java";
				else if(types[i].equals("Mapper.xml")) filename = getClassName(table) + "Mapper.xml";
				else if(isSpring) filename = types[i]; 
				else filename = info.getString("prefix") + "_" + types[i]; 

				result.addRow();
				result.put("filename", filename);
				result.put("src", p.fetch(srcDir + "/" + types[i].replace(".", "_"))
					.replace("<!@--", "<!--")
					.replace("[", "{").replace("]", "}")
					.trim()
				);
				result.put("src_html", result.getString("src").replace("<", "&lt;").replace(">", "&gt;"));
				result.put("write_path"
					, (resultDir + (types[i].indexOf(".html") != -1 ? "/html" : "")
					+ "/" + ("".equals(directory) ? info.getString("prefix") : directory) 
					) + "/" + filename
				);

				if(types[i].equals("Controller.java")) result.put("write_path", resultDir + "/controller/" + filename);
				else if(types[i].equals("Service.java")) result.put("write_path", resultDir + "/service/" + filename);
				else if(types[i].equals("ServiceImpl.java")) result.put("write_path", resultDir + "/serviceImpl/" + filename);
				else if(types[i].equals("Dao.java")) result.put("write_path", resultDir + "/dao/" + filename);
				else if(types[i].equals("Mapper.xml")) result.put("write_path", resultDir + "/xml/" + filename);
				else if(isSpring && types[i].endsWith(".jsp")) result.put("write_path", resultDir + "/jsp/" + getClassName(table) + "/" + filename);
				else {
					result.put("write_path"
						, (resultDir + (types[i].indexOf(".html") != -1 ? "/html" : "")
						+ "/" + ("".equals(directory) ? info.getString("prefix") : directory) 
						) + "/" + filename
					);
				}

				result.put("write_log", writeFile(result.getString("src"), result.getString("write_path")));
				result.put("write_flag", "success.".equals(result.getString("write_log")));
				result.put("type", types[i]);
			}
		}
		result.first();
		return result;
	}

	public String getSingularName(String name) {
		//String[] exceptions = { "sms", "tb_sms" };
		String[] exceptions = { };
		if(!Hello.inArray(name, exceptions)) {
			try {
				if(name.indexOf("_") == 2) name = name.substring(3);
				if(name.endsWith("ches")) name = name.substring(0, name.length() - 2);
				else if(name.endsWith("ies")) name = name.substring(0, name.length() - 3) + "y";
				//else if(name.endsWith("s")) name = name.substring(0, name.length() - 1);
			} catch(Exception e) { }
		}
		return name;
	}

	public String getDaoName(String str) {
		String name = "";
		str = getSingularName(str.toLowerCase());
		try {
			String[] names = str.split("\\_");
			if(names.length >= 1) {
				for(int i=0; i<names.length; i++) {
					name += i == 0 ? names[i].substring(0, 1).toLowerCase() : names[i].substring(0, 1).toUpperCase(); 
					name += names[i].substring(1).toLowerCase();
				}
			}
		} catch(Exception e) {
			name = str;
		}
		return name;
	}

	public String getClassName(String str) {
		String name = "";
		str = getSingularName(str.toLowerCase());
		try {
			String[] names = str.split("\\_");
			if(names.length >= 1) {
				for(int i=0; i<names.length; i++) {
					name += names[i].substring(0, 1).toUpperCase();
					name += names[i].substring(1).toLowerCase();
				}
			}
		} catch(Exception e) {
			name = str;
		}
		return name;
	}

    private String writeFile(String result, String path) throws Exception {
    	String msg = "success.";
        File f = new File(path);

        if(!overwrite && f.exists()) {
        	msg = "failed - file exists.";
        } else {
			if(!f.getParentFile().isDirectory()) {
				f.getParentFile().mkdirs();
				if("Linux".equals(System.getProperty("os.name"))) {
					Runtime.getRuntime().exec("chown -R " + user + ":" + user + " " + f.getParentFile());
				}
			}
			BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(path), "UTF8"));
			//bw.write(new String(result.trim().getBytes("KSC5601"), "8859_1"));
        	try {
				bw.write(result.trim());
				if("Linux".equals(System.getProperty("os.name"))) {
					Runtime.getRuntime().exec("chown -R " + user + ":" + user + " " + path);
				}
			} catch(Exception e) {
				msg = "failed - write error.";
			} finally {
				bw.close();
			}
		}
		return msg;
    }
	
	public void getMetaInfo() throws Exception {
		DB conn = new DB();
		DataSet constraints = new DataSet();
		info = new DataSet();
		columns = new DataSet();
		String dbtype = conn.getDBType();
		if("mysql".equals(dbtype)) {
			info = conn.query(
				"SELECT a.* FROM information_schema.tables a"
				+ " WHERE a.table_schema = '" + schema + "' AND a.table_name = '" + table + "' AND a.table_schema != 'information_schema'"
			);
			columns = conn.query(
				"SELECT a.*, a.numeric_precision n_precision, a.character_maximum_length max_length, IF(a.is_nullable = 'YES', 'true', 'false') is_nullable"
				+ " FROM information_schema.columns a"
				+ " WHERE a.table_schema = '" + schema + "' AND a.table_name = '" + table + "'"
				+ " ORDER BY a.ordinal_position"
			);
			constraints = conn.query(
				"SELECT b.*"
				+ " FROM information_schema.table_constraints a"
				+ " INNER JOIN information_schema.key_column_usage b ON a.table_name = b.table_name AND a.constraint_name = b.constraint_name"
				+ " WHERE a.table_schema = '" + schema + "' AND a.table_name = '" + table + "' AND a.constraint_type = 'PRIMARY KEY'"
			);
		} else if("mssql".equals(dbtype)) {
			info = conn.query(
				"SELECT a.name table_name, CAST(b.[value] as varchar) table_comment"
				+ " FROM sys.tables a LEFT JOIN sys.extended_properties b ON b.major_id = a.object_id AND b.minor_id = 0"
				+ " WHERE a.name = '" + table + "'"
			);
			columns = conn.query(
				"SELECT a.*, a.precision n_precision, a.name column_name"
				+ ", c.name tbl_name, b.name data_type"
				+ ", CAST(d.[value] as varchar) column_comment"
				+ " FROM sys.columns a"
				+ " INNER JOIN sys.types b ON a.user_type_id = b.user_type_id"
				+ " INNER JOIN sys.objects c ON c.object_id = a.object_id"
				+ " LEFT JOIN sys.extended_properties d ON d.major_id = a.object_id AND d.minor_id = a.column_id AND d.class = 1"
				+ " WHERE c.name = '" + table + "'"
				+ " ORDER BY a.column_id"
			);
			constraints = conn.query(
				"SELECT b.*"
				+ " FROM information_schema.table_constraints a"
				+ " INNER JOIN information_schema.constraint_column_usage b ON a.constraint_name = b.constraint_name"
				+ " WHERE a.table_name = '" + table + "' AND a.constraint_type = 'PRIMARY KEY'"
			);
		} else if("oracle".equals(dbtype)) {
			info = conn.query(	
				"SELECT a.*, b.comments table_comment"
				+ " FROM user_tables a LEFT JOIN user_tab_comments b ON a.table_name = b.table_name"
				+ " WHERE a.table_name = '" + table + "'"
			);
			columns = conn.query(
				"SELECT a.*, a.data_precision n_precision, a.data_length max_length, (CASE WHEN a.nullable = 'Y' THEN 'true' ELSE 'false' END) is_nullable"
				+ ", b.comments column_comment"
				+ " FROM user_tab_columns a LEFT JOIN user_col_comments b"
				+ " ON a.table_name = b.table_name AND a.column_name = b.column_name"
				+ " WHERE a.table_name='" + table + "'"
				+ " ORDER BY a.column_id"
			);
			constraints = conn.query(
				"SELECT * FROM user_cons_columns"
				+ " WHERE constraint_name = ("
					+ "SELECT constraint_name FROM user_constraints"
					+ " WHERE constraint_type = 'P' AND table_name = '" + table + "'"
				+ ")"
			);
		} else if("db2".equals(dbtype)) {
			info = conn.query(
				"SELECT a.*, b.remarks table_comment"
				+ " FROM sysibm.tables a LEFT JOIN syscat.tables b ON a.table_schema = b.tabschema AND a.table_name = b.tabname"
				+ " WHERE a.table_name = '" + table + "'"
			);
			columns = conn.query(
				"SELECT a.*, a.name column_name, a.remarks column_comment, a.longlength max_length"
				+ ", (CASE WHEN a.avgcollenchar = -1 AND a.longlength < 65535 THEN a.longlength ELSE 0 END) n_precision"
				+ ", (CASE WHEN a.identity = 'Y' THEN 'true' ELSE 'false' END) is_identity "
				+ ", (CASE WHEN a.nulls = 'Y' THEN 'true' ELSE 'false' END) is_nullable "
				+ " FROM sysibm.syscolumns a"
				+ " WHERE a.tbname = '" + table + "'"
				+ " ORDER BY a.colno"
			);
			constraints = conn.query(
				"SELECT b.*, b.colname column_name"
				+ " FROM syscat.tabconst a"
				+ " INNER JOIN syscat.keycoluse b ON a.tabname = b.tabname AND a.constname = b.constname"
				+ " WHERE a.tabname = '" + table + "' AND a.type = 'P'"
			);
		}

		primaries = new Vector<String>();
		String parameters = "";
		while(constraints.next()) {
			String column = constraints.getString("column_name").toLowerCase();
			primaries.add(column);
			parameters += column + "=[[list." + column + "]]&";
		}

		int limit = 6;
		int limit2 = 3;
		int i = 0;
		int i2 = 0;
		int len = columns.size();
		String[] ntypes = new String[] { "nchar", "nvarchar" };
		while(columns.next()) {
			columns.put("column_name", columns.getString("column_name").toLowerCase());
			columns.put("column_name2", "#{" + columns.getString("column_name").toLowerCase() + "}");
			columns.put("column_name3", "${" + columns.getString("column_name").toLowerCase() + "}");
			if("".equals(columns.getString("column_comment"))) columns.put("column_comment", columns.getString("column_name"));
			columns.put("column_comment2", columns.s("column_comment").replace(" ", "_"));
			columns.put("sAnn", ++i == limit && columns.size() > limit);
			columns.put("eAnn", len != limit && i >= limit && i == columns.size());
			columns.put("sAnn2", ++i2 == limit2 && columns.size() > limit2);
			columns.put("eAnn2", len != limit2 && i2 >= limit2 && i2 == columns.size());
			columns.put("params", parameters);

			columns.put("is_date", "" + (columns.getString("column_name").indexOf("_date") != -1 || columns.getString("column_name").startsWith("cdate_")));
			//columns.put("is_num", "" + (columns.getInt("n_precision") > 0 && !"status".equals(columns.getString("column_name"))));
			columns.put("is_num", "" + (columns.getInt("n_precision") > 0));
			columns.put("maxsize", (columns.getInt("max_length") / (Hello.inArray(columns.getString("data_type"), ntypes) ? 2 : 1)));
			columns.put("length", "true".equals(columns.getString("is_num")) ? 5 : (columns.getInt("max_length") / (Hello.inArray(columns.getString("data_type"), ntypes) ? 2 : 1)));
			columns.put("is_textarea", "" + (columns.getInt("length") > 500));
			if(columns.getInt("length") > 70) columns.put("length", 70);
			if(columns.getString("column_name").endsWith("_cnt")) columns.put("input_class", " class=\"input-mini\"");
			else columns.put("input_class", "");
			if("true".equals(columns.getString("is_date"))) { 
				columns.put("length", 10);
				columns.put("input_class", " class=\"cal01\"");
			}
			columns.put("is_reg_date", "" + ("reg_date".equals(columns.getString("column_name"))));
			columns.put("is_reg_user", "" + ("reg_user".equals(columns.getString("column_name"))));
			columns.put("is_item", "" + ("status".equals(columns.getString("column_name"))));
			columns.put("is_status", "" + false);
			columns.put("is_is", "" + (columns.getString("column_name").startsWith("is_")));
			columns.put("is_yn", "" + (columns.getString("column_name").endsWith("_yn")));
			columns.put("is_pri", "" + primaries.contains(columns.getString("column_name")));
			columns.put("is_file", "" + (columns.getString("column_name").indexOf("_file") != -1 || columns.getString("column_name").indexOf("_img") != -1 || columns.getString("column_name").indexOf("_image") != -1));
			columns.put("is_image", "" + (columns.getString("column_name").indexOf("_img") != -1 || columns.getString("column_name").indexOf("_image") != -1));

			if("mysql".equals(dbtype)) {
				columns.put("is_identity", "true".equals(columns.getString("is_pri")) && "true".equals(columns.getString("is_num")));
			} else if("oracle".equals(dbtype)) {
				columns.put("is_identity", "true".equals(columns.getString("is_pri")) && "true".equals(columns.getString("is_num")));
			}
		}
		conn.close();
    }
}