/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package org.opendcs.ratings.io.xml;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.opendcs.ratings.io.AbstractRatingContainer;
import org.opendcs.ratings.io.VirtualRatingContainer;
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
import org.jdom.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class VirtualRatingContainerXmlTest {

    private VirtualRatingContainer virtual;

    @BeforeEach
    public void setup() throws IOException, RatingException {
        try (InputStream inputStream = getClass().getResourceAsStream("virtual_rating.xml");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             Stream<String> stream = bufferedReader.lines()) {
            String text = stream.collect(Collectors.joining("\n"));
            virtual = new VirtualRatingContainer(text);
        }
    }

    @Test
    void testXmlSerialization() throws RatingException {
        String xml = virtual.toXml("", 0);
        VirtualRatingContainer newContainer = new VirtualRatingContainer(xml);
        assertEquals(virtual, newContainer, "Serialized object should equal original when deserialized");
    }

    @Test
    void testXmlDeserialization() {
        assertEquals("FSMI.Stage,Speed-Water Index;Flow.Linear.Dummy", virtual.ratingSpecId);
        assertEquals("SWT", virtual.officeId);
        assertNull(virtual.unitsId);
        assertTrue(virtual.active);
        assertEquals(Instant.parse("1900-01-01T06:00:00Z"), Instant.ofEpochMilli(virtual.effectiveDateMillis));
        assertEquals(NumericalConstants.UNDEFINED_INSTANT, Instant.ofEpochMilli(virtual.transitionStartDateMillis));
        assertEquals(Instant.parse("2017-04-06T14:54:00Z"), Instant.ofEpochMilli(virtual.createDateMillis));
        assertEquals("", virtual.description);
        assertNull(virtual.getVerticalDatumContainer());

        assertEquals(2, virtual.sourceRatings.length);
        assertEquals("R2I2=R1D", virtual.connections);
        assertArrayEquals(new String[]{"SWT/FSMI.Stage;Flow.EXSA.PRODUCTION {ft;cfs}", "SWT/I1 * 0.0 + I2 {mph,cfs;cfs}"}, virtual.sourceRatingIds);
    }
}
