<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

String secretId = "abcdefghijklmn12";
AES aes = new AES(secretId);
//aes.setDebug(out);

String enc = aes.encrypt("This is plain text. 한글입니다.");
m.p(enc);

String dec = aes.decrypt(enc);
m.p(dec);

%>