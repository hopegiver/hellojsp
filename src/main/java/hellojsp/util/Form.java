package hellojsp.util;

import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;

public class Form {

	public String name = "form1";
	public ArrayList<String[]> elements = new ArrayList<String[]>();
	public HashMap<String, Object> data = new HashMap<String, Object>();
	public HashMap<String, FileItem> uploadedFiles = new HashMap<String, FileItem>();
	public String errMsg = null;
	public String uploadDir = Config.getDataDir() + "/files";
	public String tmpDir = null;
	public int maxFileSize = Config.getInt("maxFileSize", 1024) * 1024 * 1024;
	public String encoding = Config.getEncoding();

	private static final HashMap<String, String> options = new HashMap<String, String>();
	private HttpServletRequest request = null;
	private Writer out = null;
	private boolean debug = false;
	private String allowScript = "";
	private String allowHtml = "";
	private String[] denyExt = {"jsp", "php", "asp", "aspx", "html", "htm", "exe", "sh"}; 

	static {
		options.put("email", "^[a-z0-9A-Z\\_\\.\\-]+@([a-z0-9A-Z\\.\\-]+)\\.([a-zA-Z]+)$");
		options.put("url", "^(http(s)?:\\/\\/)(.+)");
		options.put("number", "^-?[,0-9]+$");
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
			if(null != out && debug) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug) Hello.errorLog(msg, ex);
		} catch(Exception ignored) {}
	}
	
	public void setName(String nm) {
		this.name = nm;
	}
	
	public void setUploadDir(String dir) {
		this.uploadDir = dir;
	}

	public void setTmpDir(String dir) {
		this.tmpDir = dir;
	}
	
	public void setDenyExt(String[] arr) {
		this.denyExt = arr;
	}
	
	public void setRequest(HttpServletRequest req) {
		this.request = req;

		if("POST".equals(req.getMethod()) && ServletFileUpload.isMultipartContent(req)) {
			DiskFileItemFactory factory = new DiskFileItemFactory();

			// Configure a repository (to ensure a secure temp location is used)
			File repository;
			if(tmpDir == null) {
				repository = (File) req.getSession().getServletContext().getAttribute("javax.servlet.context.tempdir");
			} else {
				repository = new File(tmpDir);
			}
			factory.setRepository(repository);

			// Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);
			upload.setFileSizeMax(maxFileSize);
			upload.setHeaderEncoding(encoding);

			// Parse the request
			try { 
				List<FileItem> items = upload.parseRequest(request);
				for(FileItem item : items) {
				    if (item.isFormField()) {
				    	data.put(item.getFieldName(), item.getString(encoding));
				    } else if(item.getSize() > 0){
				    	uploadedFiles.put(item.getFieldName(), item);
				    }
				}
			}
			catch(Exception e) {
				Hello.errorLog("{Form.setRequest} Multipart Parsing Error", e);
			}
		} else {
			Enumeration e = req.getParameterNames();
			while(e.hasMoreElements()) {
				String key = e.nextElement().toString();
				data.put(key, request.getParameter(key));
			}
		}
	}
	
	public void addElement(String name, String value, String attributes) {
		String[] element = new String[3];
		element[0] = name;
		element[1] = value;
		element[2] = attributes;
		elements.add(element);
		if(null != attributes && attributes.contains("allowscript")) {
			allowScript += "[" + name + "]";
		}
		if(null != attributes && attributes.contains("allowhtml")) {
			allowHtml += "[" + name + "]";
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
		if("".equals(allowHtml) || !allowHtml.contains("[" + name + "]")) {
			value = Hello.replace( Hello.replace(value , "<", "&lt;") , ">", "&gt;");
		}
		else if("".equals(allowScript) || !allowScript.contains("[" + name + "]")) {
			String tail = value.endsWith(">") ? ">" : "";
			String[] x1 = value.split(">");
			StringBuilder res = new StringBuilder();
			for(int i=0; i<x1.length; i++) {
				String[] x2 = x1[i].split("<");
				for(int j=0; j<x2.length; j++) {
					if(j == 0) {
						res.append(x2[0]);
					} else {
						res.append("<");
						if(j == x2.length - 1) {
							res.append(x2[j].replaceAll("(?i)(x-)?(vbscript|javascript|script|expression|eval|FSCommand|onAbort|onActivate|onAfterPrint|onAfterUpdate|onBeforeActivate|onBeforeCopy|onBeforeCut|onBeforeDeactivate|onBeforeEditFocus|onBeforePaste|onBeforePrint|onBeforeUnload|onBegin|onBlur|onBounce|onCellChange|onChange|onClick|onContextMenu|onControlSelect|onCopy|onCut|onDataAvailable|onDataSetChanged|onDataSetComplete|onDblClick|onDeactivate|onDrag|onDragEnd|onDragLeave|onDragEnter|onDragOver|onDragDrop|onDrop|onEnd|onError|onErrorUpdate|onFilterChange|onFinish|onFocus|onFocusIn|onFocusOut|onHelp|onKeyDown|onKeyPress|onKeyUp|onLayoutComplete|onLoad|onLoseCapture|onMediaComplete|onMediaError|onMouseDown|onMouseEnter|onMouseLeave|onMouseMove|onMouseOut|onMouseOver|onMouseUp|onMouseWheel|onMove|onMoveEnd|onMoveStart|onOutOfSync|onPaste|onPause|onProgress|onPropertyChange|onReadyStateChange|onRepeat|onReset|onResize|onResizeEnd|onResizeStart|onResume|onReverse|onRowsEnter|onRowExit|onRowDelete|onRowInserted|onScroll|onSeek|onSelect|onSelectionChange|onSelectStart|onStart|onStop|onSyncRestored|onSubmit|onTimeError|onTrackChange|onUnload|onURLFlip|seekSegmentTime)", "x-$2"));
						} else {
							res.append(x2[j]);
						}
					}
				}
				if(i + 1 < x1.length) res.append(">");
			}
			res.append(tail);
			value = res.toString();
		}
		return value;
	}

	public HashMap<String, Object> getMap(String name) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		try {
			for(String key : data.keySet()) {
				if(key.matches("^(" + name + ")(.+)$")) {
					map.put(key, get(key));
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
		if(str.matches("^-?[,0-9]+$")) return Integer.parseInt(Hello.replace(str, ",", ""));
		else return i;
	}

	public long getLong(String name) {
		return getLong(name, 0);
	}

	public long getLong(String name, long i) {
		String str = get(name);
		if(str.matches("^-?[,0-9]+$")) return Long.parseLong(Hello.replace(str, ",", ""));
		else return i;
	}

	public double getDouble(String name) {
		return getDouble(name, 0.0);
	}

	public double getDouble(String name, double i) {
		String str = get(name);
		if(str.matches("^-?[.,0-9]+$")) return Double.parseDouble(Hello.replace(str, ",", ""));
		else return i;
	}
	
	public String getErrMsg() {
		return this.errMsg;
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
					path = uploadDir + "/" + Hello.sha1(orgname + System.currentTimeMillis()) + "." + ext;
					target = new File(path);
				} while(target.exists());
			} else {
				target = new File(path);
			}
			try {
				if(target.isDirectory()) {
					target = new File(path + "/" + f.getName());
				}
				if(!target.getParentFile().isDirectory() && !target.getParentFile().mkdirs()) {
					Hello.errorLog("{Form.saveFile}", new Exception(target.getParentFile().getAbsolutePath() + " is not writable."));
				}
				f.write(target);
				f.delete();
			} catch(Exception ex) {
				Hello.errorLog("{Form.saveFile} path:" + path, ex);
				f.delete();
			}
			if(target.exists()) return target;
		}
		return null;
	}

	public String getFileName(String name) {
		FileItem f = uploadedFiles.get(name);
		return f == null ? null : FilenameUtils.getName(f.getName());
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
			String[] arr = str.split(",");
			for (String s : arr) {
				String[] arr2;
				arr2 = s.split("[=:]");
				if (arr2.length == 2) {
					map.put(arr2[0].trim().toUpperCase(), arr2[1].replace('\'', '\0').trim());
				}
			}
		}
		return map;
	}
	
	private void isValid(String[] element) throws Exception {
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
			StringBuilder sb = new StringBuilder();
			for (String s : arr) {
				if (!"".equals(get(s.trim()))) sb.append(delim).append(get(s.trim()));
			}
			value = sb.toString();
		}

		if(attributes.containsKey("TYPE") && !"".equals(value)) {
			String type = attributes.get("TYPE");
			String re = options.get(type);
			if(re != null) {
				Pattern pattern = Pattern.compile(re);
				Matcher match = pattern.matcher(value);
				if(!match.find()) {
					throw new Exception(nicname + " is not valid");
				}
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
			if(!match.find()) {
				throw new Exception(nicname + " is not valid");
			}
		}

		if(attributes.containsKey("PATTERN") && !"".equals(value)) {
			String re = attributes.get("PATTERN");
			Pattern pattern = Pattern.compile(re);
			Matcher match = pattern.matcher(value);
			if(!match.find()) {
				throw new Exception(nicname + " is not match to " + re);
			}
		}
		
		if(attributes.containsKey("ALLOW")) {
			String filename = getFileName(name);
			String re = attributes.get("ALLOW");
			if(filename != null && !"".equals(filename) && !"".equals(re)) {
				Pattern pattern = Pattern.compile("(" + re.replace('\'', '|') + ")$");
				Matcher match = pattern.matcher(getFileName(name).toLowerCase());
				if(!match.find()) {
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
				if(match.find()) {
					throw new Exception(nicname + " is denied");
				}
			}
		}

	}

	public String getScript() {
		return getScript(null);
	}
	
	public String getScript(String nm) {
		if(nm != null) this.name = nm;
		StringBuilder sb = new StringBuilder();
		sb.append("<script type='text/javascript'>\r\n");
		sb.append("//<![CDATA[\r\n");
		sb.append("function __setElement(el, v, a) { if(typeof(el) != 'object' && typeof(el) != 'function') return; if(v != null) switch(el.type) { case 'checkbox': case 'radio': if(el.value == v) el.checked = true; else el.checked = false; break; case 'select-one': for(var i=0; i<el.options.length; i++) if(el.options[i].value == v) el.options[i].selected = true; break; case 'select-multiple': for(var i=0; i<el.length; i++) if(el[i].value == v) el[i].checked = true; el = el[0]; break; default: el.value = v; break; } if(typeof(a) == 'object') { if(el.type != 'select-one' && el.length > 1) el = el[0]; for(i in a) el.setAttribute(i, a[i]); } }\r\n");
		sb.append("if(_f = document.forms['").append(this.name).append("']) {\r\n");

		for(String[] element : elements) {
		    String value = this.get(element[0], null);
		    if(value == null && element[1] != null) {
				value = element[1];
			}
		    sb.append("\t__setElement(_f['").append(element[0]).append("'], ");
	    	sb.append(value != null ? "'" + Hello.addSlashes(value) + "'" : "null");
		    sb.append(", {").append(element[2] != null ? element[2] : "").append("});\r\n");
		}
		
		sb.append("\tif(!_f.onsubmit) _f.onsubmit = function() { return typeof obj === 'function' ? validate(this) : true; };\r\n");
		sb.append("}\r\n");
		sb.append("//]]>\r\n");
		sb.append("</script>");

		return sb.toString();
	}
}