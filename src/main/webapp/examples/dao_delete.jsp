<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DataObject dao = new DataObject("TB_BLOG");
//dao.setDebug(out);

boolean ret = dao.delete("id = " + 10);

m.p(ret);

%>