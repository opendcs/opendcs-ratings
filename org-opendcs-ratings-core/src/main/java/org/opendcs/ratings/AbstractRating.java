/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;
import hec.lang.Const;
import hec.lang.Observable;
import hec.util.TextUtil;
import mil.army.usace.hec.metadata.*;
import org.opendcs.ratings.io.AbstractRatingContainer;
import org.opendcs.ratings.io.RatingJdbcCompatUtil;
import org.opendcs.ratings.io.RatingXmlCompatUtil;
import org.opendcs.ratings.io.xml.DomRatingSpecification;
import rma.lang.Modifiable;

import java.sql.Connection;
import java.util.Arrays;
import java.util.Observer;
import java.util.logging.Logger;

import static hec.lang.Const.UNDEFINED_TIME;
import static hec.util.TextUtil.replaceAll;
import static hec.util.TextUtil.split;
import static org.opendcs.ratings.RatingConst.*;

/**
 * Base class for all cwmsRating implementations
 *
 * @author Mike Perryman
 */
public abstract class AbstractRating implements Observer, ICwmsRating , VerticalDatum, Modifiable{

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
	 * A RatingSpec object for this rating if one is known.
	 */
	protected RatingSpec ratingSpec = null;
	/**
	 * The CWMS-style rating specification identifier
	 */
	protected String ratingSpecId = null;
	/**
	 * The parameters part of ratingSpecId parsed into an array
	 */
	protected String[] ratingParameters = null;
	/**
	 * The CWMS-style units identifier for the native units of the rating
	 */
	protected String ratingUnitsId = null;
	/**
	 * The ratingUnitsId parsed into an array
	 */
	protected String[] ratingUnits = null;
	/**
	 * A CWMS-style units identifier for the data using the rating
	 */
	protected String dataUnitsId = null;
	/**
	 * The dataUnitsId parsed into an array
	 */
	protected String[] dataUnits = null;
	/**
	 * The earliest date/time at which the rating is considered to be in effect 
	 */
	protected long effectiveDate = UNDEFINED_TIME;
	/**
	 * The date/time at which to begin transition (interpolation) from the previous rating to this one.
	 * If undefined, transition from the previous rating effective date. 
	 */
	protected long transitionStartDate = UNDEFINED_TIME;
	/**
	 * The earliest date/time that the rating was available - the time it was first stored in the database
	 */
	protected long createDate = UNDEFINED_TIME;
	/**
	 * Flag specifying whether this rating should be used in the rate(), rateOne(), and reverseRate() methods.
	 */
	protected boolean active = false;
	/**
	 * Descriptive text about the rating
	 */
	protected String description = null;
	/**
	 * Contains vertical datum information, if any exists
	 */
	protected VerticalDatumContainer vdc = null;
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
	/**
	 * Generates a new AbstractRating object from XML text
	 * @param xmlText The XML text to generate the rating object from
	 * @return The generated rating object.
	 * @throws RatingException on error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#abstractRating instead
	 */
	@Deprecated
	public static AbstractRating fromXml(String xmlText) throws RatingException {
		return RatingXmlCompatUtil.getInstance().fromXml(xmlText);
	}

	/**
	 * Generates a new AbstractRating object from a CWMS database connection
	 * @param conn The connection to a CWMS database
	 * @param officeId The identifier of the office owning the rating. If null, the office associated with the connect user is used. 
	 * @param ratingSpecId The rating specification identifier
	 * @param effectiveDate Specifies (in milliseconds) a time to be an upper bound on the effective date.
	 *                      The rating with the latest effective date on or before this time is retrieved. If null, the latest rating is retrieved.
	 * @return The new AbstractRating object
	 * @throws RatingException
	 * Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#retrieve(Connection, String, String, Long) instead;
	 */
	@Deprecated
	public static AbstractRating fromDatabase(Connection conn, String officeId, String ratingSpecId, Long effectiveDate) throws RatingException {
		RatingJdbcCompatUtil service = RatingJdbcCompatUtil.getInstance();
		return service.fromDatabase(conn, officeId, ratingSpecId, effectiveDate);
	}

