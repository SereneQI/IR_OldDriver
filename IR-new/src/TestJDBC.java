import java.util.ArrayList;
import java.util.List;

import org.bson.Document;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;


/** 
* @author  QIPENG 
* @date 2017年12月13日 下午7:26:29 
* @version 1.0 
* @description 
*/
public class TestJDBC {
	public static void main(String args[]) {
		try{   
		       // 连接到 mongodb 服务
		         MongoClient mongoClient = new MongoClient( "39.106.157.67" , 27237 );
//		         MongoClient mongoClient = new MongoClient( "localhost" , 27017 );
		       
		         // 连接到数据库
		         MongoDatabase mongoDatabase = mongoClient.getDatabase("mycol");  
		       System.out.println("Connect to database successfully");
		       mongoDatabase.createCollection("qipengtest");
		       System.out.println("集合创建成功");
		        
		      }catch(Exception e){
		        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		     }
    }
}
