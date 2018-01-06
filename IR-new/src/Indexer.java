import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopFieldDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

/** 
* @author  QIPENG 
* @date 2017年11月29日 上午11:10:18 
* @version 1.0 
* @description 建立索引主文件
*/

/**
 * 功能更新： 
 * y读取整个文件夹下的所有文件建立索引; 
 * yluke查看索引; 
 * yJson转嵌套Java对象，特别注意cmts为0时需手动解析
 * y对cmts建索引
 * yMacOS遍历文件夹时删除并永不产生.DS_Store文件
 * y连接数据库建立索引(服务器上 MongoDB 3.0.6)
 * y更新数据格式
 * y索引的增删改
 * y对索引建立过程进行优化，buildIdx维护一个IndexWriter，先建内存索引，结束后将多个内存索引合并写在磁盘中
 * y 索引的增删改三个函数维护一个IndexWriter
 * y 写爬虫的时候直接调用索引的增删改Java代码
 */

public class Indexer {
	/**
	 * 对路径下的所有文件建立索引，1214不再维护
	 * 
	 * @param docPath
	 *            文件路径
	 * @param idxPath
	 *            索引路径
	 * @param createOrAppend
	 *            true
	 * @throws IOException
	 */
	public static void buildIndexfromFile(String docPath, String idxPath, boolean createOrAppend) throws IOException {
		long start = System.currentTimeMillis(); // 记录时间

		// Initialization
		// 分词
		Analyzer analyzer = new IKAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

		// create_or_append:不存在即新建，存在即打开append
		// create:新建，存在也被覆盖
		// append:打开已有的

		// if (createOrAppend) {
		// config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
		// } else {
		// config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		// }

		// Store index on disk
		Directory directory = FSDirectory.open(new File(idxPath).toPath());
		// Store the index in memory:
		// Directory directory = new RAMDirectory();

		IndexWriter iwriter = new IndexWriter(directory, config);

		// read file
		Path docDirPath = new File(docPath).toPath();
		// 遍历路径下的所有文件，建立索引
		indexDocs(iwriter, docDirPath);

		long end = System.currentTimeMillis();
		System.out.println("Time consumed:" + (end - start) + " ms");
	}

	/**
	 * 遍历路径下的所有文件，建立索引，1214不再维护
	 * 
	 * @param writer
	 * @param docdirPath
	 * @throws IOException
	 */
	public static void indexDocs(final IndexWriter writer, Path docdirPath) throws IOException {
		// 如果是目录，查找目录下的文件
		if (Files.isDirectory(docdirPath, new LinkOption[0])) {
			System.out.println("当前路径为:" + docdirPath);
			Files.walkFileTree(docdirPath, new SimpleFileVisitor() {
				public FileVisitResult visitFile(Object file, BasicFileAttributes attrs) throws IOException {
					Path path = (Path) file;
					System.out.println("当前处理文件为:" + path.getFileName());
					indexDoc(writer, path, attrs.lastModifiedTime().toMillis());
					return FileVisitResult.CONTINUE;
				}
			});
		} else {
			indexDoc(writer, docdirPath, Files.getLastModifiedTime(docdirPath, new LinkOption[0]).toMillis());
		}
	}

	/**
	 * 对单个文件建立索引，1214不再维护
	 * 
	 * @param writer
	 * @param file
	 * @param lastModified
	 * @throws IOException
	 */
	public static void indexDoc(IndexWriter writer, Path file, long lastModified) throws IOException {
		// 读入Json文件数据
		byte[] bytes = Files.readAllBytes(file);
		String line = new String(bytes);
		System.out.println("读入数据：" + line);

		// 转换为News对象
		JsonObject jsonObject = new JsonParser().parse(line).getAsJsonObject();
		Integer num = jsonObject.getAsJsonPrimitive("cmtnum").getAsInt();
		News news = new News();
		if (num!=0) {//cmts不为空
			System.out.println("cmts不为空");
			Gson gson = new Gson();
			news = gson.fromJson(line, News.class);
		}else {// cmts列表为空""的情况，不能直接转换为News对象，需要手动解析每个字段
			System.out.println("cmts为空");
			news = new News(jsonObject.getAsJsonPrimitive("newsid").toString(),
					jsonObject.getAsJsonPrimitive("title").toString(),
					jsonObject.getAsJsonPrimitive("content").toString(),
					jsonObject.getAsJsonPrimitive("author").toString(),
					jsonObject.getAsJsonPrimitive("type").toString(),
					num,
					jsonObject.getAsJsonPrimitive("ntime").getAsInt(),
					jsonObject.getAsJsonPrimitive("newsurl").toString(),
					jsonObject.getAsJsonPrimitive("ori").toString(),
					new ArrayList<Comment>());
		}
		 System.out.println(news.toString());
		
		 // News->Document
		 writer.addDocument(NewsToDoc(news, lastModified));

//		 if (writer.getConfig().getOpenMode() == IndexWriterConfig.OpenMode.CREATE) {
//		 System.out.println("adding " + file);
//		 writer.addDocument(doc);
//		 } else {
//		 System.out.println("updating " + file);
//		 writer.updateDocument(new Term("newsid", news.getNewsid()), doc);
//		 }
		writer.commit();
	}
	