	public static boolean compatibleUnits(String[] units1, String[] units2) {
		boolean compatible = false;
		comparison:
		do {
			if (units2.length != units1.length) break;
			for (int i = 0; i < units1.length; ++i) {
				if(units1[i].equals(units2[i])) continue;
				if (!UnitUtil.canConvertBetweenUnits(units1[i], units2[i])) break comparison;
			}
			compatible = true;
		} while(false);
		return compatible;
	}
	public static boolean compatibleUnits(String units1, String units2) {
		return compatibleUnits(
				split(replaceAll(units1, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L"),
				split(replaceAll(units2, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L"));
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#addObserver(java.util.Observer)
	 */
	@Override
	public void addObserver(Observer o) {
		synchronized(this) {
            if(observationTarget != null) {
                observationTarget.addObserver(o);
            }
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#deleteObserver(java.util.Observer)
	 */
	@Override
	public void deleteObserver(Observer o) {
		synchronized(this) {
            if(observationTarget != null) {
                observationTarget.deleteObserver(o);
            }
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#getRatingSpecId()
	 */
	@Override
	public String getRatingSpecId() {
		synchronized(this) {
			return ratingSpecId;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#getOfficeId()
	 */
	@Override
	public String getOfficeId() {
		synchronized(this) {
			return officeId;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#setOfficeId(java.lang.String)
	 */
	@Override
	public void setOfficeId(String officeId) {
		synchronized(this) {
			this.officeId = officeId;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#setRatingSpecId(java.lang.String)
	 */
	@Override
	public void setRatingSpecId(String ratingSpecId) {
		synchronized(this) {
			this.ratingSpecId = ratingSpecId;
			if (ratingSpecId == null) {
				ratingParameters = null;
			}
			else {
				String[] parts = split(ratingSpecId, SEPARATOR1);
				ratingParameters = TextUtil.split(parts[1].replaceAll(SEPARATOR2, SEPARATOR3), SEPARATOR3);
			}
		}
		
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#getUnitsId()
	 */
	@Override
	public String getRatingUnitsId() {
		synchronized(this) {
			return ratingUnitsId;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#setUnitsId(java.lang.String)
	 */
	@Override
	public void setRatingUnitsId(String ratingUnitsId) {
		synchronized(this) {
			this.ratingUnitsId = ratingUnitsId;
			ratingUnits = null;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#getDataUnitsId()
	 */
	@Override
	public String getDataUnitsId() {
		synchronized(this) {
			return dataUnitsId;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#setDataUnitsId(java.lang.String)
	 */
	@Override
	public void setDataUnitsId(String dataUnitsId) {
		synchronized(this) {
			try {
				setDataUnits(TextUtil.split(TextUtil.replaceAll(dataUnitsId, SEPARATOR2, SEPARATOR3), SEPARATOR3));
			}
			catch (RatingException e) {
				logger.warning("Invalid data units string : " + dataUnitsId);
			}
		}
		
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#getName()
	 */
	@Override
	public String getName() {
		synchronized(this) {
			return ratingSpecId;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.IRating#setName(java.lang.String)
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
		synchronized(this) {
			ratingSpecId = name;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#getRatingParameters()
	 */
	@Override
	public String[] getRatingParameters() {
		synchronized(this) {
			if (ratingParameters == null) {
				if (ratingSpecId == null) {
					return null;
				}
				String parametersId = split(ratingSpecId, SEPARATOR1, "L")[1];
				ratingParameters = split(replaceAll(parametersId, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L");
			}
			return Arrays.copyOf(ratingParameters, ratingParameters.length);
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#getRatingUnits()
	 */
	@Override
	public String[] getRatingUnits() {
		synchronized(this) {
			if (ratingUnits == null) {
				if (ratingUnitsId == null) {
					return null;
				}
				ratingUnits = split(replaceAll(ratingUnitsId, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L");
			}
			return Arrays.copyOf(ratingUnits, ratingUnits.length);
		}
	}
	/**
	 * Set the rating units
	 * @param units The rating units
	 * @throws RatingException on error
	 */
	public void setRatingUnits(String[] units) throws RatingException {
		if (units == null) {
			ratingUnits = null;
			return;
		}
		String[] parameters = getRatingParameters();
		if (parameters != null) {
			if (units.length != parameters.length) {
				throw new RatingException(String.format("Invalid number of rating units (%d units for %d parameters)", units.length, parameters.length));
			}
			Units parameterUnit;
			for (int i = 0; i < parameters.length; ++i) {
				try {
					parameterUnit = new Parameter(parameters[i]).getDefaultUnits();
				}
				catch (Throwable t) {
					if (!allowUnsafe) throw new RatingException(t);
					if (warnUnsafe) logger.warning(t.getMessage());
					parameterUnit = null;
				}
				if (parameterUnit != null) {
					if(!UnitUtil.canConvertBetweenUnits(units[i], parameterUnit.toString())) {
						String msg = String.format("Cannot convert from \"%s\" to \"%s\".", units[i], parameterUnit);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg);
					}
				}
			}
		}
		setDataUnits(getDataUnits());
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#getDataUnits()
	 */
	@Override
	public String[] getDataUnits() {
		synchronized(this) {
			if (dataUnits == null) {
				if (dataUnitsId == null) {
					return getRatingUnits();
				}
				dataUnits = split(replaceAll(dataUnitsId, SEPARATOR2, SEPARATOR3, "L"), SEPARATOR3, "L");
			}
			return Arrays.copyOf(dataUnits, dataUnits.length);
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#setDataUnits(java.lang.String[])
	 */
	@Override
	public void setDataUnits(String[] units) throws RatingException {
		synchronized(this) {
			if (units == null) {
				this.dataUnitsId = null;
				this.dataUnits = null;
				return;
			}
			String[] ratingUnits = getRatingUnits();
			String[] parameters = getRatingParameters();
			if (ratingUnits == null) {
				if (parameters != null && units.length != parameters.length) {
					throw new RatingException("Invalid number of data units.");
				}
			}
			else if (units.length != ratingUnits.length) {
				throw new RatingException("Invalid number of data units.");
			}
			Units dataUnit;
			Units parameterUnit;
			for (int i = 0; i < units.length; ++i) {
				try {
					dataUnit = new Units(units[i]);
				}
				catch (Throwable t) {
					if (!allowUnsafe) throw new RatingException(t);
					if (warnUnsafe) logger.warning(t.getMessage());
					dataUnit = null;
				}
				if (dataUnit != null) {
					if (ratingUnits == null) {
						//--------------------------------------------//
						// compare against default unit for parameter //
						//--------------------------------------------//
						try {
							parameterUnit = new Parameter(parameters[i]).getDefaultUnits();
						}
						catch (Throwable t) {
							if (!allowUnsafe) throw new RatingException(t);
							if (warnUnsafe) logger.warning(t.getMessage());
							parameterUnit = null;
						}
						if (parameterUnit != null && !units[i].equals(parameterUnit.toString())) {
							if(!UnitUtil.canConvertBetweenUnits(units[i], parameterUnit.toString())) {
								String msg = String.format("Cannot convert from \"%s\" to \"%s\".", units[i], parameterUnit);
								if (!allowUnsafe) throw new RatingException(msg);
								if (warnUnsafe) {
									if (i == parameters.length - 1) {
										logger.warning(msg + "  Rated values will be unconverted.");
									}
									else {
										logger.warning(msg + "  Rating will be performed using unconverted values.");
									}
								}
							}
						}
					}
					else {
						//-----------------------------//
						// compare against rating unit //
						//-----------------------------//
						if (!units[i].equals(ratingUnits[i])) {
							if(!UnitUtil.canConvertBetweenUnits(units[i], ratingUnits[i])) {
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
			}
			
			StringBuilder sb = new StringBuilder(units[0]);
			for (int i = 1; i < units.length - 1; ++i) sb.append(",").append(units[i]);
			sb.append(";").append(units[units.length-1]);
			dataUnitsId = sb.toString();
			dataUnits = Arrays.copyOf(units, units.length);
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#getIndParamCount()
	 */
	@Override
	public int getIndParamCount() throws RatingException {
		synchronized(this) {
			return this.getRatingParameters().length - 1;
		}
	}
	/**
	 * Retrieves whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesAllowUnsafe() {
		synchronized(this) {
			return allowUnsafe;
		}
	}
	/**
	 * Sets whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param allowUnsafe A flag specifying whether this object allows "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setAllowUnsafe(boolean allowUnsafe) {
		synchronized(this) {
			this.allowUnsafe = allowUnsafe;
		}
	}
	/**
	 * Retrieves whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @return A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public boolean doesWarnUnsafe() {
		synchronized(this) {
			return warnUnsafe;
		}
	}
	/**
	 * Sets whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 * @param warnUnsafe  A flag specifying whether this object outputs warning messages about "risky" operations such as working with mismatched units, unknown parameters, etc.
	 */
	public void setWarnUnsafe(boolean warnUnsafe) {
		synchronized(this) {
			this.warnUnsafe = warnUnsafe;
		}
	}
	/**
	 * Retrieves the effective date of the rating. The effective date is the earliest date/time for which the rating should be applied
	 * @return The effective date of the rating in epoch milliseconds
	 */
	public long getEffectiveDate() {
		synchronized(this) {
			return effectiveDate;
		}
	}
	/**
	 * Sets the effective date of the rating. The effective date is the earliest date/time for which the rating should be applied
	 * @param effectiveDate The effective date of the rating in epoch milliseconds
	 * @throws RatingException if effectiveDate violates other values (e.g., UsgsStreamTableRating shift effective dates)
	 */
	public void setEffectiveDate(long effectiveDate) throws RatingException {
		synchronized(this) {
			this.effectiveDate = effectiveDate;
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
		
	/**
	 * Retrieves the transition start date of the rating. The transition start date is the date/time to being transition (interpolation) from any previous rating
	 * @return The transition start date of the rating in epoch milliseconds
	 */
	public long getTransitionStartDate() {
		synchronized(this) {
			return transitionStartDate;
		}
	}
	/**
	 * Sets the transition start date of the rating. The transition start date is the date/time to being transition (interpolation) from any previous rating
	 * @param transitionStartDate The transition start date of the rating in epoch milliseconds
	 */
	public void setTransitionStartDate(long transitionStartDate) {
		synchronized(this) {
			this.transitionStartDate = transitionStartDate;
			observationTarget.setChanged();
			observationTarget.notifyObservers();
	}
	}
	/**
	 * Retrieves the creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 * This may be later than the effective date.
	 * @return The creation date of the rating in epoch milliseconds
	 */
	public long getCreateDate() {
		synchronized(this) {
			return createDate;
		}
	}
	/**
	 * Sets the creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 * This may be later than the effective date.
	 * @param createDate The creation date of the rating in epoch milliseconds
	 */
	public void setCreateDate(long createDate) {
		synchronized(this) {
			this.createDate = createDate;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#isActive()
	 */
	@Override
	public boolean isActive() {
		synchronized(this) {
			return active;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#setActive(boolean)
	 */
	@Override
	public void setActive(boolean active) {
		synchronized(this) {
			this.active = active;
			observationTarget.setChanged();
			observationTarget.notifyObservers();
	}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#getDescription()
	 */
	@Override
	public String getDescription() {
		synchronized(this) {
			return description;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#setDescription(java.lang.String)
	 */
	@Override
	public void setDescription(String description) {
		synchronized(this) {
			this.description = description;
		}
	}
	/* (non-Javadoc)
	 * @see java.util.Observer#update(java.util.Observable, java.lang.Object)
	 */
	@Override
	public void update(java.util.Observable o, Object arg) {
		synchronized(this) {
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#getDefaultValuetime()
	 */
	@Override
	public long getDefaultValueTime() {
		synchronized(this) {
			return defaultValueTime == UNDEFINED_TIME ? System.currentTimeMillis() : defaultValueTime;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.ICwmsRating#setDefaultValuetime(long)
	 */
	@Override
	public void setDefaultValueTime(long defaultValueTime) {
		synchronized(this) {
			this.defaultValueTime = defaultValueTime;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#getRatingTime()
	 */
	@Override
	public long getRatingTime() {
		synchronized(this) {
			return ratingTime;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		synchronized(this) {
			this.ratingTime = ratingTime;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#resetRatingtime()
	 */
	@Override
	public void resetRatingTime() {
		synchronized(this) {
			ratingTime = Long.MAX_VALUE;
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.IRating#getRatingExtents()
	 */
	@Override
	public double[][] getRatingExtents() throws RatingException {
		synchronized(this) {
			return getRatingExtents(getRatingTime());
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.IRating#getEffectiveDates()
	 */
	@Override
	public long[] getEffectiveDates() {
		synchronized(this) {
			return new long[] {effectiveDate};
		}
	}
	/* (non-Javadoc)
	 * @see org.opendcs.IRating#getCreateDates()
	 */
	@Override
	public long[] getCreateDates() {
		synchronized(this) {
			return new long[] {createDate};
		}
	}
	/**
	 * Retrieves a AbstractRatingContainer object for this rating.
	 * @return The RatingConainer object.
	 */
	public abstract AbstractRatingContainer getData();
	/**
	 * Sets this rating from a AbstractRatingContainer object.
	 * @param rc The AbstractRatingContainer object to set the rating from.
	 * @throws RatingException on error
	 */
	public abstract void setData(AbstractRatingContainer rc) throws RatingException;
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#rate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
		TimeSeriesContainer[] tscs = {tsc};
		return rate(tscs);
	}

	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#rate(hec.io.TimeSeriesContainer[])
	 */
	@Override
	public TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
        TimeSeriesRater tsRater = new TimeSeriesRater(this, allowUnsafe, warnUnsafe);
        return tsRater.rate(tscs);
	}
	/* (non-Javadoc)
	 * @see org.opendcs.IRating#rate(hec.hecmath.TimeSeriesMath)
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
	 * @see org.opendcs.IRating#rate(hec.hecmath.TimeSeriesMath[])
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
	 * @see org.opendcs.ratings.IRating#reverseRate(double)
	 */
	@Override
	public double reverseRate(double depVal) throws RatingException {
		long[] valTimes = {getDefaultValueTime()};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#reverseRate(double[])
	 */
	@Override
	public double[] reverseRate(double[] depVals) throws RatingException {
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, getDefaultValueTime());
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#reverseRate(long, double)
	 */
	@Override
	public double reverseRate(long valTime, double depVal) throws RatingException {
		long[] valTimes = {valTime};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#reverseRate(long, double[])
	 */
	@Override
	public double[] reverseRate(long valTime, double[] depVals) throws RatingException {
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, valTime);
		return reverseRate(valTimes, depVals);
	}
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#reverseRate(long[], double[])
	 */
	@Override
	public abstract double[] reverseRate(long[] valTimes, double[] depVals)	throws RatingException ;
	/* (non-Javadoc)
	 * @see org.opendcs.ratings.IRating#reverseRate(hec.io.TimeSeriesContainer)
	 */
	@Override
	public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
        TimeSeriesRater tsRater = new TimeSeriesRater(this, allowUnsafe, warnUnsafe);
        return tsRater.reverseRate(tsc);
	}
	/* (non-Javadoc)
	 * @see org.opendcs.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
	 */
	public TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException {
		try {
			return new TimeSeriesMath(reverseRate((TimeSeriesContainer)tsm.getData()));
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException) t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNativeVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isCurrentVerticalDatumEstimated();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toNativeVerticalDatum()
	 */
	@Override
	public boolean toNativeVerticalDatum() throws VerticalDatumException {
		AbstractRatingContainer arc = getData();
		boolean change = arc.toNativeVerticalDatum();
		try {
			setData(arc);
		}
		catch (RatingException e) {
			throw new VerticalDatumException(e);
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toNGVD29()
	 */
	@Override
	public boolean toNGVD29() throws VerticalDatumException {
		AbstractRatingContainer arc = getData();
		boolean change = arc.toNGVD29();
		try {
			setData(arc);
		}
		catch (RatingException e) {
			throw new VerticalDatumException(e);
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toNAVD88()
	 */
	@Override
	public boolean toNAVD88() throws VerticalDatumException {
		AbstractRatingContainer arc = getData();
		boolean change = arc.toNAVD88();
		try {
			setData(arc);
		}
		catch (RatingException e) {
			throw new VerticalDatumException(e);
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#toVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean toVerticalDatum(String datum) throws VerticalDatumException {
		AbstractRatingContainer arc = getData();
		boolean change = arc.toVerticalDatum(datum);
		try {
			setData(arc);
		}
		catch (RatingException e) {
			throw new VerticalDatumException(e);
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#forceVerticalDatum(java.lang.String)
	 */
	@Override
	public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
		AbstractRatingContainer arc = getData();
		boolean change = arc.forceVerticalDatum(datum);
		try {
			setData(arc);
		}
		catch (RatingException e) {
			throw new VerticalDatumException(e);
		}
		return change;
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset(unit);
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset(unit);
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset(unit);
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNGVD29OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNAVD88OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getVerticalDatumInfo();
	}
	/* (non-Javadoc)
	 * @see mil.army.usace.hec.metadata.VerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
		if (vdc == null && xmlStr != null) {
			vdc = new VerticalDatumContainer(xmlStr);
		}
		else if (xmlStr == null || xmlStr.trim().isEmpty()) {
			vdc = null;
		}
		else {
			vdc.setVerticalDatumInfo(xmlStr);
		}
	}
	/**
	 * Fills a AbstractRatingContainer object with info from this rating.
	 * @param arc The AbstractRatingContainer object to fill
	 */
	protected void getData(AbstractRatingContainer arc) {
		synchronized(this) {
			arc.officeId = officeId;
			arc.ratingSpecId = ratingSpecId;
			arc.unitsId = ratingUnitsId;
			arc.effectiveDateMillis = effectiveDate;
			arc.transitionStartDateMillis = transitionStartDate;
			arc.createDateMillis = createDate;
			arc.active = active;
			arc.description = description;
			if (vdc != null) {
				arc.setVerticalDatumContainer(vdc.clone());
			}
		}
	}
	/**
	 * Sets rating information from the specified AbstractRatingContainer object.
	 * @param arc The AbstractRatingContainer object containing the rating information
	 */
	protected void _setData(AbstractRatingContainer arc) {
		synchronized(this) {
			officeId = arc.officeId;
			ratingSpecId = arc.ratingSpecId;
			ratingUnitsId = arc.unitsId;
			effectiveDate = arc.effectiveDateMillis;
			transitionStartDate = arc.transitionStartDateMillis;
			createDate = arc.createDateMillis;
			active = arc.active;
			description = arc.description;
			if (arc.getVerticalDatumContainer() != null) {
				vdc = arc.getVerticalDatumContainer().clone();
			}
		}
	}
	
	protected double convertUnits(double val, String fromUnit, String toUnit) throws RatingException {
		if (fromUnit != null && toUnit != null) {
			try {
				val = UnitUtil.convertUnits(val, fromUnit, toUnit);
			} catch (UnitsConversionException e) {
				throw new RatingException(e); // shouldn't happen - filtered out earlier
			}
		}
		return val;
	}
	
	/**
	 * Returns an array of Rating Values. The defaultInteger will be used by function based rating curves to
	 * generate points a fixed number of dependent values apart
	 * @param defaultInterval The interval in minutes for ExpresionRating objects
	 * @return The rated values
	 */
	public abstract RatingValue[] getValues(Integer defaultInterval);
	
	
	/**
	 * @return the rating specification.
	 * @throws DataSetException on error
	 */
	public IRatingSpecification getRatingSpecification() throws DataSetException
	{
		String officeId2;
		String ratingSpecId2;
		synchronized(this) {
			officeId2 = getOfficeId();
			ratingSpecId2 = getRatingSpecId();
		}
        return new DomRatingSpecification(officeId2, ratingSpecId2);
	}
	
	/**
	 * @return the rating template
	 * @throws DataSetException on error
	 */
	public IRatingTemplate getRatingTemplate() throws DataSetException
	{
		IRatingSpecification ratingSpecification = getRatingSpecification();
        return ratingSpecification.getTemplate();
	}
	
	/**
	 * A flag to indicate that this rating curve has been modified.
	 */
	private boolean modified = false;
	
	/**
	 * Returns the modified state of this rating curve.
	 */
    @Override
	public boolean isModified()
    {
		synchronized(this) {
			return modified;
		}
    }
    /**
     * Sets the modified state of this rating curve.
     */
    @Override
	public void setModified(boolean bool)
    {
		synchronized(this) {
			modified = bool;
		}
    }	
    
    public abstract AbstractRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException;
    
    public static Logger getLogger() {
    	return logger;
    }
	/**
	 * Stores the rating  to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @throws RatingException on error
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.jdbc.RatingJdbcFactory#store(Connection, boolean) instead;
	 */
	@Deprecated
	public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
		RatingJdbcCompatUtil service = RatingJdbcCompatUtil.getInstance();
		service.storeToDatabase(this, conn, overwriteExisting);
	}
	/**
	 * Returns whether this rating has vertical datum information
	 * @return whether this rating has vertical datum information
	 */
	public boolean hasVerticalDatum() {
		return vdc != null;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public abstract boolean equals(Object obj);
	
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public abstract int hashCode();

	/**
	 * @deprecated Use mil.army.usace.hec.cwms.rating.io.xml.RatingXmlFactory#toXml(CharSequence, int) instead
	 */
	@Deprecated
	@Override
	public abstract String toXmlString(CharSequence indent, int indentLevel) throws RatingException;

	/**
	 * @return the VerticalDatumContainer
	 */
	@Override
	public VerticalDatumContainer getVerticalDatumContainer()
	{
		return vdc;
	}

	/**
	 * Sets the VerticalDatumContainer
	 * @param vdc the VerticalDatumContainer
	 */
	public void setVerticalDatumContainer(VerticalDatumContainer vdc)
	{
		this.vdc = vdc;
	}

    /**
     * Resets the default value time. This is used for rating values that have no inherent times.
     */
    @Override
    public void resetDefaultValueTime() {
        this.defaultValueTime = Const.UNDEFINED_TIME;
    };
}
