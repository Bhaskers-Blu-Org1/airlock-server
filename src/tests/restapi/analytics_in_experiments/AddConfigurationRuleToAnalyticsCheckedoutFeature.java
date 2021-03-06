package tests.restapi.analytics_in_experiments;

import java.io.IOException;

import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.apache.wink.json4j.JSONArray;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.AnalyticsRestApi;
import tests.restapi.BranchesRestApi;
import tests.restapi.ExperimentsRestApi;
import tests.restapi.FeaturesRestApi;


public class AddConfigurationRuleToAnalyticsCheckedoutFeature {
	protected String seasonID;
	protected String productID;
	protected String featureID1;
	protected String filePath;
	protected String m_url;
	protected FeaturesRestApi f;
	protected AnalyticsRestApi an;
	private String sessionToken = "";
	protected AirlockUtils baseUtils;
	private BranchesRestApi br ;
	private ExperimentsRestApi exp ;
	private String experimentID;
	private String branchID;
	private String variantID;
	private String m_analyticsUrl;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		m_analyticsUrl = analyticsUrl;
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(m_url);
		an = new AnalyticsRestApi();
		an.setURL(analyticsUrl);
		br = new BranchesRestApi();
		br.setURL(m_url);
		exp = new ExperimentsRestApi();
		exp.setURL(analyticsUrl);
        
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
	}
	
	/*BUG: after the following scenario the branch crashed
		- Add sub feature or sub configRule to checked out feature in master.
    	- Report them to analytics
    	- Update branch analytics (by adding another feature to analytics in branch for example)
	 */
	
	@Test (description="Add components")
	public void addBranch() throws Exception{
		experimentID = baseUtils.addExperiment(m_analyticsUrl, false, false);
		Assert.assertFalse(experimentID.contains("error"), "Experiment was not created: " + experimentID);
		
		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		variantID = addVariant("variant1", "branch1");
		Assert.assertFalse(variantID.contains("error"), "Variant1 was not created: " + variantID);

		//enable experiment so a range will be created and the experiment will be published to analytics server
		String airlockExperiment = exp.getExperiment(experimentID, sessionToken);
		Assert.assertFalse(airlockExperiment.contains("error"), "Experiment was not found: " + experimentID);

		JSONObject expJson = new JSONObject(airlockExperiment);
		expJson.put("enabled", true);
		
		String response = exp.updateExperiment(experimentID, expJson.toString(), sessionToken);
		Assert.assertFalse(response.contains("error"), "Experiment was not updated: " + response);		

		String feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false); 
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
		br.checkoutFeature(branchID, featureID1, sessionToken);		
	}
	
	
	@Test (dependsOnMethods="addBranch", description="Add configuration rule in master and report it to analytics")
	public void addFeaturesToAnalyticsInMaster() throws IOException, JSONException, InterruptedException{

		String config = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		String configID1 = f.addFeature(seasonID, config, featureID1, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration1 was not added to the season" + configID1);

		String response = an.addFeatureToAnalytics(configID1, BranchesRestApi.MASTER, sessionToken);
		response = an.getGlobalDataCollection(seasonID, BranchesRestApi.MASTER, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

	}
	
	@Test (dependsOnMethods="addFeaturesToAnalyticsInMaster", description="Add configuration rule in branch and report it to analytics")
	public void addFeaturesToAnalyticsInBranch() throws IOException, JSONException, InterruptedException{

		String config = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = f.addFeatureToBranch(seasonID, branchID, config, featureID1, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration2 was not added to the season" + configID2);

		String response = an.addFeatureToAnalytics(configID2, branchID, sessionToken);
		response = an.getGlobalDataCollection(seasonID, branchID, "BASIC", sessionToken);
		Assert.assertTrue(featureOnOff(response).size()==1, "Incorrect number of featureOnOff in master");

		//check that branch didn't crash
		String feature = f.getFeatureFromBranch(featureID1, branchID, sessionToken);
		JSONObject fJson = new JSONObject(feature);
		Assert.assertTrue(fJson.getJSONArray("configurationRules").size()==1, "Incorrect number of children under featureID1");		

	}

	private String addVariant(String variantName, String branchName) throws IOException, JSONException{
		String variant = FileUtils.fileToString(filePath + "experiments/variant1.txt", "UTF-8", false);
		JSONObject variantJson = new JSONObject(variant);
		variantJson.put("name", variantName);
		variantJson.put("branchName", branchName);
		return exp.createVariant(experimentID, variantJson.toString(), sessionToken);

	}
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}
	
	private JSONArray featureOnOff(String analytics) throws JSONException{
		JSONObject json = new JSONObject(analytics);
		return json.getJSONObject("analyticsDataCollection").getJSONArray("featuresAndConfigurationsForAnalytics");
	}
	
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
