
import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.wltea.analyzer.lucene.IKAnalyzer;

/** 
* @author  QIPENG 
* @date 2017年11月30日 上午9:06:50 
* @version 1.0 
* @description 分词器测试
*/
public class CTokenTest {

	/**测试多种分词器的中文分词效果
	 * @param args
	 * @throws IOException 
	 */
	
    public static void test() throws IOException{
        Analyzer analyzer=new IKAnalyzer();//可以添加扩展词典和停用词词典
//      Analyzer analyzer=new StandardAnalyzer(Version.LUCENE_30);//单字分词
//      Analyzer analyzer2=new ChineseAnalyzer();//单字分词
//      Analyzer analyzer3=new CJKAnalyzer(Version.LUCENE_30);//相连的两个字组合在一起
        
        String text = "我爱付出和服务";
        TokenStream tokenStream = analyzer.tokenStream("", text);
        tokenStream.reset();
        System.out.println("当前使用的分词器：" + analyzer.getClass());
        while (tokenStream.incrementToken()) {
            CharTermAttribute charTermAttribute = tokenStream.addAttribute(CharTermAttribute.class);
            System.out.println(charTermAttribute);
        }
    }
    
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		test();
	}

}
