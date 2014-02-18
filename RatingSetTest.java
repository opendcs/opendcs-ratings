package hec.data.cwmsRating;

import static hec.data.Parameter.PARAMID_AREA;
import static hec.data.Parameter.PARAMID_AREABASIN;
import static hec.data.Parameter.PARAMID_AREAIMPACTED;
import static hec.data.Parameter.PARAMID_AREAKA;
import static hec.data.Parameter.PARAMID_AREARESERVOIR;
import static hec.data.Parameter.PARAMID_AREASURFACE;
import static hec.data.Parameter.PARAMID_ATI;
import static hec.data.Parameter.PARAMID_BOTTOM_LENGTH;
import static hec.data.Parameter.PARAMID_COLDRATE;
import static hec.data.Parameter.PARAMID_CONC;
import static hec.data.Parameter.PARAMID_COSTKAF;
import static hec.data.Parameter.PARAMID_COUNT;
import static hec.data.Parameter.PARAMID_CURENCY;
import static hec.data.Parameter.PARAMID_DENSITY;
import static hec.data.Parameter.PARAMID_DEPOSITION;
import static hec.data.Parameter.PARAMID_DEPTH;
import static hec.data.Parameter.PARAMID_DISTANCE;
import static hec.data.Parameter.PARAMID_ELEV;
import static hec.data.Parameter.PARAMID_ENERGY;
import static hec.data.Parameter.PARAMID_EVAP;
import static hec.data.Parameter.PARAMID_EVAPRATE;
import static hec.data.Parameter.PARAMID_EXTINCTION;
import static hec.data.Parameter.PARAMID_FLOW;
import static hec.data.Parameter.PARAMID_FLOWKAF;
import static hec.data.Parameter.PARAMID_FLOWPERAREA;
import static hec.data.Parameter.PARAMID_GROWTH_RATE;
import static hec.data.Parameter.PARAMID_LENGTH;
import static hec.data.Parameter.PARAMID_LOSSDEFICIT;
import static hec.data.Parameter.PARAMID_LOSSRATE;
import static hec.data.Parameter.PARAMID_MELTRATE;
import static hec.data.Parameter.PARAMID_OPENING;
import static hec.data.Parameter.PARAMID_PENALTY;
import static hec.data.Parameter.PARAMID_PERC;
import static hec.data.Parameter.PARAMID_PERCENT;
import static hec.data.Parameter.PARAMID_PERCRATE;
import static hec.data.Parameter.PARAMID_POWER;
import static hec.data.Parameter.PARAMID_PRECIP;
import static hec.data.Parameter.PARAMID_PRESSURE;
import static hec.data.Parameter.PARAMID_RATE_OF_RISE;
import static hec.data.Parameter.PARAMID_SALINITY;
import static hec.data.Parameter.PARAMID_SEDIMENT_DISCHARGE;
import static hec.data.Parameter.PARAMID_SED_LOAD;
import static hec.data.Parameter.PARAMID_SED_SIZE;
import static hec.data.Parameter.PARAMID_SHADE_FRAC;
import static hec.data.Parameter.PARAMID_SPEED;
import static hec.data.Parameter.PARAMID_SPINRATE;
import static hec.data.Parameter.PARAMID_STAGE;
import static hec.data.Parameter.PARAMID_STOR;
import static hec.data.Parameter.PARAMID_STORKAF;
import static hec.data.Parameter.PARAMID_SUSP_SOLIDS;
import static hec.data.Parameter.PARAMID_TEMP;
import static hec.data.Parameter.PARAMID_THICKNESS;
import static hec.data.Parameter.PARAMID_TIMING;
import static hec.data.Parameter.PARAMID_TIMING_OFFSET;
import static hec.data.Parameter.PARAMID_TIMING_PERIOD;
import static hec.data.Parameter.PARAMID_TRAVEL;
import static hec.data.Parameter.PARAMID_VOLUME;
import static hec.data.Parameter.PARAMID_WIDTH;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

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
	     
	  "  </rating-points>"+
	  "</rating>"+
	"</ratings>";
	@Test
	public void testRatingSetFromXml() throws Exception
	{
			RatingSet.fromXml(_xmlText);
	}

}
