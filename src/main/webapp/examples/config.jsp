<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

Config.load();

m.p(Config.getDocRoot());

m.p(Config.getTplRoot());

m.p(Config.getDataDir());

m.p(Config.get("mailFrom"));

%>