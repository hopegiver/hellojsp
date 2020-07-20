package hellojsp.util;

import javax.servlet.jsp.JspWriter;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Writer;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Set;

public class Message {

	private Locale locale;
	
	private Writer out = null;
	private boolean debug = false;
	public String errMsg = null;
	
	public Message() {
		this.locale = Locale.getDefault();
	}

	public Message(Locale locale) {
		this.locale = locale;
	}
	
	public void setLocale(Locale locale) {
		this.locale = locale;
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
	
	public void setError(String msg, Exception ex) {
		try {
			if(null != out && debug) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug) Hello.errorLog(msg, ex);
		} catch(Exception ignored) {}
	}
	
	protected ResourceBundle getBundle() {
		ResourceBundle rb = null;
		try {
			rb = ResourceBundle.getBundle("messages.message", this.locale, new XMLResourceBundleControl());
		} catch(Exception e) {
			setError("{Message.getBundle} locale:" + this.locale, e);
		}
		return rb;
	}

	public String get(String key) {
		return get(key, "");
	}
	
	public String get(String key, String val) {
		try {
			ResourceBundle rb = getBundle();
			val = rb.getString(key);
		} catch(Exception e) {
			setError("{Message.get} key:" + key, e);
		}
		return val;
	}
	
	public void reload() {
		ResourceBundle.clearCache();
	}

	private static class XMLResourceBundleControl extends ResourceBundle.Control {
		
		public List<String> getFormats(String baseName) {
			if (baseName == null)
				throw new NullPointerException();
			return Collections.singletonList("xml");
		}
		
		public ResourceBundle newBundle(String baseName, Locale locale, String format, ClassLoader loader, boolean reload) {
			if (baseName == null || locale == null || format == null || loader == null)
		             throw new NullPointerException();
			ResourceBundle bundle = null;
			try {
				if (format.equals("xml")) {
					String bundleName = toBundleName(baseName, locale);
					String resourceName = toResourceName(bundleName, format);
					InputStream stream = null;
					if (reload) {
						URL url = loader.getResource(resourceName);
						if (url != null) {
							URLConnection connection = url.openConnection();
							if (connection != null) {
								connection.setUseCaches(false);
								stream = connection.getInputStream();
							}
						}
					} else {
						stream = loader.getResourceAsStream(resourceName);
					}
					if (stream != null) {
						BufferedInputStream bis = new BufferedInputStream(stream);
						bundle = new XMLResourceBundle(bis);
						bis.close();
					}
				}
			} catch(Exception e) {
				Hello.errorLog("{Message.newBundle} name:" + baseName, e);
			}
	        return bundle;
		}
		
	}
	
	private static class XMLResourceBundle extends ResourceBundle {
  
	    private final Properties props;

	    XMLResourceBundle(InputStream stream) throws IOException {
	      props = new Properties();
	      props.loadFromXML(stream);
	    }

	    protected Object handleGetObject(String key) {
	      return props.getProperty(key);
	    }

	    public Enumeration<String> getKeys() {
	      Set<String> handleKeys = props.stringPropertyNames();
	      return Collections.enumeration(handleKeys);
	    }
	    
	}
	
}


