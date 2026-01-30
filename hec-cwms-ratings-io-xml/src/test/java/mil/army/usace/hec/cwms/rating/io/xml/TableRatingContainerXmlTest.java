/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import hec.data.cwmsRating.io.RatingValueContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.cwmsRating.RatingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mil.army.usace.hec.metadata.constants.NumericalConstants;
import org.jdom.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TableRatingContainerXmlTest {

    private TableRatingContainer tableRatingContainer;
    private TableRatingContainer multiIndParamTableRatingContainer;

    @BeforeEach
    public void setup() throws IOException, RatingException {
        try (InputStream inputStream = getClass().getResourceAsStream("table_rating.xml");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             Stream<String> stream = bufferedReader.lines()) {
            String text = stream.collect(Collectors.joining("\n"));
            tableRatingContainer = new TableRatingContainer(text);
        }
        try (InputStream inputStream = getClass().getResourceAsStream("table_rating_multi_ind_param.xml");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             Stream<String> stream = bufferedReader.lines()) {
            String text = stream.collect(Collectors.joining("\n"));
            multiIndParamTableRatingContainer = new TableRatingContainer(text);
        }
    }

    @Test
    void testXmlSerialization() throws RatingException {
        String xml = tableRatingContainer.toXml("");
        TableRatingContainer newContainer = new TableRatingContainer(xml);
        assertEquals(tableRatingContainer, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlJDomSerialization() throws RatingException {
        String xml = tableRatingContainer.toXml("");
        Element element = RatingXmlUtil.textToJdomElement(xml).getChild("simple-rating");
        TableRatingContainer newContainer = new TableRatingContainer(element);
        assertEquals(tableRatingContainer, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlDeserialization() {
        assertEquals("ACTT2.Elev;Area.Standard.Production", tableRatingContainer.ratingSpecId);
        assertEquals("NAB", tableRatingContainer.officeId);
        assertEquals("ft;acre", tableRatingContainer.unitsId);
        assertTrue(tableRatingContainer.active);
        assertEquals(Instant.parse("2017-09-26T20:06:00Z"), Instant.ofEpochMilli(tableRatingContainer.effectiveDateMillis));
        assertEquals(Instant.parse("2017-09-28T20:06:00Z"), Instant.ofEpochMilli(tableRatingContainer.transitionStartDateMillis));
        assertEquals(Instant.parse("2017-09-26T20:06:00Z"), Instant.ofEpochMilli(tableRatingContainer.createDateMillis));
        assertEquals("", tableRatingContainer.description);
        assertNull(tableRatingContainer.getVerticalDatumContainer());

        assertEquals(126, tableRatingContainer.values.length);
        RatingValueContainer value = tableRatingContainer.values[0];
        assertEquals(0.0, value.depValue);
        assertEquals("test", value.note);
        assertEquals(370.0, value.indValue);
        assertNull(value.depTable);
        assertNull(tableRatingContainer.extensionValues);
        assertNull(tableRatingContainer.inRangeMethod);
        assertNull(tableRatingContainer.outRangeLowMethod);
        assertNull(tableRatingContainer.outRangeHighMethod);
    }

    @Test
    void testXmlDeserializationMultiIndParam() {
        assertEquals("DEZI.Elev,Opening;Flow.Standard.Production", multiIndParamTableRatingContainer.ratingSpecId);
        assertEquals("NAB", multiIndParamTableRatingContainer.officeId);
        assertEquals("ft,ft;cfs", multiIndParamTableRatingContainer.unitsId);
        assertTrue(multiIndParamTableRatingContainer.active);
        assertEquals(Instant.parse("2018-04-17T23:02:00Z"), Instant.ofEpochMilli(multiIndParamTableRatingContainer.effectiveDateMillis));
        assertEquals(NumericalConstants.UNDEFINED_INSTANT, Instant.ofEpochMilli(multiIndParamTableRatingContainer.transitionStartDateMillis));
        assertEquals(Instant.parse("2018-04-17T23:02:00Z"), Instant.ofEpochMilli(multiIndParamTableRatingContainer.createDateMillis));
        assertEquals("testing", multiIndParamTableRatingContainer.description);
        assertNull(multiIndParamTableRatingContainer.getVerticalDatumContainer());

        assertEquals(2, multiIndParamTableRatingContainer.values.length);
        RatingValueContainer value = multiIndParamTableRatingContainer.values[0];
        assertEquals(NumericalConstants.UNDEFINED_DOUBLE, value.depValue);
        assertNull(value.note);
        assertEquals(0, value.indValue);
        RatingValueContainer depVal = value.depTable.values[0];
        assertEquals(0, depVal.depValue);
        assertNull(value.note);
        assertEquals(0, depVal.indValue);
        assertNull(multiIndParamTableRatingContainer.extensionValues);
        assertNull(multiIndParamTableRatingContainer.inRangeMethod);
        assertNull(multiIndParamTableRatingContainer.outRangeLowMethod);
        assertNull(multiIndParamTableRatingContainer.outRangeHighMethod);
    }
}
