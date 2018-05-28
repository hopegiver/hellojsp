<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DB db = new DB();
//db.setDebug(out);

DataMap map = new DataMap();
map.put("subject", "Nice to meet you.");
map.put("content", "Nice to meet you too.");
map.put("id", 1);

int ret = db.update("Blog.update", map);
m.p(ret);

%>