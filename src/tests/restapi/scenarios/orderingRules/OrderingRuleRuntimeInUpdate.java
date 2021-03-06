package tests.restapi.scenarios.orderingRules;

import java.io.IOException;








import org.apache.commons.lang3.RandomStringUtils;
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
import tests.restapi.RuntimeDateUtilities;
import tests.restapi.RuntimeRestApi;
import tests.restapi.SeasonsRestApi;

public class OrderingRuleRuntimeInUpdate {
	protected String seasonID;
	protected String filePath;
	protected ProductsRestApi p;
	protected FeaturesRestApi f;
	private AirlockUtils baseUtils;
	protected String productID;
	protected String m_url;
	private String sessionToken = "";
	protected String orderingRule;
	protected String orderingRuleID;
	protected String featureID;
	protected String childID1;
	protected String childID2;
	private SeasonsRestApi s;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		p = new ProductsRestApi();
		p.setURL(m_url);
		s = new SeasonsRestApi();
		s.setURL(m_url);
		
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		JSONObject json = new JSONObject(feature);
		json.put("stage", "PRODUCTION");
		featureID = f.addFeature(seasonID, json.toString(), "ROOT", sessionToken);

	}
	

	
	@Test (description = "Add dev orderingRule")
	public void devOrderingRule() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		orderingRule = FileUtils.fileToString(filePath + "orderingRule/orderingRule1.txt", "UTF-8", false);
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("name", RandomStringUtils.randomAlphabetic(5));
		orderingRuleID = f.addFeature(seasonID, jsonOR.toString(), featureID, sessionToken);
		Assert.assertFalse(orderingRuleID.contains("error"), "Can't add orderingRule: " + orderingRuleID);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was changed");

	}
	
	@Test (dependsOnMethods="devOrderingRule", description = "Move orderingRule to prod")
	public void prodOrderingRule() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "PRODUCTION");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule: " + response);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was not changed");
		Assert.assertTrue(featureFound(responseProd.message)==1, "Ordering rule was not found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (dependsOnMethods="prodOrderingRule", description = "Add configuration and validate it in the runtime file")
	public void addConfiguration() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		feature.put("namespace", "childOfF1");
		
		feature.put("name", "F1");
		childID1 = f.addFeature(seasonID, feature.toString(), featureID, sessionToken);
		feature.put("name", "F2");
		childID2 = f.addFeature(seasonID, feature.toString(), featureID, sessionToken);
		
		
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.0");
		configJson.put(childID2, "2.0");
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule: " + response);
		
		
		String expectedConfiguration1 = "{\"childOfF1.F1\":1.0,\"childOfF1.F2\":2.0}";
		String expectedConfiguration2 = "{\"childOfF1.F2\":2.0,\"childOfF1.F1\":1.0}";
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONObject orderingRule = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);

		Assert.assertTrue(orderingRule.getString("configuration").equals(expectedConfiguration1) || orderingRule.getString("configuration").equals(expectedConfiguration2)
				, "wrong orderingRule configuration in dev runtime");	

		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(featureFound(responseProd.message)==1, "Ordering rule was found in production runtime file");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		orderingRule = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);

		Assert.assertTrue(orderingRule.getString("configuration").equals(expectedConfiguration1) || orderingRule.getString("configuration").equals(expectedConfiguration2),
				"wrong orderingRule configuration in prod runtime");	

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
	
	@Test (dependsOnMethods="addConfiguration", description = "Update ids order in configuration and validate that runtime files are not changed")
	public void updateConfigurationNoChange() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		JSONObject configJson = new JSONObject();
		configJson.put(childID2, "2.0");
		configJson.put(childID1, "1.0");
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		
		//side check that the conf is a legal json
		String currentConfStr = jsonOR.getString("configuration");
		JSONObject currentConfJson = new JSONObject(currentConfStr);
		
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule: " + response);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==304, "Runtime development feature file was not updated");

		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==304, "Runtime production feature file was changed");
		
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==304, "productionChanged.txt file was not changed");

	}
	
	
	@Test (dependsOnMethods="updateConfigurationNoChange", description = "Move orderingRule to dev")
	public void updateOrderingRuleToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("stage", "DEVELOPMENT");
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule: " + response);
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(featureFound(responseProd.message)==0, "Ordering rule was found in production runtime file");
		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
/*
	@Test (dependsOnMethods="prodOrderingRule", description = "Move orderingRule to dev")
	public void updateOrderingRuleToDev() throws JSONException, IOException, InterruptedException{
		String dateFormat = RuntimeDateUtilities.setDateFormat();
		
		JSONObject feature = new JSONObject(FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false));
		feature.put("namespace", "childOfF1");
		
		feature.put("name", "F1");
		childID1 = f.addFeature(seasonID, feature.toString(), featureID, sessionToken);
		feature.put("name", "F2");
		childID2 = f.addFeature(seasonID, feature.toString(), featureID, sessionToken);
		
		
		JSONObject configJson = new JSONObject();
		configJson.put(childID1, "1.0");
		configJson.put(childID2, "2.0");
		
		orderingRule = f.getFeature(orderingRuleID, sessionToken);		
		JSONObject jsonOR = new JSONObject(orderingRule);
		jsonOR.put("configuration", configJson.toString());
		String response = f.updateFeature(seasonID, orderingRuleID, jsonOR.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Can't update orderingRule: " + response);
		
		
		String expectedConfiguration = "{\"childOfF1.F1\":1.0, \"childOfF1.F2\":2.0}";
		
		//check if files were changed
		f.setSleep();
		RuntimeRestApi.DateModificationResults responseDev = RuntimeDateUtilities.getDevelopmentFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseDev.code ==200, "Runtime development feature file was not updated");
		JSONObject root = RuntimeDateUtilities.getFeaturesList(responseDev.message);
		JSONObject orderingRule = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);

		Assert.assertTrue(orderingRule.getString("configuration").equals(expectedConfiguration), "wrong orderingRule configuration in dev runtime");	

		Assert.assertTrue(featureFound(responseDev.message)==1, "Ordering rule was not found in development runtime file");
		RuntimeRestApi.DateModificationResults responseProd = RuntimeDateUtilities.getProductionFileDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(responseProd.code ==200, "Runtime production feature file was changed");
		Assert.assertTrue(featureFound(responseProd.message)==1, "Ordering rule was found in production runtime file");
		root = RuntimeDateUtilities.getFeaturesList(responseProd.message);
		orderingRule = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules").getJSONObject(0);

		Assert.assertTrue(orderingRule.getString("configuration").equals(expectedConfiguration), "wrong orderingRule configuration in prod runtime");	

		RuntimeRestApi.DateModificationResults prodChanged = RuntimeDateUtilities.getProductionChangedDateModification(m_url, productID, seasonID, dateFormat, sessionToken);
		Assert.assertTrue(prodChanged.code ==200, "productionChanged.txt file was not changed");

	}
*/	
	private int featureFound(String content) throws JSONException{
		JSONObject root = RuntimeDateUtilities.getFeaturesList(content);
		JSONArray orderingRules = root.getJSONArray("features").getJSONObject(0).getJSONArray("orderingRules");
		return orderingRules.size();
	}
	

	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