	/**
	 * 把Java对象(News)转换为lucene.document.Document对象
	 * @param news
	 * @param lastModified
	 * @return
	 */
	public static Document NewsToDoc(News news, long lastModified) {
		 Document doc = new Document();
		 doc.add(new StringField("newsid", news.getNewsid(), Store.YES));//StringField,不分词
		 doc.add(new TextField("title", news.getTitle(), Store.YES));// TextField，分词
		 doc.add(new TextField("content", news.getContent(), Store.YES));
		 doc.add(new StringField("author", news.getAuthor(), Store.YES));
		 doc.add(new StringField("type", news.getType(), Store.YES));
		 
		 doc.add(new IntField("cmtnum", news.getCmtnum(), Store.YES));
		 doc.add(new NumericDocValuesField("cmtnum", news.getCmtnum()));//
		 
		 doc.add(new IntField("ntime", news.getNtime(), Store.YES));
		 doc.add(new NumericDocValuesField("ntime", news.getNtime()));
		 
		 doc.add(new StringField("newsurl", news.getNewsurl(), Store.YES));
		 doc.add(new StringField("ori", news.getOri(), Store.YES));
		 if(news.getCmtlist()!=null) {//cmts为空的，该条记录不建立索引
			 doc.add(new TextField("cmtlist",news.getCmtlist(),Store.YES));
		 }
		 if(lastModified!=0) {//从文件中读取数据才有上次修改时间，从数据库中读取没有
			 doc.add(new LongField("modified", lastModified, Field.Store.YES));
		 }
		return doc;
	}
	
