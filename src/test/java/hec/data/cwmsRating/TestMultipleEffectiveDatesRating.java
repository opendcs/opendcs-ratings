package hec.data.cwmsRating;

import hec.data.RatingException;
import hec.data.cwmsRating.io.RatingValueContainer;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
public class TestMultipleEffectiveDatesRating
{

    @Disabled
	@Test
	public final void test() throws Exception
	{

		//This unit test rates based on two different RatingSets, but using the exact same values
		//The first RatingSet has a single effective date, but the second has two effective dates
		//The second RatingSet fails because the RatingSet sets the lastUsedRating to null (line 896),
		//which leads to the second valueSet to be ignored (line 784)

		// Retrieve complete RatingSet from local xml file with one effective date
		String path = "C:/Users/caleb.RMANET/Desktop/AlumCr_%-Sluice_Elev_Flow_Standard_I25_OneEffectiveDate.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null) {
			sb.append(line.trim());
		}
		RatingSet ratingSetOneEffectiveDate = RatingSet.fromXml(sb.toString());
		br.close();

		// Retrieve complete RatingSet from local xml file with two effective dates
		path = "C:/Users/caleb.RMANET/Desktop/AlumCr_%-Sluice_Elev_Flow_Standard_I25_TwoEffectiveDates.xml";
		f = new File(path);
		br = new BufferedReader(new FileReader(f));
		sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet ratingSetTwoEffectiveDates = RatingSet.fromXml(sb.toString());
		br.close();

		//rating data
		double[][] values = new double[2][2];
		values[0][0] = 0.;values[0][1] = 269.882112744141;
		values[1][0] = 50.; values[1][1] = 269.882112744141;
		long[] times = new long[]{1388692800000l, 1388692800000l};

		//set the units
		ratingSetOneEffectiveDate.setDataUnits(new String[]{"%","m","cms"});
		ratingSetTwoEffectiveDates.setDataUnits(new String[]{"%","m","cms"});

		//rate the data based on the two rating sets
		double[] ratedValuesOneEffectiveDate = ratingSetOneEffectiveDate.rate(values, times);
		double[] ratedValuesTwoEffectiveDates = ratingSetTwoEffectiveDates.rate(values, times);

		assertEquals(ratedValuesOneEffectiveDate[0], ratedValuesTwoEffectiveDates[0], "First value should be equal" );

		//this is where the test will fail
		assertEquals( ratedValuesOneEffectiveDate[1], ratedValuesTwoEffectiveDates[1], "Second value should be equal");
	}

}
