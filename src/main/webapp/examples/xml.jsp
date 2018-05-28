<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

XML xml = new XML(Config.getDataDir() + "/sample.xml");
//xml.setDebug(out);

m.p(xml.getString("//config/env/logDir"));

m.p(xml.getDataSet("//config/users/user"));

%>