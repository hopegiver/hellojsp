<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

if(m.isPost()) {
	String path = Config.getDataDir() + "/sample.pdf";
	m.download(path, "sample.pdf");
	return;
}

p.setBody("example/download");
p.print();

%>