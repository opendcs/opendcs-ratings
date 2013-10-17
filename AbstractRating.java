package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.lang.Const.UNDEFINED_LONG;
import static hec.lang.Const.UNDEFINED_TIME;
import static hec.util.TextUtil.replaceAll;
import static hec.util.TextUtil.split;
import hec.data.DataSetException;
import hec.data.Parameter;
import hec.data.RatingException;
import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.data.rating.IRatingSpecification;
import hec.data.rating.IRatingTemplate;
import hec.data.rating.JDomRatingSpecification;
import hec.heclib.util.HecTime;
import hec.hecmath.TimeSeriesMath;
import hec.io.Conversion;
import hec.io.TimeSeriesContainer;
import hec.lang.Observable;
import hec.util.TextUtil;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Observer;
import java.util.TimeZone;
import java.util.logging.Logger;

import rma.lang.Modifiable;

/**
 * Base class for all cwmsRating implementations
 *
 * @author Mike Perryman
 */
public abstract class AbstractRating implements Observer, ICwmsRating , Modifiable{

	protected static final Logger logger = Logger.getLogger(AbstractRating.class.getPackage().getName());

	/**
	 * Object that provides the Observable-by-composition functionality
	 */
	protected Observable observationTarget = null;
	/**
	 * The identifier of the office that owns the rating
	 */
	protected String officeId = null;
	/**
	 * The CWMS-style rating specification identifier
	 */
	protected String ratingSpecId = null;
	/**
	 * The CWMS-style units identifier for the native units of the rating
	 */
	protected String ratingUnitsId = null;
	/**
	 * A CWMS-style units identifier for the data using the rating
	 */
	protected String dataUnitsId = null;
	/**
	 * The earliest date/time at which the rating is considered to be in effect 
	 */
	protected long effectiveDate = UNDEFINED_LONG;
	/**
	 * The earliest date/time that the rating was available - the time it was first stored in the database
	 */
	protected long createDate = UNDEFINED_LONG;
	/**
	 * Flag specifying whether this rating should be used in the rate(), rateOne(), and reverseRate() methods.
	 */
	protected boolean active = false;
	/**
	 * Descriptive text about the rating
	 */
	protected String description = null;
	/**
	 * A time to associate with all values that don't specify their own times.  This time, along with the rating
	 * effective dates, is used to determine which ratings to use to rate values.
	 */
	protected long defaultValueTime = UNDEFINED_TIME;
	/**
	 * A time used to allow the rating of values with information that was known at a specific time. No ratings
	 * with a creation date after this time will be used to rate values.
	 */
	protected long ratingTime = Long.MAX_VALUE;
	/**
	 * Flag specifying whether this object allows "risky" operations such as using mismatched units, unknown parameters, etc.
	 */
	protected boolean allowUnsafe = true;
	/**
	 * Flag specifying whether this object outputs messages about "risky" operations such as using mismatched units, unknown parameters, etc.
	 */
	protected boolean warnUnsafe = true;
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#addObserver(java.util.Observer)
	 */
	@Override
	public void addObserver(Observer o) {
		observationTarget.addObserver(o);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#deleteObserver(java.util.Observer)
	 */
	@Override
	public void deleteObserver(Observer o) {
		observationTarget.deleteObserver(o);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getRatingSpecId()
	 */
	@Override
	public String getRatingSpecId() {
		return ratingSpecId;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getOfficeId()
	 */
	@Override
	public String getOfficeId() {
		return officeId;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setOfficeId(java.lang.String)
	 */
	@Override
	public void setOfficeId(String officeId) {
		this.officeId = officeId;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setRatingSpecId(java.lang.String)
	 */
	@Override
	public void setRatingSpecId(String ratingSpecId) {
		this.ratingSpecId = ratingSpecId;
		
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getUnitsId()
	 */
	@Override
	public String getRatingUnitsId() {
		return ratingUnitsId;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setUnitsId(java.lang.String)
	 */
	@Override
	public void setRatingUnitsId(String ratingUnitsId) {
		this.ratingUnitsId = ratingUnitsId;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getDataUnitsId()
	 */
	@Override
	public String getDataUnitsId() {
		return dataUnitsId;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setDataUnitsId(java.lang.String)
	 */
	@Override
	public void setDataUnitsId(String dataUnitsId) {
		this.dataUnitsId = dataUnitsId;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getName()
	 */
	@Override
	public String getName() {
		return ratingSpecId;
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) throws RatingException {
		String[] parts = split(name, SEPARATOR1, "L");
		if (parts.length != 4) {
			throw new RatingException(String.format("Invalid name: %s", name));
		}
		parts = split(parts[1], SEPARATOR2, "L");
		if (parts.length != 2) {
			throw new RatingException(String.format("Invalid name: %s", name));
		}
		String[] newIndParams = split(parts[0], SEPARATOR3, "L");
		if (newIndParams.length != getIndParamCount()) {
			throw new RatingException("Name has different number of independent parameters than rating");
		}
		ratingSpecId = name;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingParameters()
	 */
	@Override
	public String[] getRatingParameters() {
		String parametersId = split(ratingSpecId, SEPARATOR1, "L")[1];
		return split(replaceAll(parametersId, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L");
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingUnits()
	 */
	@Override
	public String[] getRatingUnits() {
		return split(replaceAll(ratingUnitsId, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L");
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getDataUnits()
	 */
	@Override
	public String[] getDataUnits() {
		return dataUnitsId == null ? getRatingUnits() : split(replaceAll(dataUnitsId, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L");
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setDataUnits(java.lang.String[])
	 */
	@Override
	public void setDataUnits(String[] units) throws RatingException {
		String[] ratingUnits = getRatingUnits();
		if (units.length != ratingUnits.length) {
			throw new RatingException("Invalid number of data units.");
		}
		Units unit = null;
		for (int i = 0; i < units.length; ++i) {
			try {
				unit = new Units(units[i]);
			}
			catch (Throwable t) {
				if (!allowUnsafe) throw new RatingException(t);
				if (warnUnsafe) logger.warning(t.getMessage());
				unit = null;
			}
			if (unit != null) {
				if (!units[i].equals(ratingUnits[i])) {
					if(!Units.canConvertBetweenUnits(units[i], ratingUnits[i])) {
						String msg = String.format("Cannot convert from \"%s\" to \"%s\".", units[i], ratingUnits[i]);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) {
							if (i == ratingUnits.length - 1) {
								logger.warning(msg + "  Rated values will be unconverted.");
							}
							else {
								logger.warning(msg + "  Rating will be performed using unconverted values.");
							}
						}
					}
				}
			}
		}
		
		StringBuilder sb = new StringBuilder(units[0]);
		for (int i = 1; i < units.length - 1; ++i) sb.append(",").append(units[i]);
		sb.append(";").append(units[units.length-1]);
		dataUnitsId = sb.toString();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getIndParamCount()
	 */
	@Override
	public int getIndParamCount() throws RatingException {
		return this.getRatingParameters().length - 1;
	}
	/**
	 * Retrieves whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesAllowUnsafe() {
		return allowUnsafe;
	}
	/**
	 * Sets whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param allowUnsafe A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setAllowUnsafe(boolean allowUnsafe) {
		this.allowUnsafe = allowUnsafe;
	}
	/**
	 * Retrieves whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesWarnUnsafe() {
		return warnUnsafe;
	}
	/**
	 * Sets whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param warnUnsafe  A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setWarnUnsafe(boolean warnUnsafe) {
		this.warnUnsafe = warnUnsafe;
	}
	/**
	 * Retrieves the effective date of the rating. The effective date is the earliest date/time for which the rating should be applied
	 * @return The effective date of the rating in epoch milliseconds
	 */
	public long getEffectiveDate() {
		return effectiveDate;
	}
	/**
	 * Sets the effective date of the rating. The effective date is the earliest date/time for which the rating should be applied
	 * @param effectiveDate The effective date of the rating in epoch milliseconds
	 */
	public void setEffectiveDate(long effectiveDate) {
		this.effectiveDate = effectiveDate;
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/**
	 * Retrieves the creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 * This may be later than the effective date.
	 * @return The creation date of the rating in epoch milliseconds
	 */
	public long getCreateDate() {
		return createDate;
	}
	/**
	 * Sets the creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 * This may be later than the effective date.
	 * @param createDate The creation date of the rating in epoch milliseconds
	 */
	public void setCreateDate(long createDate) {
		this.createDate = createDate;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#isActive()
	 */
	@Override
	public boolean isActive() {
		return active;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setActive(boolean)
	 */
	@Override
	public void setActive(boolean active) {
		this.active = active;
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getDescription()
	 */
	@Override
	public String getDescription() {
		return description;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String description) {
		this.description = description;
	}
	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(java.util.Observable o, Object arg) {
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getDefaultValuetime()
	 */
	@Override
	public long getDefaultValueTime() {
		return defaultValueTime;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setDefaultValuetime(long)
	 */
	@Override
	public void setDefaultValueTime(long defaultValueTime) {
		this.defaultValueTime = defaultValueTime;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#resetDefaultValuetime()
	 */
	@Override
	public void resetDefaultValuetime() {
		this.defaultValueTime = UNDEFINED_TIME;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#getRatingTime()
	 */
	@Override
	public long getRatingTime() {
		return ratingTime;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		this.ratingTime = ratingTime; 
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#resetRatingtime()
	 */
	@Override
	public void resetRatingTime() {
		ratingTime = Long.MAX_VALUE;
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents()
	 */
	@Override
	public double[][] getRatingExtents() throws RatingException {
		return getRatingExtents(getRatingTime());
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getEffectiveDates()
	 */
	@Override
	public long[] getEffectiveDates() {
		return new long[] {effectiveDate};
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#getCreateDates()
	 */
	@Override
	public long[] getCreateDates() {
		return new long[] {createDate};
	}
	/**
	 * Retrieves a AbstractRatingContainer object for this rating.
	 * @return The RatingConainer object.
	 */
	public abstract AbstractRatingContainer getData();
	/**
	 * Sets this rating from a AbstractRatingContainer object.
	 * @param rc The AbstractRatingContainer object to set the rating from.
	 * @throws RatingException
	 */
	public abstract void setData(AbstractRatingContainer rc) throws RatingException;
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
		TimeSeriesContainer[] tscs = {tsc};
		return rate(tscs);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#rate(hec.io.TimeSeriesContainer[])
	 */
	@Override
	public TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
		String[] dataUnits = getDataUnits();
		String[] newDataUnits = new String[dataUnits.length];
		String[] ratingUnits = getRatingUnits();
		String[] params = getRatingParameters();
		int ratedInterval = tscs[0].interval;
		try {
			if (tscs.length != getIndParamCount()) {
				throw new RatingException(String.format("%d data sets specified, %d required.", tscs.length, getIndParamCount()));
			}
			//------------------------//
			// validate the intervals //
			//------------------------//
			for (int i = 1; i < tscs.length; ++i) {
				if (tscs[i].interval != tscs[0].interval) {
					String msg = "TimeSeriesContainers have inconsistent intervals.";
					if (!allowUnsafe) throw new RatingException(msg);
					if (warnUnsafe) logger.warning(msg + "  Rated values will be irregular interval.");
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
					if (warnUnsafe) logger.warning(msg + "  Value times will be treated as UTC.");
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
					if (warnUnsafe) logger.warning(msg + "  Value times will be treated as UTC.");
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
					if (warnUnsafe) logger.warning(t.getMessage());
				}
				if (tscParam != null) {
					if (!tscParam.getParameter().equals(params[i])) {
						String msg = String.format("Parameter \"%s\" does not match rating parameter \"%s\".", tscParam.getParameter(), params[i]);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg);
					}
				}
				newDataUnits[i] = tscs[i].units;
			}
			//-------------------------//
			// finally - do the rating //
			//-------------------------//
			newDataUnits[tscs.length] = dataUnits[tscs.length];
			setDataUnits(newDataUnits);
			IndependentValuesContainer ivc = RatingConst.tscsToIvc(tscs, ratingUnits, tz, allowUnsafe, warnUnsafe);
			double[] depVals = rate(ivc.valTimes, ivc.indVals);
			setDataUnits(dataUnits);
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
	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath)
	 */
	@Override
	public TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException {
		try {
			return new TimeSeriesMath(rate((TimeSeriesContainer)tsm.getData()));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath[])
	 */
	@Override
	public TimeSeriesMath rate(TimeSeriesMath[] tsms) throws RatingException {
		TimeSeriesContainer[] tscs = new TimeSeriesContainer[tsms.length];
		try {
			for (int i = 0; i < tsms.length; ++i) {
				tscs[i] = (TimeSeriesContainer)tsms[i].getData();
			}
			return new TimeSeriesMath(rate(tscs));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double)
	 */
	@Override
	public double reverseRate(double depVal) throws RatingException {
		long[] valTimes = {defaultValueTime};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double[])
	 */
	@Override
	public double[] reverseRate(double[] depVals) throws RatingException {
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, defaultValueTime);
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long, double)
	 */
	@Override
	public double reverseRate(long valTime, double depVal) throws RatingException {
		long[] valTimes = {valTime};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long, double[])
	 */
	@Override
	public double[] reverseRate(long valTime, double[] depVals) throws RatingException {
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, valTime);
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(long[], double[])
	 */
	@Override
	public abstract double[] reverseRate(long[] valTimes, double[] depVals)	throws RatingException ;
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
		TimeSeriesContainer[] tscs = {tsc};
		String[] units = {tsc.units};
		TimeZone tz = null;
		if (tsc.timeZoneID != null) {
			tz = TimeZone.getTimeZone(tsc.timeZoneID);
			if (!tz.getID().equals(tsc.timeZoneID)) {
				String msg = String.format("TimeSeriesContainers have invalid time zone \"%s\".", tsc.timeZoneID);
				if (!allowUnsafe) throw new RatingException(msg);
				if (warnUnsafe) logger.warning(msg + "  Value times will be treated as UTC.");
				tz = null;
			}
		}
		IndependentValuesContainer ivc = RatingConst.tscsToIvc(tscs, units, tz, allowUnsafe, warnUnsafe);
		TimeSeriesContainer ratedTsc = new TimeSeriesContainer();
		tsc.clone(ratedTsc);
		double[] depVals = new double[ivc.indVals.length];
		for (int i = 0; i < depVals.length; ++i) depVals[i] = ivc.indVals[i][0];
		ratedTsc.values = reverseRate(ivc.valTimes, depVals);
		String paramStr = TextUtil.split(TextUtil.split(ratingSpecId, SEPARATOR1, "L")[1], SEPARATOR2, "L")[1];
		if (tsc.subParameter == null) {
			ratedTsc.fullName = TextUtil.replaceAll(tsc.fullName, tsc.parameter, paramStr, "IL");
		}
		else {
			ratedTsc.fullName = TextUtil.replaceAll(tsc.fullName, String.format("%s-%s", tsc.parameter, tsc.subParameter), paramStr, "IL");
		}
		String[] parts = TextUtil.split(paramStr, "-", "L", 2);
		ratedTsc.parameter = parts[0];
		ratedTsc.subParameter = parts.length > 1 ? parts[1] : null;
		String[] dataUnits = getDataUnits();
		ratedTsc.units = dataUnits == null ? getRatingUnits()[0] : dataUnits[0];
		return ratedTsc;
	}
	/* (non-Javadoc)
	 * @see hec.data.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
	 */
	@Override
	public TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException {
		try {
			return new TimeSeriesMath(reverseRate((TimeSeriesContainer)tsm.getData()));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/**
	 * Fills a AbstractRatingContainer object with info from this rating.
	 * @param rc The AbstractRatingContainer object to fill
	 */
	protected void getData(AbstractRatingContainer rc) {
		rc.officeId = officeId;
		rc.ratingSpecId = ratingSpecId;
		rc.unitsId = ratingUnitsId;
		rc.effectiveDateMillis = effectiveDate;
		rc.createDateMillis = createDate;
		rc.active = active;
		rc.description = description;
	}
	/**
	 * Sets rating information from the specified AbstractRatingContainer object.
	 * @param rc The AbstractRatingContainer object containing the rating information
	 */
	protected void _setData(AbstractRatingContainer rc) {
		officeId = rc.officeId;
		ratingSpecId = rc.ratingSpecId;
		ratingUnitsId = rc.unitsId;
		effectiveDate = rc.effectiveDateMillis;
		createDate = rc.createDateMillis;
		active = rc.active;
		description = rc.description;
	}
	
	protected double convertUnits(double val, String fromUnit, String toUnit) throws RatingException {
		if (fromUnit != null && toUnit != null) {
			try {
				val = Units.convertUnits(val, fromUnit, toUnit);
			} catch (UnitsConversionException e) {
				throw new RatingException(e); // shouldn't happen - filtered out earlier
			}
		}
		return val;
	}
	
	/**
	 * Returns an array of Rating Values. The defaultInteger will be used by function based rating curves to
	 * generate points a fixed number of dependent values apart
	 * @param defaultInterval
	 * @return 
	 */
	public abstract RatingValue[] getValues(Integer defaultInterval);
	
	
	/**
	 * Returns the unique identifying parts for the rating specification.
	 * 
	 * @return
	 * @throws DataSetException
	 */
	public IRatingSpecification getRatingSpecification() throws DataSetException
	{
		String officeId2 = getOfficeId();
		String ratingSpecId2 = getRatingSpecId();
		JDomRatingSpecification ratingSpecification = new JDomRatingSpecification(officeId2, ratingSpecId2);
		return ratingSpecification;
	}
	
	/**
	 * Returns the unique identifying parts for the rating template.
	 * @return
	 * @throws DataSetException
	 */
	public IRatingTemplate getRatingTemplate() throws DataSetException
	{
		IRatingSpecification ratingSpecification = getRatingSpecification();
		IRatingTemplate ratingTemplate = ratingSpecification.getTemplate();
		return ratingTemplate;
	}
	
	/**
	 * A flag to indicate that this rating curve has been modified.
	 */
	private boolean modified = false;
	
	/**
	 * Returns the modified state of this rating curve.
	 */
    public boolean isModified()
    {
    	return modified;
    }
    /**
     * Sets the modified state of this rating curve.
     */
    public void setModified(boolean bool)
    {
    	modified = bool;
    }	
    
    public abstract AbstractRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException;
    
    public static Logger getLogger() {
    	return logger;
    }
}
