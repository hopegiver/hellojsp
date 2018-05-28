<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %>
<link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css">
<script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"></script>
<%

Pager pg = new Pager(request);
pg.setTotalNum(300);

m.p(pg.getPager());

%>