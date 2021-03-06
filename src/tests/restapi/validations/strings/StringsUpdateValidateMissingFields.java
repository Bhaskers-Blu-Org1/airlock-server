package tests.restapi.validations.strings;

import org.apache.wink.json4j.JSONObject;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.ProductsRestApi;
import tests.restapi.StringsRestApi;

public class StringsUpdateValidateMissingFields {
	protected String seasonID;
	protected String stringID;
	protected String filePath;
	protected StringsRestApi t;
	protected ProductsRestApi p;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String str;
	private AirlockUtils baseUtils;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		t = new StringsRestApi();
		t.setURL(translationsUrl);
		p = new ProductsRestApi();
		p.setURL(m_url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		str = FileUtils.fileToString(filePath + "strings/string1.txt", "UTF-8", false);
		stringID = t.addString(seasonID, str, sessionToken);
	}

	@Test 
	public void emptyLastModified() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("lastModified");
		updateString(json.toString());
	}
	
	@Test 
	public void emptyUniqueId() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("uniqueId");
		String response = t.updateString(stringID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	
	}
	
	//minAppVersion is removed from strings
	/*
	@Test 
	public void emptyMinAppVersion() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.put("minAppVersion", JSONObject.NULL);
		updateString(json.toString());
	}*/
	
	@Test 
	public void emptyKey() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("key");		
		updateString(json.toString());
	}
	
	@Test 
	public void emptyCreator() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("creator");		
		updateString(json.toString());
	}
	
	@Test 
	public void emptyValue() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("value");
		updateString(json.toString());
	}
	
	@Test
	public void emptyStage() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("stage");
		updateString(json.toString());
	}

	
	@Test 
	public void emptySeasonId() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("seasonId");
		updateString(json.toString());
	}
	
	@Test 
	public void emptyOwner() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);		
		json.remove("owner");
		String response = t.updateString(stringID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	
	}
	
	
	@Test 
	public void emptyInternationalFallback() throws Exception{
		str = t.getString(stringID, sessionToken);
		JSONObject json = new JSONObject(str);
		json.remove("internationalFallback");
		String response = t.updateString(stringID, json.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Test should succeed, but instead failed: " + response );
	
	}
	
	private void updateString(String input) throws Exception{
		String response = t.updateString(stringID, input, sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
