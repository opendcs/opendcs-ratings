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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.junit.jupiter.api.Assertions;
import org.opendcs.ratings.io.RatingValueContainer;
import org.opendcs.ratings.io.TableRatingContainer;
import org.opendcs.ratings.RatingException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mil.army.usace.hec.metadata.constants.NumericalConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Element;

class TableRatingContainerXmlTest {

    private TableRatingContainer tableRatingContainer;
    private TableRatingContainer multiIndParamTableRatingContainer;

    @BeforeEach
    public void setup() throws IOException, RatingException {
        try (InputStream inputStream = getClass().getResourceAsStream("table_rating.xml")) {
            Assertions.assertNotNull(inputStream);
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                 Stream<String> stream = bufferedReader.lines()) {
                String text = stream.collect(Collectors.joining("\n"));
                tableRatingContainer = new TableRatingContainer(text);
            }
        }
        try (InputStream inputStream = getClass().getResourceAsStream("table_rating_multi_ind_param.xml")) {
            Assertions.assertNotNull(inputStream);
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                 Stream<String> stream = bufferedReader.lines()) {
                String text = stream.collect(Collectors.joining("\n"));
                multiIndParamTableRatingContainer = new TableRatingContainer(text);
            }
        }
    }

    @Test
    void testXmlSerialization() throws RatingException {
        String xml = tableRatingContainer.toXml("");
        TableRatingContainer newContainer = new TableRatingContainer(xml);
        assertEquals(tableRatingContainer, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlDomSerialization() throws RatingException {
        String xml = tableRatingContainer.toXml("");
        Element element = (Element) RatingXmlUtil.textToElement(xml).getElementsByTagName("simple-rating").item(0);
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
        assertNull(tableRatingContainer.description);
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
