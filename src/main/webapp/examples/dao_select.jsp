<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DataObject dao = new DataObject("TB_BLOG");
//dao.setDebug(out);

DataSet posts = dao.find("");

p.setLayout("blog");
p.setBody("example/blog");
p.setVar("posts", posts);
p.print();

%>