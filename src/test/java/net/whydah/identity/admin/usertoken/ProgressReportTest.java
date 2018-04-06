package net.whydah.identity.admin.usertoken;

import net.whydah.sso.config.ApplicationMode;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.whydah.identity.admin.usertoken.UserStressTest.setEnv;

public class ProgressReportTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
		Map<String, String> addToEnv = new HashMap<>();
		addToEnv.put(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
		setEnv(addToEnv);
		System.setProperty(ApplicationMode.IAM_MODE_KEY, ApplicationMode.DEV);
    }

	@Test
	public void testReportForFun(){
		List<String> newList = new ArrayList<String>();
		for(int i=0;i<=1500;i++){
			newList.add("user " + i);
		}
		
		double lastPercentReported = 0;
		double workingPercentForEachRow = (double) 100/newList.size();
		double currentPercent = 0;
		
		for(String s : newList){
			currentPercent += workingPercentForEachRow;
			//do me now
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			
			}
			
			if ((currentPercent >= 1 && lastPercentReported==0)||
					(currentPercent - lastPercentReported >= 1)) {
				lastPercentReported = currentPercent;
				
				System.out.print("Report " + currentPercent);
			}

		}
		
	}
}
