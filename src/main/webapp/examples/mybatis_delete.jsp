<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DB db = new DB();
//db.setDebug(out);

int ret = db.delete("Blog.delete", 10);

m.p(ret);

%>