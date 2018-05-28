<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

//sample/request.jsp?id=a&no=1&ids=a&ids=b&etc_a=a&etc_b=b

String id = m.reqStr("id");
m.p(id);

int no = m.reqInt("no");
m.p(no);

String[] ids = m.reqArr("ids");
m.p(ids);

HashMap<String, Object> map = m.reqMap("etc_");
m.p(map);

%>