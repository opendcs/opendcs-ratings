/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package org.opendcs.ratings.io.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;


import org.opendcs.ratings.AbstractRatingSet;
import org.opendcs.ratings.RatingException;
import org.opendcs.ratings.RatingSet;
import org.opendcs.ratings.RatingValue;
import org.opendcs.ratings.TableRating;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory;
import org.jooq.impl.DSL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import usace.cwms.db.jooq.codegen.packages.CWMS_ENV_PACKAGE;

final class JdbcTableRatingTest extends CwmsDockerIntegrationTest {

    private AbstractRatingSet tableRatingSet;
    private AbstractRatingSet usgsTableRatingSet;

    @BeforeEach
    public void setup() throws Exception {
        String tableRating = "table_rating.xml";
        tableRatingSet = storeRating(tableRating);
        String usgsTableRating = "usgs_stream_table_rating.xml";
        usgsTableRatingSet = storeRating(usgsTableRating);
    }

    private AbstractRatingSet storeRating(String usgsTableRating) throws IOException, RatingException, SQLException
    {
        try(InputStream inputStream = getClass().getResourceAsStream(usgsTableRating);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            Stream<String> stream = bufferedReader.lines())
        {
            String text = stream.collect(Collectors.joining("\n")).replace("\"NAB\"", "\"" + getInstance().getOfficeId() + "\"");
            AbstractRatingSet ratingSet = RatingXmlFactory.ratingSet(text);
            getInstance().connection(conn ->
            {
                try
                {
                    CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                    RatingJdbcFactory.store(ratingSet, conn, true, true);
                }
                catch(RatingException e)
                {
                    throw new RuntimeException(e);
                }
            });
            return ratingSet;
        }
    }

    @Test
    public void testEagerRatingSetRetrieval() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.EAGER, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertEquals(tableRatingSet, abstractRatingSet);
                assertEquals(1, tableRatingSet.getRatings().length);
                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testEagerRatingSetRetrievalBackwardsCompatible() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.EAGER, conn,
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertEquals(tableRatingSet, abstractRatingSet);
                assertEquals(1, tableRatingSet.getRatings().length);
                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testLazyRatingSetRetrievalGetConcreteRatings() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.LAZY, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(tableRatingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof LazyJdbcRatingSet);
                assertEquals(1, abstractRatingSet.getRatings().length);
                TableRating rating = (TableRating) abstractRatingSet.getRatings()[0];
                RatingValue[] ratingValues = rating.getRatingValues();
                assertNull(ratingValues);
                abstractRatingSet.getConcreteRatings(conn);
                rating = (TableRating) abstractRatingSet.getRatings()[0];
                ratingValues = rating.getRatingValues();
                assertEquals(126, ratingValues.length);
                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testLazyRatingSetRetrievalGetConcreteRatingsBackwardsCompatible() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.LAZY, conn,
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(tableRatingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof LazyJdbcRatingSet);
                assertEquals(1, abstractRatingSet.getRatings().length);
                TableRating rating = (TableRating) abstractRatingSet.getRatings()[0];
                RatingValue[] ratingValues = rating.getRatingValues();
                assertNull(ratingValues);
                abstractRatingSet.getConcreteRatings(conn);
                rating = (TableRating) abstractRatingSet.getRatings()[0];
                ratingValues = rating.getRatingValues();
                assertEquals(126, ratingValues.length);
                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testLazyRatingSetRetrieval() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.LAZY, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(tableRatingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof LazyJdbcRatingSet);
                assertEquals(12.0, abstractRatingSet.rate(conn, new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(conn, new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testLazyRatingSetRetrievalBackwardsCompatible() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.LAZY, conn,
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(tableRatingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof LazyJdbcRatingSet);
                assertEquals(12.0, abstractRatingSet.rate(conn, new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(conn, new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testReferenceRatingSetRetrieval() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.REFERENCE, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(tableRatingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof ReferenceJdbcRatingSet);
                assertEquals(0, abstractRatingSet.getRatings().length);
                abstractRatingSet.setDataUnits(conn, new String[]{"ft", "acre"});
                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testReferenceRatingSetRetrievalBackwardsCompatible() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.REFERENCE, conn,
                    getInstance().getOfficeId(), tableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(tableRatingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof ReferenceJdbcRatingSet);
                assertEquals(0, abstractRatingSet.getRatings().length);

                abstractRatingSet.setDataUnits(conn, new String[]{"ft", "acre"});
                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testLazyRatingSetReverseRate() throws SQLException {
        getInstance().connection(conn -> {
            try {
                CWMS_ENV_PACKAGE.call_SET_SESSION_OFFICE_ID(DSL.using(conn).configuration(), getInstance().getOfficeId());
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.LAZY, conn,
                        getInstance().getOfficeId(), usgsTableRatingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(usgsTableRatingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof LazyJdbcRatingSet);
                assertEquals(2.9, abstractRatingSet.rate(conn, new Date().getTime(), 1.5), 0.1);
                assertEquals(1.5, abstractRatingSet.reverseRate(conn, new Date().getTime(), 2.9), 0.1);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }
}
