package hellojsp.util;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

public class Form {

	public String name = "form1";
	public ArrayList<String[]> elements = new ArrayList<String[]>();
	public HashMap<String, Object> data = new HashMap<String, Object>();
	public HashMap<String, FileItem> uploadedFiles = new HashMap<String, FileItem>();
	public String errMsg = null;
	public String uploadDir = Config.getDataDir() + "/files";
	public int maxPostSize = Config.getInt("maxPostSize", 1024) * 1024 * 1024;
	public String encoding = Config.getEncoding();

	private static HashMap<String, String> options = new HashMap<String, String>();
	private HttpServletRequest request;
	private Writer out = null;
	private boolean debug = false;
	private String allowScript = null;
	private String allowHtml = null;
	private String allowIframe = null;
	private String[] denyExt = {"jsp", "php", "asp", "aspx", "html", "htm", "exe", "sh"}; 

	static {
		options.put("email", "^[a-z0-9A-Z\\_\\.\\-]+@([a-z0-9A-Z\\.\\-]+)\\.([a-zA-Z]+)$");
		options.put("url", "^(http:\\/\\/)(.+)");
		options.put("number", "^-?[\\,0-9]+$");
		options.put("domain", "^([a-z0-9]+)([a-z0-9\\.\\-]+)\\.([a-z]{2,4})$");
		options.put("engonly", "^([a-zA-Z]+)$");
	}

	public Form() {}

	public Form(String name) {
		this.name = name;
	}
	
	public Form(String name, HttpServletRequest request) {
		this.name = name;
		setRequest(request);
	}

