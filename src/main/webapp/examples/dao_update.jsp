<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DataObject dao = new DataObject("TB_BLOG");
//dao.setDebug(out);

dao.item("subject", "Nice to meet you. " + Hello.getUnixTime());
dao.item("content", "Nice to meet you too.");

boolean ret = dao.update("id = " + 1);
m.p(ret);

%>