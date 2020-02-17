package hellojsp.util;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;

public class Page {

	protected String root;
	protected String layout;
	protected String body;
	protected String encoding = Config.getEncoding();
	
	private Writer out;
	private boolean debug = false;
	private VelocityContext context;
	
	public Page() {
		this.root = Config.getTplRoot();
		init();
	}
	
	public Page(String root) {
		this.root = root;
		init();
		setWriter(out);
	}
	
	public void init() {
		Properties p = new Properties();
		p.setProperty("file.resource.loader.path", this.root);
		Velocity.init(p);
		context = new VelocityContext();
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
			if(null != out && debug == true) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug == true) Hello.errorLog(msg, ex);
		} catch(Exception e) {}
	}
	
	public void setEncoding(String enc) {
		this.encoding = enc;
	}

	public void setLayout(String layout) {
		if(layout == null) this.layout = null;
		else {
			this.layout = "layout/layout_" + layout.replace(".",  "/") + ".html";
		}
	}

	public void setBody(String body) {
		this.body = body.replace(".",  "/") + ".html";
	}
	
	public void setVar(String name, Object value) {
		context.put(name, value);
	}

	public void setLoop(String name, Object value) {
		context.put(name, value);
	}
	
	public void setWriter(Writer out) {
		this.out = out;
	}

	public void print() {
		print(this.out);
	}
	
	public void print(Writer out) {
		String vm = null;
		try {
			if(this.layout != null) {
				vm = this.layout;
				context.put("BODY", this.body);
			} else {
				vm = this.body;
			}
			Template template = Velocity.getTemplate(vm, encoding);
			template.merge(context, out);
		} catch(Exception e) {
			setError("{Page.print} vm:" + vm, e);
		}
	}
	
	public void print(String path) {
		this.body = path;
		print();
	}

	public void display() throws Exception {
		print();
	}

	public void display(Writer out) throws Exception {
		print(out);
	}
	
	public String fetch(String path) throws Exception {
		this.body = path;
		return fetch();
	}
	
	public String fetch() throws Exception {
		StringWriter sw = new StringWriter();
		print(sw);
		return sw.toString();
	}
}