	public Form(HttpServletRequest request) {
		this.name = "form1";
		setRequest(request);
	}

	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}
	
	public void setDebug() {
		this.out = null;
		this.debug = true;
	}

	public void setError(String msg, Exception ex) {
		try {
			if(null != out && debug == true) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug == true) Hello.errorLog(msg, ex);
		} catch(Exception e) {}
	}
	
	public void setUploadDir(String dir) {
		this.uploadDir = dir;
	}
	
	public void setDenyExt(String[] arr) {
		this.denyExt = arr;
	}
	
	public void setRequest(HttpServletRequest req) {
		this.request = req;

		if(ServletFileUpload.isMultipartContent(req)) {
			DiskFileItemFactory factory = new DiskFileItemFactory();

			// Configure a repository (to ensure a secure temp location is used)
			ServletContext servletContext = req.getServletContext();
			File repository = (File) servletContext.getAttribute("javax.servlet.context.tempdir");
			factory.setRepository(repository);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setFileSizeMax(maxPostSize);
			upload.setHeaderEncoding("utf-8");

			// Parse the request
			try { 
				List<FileItem> items = upload.parseRequest(request);
				for(FileItem item : items) {
				    if (item.isFormField()) {
				    	data.put(item.getFieldName(), item.getString("utf-8"));
				    } else {
				    	uploadedFiles.put(item.getFieldName(), item);
				    }					
				}
			
			}
			catch(Exception e) {
				Hello.errorLog("{Form.setRequest} Multipart Parsing Error", e);
			}
			
		}
	}
	
	public void addElement(String name, String value, String attributes) {
		String[] element = new String[3];
		element[0] = name;
		element[1] = value;
		element[2] = attributes;
		elements.add(element);
		if(null != attributes && attributes.indexOf("allowscript:'Y'") != -1) {
			allowScript = (null == allowScript ? "" : allowScript) + "[" + name + "]";
		}
		if(null != attributes && attributes.indexOf("allowhtml:'Y'") != -1) {
			allowHtml = (null == allowHtml ? "" : allowHtml) + "[" + name + "]";
		}
		if(null != attributes && attributes.indexOf("allowiframe:'Y'") != -1) {
			allowIframe = (null == allowIframe ? "" : allowIframe) + "[" + name + "]";
		}
	}

	public void addElement(String name, int value, String attributes) {
		addElement(name, "" + value, attributes);
	}
	
	public void put(String name, String value) {
		data.put(name, value);
	}

	public void put(String name, int value) {
		data.put(name, "" + value);
	}

	public void put(String name, double value) {
		data.put(name, "" + value);
	}

	public void put(String name, boolean value) {
		data.put(name, "" + value);
	}

	public String get(String name) {
		return get(name, "");
	}

	public String get(String name, String str) {
		if(data.containsKey(name)) {
			return xss(name, data.get(name).toString());
		} else {
			return str;
		}
	}

	public String glue(String delim, String names) {
		String[] vars = names.split(",");
		if(vars == null) return "";
		for(int i=0; i<vars.length; i++) {
			vars[i] = get(vars[i].trim());
		}
		return Hello.join(delim, vars);
	}

	public String[] getArr(String name) {
		String[] arr = request.getParameterValues(name);
		if(null != arr) {
			for(int i=0; i<arr.length; i++) {
				arr[i] = xss(name, arr[i]);
			}
		}
		return arr;
	}

	private String xss(String name, String value) {
		if(null != allowHtml && allowHtml.indexOf("[" + name + "]") == -1) {
			value = Hello.replace( Hello.replace(value , "<", "&lt;") , ">", "&gt;");
		} else {
			if(null == allowScript || allowScript.indexOf("[" + name + "]") == -1) {

				String tail = value.endsWith(">") ? ">" : "";
				String[] x1 = value.split(">");
				String res = "";
				for(int i=0; i<x1.length; i++) {
					String[] x2 = x1[i].split("<");
					for(int j=0; j<x2.length; j++) {
						if(j == 0) {
							res += x2[0];
						} else {
							if(j > 0) res += "<";
							if(j == x2.length - 1) {
								res += x2[j].replaceAll("(?i)(x-)?(vbscript|javascript|script|expression|eval|FSCommand|onAbort|onActivate|onAfterPrint|onAfterUpdate|onBeforeActivate|onBeforeCopy|onBeforeCut|onBeforeDeactivate|onBeforeEditFocus|onBeforePaste|onBeforePrint|onBeforeUnload|onBegin|onBlur|onBounce|onCellChange|onChange|onClick|onContextMenu|onControlSelect|onCopy|onCut|onDataAvailable|onDataSetChanged|onDataSetComplete|onDblClick|onDeactivate|onDrag|onDragEnd|onDragLeave|onDragEnter|onDragOver|onDragDrop|onDrop|onEnd|onError|onErrorUpdate|onFilterChange|onFinish|onFocus|onFocusIn|onFocusOut|onHelp|onKeyDown|onKeyPress|onKeyUp|onLayoutComplete|onLoad|onLoseCapture|onMediaComplete|onMediaError|onMouseDown|onMouseEnter|onMouseLeave|onMouseMove|onMouseOut|onMouseOver|onMouseUp|onMouseWheel|onMove|onMoveEnd|onMoveStart|onOutOfSync|onPaste|onPause|onProgress|onPropertyChange|onReadyStateChange|onRepeat|onReset|onResize|onResizeEnd|onResizeStart|onResume|onReverse|onRowsEnter|onRowExit|onRowDelete|onRowInserted|onScroll|onSeek|onSelect|onSelectionChange|onSelectStart|onStart|onStop|onSyncRestored|onSubmit|onTimeError|onTrackChange|onUnload|onURLFlip|seekSegmentTime)", "x-$2");
							} else {
								res += x2[j];
							}
						}
					}
					if(i + 1 < x1.length) res += ">";
				}
				res += tail;
				value = res;
			}
			if(null == allowIframe || allowIframe.indexOf("[" + name + "]") == -1) {
				value = value.replaceAll("(?i)<iframe[^>]*>", "").replaceAll("(?i)</iframe>", "");
			}
		}
		return value;
	}

	public HashMap<String, Object> getMap(String name) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		int len = name.length();
		try {
			for(String key : map.keySet()){
				if(key.matches("^(" + name + ")(.+)$")) {
					map.put(key.substring(len), xss(key, data.get(key).toString()));
				}
			}
		} catch(Exception ex) {
			Hello.errorLog("{Form.getMap} " + ex.getMessage(), ex);
		}
		return map;
	}
	
	public DataMap getDataMap(String name) {
		return new DataMap(getMap(name));
	}

	public int getInt(String name) {
		return getInt(name, 0);
	}

	public int getInt(String name, int i) {
		String str = get(name);
		if(str.matches("^-?[\\,0-9]+$")) return Integer.parseInt(Hello.replace(str, ",", ""));
		else return i;
	}

	public long getLong(String name) {
		return getLong(name, 0);
	}

	public long getLong(String name, long i) {
		String str = get(name);
		if(str.matches("^-?[\\,0-9]+$")) return Long.parseLong(Hello.replace(str, ",", ""));
		else return i;
	}

	public double getDouble(String name) {
		return getDouble(name, 0.0);
	}

	public double getDouble(String name, double i) {
		String str = get(name);
		if(str.matches("^-?[\\.\\,0-9]+$")) return Double.parseDouble(Hello.replace(str, ",", ""));
		else return i;
	}
	
	public boolean validate() {
		for(String[] element : elements) {
		    try { isValid(element); } catch(Exception e) { this.errMsg = e.getMessage(); return false; }
		}
		return true;
	}

	public File saveFile(String name) {
		return saveFile(name, null);
	}

	public File saveFile(String name, String path) {
		FileItem f = uploadedFiles.get(name);
		if(f != null) {
			String orgname = f.getName();
			String ext = Hello.getFileExt(orgname).toLowerCase();
			if(denyExt != null && Hello.inArray(ext, denyExt)) {
				f.delete();
				Hello.errorLog("{Form.saveFile} file:" + orgname, new Exception("This file extention is deined."));
				return null;
			}
			File target;
			if(path == null) {
				do {
					path = uploadDir + "/" + Hello.sha1(orgname + System.currentTimeMillis());
					target = new File(path);
				} while(target.exists());
			} else {
				target = new File(path);
			}
			try {
				if(target.isDirectory()) {
					target = new File(path + "/" + f.getName());
				}
				if(!target.getParentFile().isDirectory()) {
					target.getParentFile().mkdirs();
				}
				f.write(target);
				f.delete();
			} catch(Exception ex) {
				Hello.errorLog("{Form.saveFile} path:" + path, ex);
				f.delete();
			}
			if(target.exists()) return target;
		} else {
			Hello.errorLog("{Form.saveFile} " + name + " is not uploaded");			
		}
		return null;
	}

	public String getFileName(String name) {
		FileItem f = uploadedFiles.get(name);
		return f == null ? null : f.getName();
	}

	public String getFileType(String name) {
		FileItem f = uploadedFiles.get(name);
		return f == null ? null : f.getContentType();
	}

	public boolean isset(String name) {
		return data.containsKey(name);
	}
	
	private HashMap<String, String> getAttributes(String str) {
		HashMap<String, String> map = new HashMap<String, String>();
		if(str != null && !"".equals(str)) {
			String[] arr = str.split("\\,");
			for(int i=0; i<arr.length; i++) {
				String[] arr2 = null;
				arr2 = arr[i].split("[=:]");
				if(arr2.length == 2) {
					map.put(arr2[0].trim().toUpperCase(), arr2[1].replace('\'', '\0').trim());
				}
			}
		}
		return map;
	}
	
	private boolean isValid(String[] element) throws Exception {
		String name = element[0];
		String value = get(name);
		HashMap<String, String> attributes = getAttributes(element[2]);
		String nicname = attributes.get("TITLE");
		if(nicname == null) nicname = name;
		
		if(attributes.containsKey("REQUIRED")) {
			if(uploadedFiles.get(name) !=  null) value = getFileName(name);
			if("".equals(value.trim())) {
				throw new Exception(nicname + " is required");
			}
		}
		
		if(!"".equals(value) && attributes.containsKey("MAXBYTE")) {
			int size = Integer.parseInt(attributes.get("MAXBYTE"));
			if(value.getBytes().length > size) {
				throw new Exception(nicname + " size is over");
			}
		}

		if(!"".equals(value) && attributes.containsKey("MINBYTE")) {
			int size = Integer.parseInt(attributes.get("MINBYTE"));
			if(value.getBytes().length < size) {
				throw new Exception(nicname + " size is less");
			}
		}
		if(attributes.containsKey("FIXBYTE")) {
			int size = Integer.parseInt(attributes.get("FIXBYTE"));
			if(value.getBytes().length != size) {
				throw new Exception(nicname + " size is not equal");
			}
		}
		
		if(!"".equals(value) && attributes.containsKey("MIN")) {
			int size = Integer.parseInt(attributes.get("MIN"));
			int v = Integer.parseInt(value);
			if(v < size) {
				throw new Exception(nicname + "'s minimun valaue is " + v);
			}
		}

		if(!"".equals(value) && attributes.containsKey("MAX")) {
			int size = Integer.parseInt(attributes.get("MAX"));
			int v = Integer.parseInt(value);
			if(v > size) {
				throw new Exception(nicname + "'s maximun valaue is " + v);
			}
		}
		
		if(attributes.containsKey("GLUE")) {
			String glue = attributes.get("GLUE");
			String delim = attributes.containsKey("DELIM") ? attributes.get("DELIM") : "";
			String[] arr = glue.split("\\|");
			for(int i=0; i<arr.length; i++) {
				if(!"".equals(get(arr[i].trim()))) value += delim + get(arr[i].trim());
			}
		}

		if(attributes.containsKey("OPTION") && !"".equals(value)) {
			String option = attributes.get("OPTION");
			if("number".equals(option)) {
				value = Hello.replace(value, ",", "");
				data.put(name, value);
			}
			String re = options.get(option);
			if(re == null) re = option;
			Pattern pattern = Pattern.compile(re);
			Matcher match = pattern.matcher(value);
			if(match.find() == false) {
				throw new Exception(nicname + " is not match to " + option);
			}
		}

		if(attributes.containsKey("ALLOW")) {
			String filename = getFileName(name);
			String re = attributes.get("ALLOW");
			if(filename != null && !"".equals(filename) && !"".equals(re)) {
				Pattern pattern = Pattern.compile("(" + re.replace('\'', '|') + ")$");
				Matcher match = pattern.matcher(getFileName(name).toLowerCase());
				if(match.find() == false) {
					throw new Exception(nicname + " is not allowed");
				}
			}
		}

		if(attributes.containsKey("DENY")) {
			String filename = getFileName(name);
			String re = attributes.get("DENY");
			if(filename != null && !"".equals(filename) && !"".equals(re)) {
				Pattern pattern = Pattern.compile("(" + re.replace('\'', '|') + ")$");
				Matcher match = pattern.matcher(getFileName(name).toLowerCase());
				if(match.find() == true) {
					throw new Exception(nicname + " is denied");
				}
			}
		}

		return true;
	}
	
	public String getScript() throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("<script type='text/javascript'>\r\n");
		sb.append("//<![CDATA[\r\n");
		sb.append("function __setElement(el, v, a) { if(v) v = v.replace(/__&LT__/g, '<').replace(/__&GT__/g, '>'); if(typeof(el) != 'object' && typeof(el) != 'function') return; if(v != null) switch(el.type) { case 'text': case 'hidden': case 'password': case 'file': case 'email': el.value = v; break; case 'textarea': el.value = v; break; case 'checkbox': case 'radio': if(el.value == v) el.checked = true; else el.checked = false; break; case 'select-one': for(var i=0; i<el.options.length; i++) if(el.options[i].value == v) el.options[i].selected = true; break; default: for(var i=0; i<el.length; i++) if(el[i].value == v) el[i].checked = true; el = el[0]; break; } if(typeof(a) == 'object') { if(el.type != 'select-one' && el.length > 1) el = el[0]; for(i in a) el.setAttribute(i, a[i]); } }\r\n");
		sb.append("if(_f = document.forms['" + this.name + "']) {\r\n");

		for(String[] element : elements) {
		    String value = this.get(element[0], null);
		    if(value == null && element[1] != null) {
				value = element[1];
			}
		    sb.append("\t__setElement(_f['" + element[0] + "'], ");
			if(null != allowHtml && allowHtml.indexOf("[" + element[0] + "]") == -1) {
				sb.append(value != null ? "'" + Hello.replace(Hello.replace(Hello.replace(Hello.replace(Hello.addSlashes(value), "<", "__&LT__"), ">", "__&GT__"), "&lt;", "__&LT__"), "&gt;", "__&GT__") + "'" : "null");
			} else {
		    	sb.append(value != null ? "'" + Hello.replace(Hello.replace(Hello.addSlashes(value), "<", "__&LT__"), ">", "__&GT__") + "'" : "null");
			}
		    sb.append(", {" + (element[2] != null ? element[2] : "") + "});\r\n");
		}
		
		sb.append("\tif(!_f.onsubmit) _f.onsubmit = function() { return validate(this); };\r\n");
		sb.append("}\r\n");
		sb.append("//]]>\r\n");
		sb.append("</script>");

		return sb.toString();
	}
}