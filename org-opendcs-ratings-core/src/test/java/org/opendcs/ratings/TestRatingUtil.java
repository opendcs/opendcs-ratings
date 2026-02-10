/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

import hec.heclib.util.HecTime;
import hec.heclib.util.HecTimeArray;
import hec.io.TimeSeriesContainer;
import org.junit.jupiter.api.Test;
import org.opendcs.ratings.io.IndependentValuesContainer;

import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestRatingUtil
{
	@Test
	public void testTscsToIvc() throws RatingException
	{
		int numberOfTscs = 3;
		int numberOfValues = 100;
		TimeSeriesContainer[] tscs = new TimeSeriesContainer[numberOfTscs];
		String[] units = new String[]{"m", "m", "m"};
		TimeZone tz = TimeZone.getTimeZone("UTC");
		for(int i = 0; i < 3; i++)
		{
			TimeSeriesContainer tsc = new TimeSeriesContainer();
			tsc.setUnits("m");
			tscs[i]  = tsc;
			HecTimeArray hecTimeArray = new HecTimeArray(numberOfValues);
			double[] values = new double[numberOfValues];
			HecTime hecTime = new HecTime("01 Jan 2021", "0100");
			for(int j = 0; j < numberOfValues; j++)
			{
				values[j] = j * 25;
				hecTimeArray.set(j, new HecTime(hecTime));
				hecTime.addHours(1);
			}
			tsc.set(values, hecTimeArray);
		}
		IndependentValuesContainer independentValuesContainer = RatingUtil.tscsToIvc(tscs, units, tz, false, false);
		assertEquals(numberOfTscs, independentValuesContainer.indVals[0].length);
		assertEquals(numberOfValues, independentValuesContainer.indVals.length);
		assertEquals(numberOfValues, independentValuesContainer.valTimes.length);
	}
}
