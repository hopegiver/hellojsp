<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DataSet posts = new DataSet();
posts.addRow();
posts.put("subject", "hi");
posts.put("reg_date", "20180424");
posts.addRow();
posts.put("subject", "bye");
posts.put("reg_date", "20180424");



p.setLayout("blog");
p.setBody("example/blog");
p.setVar("posts", posts);
p.setVar("name", "Daniel");
p.setVar("age", 25);
p.print();

%>