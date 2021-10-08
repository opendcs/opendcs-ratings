/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating;

import org.junit.Test;
import static org.junit.Assert.assertNotNull;

public class RatingSetTest
{
	String _xmlText = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
			"<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">"+
	  "<rating-template office-id=\"SWT\">  <parameters-id>Elev;Area</parameters-id>"+
	    "<version>Linear</version>"+
	
	    "<ind-parameter-specs>"+
	      "<ind-parameter-spec position=\"1\">"+
	        "<parameter>Elev</parameter>"+
	        "<in-range-method>LINEAR</in-range-method>"+
	        "<out-range-low-method>NULL</out-range-low-method>"+
	        "<out-range-high-method>NULL</out-range-high-method>"+
	      "</ind-parameter-spec>"+
	    "</ind-parameter-specs>"+
	    "<dep-parameter>Area</dep-parameter>"+
	    "<description>Reservoir Surface Area Rating</description>"+
	  "</rating-template>"+
	  "<rating-spec office-id=\"SWT\">"+
	    "<rating-spec-id>ELDR.Elev;Area.Linear.Production</rating-spec-id>"+
	    "<template-id>Elev;Area.Linear</template-id>"+
	    "<location-id>ELDR</location-id>"+
	    "<version>Production</version>"+
	    "<source-agency/>"+
	    "<in-range-method>LINEAR</in-range-method>"+
	    "<out-range-low-method>NEAREST</out-range-low-method>"+
	    "<out-range-high-method>NEAREST</out-range-high-method>"+
	    "<active>true</active>"+
	    "<auto-update>false</auto-update>"+
	    "<auto-activate>false</auto-activate>"+
	    "<auto-migrate-extension>false</auto-migrate-extension>"+
	    "<ind-rounding-specs>"+
	      "<ind-rounding-spec position=\"1\">4444444444</ind-rounding-spec>"+
	    "</ind-rounding-specs>"+
	    "<dep-rounding-spec>4444444444</dep-rounding-spec>"+
	    "<description>El Dorado Lake, KS Reservoir Surface Area Production Rating</description>"+
	  "</rating-spec>"+
	  "<rating office-id=\"SWT\">"+
	    "<rating-spec-id>ELDR.Elev;Area.Linear.Production</rating-spec-id>"+
	    "<vertical-datum-info unit=\"ft\">"+
	      "<native-datum>NAVD-88</native-datum>"+
	      "<elevation>1271</elevation>"+
	    "</vertical-datum-info>"+
	    "<units-id>ft;acre</units-id>"+
	    "<effective-date>2004-01-01T00:00:00-06:00</effective-date>"+
	    "<create-date>2012-11-03T12:26:16-05:00</create-date>"+
	    "<active>true</active>"+
	    "<description/>"+
	    "<rating-points>"+
	      "<point>"+
	        "<ind>1279</ind>"+
	        "<dep>0</dep>"+
	      "</point>"+
	      "<point>"+
	        "<ind>1280</ind>"+
	        "<dep>0</dep>"+
	      "</point>"+
	      	    "</rating-points>"+
	  "</rating>"+
	"</ratings>";
	String _xmlText1 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
	"<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"http://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">"+
	  "<rating-template office-id=\"SWT\">"+
	    "<parameters-id>%-DO-Saturation,%-DO-Saturation,%-DO-Saturation,%-DO-Saturation;%-DO-Saturation</parameters-id>"+
	    "<version>Standard</version>"+
	    "<ind-parameter-specs>"+
	      "<ind-parameter-spec position=\"1\">"+
	        "<parameter>%-DO-Saturation</parameter>"+
	        "<in-range-method>ERROR</in-range-method>"+
	       "<out-range-low-method>ERROR</out-range-low-method>"+
	        "<out-range-high-method>NULL</out-range-high-method>"+
	      "</ind-parameter-spec>"+
	      "<ind-parameter-spec position=\"2\">"+
	        "<parameter>%-DO-Saturation</parameter>"+
	       "<in-range-method>NULL</in-range-method>"+
	       "<out-range-low-method>NULL</out-range-low-method>"+
	        "<out-range-high-method>NULL</out-range-high-method>"+
	     " </ind-parameter-spec>"+
	      "<ind-parameter-spec position=\"3\">"+
	        "<parameter>%-DO-Saturation</parameter>"+
	        "<in-range-method>NULL</in-range-method>"+
	        "<out-range-low-method>NULL</out-range-low-method>"+
	        "<out-range-high-method>NULL</out-range-high-method>"+
	      "</ind-parameter-spec>"+
	      "<ind-parameter-spec position=\"4\">"+
	        "<parameter>%-DO-Saturation</parameter>"+
	        "<in-range-method>NULL</in-range-method>"+
	        "<out-range-low-method>NULL</out-range-low-method>"+
	        "<out-range-high-method>NULL</out-range-high-method>"+
	      "</ind-parameter-spec>"+
	    "</ind-parameter-specs>"+
	    "<dep-parameter>%-DO-Saturation</dep-parameter>"+
	    "<description>22</description>"+
	 " </rating-template>"+
	  "<rating-spec office-id=\"SWT\">"+
	    "<rating-spec-id>ALTU.%-DO-Saturation,%-DO-Saturation,%-DO-Saturation,%-DO-Saturation;%-DO-Saturation.Standard.Production1</rating-spec-id>"+
	    "<template-id>%-DO-Saturation,%-DO-Saturation,%-DO-Saturation,%-DO-Saturation;%-DO-Saturation.Standard</template-id>"+
	    "<location-id>ALTU</location-id>"+
	    "<version>Production1</version>"+
	    "<source-agency/>"+
	    "<in-range-method>LINEAR</in-range-method>"+
	    "<out-range-low-method>NULL</out-range-low-method>"+
	    "<out-range-high-method>LINEAR</out-range-high-method>"+
	    "<active>false</active>"+
	    "<auto-update>false</auto-update>"+
	    "<auto-activate>false</auto-activate>"+
	    "<auto-migrate-extension>false</auto-migrate-extension>"+
	    "<ind-rounding-specs>"+
	      "<ind-rounding-spec position=\"1\">2222233332</ind-rounding-spec>"+
	      "<ind-rounding-spec position=\"2\">2222233332</ind-rounding-spec>"+
	      "<ind-rounding-spec position=\"3\">2222233332</ind-rounding-spec>"+
	      "<ind-rounding-spec position=\"4\">2222233332</ind-rounding-spec>"+
	    "</ind-rounding-specs>"+
	    "<dep-rounding-spec>2222233332</dep-rounding-spec>"+
	    "<description>1111</description>"+
	 " </rating-spec>"+
	  "<rating office-id=\"SWT\">"+
	    "<rating-spec-id>ALTU.%-DO-Saturation,%-DO-Saturation,%-DO-Saturation,%-DO-Saturation;%-DO-Saturation.Standard.Production1</rating-spec-id>"+
	    "<units-id>%,%,%,%;%</units-id>"+
	    "<effective-date>2014-03-11T18:13:00-05:00</effective-date>"+
	    "<create-date>2014-03-11T18:13:00-05:00</create-date>"+
	    "<active>true</active>"+
	    "<description>1111</description>"+
	    "<rating-points>"+
	      "<other-ind position=\"1\" value=\"0\"/>"+
	      "<other-ind position=\"2\" value=\"0\"/>"+
	      "<other-ind position=\"3\" value=\"0\"/>"+
	      "<point>"+
	        "<ind>0</ind>"+
	        "<dep>0</dep>"+
	      "</point>"+
	      "<point>"+
	        "<ind>1</ind>"+
	        "<dep>2</dep>"+
	     " </point>"+
	    "</rating-points>"+
	  "</rating>"+
	"</ratings>";
	
	@Test
	public void testRatingSetFromXml() throws Exception
	{
			RatingSet.fromXml(_xmlText);
			RatingSet ratingSet = RatingSet.fromXml(_xmlText1);
			AbstractRating[] absRatings = ratingSet.getRatings();
			
			int iCount = absRatings[0].getIndParamCount();
			RatingValue[] tRating = ((TableRating)absRatings[0]).values;
			// this should not be null because we have 4 ind parameters. There should be a dep Table
			assertNotNull(tRating[0].depTable);
			return;
	}

}
