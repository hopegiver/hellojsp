package hellojsp.util;

import java.util.StringTokenizer;
import javax.servlet.http.HttpServletRequest;

public class Pager {

	private int pageNum = 1;
	private int totalNum = 0;
	private int listNum = 20;
	private int naviNum = 10;

	private HttpServletRequest _request;
	private String pageVar = "page";
	private String link = "";

	public int linkType = 0;

	public Pager() {

	}

	public Pager(HttpServletRequest req) {
		_request = req;
	}

	public void setPageVar(String str) {
		pageVar = str;
	}

	public void setPageNum(int num) {
		pageNum = num;
	}

	public void setTotalNum(int num) {
		totalNum = num;
	}

	public void setListNum(int num) {
		listNum = num;
	}

	public void setNaviNum(int num) {
		naviNum = num;
	}

	public void setLink(String link) {
		this.link = link;
	}	

	public int getPageNum() {
		return pageNum;
	}

	public int getLeftPage() {
		int firstPage = (int)(( java.lang.Math.ceil( (double)pageNum / (double)naviNum ) - 1 ) * (double)naviNum + 1);
		if(firstPage > 1) return firstPage - 1;
		else return 0;
	}

	public int getRightPage() {
		int totalPage = getTotalPage();
		int firstPage = getFirstPage();
		int lastPage = firstPage + naviNum - 1;
		if(lastPage < totalPage) return lastPage + 1;
		else return 0;
	}

	public int getTotalPage() {
		return (int)(java.lang.Math.ceil((double)totalNum / (double)listNum));
	}

	public int getFirstPage() {
		return (int)(( java.lang.Math.ceil( (double)pageNum / (double)naviNum ) - 1 ) * (double)naviNum + 1);
	}

	public String getPager() {

		parseQuery();

		if(totalNum == 0) return "";
		int totalPage = getTotalPage();
		int firstPage = getFirstPage();
		int lastPage = firstPage + naviNum - 1;
		if(totalPage < lastPage) {
			lastPage = totalPage;
		}

		StringBuilder sb = new StringBuilder();
		
		sb.append("<ul class='pagination'>");
		if(totalPage > naviNum) {
			sb.append("<li class='page-item'><a class='page-link' href='").append(pageNum > 1 ? getPageLink(firstPage - 1) : "#").append("' title='Previous block'>&laquo;</a></li>");
		}
		sb.append("<li class='page-item'><a class='page-link' href='").append(pageNum > 1 ? getPageLink(pageNum - 1) : "#").append("' title='Previous'>&lsaquo;</a></li>");

		for(int i = firstPage; i <= lastPage; i++) {
			if(pageNum == i) {
				sb.append("<li class='page-item active'><a class='page-link' href='#' title='").append(i).append("'>").append(i).append("</a></li>");
			} else {
				sb.append("<li class='page-item'><a class='page-link' href='").append(getPageLink(i)).append("' title='").append(i).append("'>").append(i).append("</a></li>");
			}
		}

		sb.append("<li class='page-item'><a class='page-link' href='").append(pageNum < totalPage ? getPageLink(pageNum + 1) : "#").append("' title='Next'>&rsaquo;</a></li>");
		if(totalPage > naviNum) {
			sb.append("<li class='page-item'><a class='page-link' href='").append(pageNum < totalPage ? getPageLink((lastPage < totalPage) ? lastPage + 1 : lastPage) : "#").append("' title='Next block'>&raquo;</a></li>");
		}
		sb.append("</ul>");

		return sb.toString();
	}
	
	public DataSet getPageData() {

		parseQuery();

		if(totalNum == 0) return new DataSet();
		int totalPage = (int)(java.lang.Math.ceil((double)totalNum / (double)listNum));
		int firstPage = (int)(( java.lang.Math.ceil( (double)pageNum / (double)naviNum ) - 1 ) * (double)naviNum + 1);
		int lastPage = firstPage + naviNum - 1;
		if(totalPage < lastPage) { lastPage = totalPage; }

		DataSet info = new DataSet();
		info.addRow();
		info.put("total_page", totalPage);
		info.put("current_page", pageNum);
		info.put("first_page", firstPage);
		info.put("last_page", lastPage);

		info.put("first_link", pageNum > 1 ? getPageLink(1) : "");
		info.put("prev_link", firstPage > 1 ? getPageLink(firstPage - 1) : "");
		info.put("p_link", pageNum > 1 ? getPageLink(pageNum - 1) : "");
		info.put("n_link", pageNum < totalPage ? getPageLink(pageNum + 1) : "");
		info.put("next_link", lastPage < totalPage ? getPageLink(lastPage + 1) : "");
		info.put("last_link", pageNum < totalPage ? getPageLink(totalPage) : "");

		info.put("first_title", "First page");
		info.put("prev_title", "Previous " + naviNum + " Pages");
		info.put("p_title", "Previous Page");
		info.put("n_title", "Next Page");
		info.put("next_title", "Next " + naviNum + " Pages");
		info.put("last_title", "Last page");

		DataSet pages = new DataSet();
		for(int i = firstPage; i <= lastPage; i++) {
			pages.addRow();
			pages.put("page_link", pageNum != i ? getPageLink(i) : "");
			pages.put("pageno", i);
		}
		pages.first();
		info.put(".pages", pages);

		return info;
	}

	private void parseQuery() {
		if(_request == null) return;
		
		link = _request.getRequestURI() + "?";
		String query = _request.getQueryString();
		if(query != null) {
			StringTokenizer token = new StringTokenizer(query, "&");
			String subtoken;
			String key;
			String value;
			StringBuilder sb = new StringBuilder();
			while(token.hasMoreTokens()) {
				int itmp;
				subtoken = token.nextToken();
				if((itmp = subtoken.indexOf("=")) != -1) {
					key = subtoken.substring(0,itmp);
					value = subtoken.substring(itmp+1);
					if(!key.equals(pageVar)) {
						sb.append(key).append("=").append(value).append("&");
					} else {
						this.pageNum = Integer.parseInt(value);
					}
				}
			}
			query = sb.toString();
		}
		if(!"".equals(query) && query != null) link = link + query;
	}

	private String getPageLink(int num) {
		if(num < 1) num = 1;
		if(linkType == 1) return "javascript:MovePage(" + num + ")";
		else return link + pageVar + "=" + num;
	}
}