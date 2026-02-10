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
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.io.RatingValueContainer;
import org.opendcs.ratings.io.TableRatingContainer;
import org.opendcs.ratings.io.UsgsStreamTableRatingContainer;
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

class UsgsStreamTableRatingContainerXmlTest {

    private UsgsStreamTableRatingContainer usgsStreamTableRatingContainer;

    @BeforeEach
    public void setup() throws IOException, RatingException {
        try (InputStream inputStream = getClass().getResourceAsStream("usgs_stream_table_rating.xml")) {
            Assertions.assertNotNull(inputStream);
            try (InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                 Stream<String> stream = bufferedReader.lines()) {
                String text = stream.collect(Collectors.joining("\n"));
                usgsStreamTableRatingContainer = new UsgsStreamTableRatingContainer(text);
            }
        }
    }

    @Test
    void testXmlSerialization() throws RatingException {
        String xml = usgsStreamTableRatingContainer.toXml("");
        UsgsStreamTableRatingContainer newContainer = new UsgsStreamTableRatingContainer(xml);
        assertEquals(usgsStreamTableRatingContainer, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlDomSerialization() throws RatingException {
        String xml = usgsStreamTableRatingContainer.toXml("");
        Element element = (Element) org.opendcs.ratings.XmlUtil.textToElement(xml).getElementsByTagName("usgs-stream-rating").item(0);
        UsgsStreamTableRatingContainer newContainer = new UsgsStreamTableRatingContainer(element);
        assertEquals(usgsStreamTableRatingContainer, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlDeserialization() {
        assertEquals("AARK.Stage;Flow.Linear.Production", usgsStreamTableRatingContainer.ratingSpecId);
        assertEquals("SWT", usgsStreamTableRatingContainer.officeId);
        assertEquals("ft;cfs", usgsStreamTableRatingContainer.unitsId);
        assertTrue(usgsStreamTableRatingContainer.active);
        assertEquals(Instant.parse("2019-07-06T18:45:00Z"), Instant.ofEpochMilli(usgsStreamTableRatingContainer.effectiveDateMillis));
        assertEquals(NumericalConstants.UNDEFINED_INSTANT, Instant.ofEpochMilli(usgsStreamTableRatingContainer.transitionStartDateMillis));
        assertEquals(Instant.parse("2019-07-05T18:45:00Z"), Instant.ofEpochMilli(usgsStreamTableRatingContainer.createDateMillis));
        assertNull(usgsStreamTableRatingContainer.description);
        assertNull(usgsStreamTableRatingContainer.getVerticalDatumContainer());

        assertEquals(4, usgsStreamTableRatingContainer.values.length);
        RatingValueContainer value = usgsStreamTableRatingContainer.values[0];
        assertEquals(0.0, value.depValue);
        assertNull(value.note);
        assertEquals(0.0, value.indValue);
        assertNull(value.depTable);

        RatingValueContainer extensionValue = usgsStreamTableRatingContainer.extensionValues[0];
        assertEquals(0, extensionValue.depValue);
        assertEquals(0, extensionValue.indValue);

        RatingValueContainer offsetValue = usgsStreamTableRatingContainer.offsets.values[0];
        assertEquals(0, offsetValue.depValue);
        assertEquals(1, offsetValue.indValue);

        TableRatingContainer shifts = (TableRatingContainer) usgsStreamTableRatingContainer.shifts.abstractRatingContainers[0];
        assertEquals("AARK.Stage;Stage-shift.Linear.Production", shifts.ratingSpecId);
        assertNull(shifts.officeId);
        assertEquals("ft;ft", shifts.unitsId);
        assertTrue(shifts.active);
        assertEquals(Instant.parse("2019-07-06T22:13:00Z"), Instant.ofEpochMilli(shifts.effectiveDateMillis));
        assertEquals(NumericalConstants.UNDEFINED_INSTANT, Instant.ofEpochMilli(shifts.transitionStartDateMillis));
        assertEquals(Instant.parse("2019-07-06T22:13:00Z"), Instant.ofEpochMilli(shifts.createDateMillis));
        assertNull(shifts.description);

        assertEquals(3, shifts.values.length);
        RatingValueContainer shiftValue = shifts.values[1];
        assertEquals(0.460251046, shiftValue.depValue);
        assertNull(shiftValue.note);
        assertEquals(0.464965197, shiftValue.indValue);
        assertNull(shiftValue.depTable);

        assertNull(usgsStreamTableRatingContainer.inRangeMethod);
        assertNull(usgsStreamTableRatingContainer.outRangeLowMethod);
        assertNull(usgsStreamTableRatingContainer.outRangeHighMethod);
    }
}
