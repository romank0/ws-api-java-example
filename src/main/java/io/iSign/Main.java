package io.iSign;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class Main {

	   public static void main(String[] args) throws Exception {
	        String apiToken = System.getenv("ACCESS_TOKEN");

	        System.out.println("iSign.io API Java example ");

	        String filename = getArgument(args, 0, "test.pdf");
	        String phone = getArgument(args, 1, "+37060000666");
			String ssn = getArgument(args, 2, "50001018865");
			String host = getArgument(args, 3, "https://developers.isign.io/");

			HttpResponse prepareResponse = prepare(host, apiToken, filename, phone, ssn);

	        JsonReader jsonReader = Json.createReader(new StringReader(EntityUtils.toString(prepareResponse.getEntity(),  "UTF-8")));
	        JsonObject prepareJson = jsonReader.readObject();

	        if((prepareJson.getString("status")).equals("ok")) {
		        System.out.println("Phone will receive control code: "+prepareJson.getString("control_code"));
		        System.out.println("Prepare responded with token: "+prepareJson.getString("token"));
		        status(host, apiToken, prepareJson.getString("token"));
	        } else {
	        	System.out.println("Responded with error:" + prepareJson.getString("message") );
	        }
	    }

	private static String getArgument(String[] args, int argPosition, String defaultValue) {
		String filename;
		if (args.length > argPosition) {
			filename = args[argPosition];
		} else {
			filename = defaultValue;
		}
		return filename;
	}

	   public static void status(String host, String apiToken, String token) throws Exception {

		   System.out.println("Requesting signed file status:");
	       HttpClient client = HttpClientBuilder.create().build();
		   HttpGet statusMethod = new HttpGet(host + "/mobile/sign/status/"+token+".json?access_token="+apiToken);

	       	for (int i=0; i<60; i = i+5) {
		        HttpResponse statusResponse = client.execute(statusMethod);
				HttpEntity entity = statusResponse.getEntity();
				String statusString = EntityUtils.toString(entity,  "UTF-8");
		        JsonReader jsonReader = Json.createReader(new StringReader(statusString));
		        JsonObject statusJson = jsonReader.readObject();

		        System.out.println(statusJson.getString("status"));
		        if ((statusJson.getString("status")).equals("ok")) {
		        	JsonObject file = statusJson.getJsonObject("file");
		        	try {
		        		FileUtils.writeByteArrayToFile(new File("test_signed.pdf"), DatatypeConverter.parseBase64Binary(file.getString("content")));
		        	} catch (IOException ex) {
		        		System.out.println(ex.toString());
		        	}
		        	System.out.println("Signed. Please open ./test_signed.pdf !\n");
			        break;
		        } else if (statusJson.getString("status").equals("error")) {
		        	System.out.println("Singing failed with message: "+statusJson.getString("message"));
		        }
		        try {
	        	  Thread.sleep(5000);
	        	} catch (InterruptedException ie) {
	        	    //Handle exception
	        	}
	       	}
	   }

	   public static HttpResponse prepare(String host, String apiToken, String filename, String phone, String ssn) throws Exception {
		   byte[] fileData = loadFile(filename);

		   String base64EncodedContent = new String(DatatypeConverter.printBase64Binary(fileData));
		   String digest = toSHA1(fileData);

		   FileUtils.writeStringToFile(new File(filename + ".base64"), base64EncodedContent);
		   FileUtils.writeStringToFile(new File(filename + ".sha1"), digest);

	        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
	        nameValuePairs.add(new BasicNameValuePair("type", "pdf"));

	        nameValuePairs.add(new BasicNameValuePair("phone", phone));
	        nameValuePairs.add(new BasicNameValuePair("code", ssn));

	        nameValuePairs.add(new BasicNameValuePair("language", "EN"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[contact]", "Seventh Testnumber"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[reason]", "Agreement"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[location]", "Vilnius"));
	        //nameValuePairs.add(new BasicNameValuePair("pdf[annotation][text]", "Annotation"));
	        nameValuePairs.add(new BasicNameValuePair("pdf[files][0][name]", filename));
			nameValuePairs.add(new BasicNameValuePair("pdf[files][0][content]", base64EncodedContent));
			nameValuePairs.add(new BasicNameValuePair("pdf[files][0][digest]", digest));

	        HttpClient client = HttpClientBuilder.create().build();
	        HttpPost method = new HttpPost(host + "mobile/sign.json?access_token=" + apiToken);
	        method.setEntity(new UrlEncodedFormEntity(nameValuePairs, "UTF-8"));

	        method.addHeader("content-type", "application/x-www-form-urlencoded");
	        HttpResponse response = client.execute(method);

	        return response;
	   }

	    private static String byteArrayToHexString(byte[] b) {
	        String result = "";
	        for (int i = 0; i < b.length; i++) {
	            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
	        }
	        return result;
	    }

	    private static String toSHA1(byte[] convertme) throws NoSuchAlgorithmException {
	        MessageDigest md = MessageDigest.getInstance("SHA-1");
	        return byteArrayToHexString(md.digest(convertme));
	    }

	    private static byte[] loadFile(String name) throws IOException {
	        InputStream in = new FileInputStream(name);
			return IOUtils.toByteArray( in );
		}
}
