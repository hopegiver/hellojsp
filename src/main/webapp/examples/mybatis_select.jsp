<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DB db = new DB();
//db.setDebug(out);

DataSet posts = db.select("Blog.selectAll", null);

p.setLayout("blog");
p.setBody("example/blog");
p.setVar("posts", posts);
p.print();

%>