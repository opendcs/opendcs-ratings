/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

/**
 * 
 */
package org.opendcs.ratings;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.TimeZone;

import hec.data.Parameter;

import org.opendcs.ratings.io.IndependentValuesContainer;
import hec.heclib.util.HecTime;
import hec.io.Conversion;
import hec.io.TimeSeriesContainer;
import hec.util.TextUtil;

import static org.opendcs.ratings.RatingConst.SEPARATOR1;
import static org.opendcs.ratings.RatingConst.SEPARATOR2;

/**
 *
 * @author Mike Perryman
 */
public class TimeSeriesRater {
	
	protected IRating ratingObj;
	protected boolean allowUnsafe = true;
	protected boolean warnUnsafe  = true;
	
	protected TimeSeriesRater() {}
	
	public TimeSeriesRater(IRating ratingObj) {
		this.ratingObj = ratingObj;
	}
	
	public TimeSeriesRater(IRating ratingObj, boolean allowUnsafe, boolean warnUnsafe) {
		this.ratingObj   = ratingObj;
		this.allowUnsafe = allowUnsafe;
		this.warnUnsafe  = warnUnsafe;
	}

	public TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
		synchronized(this) {
			String[] dataUnits = ratingObj.getDataUnits();
			String[] newDataUnits = new String[dataUnits.length];
			String[] ratingUnits = ratingObj.getRatingUnits();
			String[] params = ratingObj.getRatingParameters();
			int ratedInterval = tscs[0].interval;
			try {
				if (tscs.length != ratingObj.getIndParamCount()) {
					throw new RatingException(String.format("%d data sets specified, %d required.", tscs.length, ratingObj.getIndParamCount()));
				}
				//------------------------//
				// validate the intervals //
				//------------------------//
				for (int i = 1; i < tscs.length; ++i) {
					if (tscs[i].interval != tscs[0].interval) {
						String msg = "TimeSeriesContainers have inconsistent intervals.";
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) AbstractRating.logger.warning(msg + "  Rated values will be irregular interval.");
						ratedInterval = 0;
						break;
					}
				}
				//--------------------------------------//
				// validate the time zones if specified //
				//--------------------------------------//
				String tzid = tscs[0].timeZoneID;
				for (int i = 1; i < tscs.length; ++i) {
					if (!TextUtil.equals(tscs[i].timeZoneID, tzid)) {
						String msg = "TimeSeriesContainers have inconsistent time zones.";
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) AbstractRating.logger.warning(msg + "  Value times will be treated as UTC.");
						tzid = null;
						break;
					}
				}
				TimeZone tz = null;
				if (tzid != null) {
					tz = TimeZone.getTimeZone(tzid);
					if (!tz.getID().equals(tzid)) {
						String msg = String.format("TimeSeriesContainers have invalid time zone \"%s\".", tzid);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) AbstractRating.logger.warning(msg + "  Value times will be treated as UTC.");
						tz = null;
					}
				}
				//-------------------------//
				// validate the parameters //
				//-------------------------//
				for (int i = 0; i < tscs.length; ++i) {
					Parameter tscParam = null;
					try {
						tscParam = new Parameter(tscs[i].parameter);
					}
					catch (Throwable t) {
						if (!allowUnsafe) throw new RatingException(t);
						if (warnUnsafe) AbstractRating.logger.warning(t.getMessage());
					}
					if (tscParam != null) {
						if (!tscParam.getParameter().equals(params[i])) {
							String msg = String.format("Parameter \"%s\" does not match rating parameter \"%s\".", tscParam.getParameter(), params[i]);
							if (!allowUnsafe) throw new RatingException(msg);
							if (warnUnsafe) AbstractRating.logger.warning(msg);
						}
					}
					newDataUnits[i] = tscs[i].units;
				}
				//-------------------------//
				// finally - do the rating //
				//-------------------------//
				newDataUnits[tscs.length] = dataUnits[tscs.length];
				ratingObj.setDataUnits(newDataUnits);
				IndependentValuesContainer ivc = RatingUtil.tscsToIvc(tscs, ratingUnits, tz, allowUnsafe, warnUnsafe);
				double[] depVals = ratingObj.rate(ivc.valTimes, ivc.indVals);
				ratingObj.setDataUnits(dataUnits);
				//-----------------------------------------//
				// construct the rated TimeSeriesContainer //
				//-----------------------------------------//
				TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
				tscs[0].clone(ratedTsc);
				ratedTsc.interval = ratedInterval;
				if (ivc.valTimes.length == tscs[0].times.length) {
					ratedTsc.times = Arrays.copyOf(tscs[0].times, tscs[0].times.length);
				}
				else {
					ratedTsc.times = new int[ivc.valTimes.length];
					if (tz == null) {
						for (int i = 0; i < ivc.valTimes.length; ++i) {
							ratedTsc.times[i] = Conversion.toMinutes(ivc.valTimes[i]);
						}
					}
					else {
						Calendar cal = Calendar.getInstance();
						cal.setTimeZone(tz);
						SimpleDateFormat sdf = new SimpleDateFormat("ddMMMyyyy, HH:mm");
						HecTime t = new HecTime();
						for (int i = 0; i < ivc.valTimes.length; ++i) {
							cal.setTimeInMillis(ivc.valTimes[i]);
							t.set(sdf.format(cal.getTime()));
							ratedTsc.times[i] = t.value();
						}
					}
				}
				ratedTsc.values = depVals;
				ratedTsc.numberValues = ratedTsc.times.length;
				String paramStr = params[params.length-1];
				if (tscs[0].subParameter == null) {
					ratedTsc.fullName = TextUtil.replaceAll(tscs[0].fullName, tscs[0].parameter, paramStr, "IL");
				}
				else {
					ratedTsc.fullName = TextUtil.replaceAll(tscs[0].fullName, String.format("%s-%s", tscs[0].parameter, tscs[0].subParameter), paramStr, "IL");
				}
				String[] parts = TextUtil.split(paramStr, "-", "L", 2);
				ratedTsc.parameter = parts[0];
				ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
				ratedTsc.units = dataUnits[tscs.length];
				return ratedTsc;
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException) t;
				throw new RatingException(t);
			}
		}
	}
	
	public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
		synchronized(this) {
			TimeZone tz = null;
			if (tsc.timeZoneID != null) {
				tz = TimeZone.getTimeZone(tsc.timeZoneID);
				if (!tz.getID().equals(tsc.timeZoneID)) {
					String msg = String.format("TimeSeriesContainers have invalid time zone \"%s\".", tsc.timeZoneID);
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) AbstractRating.logger.warning(msg + "  Value times will be treated as UTC.");
					tz = null;
				}
			}
			TimeSeriesContainer[] tscs = {tsc};
			String[] units = {tsc.units};
			IndependentValuesContainer ivc = RatingUtil.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
			TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
			tsc.clone(ratedTsc);
			double[] depVals = new double[ivc.indVals.length];
			for (int i = 0; i < depVals.length; ++i) depVals[i] = ivc.indVals[i][0];
			ratedTsc.values = ratingObj.reverseRate(ivc.valTimes, depVals);
			String paramStr = TextUtil.split(TextUtil.split(ratingObj.getName(), SEPARATOR1, "L")[1], SEPARATOR2, "L")[1];
			if (tsc.subParameter == null) {
				ratedTsc.fullName = TextUtil.replaceAll(tsc.fullName, tsc.parameter, paramStr, "IL");
			}
			else {
				ratedTsc.fullName = TextUtil.replaceAll(tsc.fullName, String.format("%s-%s", tsc.parameter, tsc.subParameter), paramStr, "IL");
			}
			String[] parts = TextUtil.split(paramStr, "-", "L", 2);
			ratedTsc.parameter = parts[0];
			ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
			String[] dataUnits = ratingObj.getDataUnits();
			ratedTsc.units = dataUnits == null ? ratingObj.getRatingUnits()[0] : dataUnits[0];
			return ratedTsc;
		}
	}
	
}
