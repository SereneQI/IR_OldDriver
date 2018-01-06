import java.util.ArrayList;
import java.util.List;

/** 
* @author  QIPENG 
* @date 2017年11月29日 上午11:26:23 
* @version 1.0 
* @description 新闻类
*/
public class News {//新闻类
	private String newsid;//新闻ID
	private String title;//新闻标题
	private String content;//新闻内容
	private String author;//新闻作者
	private String type;//新闻类型
	private Integer cmtnum;//评论数目
	private Integer ntime;//新闻发布时间
	private String newsurl;//新闻url
	private String ori;//新闻来源
	private ArrayList<Comment> cmtlist;//新闻评论列表

	/**
	 * @param newsid
	 * @param title
	 * @param content
	 * @param author
	 * @param type
	 * @param cmtnum
	 * @param ntime
	 * @param newsurl
	 * @param ori
	 * @param cmtlist
	 */
	public News(String newsid, String title, String content, String author, String type, Integer cmtnum, Integer ntime,
			String newsurl, String ori, ArrayList<Comment> cmtlist) {
		super();
		this.newsid = newsid;
		this.title = title;
		this.content = content;
		this.author = author;
		this.type = type;
		this.cmtnum = cmtnum;
		this.ntime = ntime;
		this.newsurl = newsurl;
		this.ori = ori;
		this.cmtlist = cmtlist;
	}
	
	/**
	 * 
	 */
	public News() {
		super();
	}

	/**
	 * @return the newsid
	 */
	public String getNewsid() {
		return newsid;
	}
	/**
	 * @param newsid the newsid to set
	 */
	public void setNewsid(String newsid) {
		this.newsid = newsid;
	}
	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}
	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}
	/**
	 * @return the content
	 */
	public String getContent() {
		return content;
	}
	/**
	 * @param content the content to set
	 */
	public void setContent(String content) {
		this.content = content;
	}
	/**
	 * @return the author
	 */
	public String getAuthor() {
		return author;
	}
	/**
	 * @param author the author to set
	 */
	public void setAuthor(String author) {
		this.author = author;
	}
	/**
	 * @return the type
	 */
	public String getType() {
		return type;
	}
	/**
	 * @param type the type to set
	 */
	public void setType(String type) {
		this.type = type;
	}
	/**
	 * @return the cmtnum
	 */
	public Integer getCmtnum() {
		return cmtnum;
	}
	/**
	 * @param cmtnum the cmtnum to set
	 */
	public void setCmtnum(Integer cmtnum) {
		this.cmtnum = cmtnum;
	}
	/**
	 * @return the newsurl
	 */
	public String getNewsurl() {
		return newsurl;
	}
	/**
	 * @param newsurl the newsurl to set
	 */
	public void setNewsurl(String newsurl) {
		this.newsurl = newsurl;
	}
	/**
	 * @return the ntime
	 */
	public int getNtime() {
		return ntime;
	}

	/**
	 * @param ntime the ntime to set
	 */
	public void setNtime(Integer ntime) {
		this.ntime = ntime;
	}

	/**
	 * @return the ori
	 */
	public String getOri() {
		return ori;
	}

	/**
	 * @param ori the ori to set
	 */
	public void setOri(String ori) {
		this.ori = ori;
	}

	/**
	 * @return the cmtlist
	 */
	public String getCmtlist() {
		String cmts_content = null;
		for (Comment cmt:cmtlist) {
			cmts_content += cmt.toString();
		}
		return cmts_content;
	}

	/**
	 * @param cmtlist the cmtlist to set
	 */
	public void setCmtlist(ArrayList<Comment> cmtlist) {
		this.cmtlist = cmtlist;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		String news_content = "News [\nnewsid=" + newsid +"\n"+ "title=" + title+"\n" + "content=" + content+"\n" + "author=" + author+"\n" + "type="
				+ type +"\n"+ "cmtnum=" + cmtnum +"\n"+ "ntime=" + ntime +"\n"+ "newsurl=" + newsurl+"\n" + "ori=" + ori+"\n"+ "]";
		for (Comment cmt:cmtlist) {
			news_content += cmt.toString();
		}
		return news_content; 
	}
	
	
	

}
