import java.net.MalformedURLException;
import java.net.URL;

import com.esi911.webeoc7.api._1.API;
import com.esi911.webeoc7.api._1.ArrayOfString;
import com.esi911.webeoc7.api._1.GetListsResponse.GetListsResult;
import com.esi911.webeoc7.api._1.WebEOCCredentials;

public class WebEOCTalker {
	public static void main(String[] args) {
		API webEOC;
		try {
			webEOC = new API(new URL(
					"http://demo.esi911.com/sandbox/api.asmx?wsdl"));

			WebEOCCredentials eocCred = new WebEOCCredentials();
			eocCred.setPassword("123456");
			eocCred.setUsername("WebEOC Administrator");
			ArrayOfString aos = webEOC.getAPISoap().getBoardNames(eocCred);
			
			for(String s : aos.getString()){
				System.out.println(s);
			}
			
			
			GetListsResult glr = webEOC.getAPISoap().getLists(eocCred);
			Object g = glr.getAny();
			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
}