	/**
	 * 从数据库中读取数据建立索引
	 * 先在内存中建立索引
	 * 结束后把内存中的索引一起写入硬盘中
	 * @param idxPath 索引文件保存路径
	 * @throws IOException
	 */
	public static void buildIndexfromDB(String idxPath) throws IOException {
		long start = System.currentTimeMillis(); // 记录时间
		Integer count =0 ;//计数
		
		final Path docDir = new File(idxPath).toPath();
		//建立文件索引库
        //Directory fileDirectory=FSDirectory.open(Paths.get(idxPath));
        Directory fileDirectory=FSDirectory.open(new File(idxPath).toPath());
        //创建内存索引库
        //Directory ramDirectory = new RAMDirectory(FSDirectory.open(Paths.get(idxPath)), null);
        Directory ramDirectory = new RAMDirectory(FSDirectory.open(new File(idxPath).toPath()), null);
		
		Analyzer analyzer = new IKAnalyzer();
		IndexWriterConfig config = new IndexWriterConfig(analyzer);
		//config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
		//操作文件的IndexWriter
        IndexWriter fileIndexWriter = new IndexWriter(fileDirectory, config);
        //操作内存的IndexWriter
        Analyzer analyzer1 = new IKAnalyzer();
        IndexWriterConfig config2 = new IndexWriterConfig(analyzer1);
        IndexWriter ramIndexWriter=new IndexWriter(ramDirectory, config2);
		
//		Directory ramDirectory = new RAMDirectory(FSDirectory.open(new File(idxPath).toPath()), null);
//		IndexWriter ramWriter = new IndexWriter(ramDirectory, config);//尽量维护一个IndexWriter
		
		//读取数据库文件，建立索引
		//连接到 mongodb 服务
        MongoClient mongoClient = new MongoClient( "localhost" , 27017 );//服务器上要改成27237
        // 连接到数据库
        MongoDatabase mongoDatabase = mongoClient.getDatabase("news");  
        System.out.println("Connect to database-news successfully");
        // 选择集合
        MongoCollection<org.bson.Document> collection = mongoDatabase.getCollection("news");
        System.out.println("集合 news 选择成功");
        //检索所有文档  
        /** 
        * 1. 获取迭代器FindIterable<Document> 
        * 2. 获取游标MongoCursor<Document> 
        * 3. 通过游标遍历检索出的文档集合 
        * */  
        FindIterable<org.bson.Document> findIterable = collection.find();  
        MongoCursor<org.bson.Document> mongoCursor = findIterable.iterator(); 
        org.bson.Document doc;
        while(mongoCursor.hasNext()){
        		count ++;
        		if (count%1000==0)
        			System.out.println("正在处理："+count);
        		//System.out.println("ing");
        		doc = mongoCursor.next();
        		//将org.bson.Document转换为News对象
        		//Mongodb官方没有提供org.bson.Document转Java对象的方法，手动解析
        		//解析cmts字段（列表）
	    		ArrayList<Comment> cmtslistCmt = new ArrayList<Comment>();
	    		//System.out.println(doc.getInteger("cmtnum"));
	    		//if(Integer.valueOf(doc.getString("cmtnum"))!=0) {
	    		//System.out.println(doc.getString("newsid"));
	    		if(doc.get("cmtnum") instanceof String) {
	    			System.out.println(doc.getString("newsid"));
	    			continue;
	    		}
	    		if(doc.get("ntime") instanceof String) {
	    			System.out.println(doc.getString("ntime"));
	    			continue;
	    		}
	    			
	    		//cmtnum:999+ 改成999
	    		try {
					if(doc.getInteger("cmtnum")!=0) {
						ArrayList<org.bson.Document> cmtlistDoc =(ArrayList<org.bson.Document>) doc.get("cmtlist");
						if(!cmtlistDoc.isEmpty()) {
							for (org.bson.Document cmt: cmtlistDoc){
								if(cmt.get("cmter") instanceof Integer) {
									cmtslistCmt.add(new Comment(cmt.get("cmter").toString(),cmt.getString("cmt"),cmt.getString("cmttime")));
								}
								else {
									cmtslistCmt.add(new Comment(cmt.getString("cmter"),cmt.getString("cmt"),cmt.getString("cmttime")));
								}							
							}
						}
					}
				} catch (java.lang.ClassCastException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    		//System.out.println(doc.get("content"));
	    		//System.out.println(doc.get("ntime").getClass());
	    		//建立News对象
        		News	 news = new News(
        				doc.getString("newsid").toString(),
        				doc.getString("title").toString(),
        				doc.getString("content").toString(),
        				doc.getString("author").toString(),
        				doc.getString("type").toString(),
        				doc.getInteger("cmtnum"),
        				//Integer.valueOf(doc.getString("cmtnum")),
        				doc.getInteger("ntime"),
        				//doc.getString("ntime").toString(),
        				doc.getString("newsurl").toString(),
        				doc.getString("ori").toString(),
					cmtslistCmt);
        		//System.out.println(news.toString());
        		//将News对象转换为lucene.document.Document对象
        		long lastModified = System.currentTimeMillis();//为了跟从文档中读写保持一致
        		ramIndexWriter.addDocument(NewsToDoc(news,lastModified));
        		//写入索引
        		//ramWriter.commit();
        }  
        ramIndexWriter.close();
        
        //内存索引写入硬盘文件

//        IndexWriterConfig cconfig = new IndexWriterConfig(analyzer);
//        cconfig.setRAMBufferSizeMB(50);//Buffer索引文档超过50M，写入硬盘
//        cconfig.setMaxBufferedDocs(80);//内存中索引文档到达80，写入硬盘，和setRAMBufferSizeMB有一个达到即可
      //内存中存储80个文档时写成磁盘一个块
        //writer.MergFactor(80);
        
        //IndexWriterConfig config2 = new IndexWriterConfig(analyzer);
		//config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
 
        //IndexWriter fileWriter = new IndexWriter(fileDirectory, config2);
        fileIndexWriter.addIndexes(ramDirectory);
        //fileWriter.optimize(); 不能用啦？
        fileIndexWriter.close();
        
		long end = System.currentTimeMillis();
		System.out.println("Time consumed:" + (end - start) + " ms");
	}
	
	
//	public void sorttest(String idxPath) {
//        IndexReader reader;
//        try {
//            reader = DirectoryReader.open(FSDirectory.open(new File(idxPath).toPath())); 
//            IndexSearcher searcher = new IndexSearcher(reader);
//            TermQuery query = new TermQuery(new Term("cmtnum", "1"));
//            // SortField field = new SortField("f1", SortField.STRING);// 有问题
//            // SortField field = new SortField("f1", SortField.INT);// 没问题
//            // SortField field = new SortField("f1", SortField.FLOAT);// 没问题
// 
//            // SortField field = new SortField("f2", SortField.STRING);// 有问题
//            // SortField field = new SortField("f2", SortField.INT);//有问题
//            // SortField field = new SortField("f2", SortField.FLOAT);// 没问题
// 
//            // SortField field = new SortField("f3", SortField.STRING);// 有问题
//            // SortField field = new SortField("f3", SortField.INT);//没问题
//            // SortField field = new SortField("f3", SortField.FLOAT);// 没问题
// 
//            // SortField field = new SortField("f3", SortField.STRING);// 没问题
//            // SortField field = new SortField("f3", SortField.INT);// 没问题
//            SortField field = new SortField("cmtnum", SortField.INT);// 没问题
//            Sort sort = new Sort(field);
//            TopFieldDocs docs = searcher.search(query, 20, sort);
//            ScoreDoc[] sds = docs.scoreDocs;
//            for (ScoreDoc sd : sds) {
//                Document doc = reader.document(sd.doc);
//                System.out.println(doc.get("f1") + "\t" + doc.get("f2") + "\t"
//                        + doc.get("f3") + "\t" + doc.get("f4"));
//            }
//        } catch (CorruptIndexException e) {
//            e.printStackTrace();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

	/**
	 * 增加索引
	 * @param newDoc
	 * @throws IOException 
	 */
	public static void addIndex(String newDoc, String idxPath, IndexWriter iwriter) throws IOException {
		// 解析Json字符串，转换为News对象
		JsonObject jsonObject = new JsonParser().parse(newDoc).getAsJsonObject();
		Integer num = jsonObject.getAsJsonPrimitive("cmtnum").getAsInt();
		News news = new News();
		if (num!=0) {//cmts不为空
			System.out.println("cmts不为空");
			Gson gson = new Gson();
			news = gson.fromJson(newDoc, News.class);
		}else {// cmts列表为空""的情况，不能直接转换为News对象，需要手动解析每个字段
		System.out.println("cmts为空");
		news = new News(jsonObject.getAsJsonPrimitive("newsid").toString(),
				jsonObject.getAsJsonPrimitive("title").toString(),
				jsonObject.getAsJsonPrimitive("content").toString(),
				jsonObject.getAsJsonPrimitive("author").toString(),
				jsonObject.getAsJsonPrimitive("type").toString(),
				num,
				jsonObject.getAsJsonPrimitive("ntime").getAsInt(),
				jsonObject.getAsJsonPrimitive("newsurl").toString(),
				jsonObject.getAsJsonPrimitive("ori").toString(),
				new ArrayList<Comment>());
		}
		System.out.println(news.toString());
				
		// News->Document
		long lastModified = System.currentTimeMillis();
		iwriter.addDocument(NewsToDoc(news, lastModified));
		iwriter.commit();		
	}
	
	/**
	 * 删除索引
	 * @param newsid
	 * @throws IOException 
	 */
	public static void delIndex(String newsurl, String idxPath, IndexWriter iwriter) throws IOException {
		Query query = new TermQuery(new Term("newsurl",newsurl));
		iwriter.deleteDocuments(query);
		iwriter.commit();
		System.out.println("delete index -- "+newsurl);
	}
	
	
	/**
	 * 修改索引，先删后加
	 * @param newDoc
	 * @throws IOException 
	 */
	public static void updateIndex(String newsid, String newDoc, String idxPath, IndexWriter iwriter) throws IOException {
		delIndex(newsid,idxPath,iwriter);
		addIndex(newDoc,idxPath,iwriter);
	}
	
	
	/**
	 * 查询，测试版，非专业
	 * @param idxPath
	 * @throws IOException
	 * @throws ParseException
	 */
	public static void search(String idxPath) throws IOException, ParseException {
		/**
		 * 多种查询 关键词检索 通配符检索 模糊检索
		 * 
		 */
		News news;

		// open index
		Directory directory = FSDirectory.open(new File(idxPath).toPath());
		DirectoryReader ireader = DirectoryReader.open(directory);
		IndexSearcher isearcher = new IndexSearcher(ireader);

		// 查询字段处理
		Analyzer analyzer = new IKAnalyzer();// 要和建索引时保持一致

		/**
		 * Term Query Term,基本单位 分词后的词条，time,date等 直接拿原始词条查询，不经过任何处理
		 */
		// Query query = new TermQuery(new Term(fieldName,queryString));

		// 指定查询字段
		//Query query = new TermQuery(new Term("ori","凤凰新闻"));  
		QueryParser parser = new QueryParser("cmtnum", analyzer);
		// 把查询字符串转换为查询对象
		Query query = parser.parse("0");

		// 查询，返回按照得分进行排序后的前n条结果的信息
		ScoreDoc[] scoreDocs = isearcher.search(query, null, 1000).scoreDocs;

		// 显示查询结果
		System.out.println("结果数为：" + scoreDocs.length);
		for (int i = 0; i < scoreDocs.length; i++) {
			Document hitDoc = isearcher.doc(scoreDocs[i].doc);
			// 把查询结果doc转为News对象
			// news = new News(hitDoc.getField("newsid").stringValue(),
			// hitDoc.getField("title").stringValue(),
			// hitDoc.getField("content").stringValue(),
			// hitDoc.getField("author").stringValue(),
			// hitDoc.getField("type").stringValue(),
			// Integer.parseInt(hitDoc.getField("cmtnum").stringValue()),
			// hitDoc.getField("time").stringValue(),
			// hitDoc.getField("newsurl").stringValue());
			// System.out.println(news.toString());
		}
		ireader.close();
		directory.close();
	}
	

	public static void main(String[] args) throws IOException, ParseException {
		
		//参数：要输入4个参数
		//args[0]: String,索引路径
		//args[1]: 1,读取数据库建立索引 2,添加索引 3,删除索引 4,更新索引
		//args[2]: String, newsurl
		//args[3]: String, new_doc
		
		if(args.length!=4) {
			System.out.println("Wrong Input!");
			return;
		}
		else {
			String idxPath = args[0];
			
			if(args[1].equals("1")) {
				System.out.println("正在对数据库中相关数据建立索引...");
				buildIndexfromDB(idxPath);
				System.out.println("建索引完成！");
			}
			else {
				Analyzer analyzer = new IKAnalyzer();
				IndexWriterConfig config = new IndexWriterConfig(analyzer);
				config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
				Directory directory = FSDirectory.open(new File(idxPath).toPath());
				IndexWriter iwriter = new IndexWriter(directory, config);
				
				switch(args[1]){
					case "2":	
						addIndex(args[3],idxPath,iwriter);
						break;
					case "3":
						delIndex(args[2],idxPath,iwriter);
						break;
					case "4":
						updateIndex(args[2], args[3], idxPath, iwriter);
						break;
				}
				
				iwriter.close();
			}
		}
		
		
		//String docPath = "./data/"; // 数据文件所在目录
		//String idxPath = "./indexDir/"; // 索引所在目录，已存在的目录
		//buildIndexfromFile(docPath, idxPath, true);
		
		//search(idxPath);
		
		//buildIndexfromDB(idxPath);
		
//		Analyzer analyzer = new IKAnalyzer();
//		IndexWriterConfig config = new IndexWriterConfig(analyzer);
//		config.setOpenMode(IndexWriterConfig.OpenMode.APPEND);
//		Directory directory = FSDirectory.open(new File(idxPath).toPath());
//		IndexWriter iwriter = new IndexWriter(directory, config);
		
		//为增删改维护一个IndexWriter	
		
//插入
//		String doc = "{\"content\": \"近期，有关饮用水中锰超标的新闻引起了很多人的关注。“锰”是一个什么东东，对我们的健康到底是有利还是有害呢？今天就让我们认识一下这位锰。\\n锰约占地壳的0.1%，是第12位最丰富的元素。锰还是组成人体的微量元素之一，分布在人体各种组织和体液中，骨骼、肝脏、胰脏、肾脏中锰浓度较高，脑、心、肺、肌肉中锰浓度较低。一个成年人体内锰的总量大约有10-20毫克。\\n锰在人体内含量很少，但起着重要的生理功能，主要表现在：\\n作为酶的组成成分或激活剂。\\n维持骨骼正常发育。\\n促进糖和脂肪代谢及抗氧化功能。\\n与生殖功能有关，缺锰可使生殖功能紊乱。\\n缺锰还可引起神经功能障碍，发生抽搐、共济失调等症状。\\n水、空气、土壤、食物中都有锰的存在。人体内的锰，大多是通过食物吃进去的，主要在小肠被吸收。当吃进去的锰多的时候，身体对锰的吸收率会降低；当吃进去的锰缺乏的时候，身体对锰的吸收率就会升高。食物中的膳食纤维、钙、磷含量多，可促进锰的吸收；食物中的植酸盐、铁含量多，会抑制锰的吸收。锰几乎完全通过肠道排泄，仅有微量经尿排泄。目前还没有可靠的生物学指标来评价锰的营养状况。\",\"ntime\": \"1510387831\",\"newsurl\": \"https://m.sohu.com/a/206148577_100163?_f=m-channel_24_feeds_11\",\"newsid\": \"12345\",\"title\": \"“锰”对健康到底是好是坏？\",\"author\": \"马博士谈营养\",\"type\": \"健康\",\"ori\":\"搜狐新闻\",\"cmtnum\": \"0\",\"cmtlist\": \"\"}\n";
//		addIndex(doc,idxPath,iwriter);
		
//		//按照newsurl进行删除
//		String newsurl="http://3g.163.com/ntes/17/1108/11/D2NF14RH00997VCT.html";
//		delIndex(newsurl,idxPath,iwriter);
		
//		String new_doc = "{\"content\": \"有关饮用水中锰超标的新闻引起了很多人的关注。“锰”是一个什么东东，对我们的健康到底是有利还是有害呢？今天就让我们认识一下这位锰。\\n锰约占地壳的0.1%，是第12位最丰富的元素。锰还是组成人体的微量元素之一，分布在人体各种组织和体液中，骨骼、肝脏、胰脏、肾脏中锰浓度较高，脑、心、肺、肌肉中锰浓度较低。一个成年人体内锰的总量大约有10-20毫克。\\n锰在人体内含量很少，但起着重要的生理功能，主要表现在：\\n作为酶的组成成分或激活剂。\\n维持骨骼正常发育。\\n促进糖和脂肪代谢及抗氧化功能。\\n与生殖功能有关，缺锰可使生殖功能紊乱。\\n缺锰还可引起神经功能障碍，发生抽搐、共济失调等症状。\\n水、空气、土壤、食物中都有锰的存在。人体内的锰，大多是通过食物吃进去的，主要在小肠被吸收。当吃进去的锰多的时候，身体对锰的吸收率会降低；当吃进去的锰缺乏的时候，身体对锰的吸收率就会升高。食物中的膳食纤维、钙、磷含量多，可促进锰的吸收；食物中的植酸盐、铁含量多，会抑制锰的吸收。锰几乎完全通过肠道排泄，仅有微量经尿排泄。目前还没有可靠的生物学指标来评价锰的营养状况。\",\"ntime\": \"1510387831\",\"newsurl\": \"https://m.sohu.com/a/206148577_100163?_f=m-channel_24_feeds_11\",\"newsid\": \"206148577_100163\",\"title\": \"“锰”对健康到底是好是坏？\",\"author\": \"马博士\",\"type\": \"健康\",\"ori\":\"搜sou新闻\",\"cmtnum\": \"0\",\"cmtlist\": \"\"}\n";
//	
//		String newsurl = "http://3g.163.com/ntes/17/1108/11/D2NERHIP00997VCT.html";
//		updateIndex(newsurl, new_doc, idxPath, iwriter);
				
		// search(indexPath);
		
//		iwriter.close();
	}

}
