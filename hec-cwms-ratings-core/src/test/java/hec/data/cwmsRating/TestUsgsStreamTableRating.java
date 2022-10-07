/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating;

import static org.junit.jupiter.api.Assertions.*;

import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import hec.data.RatingException;
import hec.data.cwmsRating.io.RatingValueContainer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;


public class TestUsgsStreamTableRating
{

	private static UsgsStreamTableRating _rating;

	@BeforeAll
	public static void setup() throws RatingException {
		long millis = ZonedDateTime.of(2022, 10, 6, 16, 0, 0, 0, ZoneId.of("UTC")).toInstant().toEpochMilli();
		UsgsStreamTableRatingContainer urc = new UsgsStreamTableRatingContainer();
		urc.active = true;
		urc.createDateMillis = millis;
		urc.description = "unit testing";
		urc.effectiveDateMillis = millis;
		urc.officeId = "SWT";
		urc.ratingSpecId = "Test.Stage;Flow.Test.Test";
		urc.unitsId = "ft;cfs";
		urc.inRangeMethod = RatingConst.RatingMethod.LINEAR.toString();
		urc.outRangeHighMethod = RatingConst.RatingMethod.LINEAR.toString();
		urc.outRangeLowMethod = RatingConst.RatingMethod.LINEAR.toString();
		RatingValueContainer v1 = new RatingValueContainer();
		v1.indValue = 0;
		v1.depValue = 1;
		RatingValueContainer v2 = new RatingValueContainer();
		v2.indValue = 100;
		v2.depValue = 1_000;
		RatingValueContainer v3 = new RatingValueContainer();
		v3.indValue = 1_000;
		v3.depValue = 1_000_000;
		urc.values = new RatingValueContainer[]{v1, v2, v3};
		_rating = new UsgsStreamTableRating(urc);
	}

	@ParameterizedTest
	@EnumSource(value = RatingConst.RatingMethod.class, names = {"NULL", "NEAREST", "PREVIOUS", "NEXT", "LOWER", "HIGHER", "CLOSEST", "LIN_LOG", "LOG_LIN", "LINEAR", "LOGARITHMIC"})
	public void testReverseRateValidInRange(RatingConst.RatingMethod method) {
		_rating.inRangeMethod = method;
		assertDoesNotThrow(() -> _rating.reverseRate(1.0));
		assertDoesNotThrow(() -> _rating.reverseRate(5.0));
		assertDoesNotThrow(() -> _rating.reverseRate(100_000));
		assertDoesNotThrow(() -> _rating.reverseRate(999_999));
		assertDoesNotThrow(() -> _rating.reverseRate(1_000_000));
	}

	@ParameterizedTest
	@EnumSource(value = RatingConst.RatingMethod.class, names = {"ERROR"})
	public void testReverseRateInvalidInRange(RatingConst.RatingMethod method) {
		_rating.inRangeMethod = method;
		assertThrows(RatingException.class, () -> _rating.reverseRate(1.0));
		assertThrows(RatingException.class, () -> _rating.reverseRate(5.0));
		assertThrows(RatingException.class, () -> _rating.reverseRate(100_000));
		assertThrows(RatingException.class, () -> _rating.reverseRate(999_999));
		assertThrows(RatingException.class, () -> _rating.reverseRate(1_000_000));
	}

	@ParameterizedTest
	@EnumSource(value = RatingConst.RatingMethod.class, names = {"NULL", "NEAREST", "PREVIOUS", "LOWER", "CLOSEST", "LIN_LOG", "LOG_LIN", "LINEAR", "LOGARITHMIC"})
	public void testReverseRateValidOutOfRangeHigh(RatingConst.RatingMethod method) {
		_rating.outRangeHighMethod = method;
		assertDoesNotThrow(() -> _rating.reverseRate(1_000_001));
		assertDoesNotThrow(() -> _rating.reverseRate(10_000_000));
	}

	@ParameterizedTest
	@EnumSource(value = RatingConst.RatingMethod.class, names = {"ERROR", "NEXT", "HIGHER"})
	public void testReverseRateInvalidOutOfRangeHigh(RatingConst.RatingMethod method) {
		_rating.outRangeHighMethod = method;
		assertThrows(RatingException.class, () -> _rating.reverseRate(1_000_001));
		assertThrows(RatingException.class, () -> _rating.reverseRate(10_000_000));
	}

