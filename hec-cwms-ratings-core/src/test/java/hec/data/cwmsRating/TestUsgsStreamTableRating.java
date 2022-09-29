/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import hec.data.RatingException;
import hec.data.cwmsRating.io.RatingValueContainer;


public class TestUsgsStreamTableRating
{

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
