package net.whydah.identity.admin.usertoken;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

public class ProgressReportTest {

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
