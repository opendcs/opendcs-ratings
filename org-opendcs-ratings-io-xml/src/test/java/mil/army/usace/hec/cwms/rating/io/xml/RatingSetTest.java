/*
* Where Applicable, Copyright 2026 OpenDCS Consortium and/or its contributors
* 
* Licensed under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License. You may obtain a copy
* of the License at
* 
*   http://www.apache.org/licenses/LICENSE-2.0
* 
* Unless required by applicable law or agreed to in writing, software 
* distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
* License for the specific language governing permissions and limitations 
* under the License.
*/
package org.opendcs.ratings.io.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.AbstractRating;
import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.RatingSet;
import org.opendcs.ratings.RatingValue;
import org.opendcs.ratings.TableRating;
import hec.util.TextUtil;


import org.junit.jupiter.api.Test;

public class RatingSetTest
{
	private static final String XML_TEXT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
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
	private static final String XML_TEXT_1 = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"+
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

	private static final String XML_WITH_NO_POINTS = "<ratings xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:noNamespaceSchemaLocation=\"https://www.hec.usace.army.mil/xmlSchema/cwms/Ratings.xsd\">\n" +
		"  <rating-template office-id=\"SWT\">\n" +
		"    <parameters-id>Elev;Area</parameters-id>\n" +
		"    <version>Workshop</version>\n" +
		"    <ind-parameter-specs>\n" +
		"      <ind-parameter-spec position=\"1\">\n" +
		"        <parameter>Elev</parameter>\n" +
		"        <in-range-method>LINEAR</in-range-method>\n" +
		"        <out-range-low-method>HIGHER</out-range-low-method>\n" +
		"        <out-range-high-method>LINEAR</out-range-high-method>\n" +
		"      </ind-parameter-spec>\n" +
		"    </ind-parameter-specs>\n" +
		"    <dep-parameter>Area</dep-parameter>\n" +
		"    <description>demo only</description>\n" +
		"  </rating-template>\n" +
		"  <rating-spec office-id=\"SWT\">\n" +
		"    <rating-spec-id>COPA.Elev;Area.Workshop.Production</rating-spec-id>\n" +
		"    <template-id>Elev;Area.Workshop</template-id>\n" +
		"    <location-id>COPA</location-id>\n" +
		"    <version>Production</version>\n" +
		"    <source-agency>CESWT</source-agency>\n" +
		"    <in-range-method>LINEAR</in-range-method>\n" +
		"    <out-range-low-method>NULL</out-range-low-method>\n" +
		"    <out-range-high-method>LINEAR</out-range-high-method>\n" +
		"    <active>true</active>\n" +
		"    <auto-update>false</auto-update>\n" +
		"    <auto-activate>false</auto-activate>\n" +
		"    <auto-migrate-extension>false</auto-migrate-extension>\n" +
		"    <ind-rounding-specs>\n" +
		"      <ind-rounding-spec position=\"1\">2222256782</ind-rounding-spec>\n" +
		"    </ind-rounding-specs>\n" +
		"    <dep-rounding-spec>2222233332</dep-rounding-spec>\n" +
		"    <description>test rating</description>\n" +
		"  </rating-spec>\n" +
		"  <simple-rating office-id=\"SWT\">\n" +
		"    <rating-spec-id>COPA.Elev;Area.Workshop.Production</rating-spec-id>\n" +
		"    <units-id>ft;acre</units-id>\n" +
		"    <effective-date>2014-11-18T19:22:00Z</effective-date>\n" +
		"    <create-date>2014-11-18T19:23:00Z</create-date>\n" +
		"    <active>true</active>\n" +
		"    <description>test rating</description>\n" +
		"    <rating-points/>\n" +
		"  </simple-rating>\n" +
		"</ratings>";

	@Test
	public void testRatingSetFromXml() throws Exception {
		RatingSet.fromXml(XML_TEXT);
		RatingSet ratingSet = RatingSet.fromXml(XML_TEXT_1);
		AbstractRating[] absRatings = ratingSet.getRatings();

		int iCount = absRatings[0].getIndParamCount();
		RatingValue[] tRating = ((TableRating)absRatings[0]).getRatingValues();
		// this should not be null because we have 4 ind parameters. There should be a dep Table
		assertNotNull(tRating[0].getDepValues(),"Table not read");
		return;
	}

	@Test
	public void testRatingSetFromXmlCompressed() throws Exception {
		String base64 = TextUtil.compress(XML_TEXT, "base64");
		AbstractRatingSet abstractRatingSet = RatingXmlFactory.ratingSet(base64);
		AbstractRatingSet fromUncompressed = RatingXmlFactory.ratingSet(XML_TEXT);
		assertEquals(abstractRatingSet, fromUncompressed);
	}

	@Test
	public void testRatingSetFromInvalidXml() throws Exception {
		String base64 = TextUtil.compress(XML_TEXT, "base64");
		assertThrows(RatingException.class, () -> RatingXmlFactory.ratingSet(base64 + "b"));
	}

	@Test
	public void testTableRatingXmlNoPoints() throws Exception {
		AbstractRating rating = RatingXmlFactory.ratingSet(XML_WITH_NO_POINTS).getRatings()[0];
		RatingValue[] ratingValues = ((TableRating) rating).getRatingValues();
		assertNull(ratingValues);
	}

}
