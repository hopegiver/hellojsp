<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

String enc = Base64Coder.encode("This is plain text. 한글입니다.");
m.p(enc);

String dec = Base64Coder.decode(enc);
m.p(dec);

%>