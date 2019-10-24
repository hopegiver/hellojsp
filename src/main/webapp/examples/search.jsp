<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%



ListManager lm = new ListManager();
lm.setRequest(request);
lm.setTable("TB_BLOG");
lm.setFields("*");
lm.addSearch("subject,content", f.get("keyword"), "LIKE");
lm.setOrderBy("id DESC");

DataSet posts = lm.getDataSet();

p.setLayout("blog");
p.setBody("example/search");
p.setVar("posts", posts);
p.print();

%>