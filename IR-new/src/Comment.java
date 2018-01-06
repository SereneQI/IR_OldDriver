/** 
* @author  QIPENG 
* @date 2017年12月5日 上午10:40:56 
* @version 1.0 
* @description 评论类
*/

/**
 * 评论类
 */
public class Comment {//评论类
	private String cmter;//评论作者
	private String cmt;//评论内容
	private String cmttime;//评论时间
	
	/**
	 * @param cmter
	 * @param cmt
	 * @param cmttime
	 */
	public Comment(String cmter, String cmt, String cmttime) {
		super();
		this.cmter = cmter;
		this.cmt = cmt;
		this.cmttime = cmttime;
	}
	/**
	 * @return the cmter
	 */
	public String getCmter() {
		return cmter;
	}
	/**
	 * @param cmter the cmter to set
	 */
	public void setCmter(String cmter) {
		this.cmter = cmter;
	}
	/**
	 * @return the cmt
	 */
	public String getCmt() {
		return cmt;
	}
	/**
	 * @param cmt the cmt to set
	 */
	public void setCmt(String cmt) {
		this.cmt = cmt;
	}
	/**
	 * @return the cmttime
	 */
	public String getCmttime() {
		return cmttime;
	}
	/**
	 * @param cmttime the cmttime to set
	 */
	public void setCmttime(String cmttime) {
		this.cmttime = cmttime;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return "Comment [cmter=" + cmter + ", cmt=" + cmt + ", cmttime=" + cmttime + "]";
	}
	
	
	
}
