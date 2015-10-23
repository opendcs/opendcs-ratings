package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.SEPARATOR1;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.lang.Const.UNDEFINED_LONG;
import static hec.lang.Const.UNDEFINED_TIME;
import static hec.util.TextUtil.replaceAll;
import static hec.util.TextUtil.split;
import hec.data.DataSetException;
import hec.data.IVerticalDatum;
import hec.data.Parameter;
import hec.data.RatingException;
import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.data.VerticalDatumException;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.ExpressionRatingContainer;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import hec.data.rating.IRatingSpecification;
import hec.data.rating.IRatingTemplate;
import hec.data.rating.JDomRatingSpecification;
import hec.heclib.util.HecTime;
import hec.hecmath.TimeSeriesMath;
import hec.io.Conversion;
import hec.io.TimeSeriesContainer;
import hec.io.VerticalDatumContainer;
import hec.lang.Observable;
import hec.util.TextUtil;

import java.sql.CallableStatement;
import java.sql.Clob;
import java.sql.Connection;
import java.sql.Types;
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
public abstract class AbstractRating implements Observer, ICwmsRating , IVerticalDatum, Modifiable{

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
	 * Object to use for rating TimeSeriesContainer and TimeSeriesMath objects
	 */
	protected TimeSeriesRater tsRater = null;
	/**
	 * Generates a new AbstractRating object from XML text
	 * @param xmlText The XML text to generate the rating object from
	 * @return The generated rating object.
	 * @throws RatingException
	 */
	public static AbstractRating fromXml(String xmlText) throws RatingException {
		AbstractRatingContainer arc = AbstractRatingContainer.fromXml(xmlText);
		if (arc instanceof UsgsStreamTableRatingContainer) return new UsgsStreamTableRating((UsgsStreamTableRatingContainer)arc);
		if (arc instanceof TableRatingContainer) return new TableRating((TableRatingContainer)arc);
		if (arc instanceof ExpressionRatingContainer) return new ExpressionRating((ExpressionRatingContainer)arc);
		return null;
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
	 */
	public static AbstractRating fromDatabase(
			Connection conn, 
			String officeId, 
			String ratingSpecId, 
			Long effectiveDate)
			throws RatingException {
		try {
			String sql = 
					"declare " +
					   "l_millis_end           integer := :1;" +
					   "l_effective_date_start date;" +
					   "l_effective_date_end   date;" +
					"begin " +
					   "if l_millis_end is not null then "    +
					      "l_effective_date_end := cast(cwms_util.to_timestamp(l_millis_end) as date);" +
					   "end if;" +
					   "cwms_rating.retrieve_ratings_xml2("   +
					      "p_ratings              => :2,"     +
					      "p_spec_id_mask         => :3,"     +
					      "p_effective_date_start => null,"   +
					      "p_effective_date_end   => l_effective_date_end,"   +
					      "p_time_zone            => 'UTC',"  +
					      "p_office_id_mask       => :4);"    +
					"end;";
			CallableStatement stmt = conn.prepareCall(sql);
			stmt.registerOutParameter(2, Types.CLOB);
			stmt.setString(3, ratingSpecId);
			if (effectiveDate == null) {
				stmt.setNull(1, Types.INTEGER);
			}
			else {
				stmt.setLong(1, effectiveDate);
			}
			if (officeId == null) {
				stmt.setNull(4, Types.VARCHAR);
			}
			else {
				stmt.setString(4, officeId);
			}
			stmt.execute();
			Clob clob = stmt.getClob(2);
			stmt.close();
			if (clob.length() > Integer.MAX_VALUE) {
				throw new RatingException("CLOB too long.");
			}
			String xmlText = clob.getSubString(1, (int)clob.length());
			return fromXml(xmlText);
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
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
		return defaultValueTime == UNDEFINED_TIME ? System.currentTimeMillis() : defaultValueTime;
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
		if (tsRater == null) {
			tsRater = new TimeSeriesRater(this, allowUnsafe, warnUnsafe);
		}
		return tsRater.rate(tscs);
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
		long[] valTimes = {getDefaultValueTime()};
		double[] depVals = {depVal};
		return reverseRate(valTimes, depVals)[0];
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.IRating#reverseRate(double[])
	 */
	@Override
	public double[] reverseRate(double[] depVals) throws RatingException {
		long[] valTimes = new long[depVals.length];
		Arrays.fill(valTimes, getDefaultValueTime());
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
		if (tsRater == null) {
			tsRater = new TimeSeriesRater(this, allowUnsafe, warnUnsafe);
		}
		return tsRater.reverseRate(tsc);
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
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNativeVerticalDatum()
	 */
	@Override
	public String getNativeVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNativeVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentVerticalDatum()
	 */
	@Override
	public String getCurrentVerticalDatum() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentVerticalDatum();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isCurrentVerticalDatumEstimated()
	 */
	@Override
	public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isCurrentVerticalDatumEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#toNativeVerticalDatum()
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
	 * @see hec.data.IVerticalDatum#toNGVD29()
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
	 * @see hec.data.IVerticalDatum#toNAVD88()
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
	 * @see hec.data.IVerticalDatum#toVerticalDatum(java.lang.String)
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
	 * @see hec.data.IVerticalDatum#forceVerticalDatum(java.lang.String)
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
	 * @see hec.data.IVerticalDatum#getCurrentOffset()
	 */
	@Override
	public double getCurrentOffset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getCurrentOffset(java.lang.String)
	 */
	@Override
	public double getCurrentOffset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getCurrentOffset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset()
	 */
	@Override
	public double getNGVD29Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNGVD29Offset(java.lang.String)
	 */
	@Override
	public double getNGVD29Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNGVD29Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset()
	 */
	@Override
	public double getNAVD88Offset() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getNAVD88Offset(java.lang.String)
	 */
	@Override
	public double getNAVD88Offset(String unit) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getNAVD88Offset(unit);
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNGVD29OffsetEstimated()
	 */
	@Override
	public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNGVD29OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#isNAVD88OffsetEstimated()
	 */
	@Override
	public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.isNAVD88OffsetEstimated();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#getVerticalDatumInfo()
	 */
	@Override
	public String getVerticalDatumInfo() throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		return vdc.getVerticalDatumInfo();
	}
	/* (non-Javadoc)
	 * @see hec.data.IVerticalDatum#setVerticalDatumInfo(java.lang.String)
	 */
	@Override
	public void setVerticalDatumInfo(String xmlStr) throws VerticalDatumException {
		if (vdc == null) throw new VerticalDatumException("Object does not have vertical datum information");
		vdc.setVerticalDatumInfo(xmlStr);
	}
	/**
	 * Fills a AbstractRatingContainer object with info from this rating.
	 * @param arc The AbstractRatingContainer object to fill
	 */
	protected void getData(AbstractRatingContainer arc) {
		arc.officeId = officeId;
		arc.ratingSpecId = ratingSpecId;
		arc.unitsId = ratingUnitsId;
		arc.effectiveDateMillis = effectiveDate;
		arc.createDateMillis = createDate;
		arc.active = active;
		arc.description = description;
		if (vdc != null) {
			arc.vdc = vdc.clone();
		}
	}
	/**
	 * Sets rating information from the specified AbstractRatingContainer object.
	 * @param arc The AbstractRatingContainer object containing the rating information
	 */
	protected void _setData(AbstractRatingContainer arc) {
		officeId = arc.officeId;
		ratingSpecId = arc.ratingSpecId;
		ratingUnitsId = arc.unitsId;
		effectiveDate = arc.effectiveDateMillis;
		createDate = arc.createDateMillis;
		active = arc.active;
		description = arc.description;
		if (arc.vdc != null) {
			vdc = arc.vdc.clone();
		}
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
    @Override
	public boolean isModified()
    {
    	return modified;
    }
    /**
     * Sets the modified state of this rating curve.
     */
    @Override
	public void setModified(boolean bool)
    {
    	modified = bool;
    }	
    
    public abstract AbstractRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException;
    
    public static Logger getLogger() {
    	return logger;
    }
	/**
	 * Stores the rating  to a CWMS database
	 * @param conn The connection to the CWMS database
	 * @param overwriteExisting Flag specifying whether to overwrite any existing rating data
	 * @throws RatingException
	 */
	public void storeToDatabase(Connection conn, boolean overwriteExisting) throws RatingException {
		RatingSet.storeToDatabase(conn, getData().toXml(""), overwriteExisting);
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
	public boolean equals(Object obj) {
		boolean result = false;
		if (obj instanceof AbstractRating) {
			AbstractRating other = (AbstractRating)obj;
			result = other.getClass() == this.getClass();
			if (result) {
				result = toString().equals(other.toString()) && effectiveDate == other.effectiveDate;
				if (result) {
					try {
						result = toXmlString("", 0).trim().equals(other.toXmlString("", 0).trim());
					}
					catch (RatingException e) {
						throw new RuntimeException(e);
					}
				}
			}
		}
		return result;
	}
	@Override
	public abstract String toXmlString(CharSequence indent, int indentLevel) throws RatingException;
}
