<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

f.addElement("file", null, "required:'Y'");

if(m.isPost() && f.validate()) {
	
	File file = f.saveFile("file");
	if(file != null) {
		m.p(f.get("name"));
		m.p(file.getAbsolutePath());
		m.p(file.getName());
		m.p(f.getFileName("file"));
		
		//file.delete();
		return;
	}
}

p.setBody("example/upload");
p.setVar("form_script", f.getScript());
p.print();

%>