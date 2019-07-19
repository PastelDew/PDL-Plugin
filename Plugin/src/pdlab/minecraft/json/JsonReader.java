package pdlab.minecraft.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class JsonReader {
	  public static JSONObject readJsonFromUrl(String url) throws IOException, ParseException {
	    InputStream is = new URL(url).openStream();
	    try {
	      BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
	      JSONParser parser = new JSONParser();
	      JSONObject json = (JSONObject) parser.parse(rd);
	      return json;
	    } finally {
	      is.close();
	    }
	  }
}
