package hec.data.cwmsRating;

import hec.hecmath.TimeSeriesMath;
import hec.io.TimeSeriesContainer;

/**
 * Generalized rating interface
 *
 * @author Mike Perryman
 */
public interface IRating {

	/**
	 * Retrieves the name of the rating.
	 * @return The name of the rating
	 */
	public abstract String getName();
	
	/**
	 * Sets the name of the rating
	 * @param name The new name of the rating
	 * @throws RatingException
	 */
	public abstract void setName(String name) throws RatingException;
	
	/**
	 * Retrieves the rating parameters.
	 * @return The independent and dependent parameters of the rating
	 */
	public abstract String[] getRatingParameters();
	
	/**
	 * Retrieves the rating ratingUnits. These are the ratingUnits of the underlying rating, which may be different than the
	 * data ratingUnits, as long as valid unit conversions exist between rating ratingUnits and data ratingUnits.
	 * @return The ratingUnits, one unit for each parameter
	 */
	public abstract String[] getRatingUnits();

	/**
	 * Retrieves the data ratingUnits. These are the ratingUnits expected for independent parameters and the unit produced 
	 * for the dependent parameter.  If the underlying rating uses different ratingUnits, the rating must perform unit 
	 * conversions.
	 * @return The ratingUnits identifier, one unit for each parameter
	 */
	public abstract String[] getDataUnits();

	/**
	 * Sets the data ratingUnits. These are  the ratingUnits expected for independent parameters and the unit produced 
	 * for the dependent parameter.  If the underlying rating uses different ratingUnits, the rating must perform unit 
	 * conversions.
	 * @param units The ratingUnits, one unit for each parameter
	 */
	public abstract void setDataUnits(String[] units) throws RatingException;

	/**
	 * Retrieves the default value time. This is used for rating values that have no inherent times.
	 * @return The default value time
	 */
	public abstract long getDefaultValueTime();
	
	/**
	 * Sets the default value time. This is used for rating values that have no inherent times.
	 * @param defaultValueTime The default value time in Java milliseconds
	 */
	public abstract void setDefaultValueTime(long defaultValueTime);

	/**
	 * Resets the default value time. This is used for rating values that have no inherent times.
	 */
	public abstract void resetDefaultValuetime();
	
	/**
	 * Retrieves the rating time. This rate values at a time in the past. No rating information with a creation date
	 * later than the rating time will be used to rate values.
	 * @return The rating time in Java milliseconds
	 */
	public abstract long getRatingTime();

	/**
	 * Sets the rating time. This rate values at a time in the past. No rating information with a creation date
	 * later than the rating time will be used to rate values.
	 * @param ratingTime The rating time in Java milliseconds
	 */
	public abstract void setRatingTime(long ratingTime);

	/**
	 * Resets (un-sets) the rating time.
	 */
	public abstract void resetRatingTime();
	
	/**
	 * Retrieves the min and max value for each parameter of the rating, in the current ratingUnits.
	 * @return The min and max values for each parameter. The outer (first) dimension will be 2, with the first containing
	 *         min values and the second containing max values. The inner (second) dimension will be the number of independent
	 *         parameters for the rating plus one. The first value will be the extent for the first independent parameter, and
	 *         the last value will be the extent for the dependent parameter.
	 */
	public double[][] getRatingExtents() throws RatingException;
	
	/**
	 * Retrieves the min and max value for each parameter of the rating, in the current ratingUnits.
	 * @param ratingTime The time to use in determining the rating extents
	 * @return The min and max values for each parameter. The outer (first) dimension will be 2, with the first containing
	 *         min values and the second containing max values. The inner (second) dimension will be the number of independent
	 *         parameters for the rating plus one. The first value will be the extent for the first independent parameter, and
	 *         the last value will be the extent for the dependent parameter.
	 */
	public double[][] getRatingExtents(long ratingTime) throws RatingException;
	
	/**
	 * Retrieves the effective dates of the rating in milliseconds, one for each contained rating
	 */
	public long[] getEffectiveDates();
	
	/**
	 * Retrieves the creation dates of the rating in milliseconds, one for each contained rating
	 */
	public long[] getCreateDates();

	/**
	 * Finds the dependent value for a single independent value.  The rating must be for a single independent parameter.
	 * @param indVal The independent value to rate.
	 * @return The dependent value
	 * @throws RatingException
	 */
	public abstract double rate(double indVal) throws RatingException;

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public abstract double rateOne(double... indVals) throws RatingException;

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public abstract double rateOne2(double[] indVals) throws RatingException;

	/**
	 * Finds multiple dependent values for multiple single independent values.  The rating must be for a single independent parameter.
	 * @param indVals The independent values to rate
	 * @return The dependent values
	 * @throws RatingException
	 */
	public abstract double[] rate(double[] indVals) throws RatingException;

	/**
	 * Finds multiple dependent values for multiple sets of independent values.  The rating must be for as many independent
	 * parameters as the length of each independent parameter set.
	 * @param indVals The independent values to rate. Each set of independent values must be the same length.
	 * @return The dependent values
	 * @throws RatingException
	 */
	public abstract double[] rate(double[][] indVals) throws RatingException;

	/**
	 * Finds the dependent value for a single independent value at a specified time.  The rating must be for a single independent parameter.
	 * @param valTime The time associated with the value to rate, in Java milliseconds
	 * @param indVal The independent value to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public abstract double rate(long valTime, double indVal) throws RatingException;

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param valTime The time associated with the set of value to rate, in Java milliseconds
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public abstract double rateOne(long valTime, double... indVals) throws RatingException;

	/**
	 * Finds the dependent value for a set of independent values. The rating must be for as many independent parameters as there are arguments.
	 * @param valTime The time associated with the set of value to rate, in Java milliseconds
	 * @param indVals The independent parameters to rate
	 * @return The dependent value
	 * @throws RatingException
	 */
	public abstract double rateOne2(long valTime, double[] indVals) throws RatingException;

