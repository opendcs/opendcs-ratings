/*
 * Copyright (c) 2022
 * United States Army Corps of Engineers - Hydrologic Engineering Center (USACE/HEC)
 * All Rights Reserved.  USACE PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 */

package mil.army.usace.hec.cwms.rating.io.jdbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import hec.data.RatingException;
import hec.data.cwmsRating.AbstractRatingSet;
import hec.data.cwmsRating.RatingSet;
import hec.data.cwmsRating.RatingValue;
import hec.data.cwmsRating.TableRating;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Date;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

final class JdbcTableRatingTest extends CwmsDockerIntegrationTest {

    private AbstractRatingSet ratingSet;

    @BeforeEach
    public void setup() throws Exception {
        try (InputStream inputStream = getClass().getResourceAsStream("table_rating.xml");
             InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
             Stream<String> stream = bufferedReader.lines()) {
            String text = stream.collect(Collectors.joining("\n"));
            ratingSet = RatingXmlFactory.ratingSet(text);
            getInstance().connection(conn -> {
                try {
                    RatingJdbcFactory.store(ratingSet, conn, true, true);
                } catch (RatingException e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    public void testEagerRatingSetRetrieval() throws SQLException {
        getInstance().connection(conn -> {
            try {
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.EAGER, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertEquals(ratingSet, abstractRatingSet);
                assertEquals(1, ratingSet.getRatings().length);
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
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.EAGER, conn,
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertEquals(ratingSet, abstractRatingSet);
                assertEquals(1, ratingSet.getRatings().length);
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
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.LAZY, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(ratingSet, abstractRatingSet);
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
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.LAZY, conn,
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(ratingSet, abstractRatingSet);
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
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.LAZY, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(ratingSet, abstractRatingSet);
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
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.LAZY, conn,
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(ratingSet, abstractRatingSet);
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
                AbstractRatingSet abstractRatingSet = RatingJdbcFactory.ratingSet(RatingSet.DatabaseLoadMethod.REFERENCE, new TransientConnectionProvider(conn),
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(ratingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof ReferenceJdbcRatingSet);
                assertEquals(0, abstractRatingSet.getRatings().length);
                //The following assertions are currently failing due to CWDB-194
//                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
//                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Test
    public void testReferenceRatingSetRetrievalBackwardsCompatible() throws SQLException {
        getInstance().connection(conn -> {
            try {
                RatingSet abstractRatingSet = RatingSet.fromDatabase(RatingSet.DatabaseLoadMethod.REFERENCE, conn,
                    getInstance().getOfficeId(), ratingSet.getRatingSpec().getRatingSpecId(), null, null, true);
                assertNotEquals(ratingSet, abstractRatingSet);
                assertTrue(abstractRatingSet instanceof ReferenceJdbcRatingSet);
                assertEquals(0, abstractRatingSet.getRatings().length);

                //The following assertions are currently failing due to CWDB-194
//                assertEquals(12.0, abstractRatingSet.rate(new Date().getTime(), 392.0), 0.0);
//                assertEquals(392.0, abstractRatingSet.reverseRate(new Date().getTime(), 12.0), 0.0);
            } catch (RatingException e) {
                throw new RuntimeException(e);
            }
        });
    }


}
