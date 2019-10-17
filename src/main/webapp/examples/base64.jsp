<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

String enc = Hello.encode("This is plain text. 한글입니다.");
m.p(enc);

String dec = Hello.decode(enc);
m.p(dec);

%>