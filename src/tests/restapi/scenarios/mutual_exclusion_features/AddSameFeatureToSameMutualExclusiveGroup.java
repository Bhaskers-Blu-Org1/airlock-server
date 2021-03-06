package tests.restapi.scenarios.mutual_exclusion_features;

import java.io.IOException;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import tests.com.ibm.qautils.FileUtils;
import tests.restapi.AirlockUtils;
import tests.restapi.FeaturesRestApi;
import tests.restapi.ProductsRestApi;

public class AddSameFeatureToSameMutualExclusiveGroup {
		   
	protected String productID;
	protected String seasonID;
	protected String featureID1;
	protected String featureID2;
	protected FeaturesRestApi f;
	protected ProductsRestApi p;
	private AirlockUtils baseUtils;
	protected String feature;
	protected List<Integer> actualResult;
	protected String childID1;
	protected String childID2;
	protected String filePath;
	private String sessionToken = "";

	/*
	 * This test validates that the same features can't be added to the same mutually exclusive group
	 */
	
	@BeforeClass
	@Parameters({"url", "analyticsUrl", "translationsUrl", "configPath", "sessionToken", "userName", "userPassword", "appName", "productsToDeleteFile"})
	public void init(String url, String analyticsUrl, String translationsUrl, String configPath, String sToken, String userName, String userPassword, String appName, String productsToDeleteFile) throws Exception{
		p = new ProductsRestApi();
		p.setURL(url);
		baseUtils = new AirlockUtils(url, analyticsUrl, translationsUrl, configPath, sToken, userName, userPassword, appName, productsToDeleteFile);
		sessionToken = baseUtils.sessionToken;
		productID = baseUtils.createProduct();
		baseUtils.printProductToFile(productID);
		seasonID = baseUtils.createSeason(productID);
		filePath = configPath;
		f = new FeaturesRestApi();
		f.setURL(url);
		feature = FileUtils.fileToString(configPath + "feature-mutual.txt", "UTF-8", false);
		
		featureID1 = f.addFeature(seasonID, feature, "ROOT", sessionToken);
	}
	


 //add the same child to the same parent
	   @Test(description = "add the same sub-feature twice to the same MIX group")
	    public void addFeature() throws IOException, InterruptedException {
		   feature = FileUtils.fileToString(filePath + "feature1.txt", "UTF-8", false);
		   childID1 = f.addFeature(seasonID, feature, featureID1, sessionToken);
		   childID2 = f.addFeature(seasonID, feature, featureID1, sessionToken);
		   Assert.assertTrue(childID2.contains("error"), "Test should fail, but instead passed: " + childID2 );
	    }
	   
	   @AfterTest
	   public void validate(){
		   baseUtils.reset(productID, sessionToken);
	   }

}
