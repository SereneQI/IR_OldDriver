import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class mathcerw
{

	public static void test(String a){
		System.out.println(a);
		//System.out.println(b);
	}

	public static void cate(String curl){
		String pat = "c.*";
		String[] temp = curl.split("/");
		String con = "con";
		if (!Pattern.matches(pat,temp[temp.length-2])){
			System.out.println("1:"+temp[temp.length-2]);
			System.out.println("1:"+temp[temp.length-1]);
		} else {
			System.out.println("2:" + temp[temp.length-1]);
		}
		for (int i=0;i<10;i++){
			con = con + i + "hhh";
			System.out.println(con);
		}
		// for (int i = 0;i<temp.length;i++){
		// 	System.out.println(temp[i]);
		// }
	}

	public static void match(String url){
		String pattern = "(\\d+_\\d+)|([0-9]+)";

		Pattern r = Pattern.compile(pattern);
		Matcher m = r.matcher(url);
		if (m.find()){
			System.out.println(m.group(0));
		}
	}
	public static void main(String args[]){
		//String url = "https://m.sohu.com/n/521560556/";
		//String url = "http://m.sohu.com/a/209551723_267106";
		String curl = "http://m.sohu.com/ch/15";
		String curl2 = "http://m.sohu.com/ch/15/994";
		cate(curl2);
		
	}
}