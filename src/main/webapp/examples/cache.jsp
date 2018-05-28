<%@ page contentType="text/html; charset=utf-8" %><%@ include file="/init.jsp" %><%

Cache cache = new Cache();
boolean cacheFlag = false;

DataSet posts = cache.getDataSet("posts_cache");
if(posts == null) {
	DB db = new DB();
	posts = db.select("Blog.selectAll", null);
	cache.save("posts_cache", posts);
} else {
	cacheFlag = true;
}

m.p(cacheFlag);
m.p(posts);

%>