package hellojsp.util;

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class Page {

	protected String root;
	protected String layout;
	protected String body;
	protected String encoding = Config.getEncoding();
	
	private int type = 0;
	private Writer out;
	private boolean debug = false;
	private final HashMap<String, Object> data = new HashMap<String, Object>();
	
	public Page() {
		this.root = Config.getTplRoot();
	}
	
	public Page(String root) {
		this.root = root;
	}

	public void setWriter(Writer out) {
		this.out = out;
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
			if(null != out && debug) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug) Hello.errorLog(msg, ex);
		} catch(Exception ignored) {}
	}
	
	public void setEncoding(String enc) {
		this.encoding = enc;
	}
	
	public void setType(int type) {
		this.type = type;
	}

	public void setLayout(String layout) {
		if(layout == null) this.layout = null;
		else this.layout = "layout/layout_" + layout.replace(".",  "/") + ".html";
	}

	public void setBody(String body) {
		this.body = body.replace(".",  "/") + ".html";
	}
	
	public void setVar(String name, Object value) {
		data.put(name, value);
	}

	public void setVar(String name, String value) {
		data.put(name, value);
	}

	public void setVar(String name, int value) {
		data.put(name, value);
	}

	public void setVar(String name, double value) {
		data.put(name, value);
	}

	public void setVar(String name, boolean value) {
		data.put(name, value);
	}

	public void setVar(String name, DataSet value) {
		data.put(name, value.getRow());
	}	
	
	public void setLoop(String name, DataSet value) {
		data.put(name, value);
	}
	
	public void print() {
		if(this.layout == null && this.body == null) this.type = 1;
		print(this.out);
	}

	public void print(int type) {
		this.type = type;
		print(this.out);
	}
	
	public void print(Writer out) {
		try {
			if(type == 0) {
				Properties p = new Properties();
				p.setProperty("file.resource.loader.path", this.root);
				
				Velocity.init(p);
				
				VelocityContext context = new VelocityContext();
				for(String key : data.keySet()) context.put(key,  data.get(key));

				String vm;
				if(this.layout != null) {
					vm = this.layout;
					context.put("BODY", this.body);
				} else {
					vm = this.body;
				}
				
				Template template = Velocity.getTemplate(vm, encoding);
				template.merge(context, out);

			} else if(type == 1) {
				data.remove("m");
				Json j = new Json();
				j.setWriter(out);
				j.print(0, "success", data);
			}
		} catch (Exception e) {
			setError("{Page.print} type:" + type, e);
		}
	}
	
	public void print(String path) {
		this.body = path.replace(".",  "/") + ".html";
		print();
	}

	public void display() {
		print();
	}

	public void display(Writer out) {
		print(out);
	}
	
	public String fetch(String path) {
		this.body = path.replace(".",  "/") + ".html";
		return fetch();
	}
	
	public String fetch() {
		StringWriter sw = new StringWriter();
		print(sw);
		return sw.toString();
	}
}