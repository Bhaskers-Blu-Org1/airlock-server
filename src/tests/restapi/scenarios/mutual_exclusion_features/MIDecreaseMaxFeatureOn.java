package tests.restapi.scenarios.mutual_exclusion_features;

import java.io.IOException;


import org.apache.wink.json4j.JSONArray;
import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class MIDecreaseMaxFeatureOn {
	protected String seasonID;
	protected String parentID;
	protected String childID1;
	protected String childID2;
	protected String filePath;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String productID;
	private String sessionToken = "";
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
	}
	
	/**
	 * Create mutually exclusive group
	 */
	@Test (description = "Create mutually exclusive group with maxFeaturesOn=2 and 2 features in Production with defaultSystemIsDown=true ")
	public void createMutuallyExclusiveGroup() throws JSONException{
		try {
			String parent = FileUtils.fileToString(filePath + "feature-mutual.txt", "UTF-8", false);
			JSONObject parentJson = new JSONObject(parent);
			parentJson.put("maxFeaturesOn", 2);
			parentID = f.addFeature(seasonID, parentJson.toString(), "ROOT", sessionToken);
			
			String child1 = createFeature("feature1.txt");
			String child2 = createFeature("feature2.txt");
			childID1 = f.addFeature(seasonID, child1, parentID, sessionToken);
			childID2 = f.addFeature(seasonID, child2, parentID, sessionToken);
			
			parent = f.getFeature(parentID, sessionToken);
			JSONObject json = new JSONObject(parent);
			JSONArray children = json.getJSONArray("features");
			if (children.size() == 2){
				Assert.assertEquals(children.getJSONObject(0).get("uniqueId"), childID1, "The first child is incorrect");
				Assert.assertEquals(children.getJSONObject(1).get("uniqueId"), childID2, "The second child is incorrect");
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	@Test (dependsOnMethods="createMutuallyExclusiveGroup", description = "Change maxFeaturesOn to 1 - should fail as there are already 2 features in production") 
	public void decreaseMaxFeaturesOn() throws JSONException, IOException{
		String parent = f.getFeature(parentID, sessionToken);
		JSONObject json = new JSONObject(parent);
		json.put("maxFeaturesOn", 1);
		String response = f.updateFeature(seasonID, parentID, json.toString(), sessionToken);
		Assert.assertTrue(response.contains("error"), "Test should fail, but instead passed: " + response );
	}
	

	
	private String createFeature(String fileName) throws IOException, JSONException{
		String feature = FileUtils.fileToString(filePath + fileName, "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		json.put("defaultIfAirlockSystemIsDown", true);
		return json.toString();
	}
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}

}
