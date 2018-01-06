package main.java.SohuSpider.bean;

public class NewsBean {
	String newsurl; //新闻url
	
	String category; //新闻类别
	
	String sourceFrom; //新闻源
	
	String title; //新闻标题
	
	String content; //新闻内容
	
	String date; //发布时间
	
	String ori; //新闻来源

	String newsid; //新闻id

	String cmtnum; //评论数目

	public String getUrl(){
		return newsurl;
	}
	
	public String getCategory(){
		return category;
	}
	
	public String getSourceFrom(){
		return sourceFrom;
	}
	
	public String getTitle(){ return title; }
	
	public String getContent(){
		return content;
	}
	
	public String getDate(){
		return date;
	}
	
	public String getOri(){
		return "搜狐新闻";
	}

	public String getNewsID() { return newsid; }

	public String getCmtnum() { return cmtnum; }

	
	public void setUrl(String newsurl){
		this.newsurl = newsurl;
	}
	
	public void setCategory(String category){
		this.category = category;
	}
	
	public void setSourceFrom(String sourceFrom){
		this.sourceFrom = sourceFrom;
	}
	
	public void setTitle(String title){
		this.title = title;
	}
	
	public void setContent(String content){
		this.content = content;
	}
	
	public void setDate(String date){
		this.date = date;
	}

	public void setNewsID(String newsid) { this.newsid = newsid; }

	public void setCmts(String cmts) {this.cmtnum = cmtnum; }

	
	@Override
	public String toString(){

		return "{" +
				"'newsid:'" + newsid + "," +
				"'newsurl:'" + newsurl + "," +
				"'author:'" + sourceFrom + "," +
				"'title:'" + title + "," +
				"'type:'" + category + "," +
				"'ntime:'" + date + "," +
				"'content:'" + content + "," +
				"'cmtnum:'" + cmtnum + "," +
				"'ori:'" + "搜狐新闻" + "," +
				"'cmtlist:'" + "" +
				"}";
	}
	
	
}
