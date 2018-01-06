package main.java.SohuSpider.service;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;

import org.json.JSONException;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import main.java.SohuSpider.bean.NewsBean;
import main.java.SohuSpider.filter.BloomFilter;
import main.java.SohuSpider.util.DBStatement;
import static main.java.SohuSpider.util.XmlUtils.getAllChannels;
import static main.java.SohuSpider.util.JSoupUtils.getDocument;
import static main.java.SohuSpider.util.JsonUtils.parseRestContent;
import static main.java.SohuSpider.util.XmlUtils.writeEntryUrls;
import static main.java.SohuSpider.util.XmlUtils.loadEntryUrls;

public class SpiderService implements Serializable {
	
	//使用BloomFilter算法去重
	static BloomFilter filter = new BloomFilter();
	
	 //url阻塞队列
	BlockingQueue<String> urlQueue = null;
	
	//数据库连接
	//static Connection con = DBStatement.getCon();
	
	//static Statement stmt = DBStatement.getInstance();
	
	//static PreparedStatement ps = null;
	
	//线程池
	static Executor executor = Executors.newFixedThreadPool(40);

	static String urlHost = "http://m.sohu.com";
	
	//导航页面url
	static String urlNavigation = "https://m.sohu.com/c/395/?_once_=000025_zhitongche_daohang_v3";
	
	//爬取深度
	static int DEFAULT_DEPTH = 10;
	
	static int DEFAULT_THREAD_NUM = 30;
	
	public void start() throws InterruptedException{
		
		File urlsSer = new File("urlQueue.ser"); 
		if (urlsSer.exists()){
		
			try{
			   //对象反序列化
				ObjectInputStream ois = new ObjectInputStream(new FileInputStream(urlsSer));
				urlQueue = (BlockingQueue<String>) ois.readObject();
				
				ois.close();
			} catch (Exception e) {
				e.printStackTrace();
			}  
		}
		else{
			//创建阻塞队列
			urlQueue = new LinkedBlockingQueue<String>();
			
			//获取入口Url
			List<String> urlChannels = genEntryChannel();
			
			for (String url : urlChannels) {
				urlQueue.add(url);
			}
		}
		
		
		//添加程序监听结束,程序结束时候应序列化两个重要对象--urlQueue和filter
		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable(){

			public void run() {
				System.out.println(urlQueue.isEmpty());
				try{
					if (urlQueue.isEmpty() == false) {
						//序列化urlQueue
						ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("urlQueue.ser"));
						os.writeObject(urlQueue);
						os.close();
							
					 }
						
					//序列化bits
					ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream("bits.ser"));
					os.writeObject(filter.getBitset());
					os.close();
				} catch(Exception e) {
					e.printStackTrace();
				}
				
			}
		}));
				
		for(int i = 0; i < DEFAULT_THREAD_NUM; i++){
			Thread a = new Thread(new Runnable() {

				public void run() {
				     while (true) {
	                        String url = getAUrl();
	                        if (!filter.contains(url)) {
	                            System.out.println(Thread.currentThread().getName()+" 正在爬取url: " + url);
	                            if (url != null) {
									crawler(url);
	                            }
	                        }else {
	                            System.out.println("1此url存在，不爬了." + url);
	                        }
	                    }
					
				}
				
			});
			executor.execute(a);
		}
		
		//线程池监视线程
		new Thread(new Runnable(){  
			public void run() {
				while(true) {
					try{
						if (((ThreadPoolExecutor)executor).getActiveCount() < 10) {
							Thread a = new Thread(new Runnable() {
								public void run() {
									while (true) {
										String url = getAUrl();
										if (!filter.contains(url)) {
											System.out.println(Thread.currentThread().getName()+"正在爬取url:" + url);
											if (url != null) {
												crawler(url);
											}
										}else {
											System.out.println("2此url存在， 不爬了." + url);
										}
									}
								}
							});
							executor.execute(a);
							if (urlQueue.size() == 0) {
								System.out.println("队列为0了！！！！！！！");
							}
						}
						Thread.sleep(3000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
			}
			
		}).start();
		
	}

	/* 从导航页解析入口新闻url */
	public static List<String> genEntryChannel () {

		List<String> urlArray = new ArrayList<String>();
		String genurl;
		//int[] author = {255783, 157635, 267106, 260616, 162522};
		int[] author = {162522, 267106, 260616, 255783, 157635};
		for (int i=0;i<author.length;i++){
			for(int j=210936000; j>210000000;j--){
				genurl = "http://m.sohu.com/a/" + j + "_" + author[i];
				urlArray.add(genurl);
			}
		}
		writeEntryUrls(urlArray);
		return urlArray;
	}
	
	/* 爬取新闻网页 */
	public void crawler(String url) {
		
		Document doc = getDocument(url);
		if(doc == null) {//返回的Document对象一定是正确的
			return;
		}

		String title = "";
		String category = null;
		String sourceFrom = null; //author
		String date = null;
		String content = "";
		String newsid = null; //wjc

		NewsBean news = new NewsBean();

		news.setUrl(url);

		//set newsid
		String idpattern = "\\d+_\\d+";
		Pattern r = Pattern.compile(idpattern);
		Matcher m = r.matcher(url);
		if(m.find()){
			newsid = m.group(0);
			news.setNewsID(newsid);
		} else {
			return;
		}

		try{
			/**
			 * 新闻标题格式 题目-类别-手机搜狐
			 * 但是有些题目中本身就含有 "-"
			 */
			String[] temp = doc.title().trim().split("-");
			category = temp[temp.length - 2].substring(0, 2);
			for (int i = 0; i < temp.length - 2; i++){
				title += temp[i];
			}
		} catch (ArrayIndexOutOfBoundsException e) {
			//e.printStackTrace();
			return ;
		}

		news.setCategory(category);
		news.setTitle(title);

		sourceFrom = doc.body().select("header.name").first().text();
		news.setSourceFrom(sourceFrom);// author

		date = doc.body().select("footer.time").first().text();

		news.setDate(date);

		Elements paras = doc.body().select("article p");
		if ( paras.isEmpty() == false) {
			for (Element e : paras) {
				content += e.text();
				content += "\n";
			}
		}

		news.setContent(content);

		if (content.length() > 8000) {
			return ;
		}
		//打印用户信息
		System.out.println("爬取成功：" + news);
		filter.add(url); //将该url加入filter中
	}
	
	
	public String getAUrl() {
		String tmpAUrl;
        try {
            tmpAUrl= urlQueue.take();
            return tmpAUrl;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return null;
	}
}