	/**
	 * Finds multiple dependent values for multiple single independent values at a specified time.  The rating must be for a single independent parameter.
	 * @param valTime The time associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate
	 * @return The dependent values
	 * @throws RatingException
	 */
	public abstract double[] rate(long valTime, double[] indVals) throws RatingException;

	/**
	 * Finds multiple dependent values for multiple single independent and times.  The rating must be for a single independent parameter.
	 * @param valTimes The times associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate
	 * @return The dependent values
	 * @throws RatingException
	 */
	public abstract double[] rate(long[] valTimes, double[] indVals) throws RatingException;

	/**
	 * Finds multiple dependent values for multiple sets of independent values at a specified time.  The rating must be for as many independent
	 * parameters as the length of each independent parameter set.
	 * @param valTime The time associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate. Each set of independent values must be the same length.
	 * @return The dependent values
	 * @throws RatingException
	 */
	public abstract double[] rate(long valTime, double[][] indVals) throws RatingException;

	/**
	 * Finds multiple dependent values for multiple sets of independent values and times.  The rating must be for as many independent
	 * parameters as the length of each independent parameter set.
	 * @param valTimes The time associated with the values to rate, in Java milliseconds
	 * @param indVals The independent values to rate. Each set of independent values must be the same length.
	 * @return The dependent values
	 * @throws RatingException
	 */
	public abstract double[] rate(long[] valTimes, double[][] indVals) throws RatingException;
	
	/**
	 * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
	 * @param tsc The TimeSeriesContainer of independent values.
	 * @return The TimeSeriesContainer of dependent values.
	 * @throws RatingException
	 */
	public abstract TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException;
	
	/**
	 * Rates the values in the specified TimeSeriesContainer objects to generate a resulting TimeSeriesContainer. The rating must be for as many independent parameters as the length of tscs.
	 * @param tscs The TimeSeriesContainers of independent values, one for each independent parameter.
	 * @return The TimeSeriesContainer of dependent values.
	 * @throws RatingException
	 */
	public abstract TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException;
	
	/**
	 * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
	 * @param tsm The TimeSeriesMath of independent values.
	 * @return The TimeSeriesMath of dependent values.
	 * @throws RatingException
	 */
	public abstract TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException;
	
	/**
	 * Rates the values in the specified TimeSeriesMath objects to generate a resulting TimeSeriesMath. The rating must be for as many independent parameters as the length of tscs.
	 * @param tsms The TimeSeriesMaths of independent values, one for each independent parameter.
	 * @return The TimeSeriesMath of dependent values.
	 * @throws RatingException
	 */
	public abstract TimeSeriesMath rate(TimeSeriesMath[] tsms) throws RatingException;

	/**
	 * Finds the independent value for a single independent value.  The rating must be for a single independent parameter.
	 * @param depVal The dependent value to rate.
	 * @return The independent value
	 * @throws RatingException
	 */
	public abstract double reverseRate(double depVal) throws RatingException;

	/**
	 * Finds multiple independent values for multiple single independent values.  The rating must be for a single independent parameter.
	 * @param depVals The dependent values to rate
	 * @return The independent values
	 * @throws RatingException
	 */
	public abstract double[] reverseRate(double[] depVals) throws RatingException;

	/**
	 * Finds the independent value for a single independent value at a specified time.  The rating must be for a single independent parameter.
	 * @param valTime The time associated with the value to rate, in Java milliseconds
	 * @param depVal The dependent value to rate
	 * @return The independent value
	 * @throws RatingException
	 */
	public abstract double reverseRate(long valTime, double depVal) throws RatingException;

	/**
	 * Finds multiple independent values for multiple single independent values at a specified time.  The rating must be for a single independent parameter.
	 * @param valTime The time associated with the values to rate, in Java milliseconds
	 * @param depVals The dependent values to rate
	 * @return The independent values
	 * @throws RatingException
	 */
	public abstract double[] reverseRate(long valTime, double[] depVals) throws RatingException;

	/**
	 * Finds multiple independent values for multiple single independent and times.  The rating must be for a single independent parameter.
	 * @param valTimes The times associated with the values to rate, in Java milliseconds
	 * @param depVals The dependent values to rate
	 * @return The independent values
	 * @throws RatingException
	 */
	public abstract double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException;

	/**
	 * Rates the values in the specified TimeSeriesContainer to generate a resulting TimeSeriesContainer. The rating must be for a single independent parameter.
	 * @param tsc The TimeSeriesContainer of dependent values.
	 * @return The TimeSeriesContainer of independent values.
	 * @throws RatingException
	 */
	public abstract TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException;

	/**
	 * Rates the values in the specified TimeSeriesMath to generate a resulting TimeSeriesMath. The rating must be for a single independent parameter.
	 * @param tsm The TimeSeriesMath of dependent values.
	 * @return The TimeSeriesMath of independent values.
	 * @throws RatingException
	 */
	public abstract TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException;

	/**
	 * Retrieves the number of independent parameters for this rating.
	 * @return The number of independent parameters for this rating.
	 * @throws RatingException
	 */
	public abstract int getIndParamCount() throws RatingException;

}