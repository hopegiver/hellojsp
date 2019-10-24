<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

DataMap map = new DataMap();
map.put("subject", "Nice to meet you.");
map.put("content", "Nice to meet you too.");
map.put("reg_date", Hello.time());
map.put("status", 1);

String json = Json.encode(map);
m.p(json);

HashMap<String, Object> map2 = Json.decode(json);
m.p(map2);

String url = m.getWebUrl() + "/hellojsp/data/sample.json";

Json j = new Json(url);
//j.setDebug(out);

String value = j.getString("menu.popup.menuitem[0].value");
m.p(value);

DataSet items = j.getDataSet("menu.popup.menuitem");
m.p(items);

%>