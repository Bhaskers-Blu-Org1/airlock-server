package tests.restapi.in_app_purchases;

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
import tests.restapi.BranchesRestApi;
import tests.restapi.InAppPurchasesRestApi;

//TODO: extend test to include purchaseOptions

public class CheckoutPurchaseScenario8 {
	protected String productID;
	protected String seasonID;
	private String branchID;
	private String inAppPurchaseID1;
	private String inAppPurchaseID2;
	private String configID2;
	private String configID1;
	private String mixConfigID;
	private String mixID1;
	private JSONObject inAppPurJson;
	private JSONObject purOptJson;
	protected String filePath;
	protected String m_url;
	private String sessionToken = "";
	private AirlockUtils baseUtils;
	private BranchesRestApi br ;
	protected InAppPurchasesRestApi purchasesApi;
	
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		m_url = url;
		filePath = configPath ;
		purchasesApi = new InAppPurchasesRestApi();
		purchasesApi.setURL(m_url);
		br = new BranchesRestApi();
		br.setURL(m_url);

		baseUtils = new AirlockUtils(m_url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;

		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		
		String inAppPur = FileUtils.fileToString(filePath + "purchases/inAppPurchase1.txt", "UTF-8", false);
		inAppPurJson = new JSONObject(inAppPur);
		
		String purOpt = FileUtils.fileToString(filePath + "purchases/purchaseOptions1.txt", "UTF-8", false);
		purOptJson = new JSONObject(purOpt);		
	}
	

	
	@Test (description ="IAP1 -> MIX->IAP2+(MIXCR-> CR1+CR2), checkout IAP2 ") 
	public void scenario8 () throws Exception {

		branchID = addBranch("branch1");
		Assert.assertFalse(branchID.contains("error"), "Branch1 was not created: " + branchID);

		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID1 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), "ROOT", sessionToken);
		Assert.assertFalse(inAppPurchaseID1.contains("error"), "inAppPurchase was not added to the season: " + inAppPurchaseID1);

		String iapMix = FileUtils.fileToString(filePath + "purchases/inAppPurchaseMutual.txt", "UTF-8", false);
		mixID1 = purchasesApi.addPurchaseItem(seasonID, iapMix, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(mixID1.contains("error"), "iap mtx was not added to the season: " + mixID1);

		inAppPurJson.put("name", RandomStringUtils.randomAlphabetic(5));
		inAppPurchaseID2 = purchasesApi.addPurchaseItem(seasonID, inAppPurJson.toString(), mixID1, sessionToken);
		Assert.assertFalse(inAppPurchaseID2.contains("error"), "mtx was not added to the season: " + inAppPurchaseID2);
			
		String configurationMix = FileUtils.fileToString(filePath + "configuration_feature-mutual.txt", "UTF-8", false);
		mixConfigID = purchasesApi.addPurchaseItem(seasonID, configurationMix, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(mixConfigID.contains("error"), "Configuration mix was not added to the season");

		String configuration1 = FileUtils.fileToString(filePath + "configuration_rule1.txt", "UTF-8", false);
		configID1 = purchasesApi.addPurchaseItem(seasonID, configuration1, mixConfigID, sessionToken);
		Assert.assertFalse(configID1.contains("error"), "Configuration rule1 was not added to the season");

		String configuration2 = FileUtils.fileToString(filePath + "configuration_rule2.txt", "UTF-8", false);
		configID2 = purchasesApi.addPurchaseItem(seasonID, configuration2, mixConfigID, sessionToken);
		Assert.assertFalse(configID2.contains("error"), "Configuration rule2 was not added to the season");

		String response = br.checkoutFeature(branchID, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(response.contains("error"), "inAppPurchase was not checked out to branch");
		
		//check that inAppPurchase was checked out
		response = br.getBranchWithFeatures(branchID, sessionToken);
		JSONObject brJson = new JSONObject(response);
		JSONArray inAppPurchases = brJson.getJSONArray("entitlements");
		
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);
		
		//IAP1		
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 status is not checked_out in get branch" );	//get branch
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 status is not checked_out in get inAppPurchases" );	//get branch
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase1 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch
		
		//MIX
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIX status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not checked_out in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch

		//IAP2
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not checked_out in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "Feature3 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch
		
		//MIXCR
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch

		//CR1
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "config1 status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "config1 status is not checked_out in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "config1 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch

		
		//CR2
		Assert.assertTrue(inAppPurchases.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "config2 status is not checked_out in get branch" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("CHECKED_OUT"), "config2 status is not checked_out in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("CHECKED_OUT"), "config2 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch

	}
	
	@Test (dependsOnMethods="scenario8", description="Uncheck IAP1")
	public void uncheckIAP1() throws JSONException, Exception{				
		
		//uncheckout IAP1
		String res = br.cancelCheckoutFeature(branchID, inAppPurchaseID1, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject inAppPurchaseFromBranch = new JSONObject( purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID1, branchID, sessionToken));
		//IAP1
		Assert.assertTrue(inAppPurchaseFromBranch.getString("branchStatus").equals("NONE"), "Incorrect inAppPurchase1 status in get inAppPurchase from branch");
		

		//first mix
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIX1 status is not checked_out" );
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchFeatureParentName").equals(inAppPurchaseFromBranch.getString("namespace")+"."+inAppPurchaseFromBranch.getString("name")), "MIX1 status is not checked_out" );
		//IAP2
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not checked_out" );
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "inAppPurchase2 status is not CHECKED_OUT in get inAppPurchases" );

		//MIXCR
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIXCR status is not checked_out in get branch" );

	}	

	@Test (dependsOnMethods="uncheckIAP1", description="Uncheck IAP2")
	public void uncheckIAP2() throws JSONException, Exception{				
		
		//uncheckout IAP2
		String res = br.cancelCheckoutFeature(branchID, inAppPurchaseID2, sessionToken);
		Assert.assertFalse(res.contains("error"), "Feature was not unchecked out: " + res);
		JSONObject brJson = new JSONObject(br.getBranchWithFeatures(branchID, sessionToken));
		
		
		JSONArray inAppPurchasesInBranch = purchasesApi.getPurchasesBySeasonFromBranch(seasonID, branchID, sessionToken);		
		JSONObject inAppPurchaseFromBranch = new JSONObject( purchasesApi.getPurchaseItemFromBranch(inAppPurchaseID2, branchID, sessionToken));
		
		//IAP2
		Assert.assertTrue(inAppPurchaseFromBranch.getString("branchStatus").equals("NONE"), "Incorrect inAppPurchase2 status in get inAppPurchase from branch");
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "inAppPurchase2 status is not NONE in get inAppPurchases" );
		

		//first mix
		Assert.assertTrue(brJson.getJSONArray("entitlements").getJSONObject(0)
				.getString("branchStatus").equals("CHECKED_OUT"), "MIX1 status is not checked_out" );

		//MIXCR

		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "MIXCR status is not NONE in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(mixConfigID, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "MIXCR status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch

		//CR1
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getString("branchStatus").equals("NONE"), "config1 status is not NONE in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID1, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "CR1 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch

		
		//CR2
		Assert.assertTrue(inAppPurchasesInBranch.getJSONObject(0).getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("entitlements").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(0)
				.getJSONArray("configurationRules").getJSONObject(1)
				.getString("branchStatus").equals("NONE"), "config2 status is not NONE in get inAppPurchases" );
		Assert.assertTrue(new JSONObject(purchasesApi.getPurchaseItemFromBranch(configID2, branchID, sessionToken)).getString("branchStatus").equals("NONE"), "CR2 status is not checked_out in get inAppPurchase");	//get inAppPurchase from branch

	}	
	
	private String addBranch(String branchName) throws JSONException, IOException{
		String branch = FileUtils.fileToString(filePath + "experiments/branch1.txt", "UTF-8", false);
		JSONObject branchJson = new JSONObject(branch);
		branchJson.put("name", branchName);
		return br.createBranch(seasonID, branchJson.toString(), BranchesRestApi.MASTER, sessionToken);

	}

	@AfterTest
	private void reset(){
		baseUtils.reset(productID, sessionToken);
	}
}
