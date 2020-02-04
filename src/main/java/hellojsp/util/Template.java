package hellojsp.util;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Template {
	
	protected String root = Config.getTplRoot();
	protected String layout = null;
	protected String body = null;
	protected Hashtable<String, String> var = new Hashtable<String, String>();
	protected Hashtable<String, DataSet> loop = new Hashtable<String, DataSet>();
	protected Writer out = null;
	private HttpServletRequest request = null;
	private HttpServletResponse response = null;
	private boolean debug = false;
	private String encoding = Config.getEncoding();
	private Message message = null;

	public Template() {
		setRoot(Config.getTplRoot());
	}

	public Template(String path) {
		setRoot(path);
	}

	public void setDebug() {
		this.out = null;
		debug = true;
	}

	public void setDebug(Writer out) {
		this.out = out;
		debug = true;
	}
	
	public void setWriter(Writer out) {
		this.out = out;
	}	

	public void setRequest(HttpServletRequest request, HttpServletResponse response) {
		this.response = response;
		setRequest(request);
	}
	public void setResponse(HttpServletResponse response) {
		this.response = response;
	}
	public void setRequest(HttpServletRequest request) {
		this.request = request;
		Enumeration<String> e = request.getParameterNames();
		while(e.hasMoreElements()) {
			String key = e.nextElement();
			String value = request.getParameter(key);

			//xss
			String value2 = replace(value.toLowerCase(), " ", "");
			if(value2.indexOf("\"") != -1) continue;
			if(value2.indexOf("'") != -1) continue;
			if(value2.indexOf("/*") != -1) continue;
			if(value2.indexOf("*/") != -1) continue;
			if(value2.indexOf("script") != -1) continue;
			if(value2.indexOf("javascript") != -1) continue;
			if(value2.indexOf("expression(") != -1) continue;
			if(value2.indexOf("{") != -1) continue;
			if(value2.indexOf("}") != -1) continue;

			var.put(key, replace(replace(value, "<", "&lt;"), ">", "&gt;"));
		}
	}

	public void setRoot(String path) {
		root = path + "/";
	}

	public void setMessage(Message msg) {
		message = msg;
	}
	
	public void setLayout(String layout) {
		if(layout == null) this.layout = null;
		else {
			this.layout = "layout/layout_" + layout + ".html";
		}
	}

	public void setBody(String body) {
		this.body = body.replace('.', '/') + ".html";
	}	

	public void setVar(String name, String value) {
		if(name == null) return;
		var.put(name, value == null ? "" : value);
	}

	public void setVar(String name, int value) {
		setVar(name, "" + value);
	}

	public void setVar(String name, long value) {
		setVar(name, "" + value);
	}
	
	public void setVar(String name, boolean value) {
		setVar(name, value == true ? "true" : "false");
	}

	public void setVar(Hashtable<String, Object> values) {
		setVar(new HashMap<String, Object>(values));
	}

	public void setVar(HashMap<String, Object> values) {
		if(values == null) return;
		for(String key : values.keySet()) {
			if(values.get(key) != null) {
				setVar(key, values.get(key).toString());
			}
		}
	}
	
	public void setVar(DataSet values) {
		if(values != null && values.size() > 0) {
			if(values.getIndex() == -1) values.next();
			this.setVar(values.getRow());
		}
	}

	public void setVar(String name, DataSet values) {
		if(values.getIndex() == -1) values.next();
		this.setVar(name, values.getRow());
	}
	
	public void setVar(String name, HashMap<String, Object> values) {
		if(name == null || values == null) return;
		for(String key : values.keySet()) {
			if(values.get(key) == null || key.length() == 0) continue;
			if(key.charAt(0) != '.') {
				setVar(name + "." + key, values.get(key).toString());
			} else {
				setLoop(key.substring(1), (DataSet)values.get(key));
			}
		}
	}

	public void setLoop(String name, DataSet rs) {
		if(rs != null && rs.size() > 0) {
			rs.first();
			loop.put(name, rs);
			setVar(name, true);
		} else {
			loop.put(name, new DataSet());
			setVar(name, false);
		}
	}

	public String fetch(String filename) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		this.out = new OutputStreamWriter(bos);
		parseTag(readFile(filename));
		return bos.toString();
	}

	public String fetchString(String str) throws Exception {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		this.out = new OutputStreamWriter(bos);
		parseTag(str);
		return bos.toString();
	}

	public String fetchAll() throws Exception {
		if(this.body != null) this.setVar("BODY", this.body);
		return fetch(null == this.layout ? this.body : this.layout);
	}
	
	public void print(Writer out, String filename) throws Exception {
		this.out = out;
		parseTag(readFile(filename));
		clear();
	}
	
	public void print(String filename, Writer out) throws Exception {
		this.out = out;
		parseTag(readFile(filename));
		clear();
	}

	public void print() throws Exception {
		print(this.out);
	}

	public void print(Writer out) throws Exception {
		if(this.layout == null) this.print(out, this.body);
		else {
			setVar("BODY", this.body);
			print(out, this.layout);
		}
	}	

	public void display() throws Exception {
		print(this.out);
	}

	public void display(Writer out) throws Exception {
		print(out);
	}
	
	private void parseTag(String buffer) throws Exception {
		int pos = 0, offset = 0;
		while((pos = buffer.indexOf("<!--", offset)) != -1) {
			parseVar(buffer.substring(offset, pos));
			offset = pos + 4;
			
			String str = buffer.substring(offset, offset + 3);
			if( !str.equals("@in")
				&& !str.equals("@ex")
				&& !str.equals("@lo")
				&& !str.equals("@if")
				&& !str.equals("@ni")
				&& !str.equals("/lo")
				&& !str.equals("/if")
				&& !str.equals("/ni")
			) { out.write("<!--"); continue; }

			int end = buffer.indexOf("-->", pos);
			if(end != -1) {
				offset = end + 3;
				String cmd = buffer.substring(pos + 4, end).trim();
				if(cmd.startsWith("@include(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					if(var.get(names[2]) != null) {
						parseTag(readFile(var.get(names[2]).toString()));
					} else {
						parseTag(readFile(names[2]));
					}
				} else if(cmd.startsWith("@loop(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					DataSet rs = (DataSet)loop.get(names[2]);
					String etag = "<!--/loop(" + names[2] + ")-->";
					int loop_end = buffer.indexOf(etag, offset);

					if(loop_end != -1) {
						if(rs != null) {
							rs.first();
							while(rs.next()) {
								setVar(names[2], rs.getRow());
								parseTag(buffer.substring(end + 3, loop_end));
							}
						} else {
							setError("Loop Data is not exists, name is " + names[2]);
						}
						offset = loop_end + etag.length();
					} else {
						setError("Loop end tag is not found, name is " + names[2]);
					}
				} else if(cmd.startsWith("@if(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					String etag = "<!--/if(" + names[2] + ")-->";
					int if_end = buffer.indexOf(etag, offset);
					if(if_end != -1) {
						if(names[2].indexOf(':') != -1) {
							String[] arr = names[2].split(":", -1);
							if(!arr[1].equals(var.get(arr[0]))) {
								offset = if_end + etag.length();
							}
						} else if(var.get(names[2]) == null 
							|| "false".equals(var.get(names[2])) 
							|| "".equals(var.get(names[2])) 
							|| (names[2].indexOf("_yn") != -1 && "N".equals(var.get(names[2])))
							|| (names[2].indexOf("is_") != -1 && "0".equals(var.get(names[2])))
						) {
							offset = if_end + etag.length();
						}
					} else {
						setError("If end tag is not found, name is " + names[2]);
					}
				} else if(cmd.startsWith("@nif(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;

					String etag = "<!--/nif(" + names[2] + ")-->";
					int if_end = buffer.indexOf(etag, offset);
					if(if_end != -1) {
						if(names[2].indexOf(':') != -1) {
							String[] arr = names[2].split(":", -1);
							if(arr[1].equals(var.get(arr[0]))) {
								offset = if_end + etag.length();
							}
						} else if(var.get(names[2]) != null 
							&& !"false".equals(var.get(names[2])) 
							&& !"".equals(var.get(names[2]))
							&& !(names[2].indexOf("_yn") != -1 && "N".equals(var.get(names[2])))
							&& !(names[2].indexOf("is_") != -1 && "0".equals(var.get(names[2])))
						) {
							offset = if_end + etag.length();
						}
					} else {
						setError("If end tag is not found, name is " + names[2]);
					}
				} else if(cmd.startsWith("@execute(")) {
					String[] names = parseCmd(cmd);
					if(names == null) continue;
					if(null != request && null != response) {
						RequestDispatcher dispatcher = request.getRequestDispatcher(names[2]);
						dispatcher.include(request, response);
					}
				}
			} else {
				setError("Command end tag is not found");
				out.write("<!-- ");
			}
		}
		parseVar(buffer.substring(offset));
		buffer = null;
	}

	private String[] parseCmd(String buffer) {
		buffer = buffer.trim();
		String[] ret = new String[3];
		if(buffer.startsWith("@")) { 
			String[] arr1 = buffer.split("\\(");
			if(arr1.length != 2) return null;
			ret[0] = arr1[0].substring(1);
			ret[1] = ret[0].equals("name") ? "NAME" : "FILE";
			ret[2] = parseString(arr1[1].substring(0, arr1[1].length() - 1));
		} else {
			String[] arr1 = buffer.split(" ");
			if(arr1.length != 3) return null;
			ret[0] = arr1[0].toUpperCase();
			ret[1] = arr1[1].toUpperCase();
			ret[2] = parseString(arr1[2].substring(1, arr1[2].length() - 1));
		}
		return ret;
	}

	public String parseString(String buffer) {
        String arr1[] = buffer.split("\\}\\}");
        StringBuffer sb = new StringBuffer();

        for(int i=0; i<arr1.length; i++) {
            String arr2[] = arr1[i].split("\\{\\{");
			sb.append(arr2[0]);
            if(arr2.length == 2) {
                if(var.containsKey(arr2[1])) {
					sb.append(var.get(arr2[1]).toString());
				}
            }
        }
		buffer = null;
		return sb.toString();
    }

	private void parseVar(String buffer) throws Exception {
		int tail = 0, offset = buffer.length() - 2;
		if(offset >= 0 && buffer.substring(offset).equals("}}")) {
			buffer += " "; tail = 1;
		}
        String arr1[] = buffer.split("\\}\\}");
		if(arr1.length > 1) {
			for(int i=0, len=arr1.length - tail; i<len; i++) {
				String arr2[] = arr1[i].split("\\{\\{");
				out.write(arr2[0]);
				if(arr2.length == 2) {
					if(var.containsKey(arr2[1])) {
						out.write(var.get(arr2[1]).toString());
					} else if(message != null) {
						out.write(message.get(arr2[1], ""));
					}

				} else if(arr2.length > 2) {
					int max = arr2.length - 1;
					for(int j=1; j<max; j++) {
						out.write("{{" + arr2[j]);
					}
					if(var.containsKey(arr2[max])) {
						out.write(var.get(arr2[max]).toString());
					}
				} else if(i != (arr1.length - 1)) {
					out.write("}}");
				}
			}
		} else {
			out.write(buffer);
		}
		out.flush();
		buffer = null;
    }

    public String readFile(String filename) throws Exception {
		filename = replace(filename, "..", "");
        File f = new File(root + filename);
        if(!f.exists()) {
            f = new File(filename);
            if(!f.exists()) {
                setError("File not found!!, filename is " + root + filename);
                return "";
            }
        }

        FileInputStream fin = new FileInputStream(f);
        Reader reader = new InputStreamReader(fin, encoding);
        BufferedReader br = new BufferedReader(reader);

        StringBuffer sb = new StringBuffer();
		int c = 0;
		while((c = br.read()) != -1) {
			sb.append((char)c);
		}
        br.close();
        reader.close();
        fin.close();

		return sb.toString();
    }

	private void setError(String msg) throws Exception {
		if(debug == true) {
			if(null != out) out.write("<hr>" + msg + "<hr>\n");
			else Hello.errorLog(msg);
		}
	}

	public void clear() {
		var.clear();
		loop.clear();
	}

	public String replace(String s, String sub, String with) {
		int c = 0;
		int i = s.indexOf(sub,c);
		if (i == -1) return s;

		StringBuffer buf = new StringBuffer(s.length() + with.length());

		synchronized(buf) {
			do {
				buf.append(s.substring(c, i));
				buf.append(with);
				c = i + sub.length();
			} while((i = s.indexOf(sub, c)) != -1);
			if(c < s.length()) {
				buf.append(s.substring(c, s.length()));
			}
			return buf.toString();
		}
	}
}
