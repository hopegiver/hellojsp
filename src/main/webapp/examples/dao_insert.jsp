<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DataObject dao = new DataObject("TB_BLOG");
dao.setDebug(out);

dao.item("subject", "Nice to meet you.");
dao.item("content", "Nice to meet you too.");
dao.item("reg_date", Hello.time());
dao.item("status", 1);

boolean ret = dao.insert();
m.p(ret);

%>