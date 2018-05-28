<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DB db = new DB();
//db.setDebug(out);

DataMap map = new DataMap();
map.put("subject", "Nice to meet you.");
map.put("content", "Nice to meet you too.");
map.put("reg_date", Hello.time());
map.put("status", 1);

db.begin();
db.insert("Blog.insert", map);
map.put("id", 1);
db.update("Blog.update", map);
db.commit();

m.p(db.errMsg);

%>