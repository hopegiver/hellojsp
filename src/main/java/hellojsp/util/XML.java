package hellojsp.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.Writer;

import javax.servlet.jsp.JspWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XML {

	private String xml = null;
	private Document doc = null;
	private String encoding = Config.getEncoding();
	
	private Writer out = null;
	private boolean debug = false;

	public XML() {
		
	}

	public XML(String str) {
		str = str.trim();
		if(str.startsWith("http://") || str.startsWith("https://")) {
			setUrl(str);
		} else if(str.startsWith("<?xml")) {
			setXML(str);
		} else if(new File(str).exists()) {
			setFile(str);
		}
	}

	public void setDebug() {
		this.debug = true;
	}
	
	public void setDebug(Writer out) {
		this.out = out;
		this.debug = true;
	}

	public void setDebug(JspWriter out) {
		this.debug = true;
		this.out = out;
	}

	public void setError(String msg, Exception ex) {
		try {
			if(null != out && debug) out.write("<hr>" + msg + "###" + ex + "<hr>");
			if(ex != null || debug) Hello.errorLog(msg, ex);
		} catch(Exception ignored) {}
	}
	
	public void setUrl(String url) {
		try { 
			Http http = new Http(url);
			if(this.debug) http.setDebug(this.out);
			http.setEncoding(this.encoding);
			setXML(http.send());
		} catch(Exception e) {
			setError("{XML.setUrl} url:" + url, e);
		}
	}
	
	public void setXML(String str) {
		this.xml = str;
	}
	
	public void setFile(String path) {
		try {
			setXML(Hello.readFile(path, this.encoding));
		} catch(Exception e) {
			setError("{XML.setFile} path:" + path, e);
		}
	}

	public void setEncoding(String enc) {
		this.encoding = enc;
	}

	private void parse() throws Exception {
		InputStream is = new ByteArrayInputStream(this.xml.getBytes(this.encoding));
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		doc = dBuilder.parse(is);
		doc.getDocumentElement().normalize();
	}

	public String getAttribute(String xstr, String attr) {
		try {
			if(doc == null) parse();
			XPath xpath = XPathFactory.newInstance().newXPath();
			Node node = (Node)xpath.evaluate(xstr, doc, XPathConstants.NODE);
			return node.getAttributes().getNamedItem(attr).getTextContent();
		} catch(Exception e) {
			setError("{XML.getAttribute} xpath:" + xstr + ", attribute:" + attr, e);
		}
		return "";
	}

	public String getString(String xstr) {
		try { 
			if(doc == null) parse();			
			XPath xpath = XPathFactory.newInstance().newXPath();
			return (String)xpath.evaluate(xstr, doc, XPathConstants.STRING);
		} catch(Exception e) {
			setError("{XML.getString} xpath:" + xstr, e);
		}
		return "";
	}

	public int getInt(String xstr) {
		try {
			if(doc == null) parse();
			XPath xpath = XPathFactory.newInstance().newXPath();
			return ((Number)xpath.evaluate(xstr, doc, XPathConstants.NUMBER)).intValue();
		} catch(Exception e) {
			setError("{XML.getInt} xpath:" + xstr, e);
		}
		return 0;
	}
	
	public double getDouble(String xstr) {
		try { 
			if(doc == null) parse();
			XPath xpath = XPathFactory.newInstance().newXPath();
			return ((Number)xpath.evaluate(xstr, doc, XPathConstants.NUMBER)).doubleValue();
		} catch(Exception e) {
			setError("{XML.getDouble} xpath:" + xstr, e);
		}
		return 0.0d;
	}
	
	public long getLong(String xstr) {
		try {
			if(doc == null) parse();
			XPath xpath = XPathFactory.newInstance().newXPath();
			return ((Number)xpath.evaluate(xstr, doc, XPathConstants.NUMBER)).longValue();
		} catch(Exception e) {
			setError("{XML.getLong} xpath:" + xstr, e);
		}
		return 0L;
	}
	
	public boolean getBoolean(String xstr) {
		return Boolean.parseBoolean(getString(xstr));
	}
	
	public DataSet getDataSet(String xstr) {
		DataSet rs = new DataSet();
		try {
			if(doc == null) parse();
			XPath xpath = XPathFactory.newInstance().newXPath();
			NodeList list = (NodeList)xpath.evaluate(xstr, doc, XPathConstants.NODESET);
			for(int i = 0; i < list.getLength(); i++) {
				rs.addRow();
				NodeList nodes = list.item(i).getChildNodes();
				if(nodes == null) continue;
				for(int j = 0; j < nodes.getLength(); j++) {
					Node node = nodes.item(j);
					if(!"#text".equals(node.getNodeName())) {
						rs.put(node.getNodeName(), node.getTextContent());
					}
				}
			}
			rs.first();
		} catch(Exception e) {
			setError("{XML.getDataSet} xpath:" + xstr, e);
		}
		return rs;
	}
	
}