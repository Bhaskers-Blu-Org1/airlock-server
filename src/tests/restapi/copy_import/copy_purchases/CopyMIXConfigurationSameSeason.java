package tests.restapi.copy_import.copy_purchases;

import java.io.IOException;


import org.apache.wink.json4j.JSONException;
import org.apache.wink.json4j.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;

import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.BranchesRestApi;
import tests.restapi.FeaturesRestApi;
import tests.restapi.InAppPurchasesRestApi;
import tests.restapi.ProductsRestApi;

public class CopyMIXConfigurationSameSeason {
	private String seasonID;
	private String productID;
	private String entitlementID1;
	private String entitlementD2;
	private String configID;
	private String mixConfigID;
	private String filePath;
	private String m_url;
	private ProductsRestApi p;
	private FeaturesRestApi f;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private String srcBranchID;
	private InAppPurchasesRestApi purchasesApi;
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile", "runOnMaster"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile, Boolean runOnMaster) throws Exception{
		m_url = url;
		filePath = configPath;
		p = new ProductsRestApi();
		p.setURL(m_url);
		f = new FeaturesRestApi();
		f.setURL(m_url);
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
	    
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		try {
			if (runOnMaster) {
				srcBranchID = BranchesRestApi.MASTER;
			} else {
				srcBranchID = baseUtils.createBranchInExperiment(analyticsUrl);
			}
		}catch (Exception e){
			srcBranchID = null;
		}
	}
	
	/*
	MIX Config under entitlement - allowed
	MIX Config under config - allowed
	MIX Config under mix of configs - allowed
	MIX Config under root - not allowed
	MIX Config under mix of entitlements - not allowed
	 */
	
	@Test (description="Copy mix configuration under another entitlement in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderEntitlement() throws IOException, JSONException{
		String entitlement1 = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		entitlementID1 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement1, "ROOT", sessionToken);
		Assert.assertFalse(entitlementID1.contains("error"), "Entitlement was not added to the season");
		
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configurationMix, entitlementID1, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Entitlement mix was not added to the season");

		String configuration = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, mixConfigID, sessionToken);
		Assert.assertFalse(configID.contains("error"), "cr was not added to the season");
				
		String entitlement2 = FileUtils.fileToString(filePath + "purchases/inAppPurchase2.txt", "UTF-8", false);
		entitlementD2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlement2, "ROOT", sessionToken);
		Assert.assertFalse(entitlementD2.contains("error"), "cr was not added to the season");
		
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixConfigID, entitlementD2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Entitlement was copied with existing name ");

		response = f.copyItemBetweenBranches(mixConfigID, entitlementD2, "ACT", null, "suffix1", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Entitlement was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderEntitlement", description="Copy mix configuration under mix entitlement in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderMixEntitlement() throws IOException{
		String entitlementMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		String mixId = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, entitlementMix, entitlementD2, sessionToken);
		Assert.assertFalse(mixId.contains("error"), "Entitlement was not added to the season" + mixId);
						
		String response = f.copyItemBetweenBranches(mixConfigID, mixId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, mixId, "ACT", null, "suffix2", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under entitlementד mix ");
	}
	
	@Test (dependsOnMethods="copyConfigurationUnderMixEntitlement", description="Copy mix configuration under root in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderRoot() throws IOException{
		String rootId = purchasesApi.getBranchRootId(seasonID, srcBranchID, sessionToken);
		
		String response = f.copyItemBetweenBranches(mixConfigID, rootId, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied under root ");
		
		response = f.copyItemBetweenBranches(mixConfigID, rootId, "ACT", null, "suffix3", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("error"), "Configuraiton mix was copied under root");
	}
	
	
	@Test (dependsOnMethods="copyConfigurationUnderRoot", description="Copy mix configuration under itself in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderItself() throws IOException, JSONException{
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixConfigID, mixConfigID, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");

		response = f.copyItemBetweenBranches(mixConfigID, mixConfigID, "ACT", null, "suffix4", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		JSONObject oldEntitlement = new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken));
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), oldEntitlement.getJSONArray("configurationRules").getJSONObject(1)));


	}
	
	@Test (dependsOnMethods="copyConfigurationUnderItself", description="Copy mix configuration under configuration in the same entitlement in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copyConfigurationUnderConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		String configID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementID1, sessionToken);
		Assert.assertFalse(configID.contains("error"), "entitlement was not added to the season");
				
		//should fail copy without suffix
		String response = f.copyItemBetweenBranches(mixConfigID, configID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, configID2, "ACT", null, "suffix5", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuraiton mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));

	}

	@Test (dependsOnMethods="copyConfigurationUnderConfiguration", description="Copy mix configuration under mix configuration in the same season. First, copy without namesuffix, then copy with namesuffix")
	public void copySingleEntitlementUnderMixConfiguration() throws IOException, JSONException{
		String configuration = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		String mixConfigID2 = purchasesApi.addPurchaseItemToBranch(seasonID, srcBranchID, configuration, entitlementD2, sessionToken);
		Assert.assertFalse(mixConfigID2.contains("error"), "cr mix was not added to the season");
			
		String response = f.copyItemBetweenBranches(mixConfigID, mixConfigID2, "ACT", null, null, sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("illegalName"), "Configuraiton mix was copied with existing name ");
		
		response = f.copyItemBetweenBranches(mixConfigID, mixConfigID2, "ACT", null, "suffix6", sessionToken, srcBranchID, srcBranchID);
		Assert.assertTrue(response.contains("newSubTreeId"), "Configuration mix was not copied: " + response);
		
		JSONObject result = new JSONObject(response);
		String newEntitlement = purchasesApi.getPurchaseItemFromBranch(result.getString("newSubTreeId"), srcBranchID, sessionToken);
		String oldEntitlement = purchasesApi.getPurchaseItemFromBranch(mixConfigID, srcBranchID, sessionToken);
		Assert.assertTrue(f.jsonObjsAreEqual(new JSONObject(newEntitlement), new JSONObject(oldEntitlement)));
	}
	
	
	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}