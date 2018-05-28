<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

msg.setLocale(Locale.ENGLISH);
//msg.setLocale(Locale.KOREAN);
//msg.reload();

m.p(msg.get("name"));
m.p(msg.get("course"));

%>