	@ParameterizedTest
	@EnumSource(value = RatingConst.RatingMethod.class, names = {"NULL", "NEAREST", "NEXT", "HIGHER", "CLOSEST", "LIN_LOG", "LOG_LIN", "LINEAR", "LOGARITHMIC"})
	public void testReverseRateValidOutOfRangeLow(RatingConst.RatingMethod method) {
		_rating.outRangeLowMethod = method;
		assertDoesNotThrow(() -> _rating.reverseRate(-1));
		assertDoesNotThrow(() -> _rating.reverseRate(-500));
	}

	@ParameterizedTest
	@EnumSource(value = RatingConst.RatingMethod.class, names = {"ERROR", "PREVIOUS", "LOWER"})
	public void testReverseRateInvalidOutOfRangeLow(RatingConst.RatingMethod method) {
		_rating.outRangeLowMethod = method;
		assertThrows(RatingException.class, () -> _rating.reverseRate(-1));
		assertThrows(RatingException.class, () -> _rating.reverseRate(-500));
	}

    @Disabled
	@Test
	public final void testStreamRatingLessThanMinExtent() throws Exception
	{
		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long time = cal.getTimeInMillis();
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());
		AbstractRating rating = ratings[0];
		double[][] ratingExtents = rating.getRatingExtents(time);
		double[] ratingExtent = ratingExtents[0];
		logger.info(ratingExtent[0] + "  " + ratingExtent[1]);
		logger.info("rating at: " + (ratingExtent[0] - 10.0));
		Assertions.assertThrows(RatingException.class, () -> { // out of range - low
			double rateOne = testRatingSet.rateOne(time, ratingExtent[0] - 10.0);
		});

	}

    @Disabled
	@Test
	public final void testStreamRatingGreaterThanMaxExtent() throws Exception
	{
		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long time = cal.getTimeInMillis();
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());
		AbstractRating rating = ratings[0];
		double[][] ratingExtents = rating.getRatingExtents(time);
		double[] ratingExtent = ratingExtents[ratingExtents.length - 1];
		logger.info(ratingExtent[0] + "  " + ratingExtent[1]);
		logger.info("rating at: " + (ratingExtent[0] + 10.0));
		assertThrows( RatingException.class, () ->{ // out of range - high
			double rateOne = testRatingSet.rateOne(time, ratingExtent[0] + 10.0);
		});

	}

    @Disabled
	@Test
	public final void testStreamRatingOnMinExtent() throws Exception
	{
		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long time = cal.getTimeInMillis();
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());
		AbstractRating rating = ratings[0];
		double[][] ratingExtents = rating.getRatingExtents(time);
		double[] ratingExtent = ratingExtents[0];
		logger.info(ratingExtent[0] + "  " + ratingExtent[1]);
		double rateOne = testRatingSet.rateOne(time, ratingExtent[0]);
		assertEquals(ratingExtent[1], rateOne, 0.001);
	}

    @Disabled
	@Test
	public final void testStreamRatingOnMaxExtent() throws Exception
	{
		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long time = cal.getTimeInMillis();
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());
		AbstractRating rating = ratings[0];
		double[][] ratingExtents = rating.getRatingExtents(time);
		double[] ratingExtent = ratingExtents[ratingExtents.length - 1];
		logger.info(ratingExtent[0] + "  " + ratingExtent[1]);
		double rateOne = testRatingSet.rateOne(time, ratingExtent[0]);
		assertEquals(ratingExtent[1], rateOne, 0.001);
	}

    @Disabled
	@Test
	public final void testStreamRatingInMiddleOfExtents() throws Exception
	{
		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long time = cal.getTimeInMillis();
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());
		AbstractRating rating = ratings[0];
		double[][] ratingExtents = rating.getRatingExtents(time);
		double[] minRatingExtent = ratingExtents[0];
		double[] maxRatingExtent = ratingExtents[1];
		double midIndep = (minRatingExtent[0] + maxRatingExtent[0]) / 2.0;
		double midDep = (minRatingExtent[1] + maxRatingExtent[1]) / 2.0;
		logger.info(midIndep + "  " + midDep);
		double rateOne = testRatingSet.rateOne(time, midIndep);
		UsgsStreamTableRating streamTableRating = (UsgsStreamTableRating) rating;
		RatingSet shifts = streamTableRating.getShifts();


	}

    @Disabled
	@Test
	public final void testCreatingShifts() throws Exception
	{
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());

		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012-noshifts.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		AbstractRating rating = ratings[0];
		UsgsStreamTableRating streamTableRating = (UsgsStreamTableRating) rating;
		RatingSet shifts = streamTableRating.getShifts();
		assertNull(shifts);
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(2014, 9, 1);
		Date shiftDate = calendar.getTime();

		List<RatingValueContainer> stageShiftValues = new ArrayList<>();
		RatingValueContainer rvc = new RatingValueContainer();
		rvc.indValue = 15.0;
		rvc.depValue = -1.0;
		stageShiftValues.add(rvc);
		rvc = new RatingValueContainer();
		rvc.indValue = 17.0;
		rvc.depValue = 1.0;
		stageShiftValues.add(rvc);

		boolean shiftActive = true;
		TableRating newShift = streamTableRating.addShift(shiftDate, stageShiftValues, shiftActive);
		assertNotNull(newShift);

		calendar.add(Calendar.MONTH, 1);
		shiftDate = calendar.getTime();

		shiftActive = false;
		TableRating newShift2 = streamTableRating.addShift(shiftDate, stageShiftValues, shiftActive);
		assertNotNull(newShift2);

		AbstractRatingSet shiftsRatingSet = (AbstractRatingSet) streamTableRating.getShifts();
		assertTrue(!shiftsRatingSet.activeRatings.isEmpty());

		double rateOne = streamTableRating.rateOne(System.currentTimeMillis(),16.0);

	}

    @Disabled
	@Test
	public final void testRateOneManyShifts() throws Exception
	{
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());

		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012-manyshifts.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		AbstractRating rating = ratings[0];
		UsgsStreamTableRating streamTableRating = (UsgsStreamTableRating) rating;
		long time = 1415230649194L;//System.currentTimeMillis();
		double[][] ratingExtents = rating.getRatingExtents(time);
		double[] ratingExtent = ratingExtents[0];
		logger.info(ratingExtent[0] + "  " + ratingExtent[1]);
		double rateOne = testRatingSet.rateOne(time, ratingExtent[0]);
		logger.info(streamTableRating.toXmlString("  ", 1));

		//add a shift and rate again.
		Calendar calendar = Calendar.getInstance();
		calendar.clear();
		calendar.set(2014, 11, 1);
		Date shiftDate = calendar.getTime();

		List<RatingValueContainer> stageShiftValues = new ArrayList<>();
		RatingValueContainer rvc = new RatingValueContainer();
		rvc.indValue = 15.0;
		rvc.depValue = -1.0;
		stageShiftValues.add(rvc);
		rvc = new RatingValueContainer();
		rvc.indValue = 17.0;
		rvc.depValue = 1.0;
		stageShiftValues.add(rvc);

		boolean shiftActive = true;
		TableRating newShift = streamTableRating.addShift(shiftDate, stageShiftValues, shiftActive);

		ratingExtents = rating.getRatingExtents(shiftDate.getTime());
		ratingExtent = ratingExtents[0];
		logger.info(ratingExtent[0] + "  " + ratingExtents[1][0]);
		rateOne = testRatingSet.rateOne(time, ((ratingExtent[0] + ratingExtents[1][0]) / 2));
		logger.info(streamTableRating.toXmlString("  ", 1));
	}

    @Disabled
	@Test
	public final void testStreamRatingNonMonotonicallyIncreasing() throws Exception
	{
		// Retrieve complete RatingSet from local xml file
		String path = "C:/temp/92F_Stage_Flow_BASE_PRODUCTION-2012-not-monotonically-increasing.xml";
		File f = new File(path);
		BufferedReader br = new BufferedReader(new FileReader(f));
		String line;
		StringBuilder sb = new StringBuilder();
		while ((line = br.readLine()) != null)
		{
			sb.append(line.trim());
		}
		RatingSet testRatingSet = RatingSet.fromXml(sb.toString());
		AbstractRating[] ratings = testRatingSet.getRatings();
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		long time = cal.getTimeInMillis();
		Logger logger = Logger.getLogger(TestUsgsStreamTableRating.class.getName());
		AbstractRating rating = ratings[0];
		double[][] ratingExtents = rating.getRatingExtents(time);
		double[] ratingExtent = ratingExtents[ratingExtents.length - 1];
		logger.info(ratingExtent[0] + "  " + ratingExtent[1]);
		logger.info("rating at: " + (ratingExtent[0] + 10.0));
		Assertions.assertThrows( RatingException.class, () -> { // out of range - high
			double rateOne = testRatingSet.rateOne(time, ratingExtent[0] + 10.0);
		});

	}

}
