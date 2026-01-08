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
package hec.data.cwmsRating;

import java.util.Arrays;
import java.util.Observer;
import java.util.TimeZone;

import hec.data.DataSetIllegalArgumentException;
import hec.data.IRating;
import hec.data.Interval;

import hec.data.Units;
import hec.data.UnitsConversionException;
import hec.data.cwmsRating.io.IndependentValuesContainer;
import hec.data.cwmsRating.io.SourceRatingContainer;
import hec.hecmath.HecMathException;
import hec.hecmath.TimeSeriesMath;
import hec.hecmath.computation.ComputationException;
import hec.hecmath.computation.Constants.Notation;
import hec.hecmath.computation.MathExpression;
import hec.hecmath.computation.VariableSet;
import hec.io.Conversion;
import hec.io.TimeSeriesContainer;
import hec.lang.Const;
import hec.lang.Observable;
import hec.util.TextUtil;
import mil.army.usace.hec.metadata.VerticalDatum;
import mil.army.usace.hec.metadata.VerticalDatumContainer;
import mil.army.usace.hec.metadata.VerticalDatumException;

import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.data.cwmsRating.RatingConst.SEPARATOR3;
import static hec.lang.Const.UNDEFINED_DOUBLE;
import static hec.lang.Const.UNDEFINED_INT;
import static hec.lang.Const.UNDEFINED_TIME;

/**
 * Holds a source rating of a virtual or transitional rating.
 *
 * @author Mike Perryman
 */
public class SourceRating implements IRating, VerticalDatum, Observer {

		/**
		 * A math expression to be evaluated instead of using the rating set.
		 */
		protected MathExpression mathExpression = null;
		/**
		 * The variables for the match epxression, if used
		 */
		protected VariableSet vars = null;
		/**
		 * The source rating set
		 */
		protected RatingSet ratings = null;
		/**
		 * The rating units of the math expression or rating set
		 */
		protected String[] ratingUnits = null;
		/**
		 * The expected data units of the math expression or rating set
		 */
		protected String[] dataUnits = null;
		/**
		 * Object that provides the Observable-by-composition functionality
		 */
		protected Observable observationTarget = null;
		/**
		 * Default constructor
		 */
		public SourceRating() {
			this.observationTarget = new Observable();
		}
		/**
		 * Public constructor from SourceRatingContainer object
		 * @param src The SourceRatingContainer object to construct from
		 * @throws RatingException
		 */
		public SourceRating(SourceRatingContainer src) throws RatingException {
			this.observationTarget = new Observable();
			setData(src);
		}
		/**
		 * Public constructor from a RatingSet object and units
		 * @param ratingSet the RatingSet object to use
		 * @param ratingUnits the rating units to use
		 * @throws RatingException
		 */
		public SourceRating(RatingSet ratingSet, String[] ratingUnits) throws RatingException {
			this.observationTarget = new Observable();
			setRatingSet(ratingSet, ratingUnits);
		}
		/**
		 * Public constructor from a RatingSet object and units
		 * @param ratingSet the RatingSet object to use
		 * @param ratingUnitsId the rating units to use
		 * @throws RatingException
		 */
		public SourceRating(RatingSet ratingSet, String ratingUnitsId) throws RatingException {
			this.observationTarget = new Observable();
			setRatingSet(ratingSet, ratingUnitsId);
		}
		/**
		 * Public constructor from a RatingSet object and default units
		 * @param ratingSet the RatingSet object to use
		 * @throws RatingException
		 */
		public SourceRating(RatingSet ratingSet) throws RatingException {
			this.observationTarget = new Observable();
			setRatingSet(ratingSet);
		}
		/**
		 * Initialize the object from a SourceRatingContainer object
		 * @param src The SourceRatingContainer object to initialize from
		 * @throws RatingException
		 */
		void setData(SourceRatingContainer src) throws RatingException {
			try {
				if (src.mathExpression != null) {
					setMathExpression(src.mathExpression, src.units);
				}
				else if (src.rsc != null) {
					setRatingSet(RatingSetFactory.ratingSet(src.rsc), src.units);
				}
				else {
					throw new RatingException("RatingSetContainer contains no data.");
				}
			}
			catch (Exception e) {
				if (e instanceof RatingException) throw (RatingException)e;
				throw new RatingException(e);
			}
		}
		/**
		 * Retrieves the source rating as a SourceRatingContainer object
		 * @return The source rating as a SourceRatingContainer object
		 */
		SourceRatingContainer getData() {
			SourceRatingContainer src = new SourceRatingContainer();
			if (ratingUnits != null) {
				src.units = Arrays.copyOf(ratingUnits, ratingUnits.length);
			}
			if (mathExpression != null) {
				src.mathExpression = getMathExpression();
			}
			else if (ratings != null) {
				src.rsc = ratings.getData();
			}
			return src;
		}
		/**
		 * @return the mathExpression
		 */
		public String getMathExpression() {
			String mathExpressionString = null;
			if (mathExpression != null) {
				try {
					mathExpressionString = mathExpression.toNotation(Notation.INFIX);
					mathExpressionString = mathExpressionString.replaceAll("\\$([I|R]\\d+)", "$1");
				}
				catch (ComputationException e) {
					AbstractRating.logger.warning(e.getMessage());
				}
			}
			return mathExpressionString;
		}
		/**
		 * @param mathExpression the mathExpression to set
		 * @param units the units as a string array
		 * @throws ComputationException
		 */
		public void setMathExpression(String mathExpression, String[] units) throws ComputationException, RatingException {
			mathExpression = mathExpression.replaceAll("[I|R](\\d+)", "\\$I$1");
			this.mathExpression = new MathExpression(mathExpression);
			setRatingUnits(units);
			ratings = null;
		}
		/**
		 * @param mathExpression the mathExpression to set
		 * @param units the units as a string
		 * @throws ComputationException
		 */
		public void setMathExpression(String mathExpression, String units) throws ComputationException, RatingException {
			mathExpression = mathExpression.replaceAll("[I|R](\\d+)", "\\$I$1");
			this.mathExpression = new MathExpression(mathExpression);
			setRatingUnits(units);
			ratings = null;
		}
		/**
		 * @return the ratings
		 */
		public RatingSet getRatingSet() {
			return ratings;
		}
		/**
		 * @param ratings the ratings to set
		 * @param units the units as a string array
		 * @throws RatingException
		 */
		public void setRatingSet(RatingSet ratings, String[] units) throws RatingException {
			this.ratings = ratings;
			if (this.ratings != null) {
				this.ratings.addObserver(this);
			}
			setRatingUnits(units);
			mathExpression = null;
		}
		/**
		 * @param ratings the ratings to set
		 * @param units the units as a string
		 * @throws RatingException
		 */
		public void setRatingSet(RatingSet ratings, String units) throws RatingException {
			this.ratings = ratings;
			if (this.ratings != null) {
				this.ratings.addObserver(this);
			}
			setRatingUnits(units);
			mathExpression = null;
		}
		/**
		 * @param ratings the ratings to set, using the RatingUnits of the set
		 * @throws RatingException
		 */
		public void setRatingSet(RatingSet ratings) throws RatingException {
			this.ratings = ratings;
			if (this.ratings != null) {
				this.ratings.addObserver(this);
				setRatingUnits(ratings.getRatingUnits());
			}
			mathExpression = null;
		}
		/**
		 * @return the ratingUnits
		 */
		public String getRatingUnitsString() {
			StringBuilder sb = new StringBuilder();
			if (ratingUnits != null) {
				sb.append(ratingUnits[0]);
				for (int i = 1; i < ratingUnits.length; ++i) {
					sb.append(i == ratingUnits.length - 1 ? SEPARATOR2 : SEPARATOR3).append(ratingUnits[i]);
				}
			}
			return sb.toString();
		}
		/**
		 * Sets the ratingUnits array from a string array
		 * @param units The string array to set the ratingUnits array from
		 * @throws RatingException
		 */
		protected void setRatingUnits(String[] units) throws RatingException {
			if (ratings != null) {
				for (AbstractRating r : ratings.getRatings()) {
					r.setRatingUnits(units);
				}
			}
			if (mathExpression != null) {
				int varCount = mathExpression.getVariables().getVariableCount();
				if (units != null && units.length != varCount + 1) {
					throw new RatingException(String.format("Expected %d units for math expression (%s), got %d", varCount+1, mathExpression.toString(), units.length));
				}
			}
			ratingUnits = units == null ? getRatingSet().getRatingUnits() : units;
			setDataUnits(getDataUnits());
		}
		/**
		 * Sets the ratingUnits array from a string
		 * @param units The string to set the ratingUnits array from
		 * @throws RatingException
		 */
		protected void setRatingUnits(String units) throws RatingException {
			if (units == null) {
				setRatingUnits(getRatingSet().getRatingUnits());
			}
			else {
				setRatingUnits(TextUtil.split(units.replace(SEPARATOR2, SEPARATOR3), SEPARATOR3));
			}
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getNativeVerticalDatum()
		 */
		@Override
		public String getNativeVerticalDatum() throws VerticalDatumException {
			return (ratings == null) ? null : ratings.getNativeVerticalDatum();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getCurrentVerticalDatum()
		 */
		@Override
		public String getCurrentVerticalDatum() throws VerticalDatumException {
			return (ratings == null) ? null : ratings.getCurrentVerticalDatum();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#isCurrentVerticalDatumEstimated()
		 */
		@Override
		public boolean isCurrentVerticalDatumEstimated() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.isCurrentVerticalDatumEstimated();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#toNativeVerticalDatum()
		 */
		@Override
		public boolean toNativeVerticalDatum() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.toNativeVerticalDatum();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#toNGVD29()
		 */
		@Override
		public boolean toNGVD29() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.toNGVD29();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#toNAVD88()
		 */
		@Override
		public boolean toNAVD88() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.toNAVD88();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#toVerticalDatum(java.lang.String)
		 */
		@Override
		public boolean toVerticalDatum(String datum) throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.toVerticalDatum(datum);
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#forceVerticalDatum(java.lang.String)
		 */
		@Override
		public boolean forceVerticalDatum(String datum) throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.forceVerticalDatum(datum);
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getCurrentOffset()
		 */
		@Override
		public double getCurrentOffset() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.getCurrentOffset();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getCurrentOffset(java.lang.String)
		 */
		@Override
		public double getCurrentOffset(String unit) throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.getCurrentOffset();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getNGVD29Offset()
		 */
		@Override
		public double getNGVD29Offset() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.getNGVD29Offset();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getNGVD29Offset(java.lang.String)
		 */
		@Override
		public double getNGVD29Offset(String unit) throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.getNGVD29Offset(unit);
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getNAVD88Offset()
		 */
		@Override
		public double getNAVD88Offset() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.getNAVD88Offset();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getNAVD88Offset(java.lang.String)
		 */
		@Override
		public double getNAVD88Offset(String unit) throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.getNAVD88Offset(unit);
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#isNGVD29OffsetEstimated()
		 */
		@Override
		public boolean isNGVD29OffsetEstimated() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.isNGVD29OffsetEstimated();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#isNAVD88OffsetEstimated()
		 */
		@Override
		public boolean isNAVD88OffsetEstimated() throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			return ratings.isNAVD88OffsetEstimated();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#getVerticalDatumInfo()
		 */
		@Override
		public String getVerticalDatumInfo() throws VerticalDatumException {
			return ratings == null ? null : ratings.getVerticalDatumInfo();
		}
		/* (non-Javadoc)
		 * @see hec.data.VerticalDatum#setVerticalDatumInfo(java.lang.String)
		 */
		@Override
		public void setVerticalDatumInfo(String initStr) throws VerticalDatumException {
			if (ratings == null) {
				throw new VerticalDatumException("Source rating has no ratings");
			}
			ratings.setVerticalDatumInfo(initStr);
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getName()
		 */
		@Override
		public String getName() {
			return ratings == null ? mathExpression == null ? null : mathExpression.toString() : ratings.getName();
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#setName(java.lang.String)
		 */
		@Override
		public void setName(String name) throws RatingException {
			if (ratings == null) {
				throw new RatingException("Source rating has no ratings");
			}
			ratings.setName(name);
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getRatingParameters()
		 */
		@Override
		public String[] getRatingParameters() {
			return ratings == null ? null : ratings.getRatingParameters();
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getRatingUnits()
		 */
		@Override
		public String[] getRatingUnits() {
			return ratingUnits == null ? null : ratingUnits;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getDataUnits()
		 */
		@Override
		public String[] getDataUnits() {
			return dataUnits == null ? getRatingUnits() : dataUnits;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#setDataUnits(java.lang.String[])
		 */
		@Override
		public void setDataUnits(String[] units) throws RatingException {
			if (ratings != null) {
				ratings.setDataUnits(units);
			}
			if (mathExpression != null) {
				int varCount = mathExpression.getVariables().getVariableCount();
				if (units != null && units.length != varCount + 1) {
					throw new RatingException(String.format("Expected %d units for math expression (%s), got %d", varCount+1, mathExpression.toString(), units.length));
				}
			}
			dataUnits = units == null ? null : units;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getDefaultValueTime()
		 */
		@Override
		public long getDefaultValueTime() {
			return ratings == null ? Const.UNDEFINED_TIME : ratings.getDefaultValueTime();
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#setDefaultValueTime(long)
		 */
		@Override
		public void setDefaultValueTime(long defaultValueTime) {
			if (ratings != null) {
				ratings.setDefaultValueTime(defaultValueTime);
			}
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#resetDefaultValuetime()
		 */
		@Override
		public void resetDefaultValuetime() {
			setDefaultValueTime(UNDEFINED_TIME);
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getRatingTime()
		 */
		@Override
		public long getRatingTime() {
			return ratings == null ? null : ratings.getRatingTime();
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#setRatingTime(long)
		 */
		@Override
		public void setRatingTime(long ratingTime) {
			if (ratings != null) {
				ratings.setRatingTime(ratingTime);
			}
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#resetRatingTime()
		 */
		@Override
		public void resetRatingTime() {
			setRatingTime(UNDEFINED_TIME);
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getRatingExtents()
		 */
		@Override
		public double[][] getRatingExtents() throws RatingException {
			if (ratings == null) {
				throw new RatingException("Source rating has no ratings");
			}
			return ratings.getRatingExtents();
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getRatingExtents(long)
		 */
		@Override
		public double[][] getRatingExtents(long ratingTime) throws RatingException {
			if (ratings == null) {
				throw new RatingException("Source rating has no ratings");
			}
			return ratings.getRatingExtents(ratingTime);
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getEffectiveDates()
		 */
		@Override
		public long[] getEffectiveDates() {
			return ratings == null ? null :  ratings.getEffectiveDates();
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getCreateDates()
		 */
		@Override
		public long[] getCreateDates() {
			return ratings == null ? null :  ratings.getCreateDates();
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(double)
		 */
		@Override
		public double rate(double indVal) throws RatingException {
			double result = UNDEFINED_DOUBLE;
			if (ratings != null) {
				result = ratings.rate(indVal);
			}
			else if (mathExpression != null) {
				try {
					VariableSet vars = mathExpression.getVariables();
					vars.reset();
					vars.getVariable("$I1").setValue(indVal);
					result = mathExpression.evaluate();
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return result;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rateOne(double[])
		 */
		@Override
		public double rateOne(double... indVals) throws RatingException {
			return rateOne2(indVals);
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rateOne(double[])
		 */
		@Override
		public double rateOne2(double[] indVals) throws RatingException {
			double result = UNDEFINED_DOUBLE;
			if (ratings != null) {
				result = ratings.rateOne(indVals);
			}
			else if (mathExpression != null) {
				try {
					VariableSet vars = mathExpression.getVariables();
					vars.reset();
					for (int i = 0; i < vars.getVariableCount(); ++i) {
						vars.getVariable("$I"+(i+1)).setValue(indVals[i]);
					}
					result = mathExpression.evaluate();
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return result;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(double[])
		 */
		@Override
		public double[] rate(double[] indVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.rate(indVals);
			}
			else if (mathExpression != null) {
				try {
					VariableSet vars = mathExpression.getVariables();
					vars.reset();
					results = new double[indVals.length];
					for (int i = 0; i < indVals.length; ++i) {
						vars.getVariable("$I1").setValue(indVals[i]);
						results[i] = mathExpression.evaluate();
					}
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(double[][])
		 */
		@Override
		public double[] rate(double[][] indVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.rate(indVals);
			}
			else if (mathExpression != null) {
				try {
					for (int i = 0; i < indVals.length; ++i) {
						if (indVals[i] == null || indVals[i].length != indVals[0].length) {
							throw new RatingException("Inconsistent independent values set");
						}
					}
					if (vars == null) {
						vars = mathExpression.getVariables();
					}
					results = new double[indVals.length];
					for (int i = 0; i < indVals.length; ++i) {
						for (int j = 0; j < indVals[0].length; ++j) {
							vars.getVariable("$I"+(j+1)).setValue(indVals[i][j]);
						}
						results[i] = mathExpression.evaluate();
					}
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(long, double)
		 */
		@Override
		public double rate(long valTime, double indVal) throws RatingException {
			double result = UNDEFINED_DOUBLE;
			if (ratings != null) {
				result = ratings.rate(valTime, indVal);
			}
			else if (mathExpression != null) {
				try {
					VariableSet vars = mathExpression.getVariables();
					vars.reset();
					vars.getVariable("$I1").setValue(indVal);
					result = mathExpression.evaluate();
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return result;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rateOne(long, double[])
		 */
		@Override
		public double rateOne(long valTime, double... indVals) throws RatingException {
			return rateOne2(valTime, indVals);
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rateOne(long, double[])
		 */
		@Override
		public double rateOne2(long valTime, double[] indVals) throws RatingException {
			double result = UNDEFINED_DOUBLE;
			if (ratings != null) {
				result = ratings.rateOne(valTime, indVals);
			}
			else if (mathExpression != null) {
				try {
					VariableSet vars = mathExpression.getVariables();
					vars.reset();
					for (int i = 0; i < vars.getVariableCount(); ++i) {
						vars.getVariable("$I"+(i+1)).setValue(indVals[i]);
					}
					result = mathExpression.evaluate();
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return result;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(long, double[])
		 */
		@Override
		public double[] rate(long valTime, double[] indVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.rate(valTime, indVals);
			}
			else if (mathExpression != null) {
				results = rate(indVals);
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(long[], double[])
		 */
		@Override
		public double[] rate(long[] valTimes, double[] indVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.rate(valTimes, indVals);
			}
			else if (mathExpression != null) {
				results = rate(indVals);
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(long, double[][])
		 */
		@Override
		public double[] rate(long valTime, double[][] indVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.rate(valTime, indVals);
			}
			else if (mathExpression != null) {
				results = rate(indVals);
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(long[], double[][])
		 */
		@Override
		public double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.rate(valTimes, indVals);
			}
			else if (mathExpression != null) {
				results = rate(indVals);
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(hec.io.TimeSeriesContainer)
		 */
		@Override
		public TimeSeriesContainer rate(TimeSeriesContainer tsc) throws RatingException {
			TimeSeriesContainer results = null;
			if (ratings != null) {
				results = ratings.rate(tsc);
			}
			else if (mathExpression != null) {
				// we don't have enough information to set parameter, type, or units.
				results = new TimeSeriesContainer();
				tsc.clone(results);
				double[] values = Arrays.copyOf(tsc.values, tsc.values.length);
				try {
					Units.convertUnits(values, tsc.units, ratingUnits[0]);
				}
				catch (UnitsConversionException e) {
					throw new RatingException(e);
				}
				results.values = rate(values);
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(hec.io.TimeSeriesContainer[])
		 */
		@Override
		public TimeSeriesContainer rate(TimeSeriesContainer[] tscs) throws RatingException {
			TimeSeriesContainer results = null;
			if (ratings != null) {
				results = ratings.rate(tscs);
			}
			else if (mathExpression != null) {
				// we don't have enough information to set parameter, type, or units.
				results = new TimeSeriesContainer();
				tscs[0].clone(results);
				TimeZone tz = TimeZone.getTimeZone(String.format("Etc/GMT+%+d", -tscs[0].timeZoneRawOffset));
				IndependentValuesContainer ivc = RatingUtil.tscsToIvc(tscs, ratingUnits, tz, true, true);
				VariableSet vars = mathExpression.getVariables();
				results.numberValues = ivc.valTimes.length;
				results.times = new int[results.numberValues];
				results.values = new double[results.numberValues];
				try {
					for (int i = 0; i < results.numberValues; ++i) {
						results.times[i] = Conversion.toMinutes(ivc.valTimes[i]);
						vars.reset();
						for (int v = 0; v < vars.getVariableCount(); ++v) {
							vars.getVariable("$I"+(v+1)).setValue(ivc.indVals[v][i]);
						}
						results.values[i] = mathExpression.evaluate();
					}
				}
				catch (ComputationException e) {
					throw new RatingException(e);
				}
				if (results.numberValues != tscs[0].numberValues) {
					//---------------------//
					// change the interval //
					//---------------------//
					if (results.numberValues == 0) {
						results.startTime = 0;
						results.endTime = 0;
					}
					else {
						results.startTime = results.times[0];
						results.endTime = results.times[results.times.length-1];
					}
					int intvl;
					if (results.numberValues == 1) {
						intvl = 0;
					}
					else {
						intvl = results.times[1] = results.times[0];
						for (int i = 2; i < results.numberValues; ++i) {
							if (results.times[i] - results.times[i-1] != intvl) {
								intvl = 0;
								break;
							}
						}
					}
					Interval cwmsIntvl = null;
					try {cwmsIntvl = new Interval(intvl);}
					catch (DataSetIllegalArgumentException e) {}
					String intvlStr = cwmsIntvl == null ? "0" : cwmsIntvl.getInterval();
					String[] parts = TextUtil.split(results.fullName, "/");
					if (parts.length == 8) {
						// DSS pathname
						if (intvlStr.equals("0")) {
							if (results.numberValues < 2) {
								intvlStr = "IR-MONTH";
							}
							else {
								double numPerDay = results.numberValues / ((results.endTime - results.startTime) / 1440.);
								if (numPerDay > 100.) {
									intvlStr = "IR-DAY";
								}
								else if (numPerDay > 100. / 30.) {
									intvlStr = "IR-MONTH";
								}
								else if (numPerDay > 100. / 365.) {
									intvlStr = "IR-YEAR";
								}
								else if (numPerDay > 100. / 3650.) {
									intvlStr = "IR-DECADE";
								}
								else {
									intvlStr = "IR-CENTURY";
								}
							}
						}
						else {
							intvlStr = intvlStr.toUpperCase()
					                   .replaceFirst("S$", "")
					                   .replace("MINUTE", "MIN")
					                   .replace("MONTH", "MON");
							parts[5] = intvlStr;
						}
						results.fullName = TextUtil.join("/", parts);
					}
					else {
						parts = TextUtil.split(results.fullName, ".");
						if (parts.length == 6) {
							// CWMS tsid
							parts[3] = intvlStr;
							results.fullName = TextUtil.join(".", parts);
						}
						else {
							// tscs[0].fullName is of unknown format, so leave it alone
						}
					}
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath)
		 */
		@Override
		public TimeSeriesMath rate(TimeSeriesMath tsm) throws RatingException {
			TimeSeriesMath results = null;
			if (ratings != null) {
				results = ratings.rate(tsm);
			}
			else if (mathExpression != null) {
				try {
					TimeSeriesContainer tsc = rate((TimeSeriesContainer)tsm.getData());
					results = new TimeSeriesMath(tsc);
				}
				catch (HecMathException e) {
					throw new RatingException(e);
				}
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#rate(hec.hecmath.TimeSeriesMath[])
		 */
		@Override
		public TimeSeriesMath rate(TimeSeriesMath[] tsms) throws RatingException {
			TimeSeriesMath results = null;
			TimeSeriesContainer[] tscs = new TimeSeriesContainer[tsms.length];
			try {
				for (int i = 0; i < tscs.length; ++i) {
						tscs[i] = (TimeSeriesContainer)tsms[i].getData();
				}
				TimeSeriesContainer tsc = rate(tscs);
				results = new TimeSeriesMath(tsc);
			}
			catch (HecMathException e) {
				throw new RatingException(e);
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#reverseRate(double)
		 */
		@Override
		public double reverseRate(double depVal) throws RatingException {
			double result = UNDEFINED_DOUBLE;
			if (ratings != null) {
				result = ratings.reverseRate(depVal);
			}
			else if (mathExpression != null) {
				throw new RatingException("Cannot reverse rate a source rating formula");
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return result;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#reverseRate(double[])
		 */
		@Override
		public double[] reverseRate(double[] depVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.reverseRate(depVals);
			}
			else if (mathExpression != null) {
				throw new RatingException("Cannot reverse rate a source rating formula");
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#reverseRate(long, double)
		 */
		@Override
		public double reverseRate(long valTime, double depVal) throws RatingException {
			double result = UNDEFINED_DOUBLE;
			if (ratings != null) {
				result = ratings.reverseRate(valTime, depVal);
			}
			else if (mathExpression != null) {
				throw new RatingException("Cannot reverse rate a source rating formula");
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return result;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#reverseRate(long, double[])
		 */
		@Override
		public double[] reverseRate(long valTime, double[] depVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.reverseRate(valTime, depVals);
			}
			else if (mathExpression != null) {
				throw new RatingException("Cannot reverse rate a source rating formula");
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#reverseRate(long[], double[])
		 */
		@Override
		public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
			double[] results = null;
			if (ratings != null) {
				results = ratings.reverseRate(valTimes, depVals);
			}
			else if (mathExpression != null) {
				throw new RatingException("Cannot reverse rate a source rating formula");
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#reverseRate(hec.io.TimeSeriesContainer)
		 */
		@Override
		public TimeSeriesContainer reverseRate(TimeSeriesContainer tsc) throws RatingException {
			TimeSeriesContainer results = null;
			if (ratings != null) {
				results = ratings.reverseRate(tsc);
			}
			else if (mathExpression != null) {
				throw new RatingException("Cannot reverse rate a source rating formula");
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#reverseRate(hec.hecmath.TimeSeriesMath)
		 */
		@Override
		public TimeSeriesMath reverseRate(TimeSeriesMath tsm) throws RatingException {
			TimeSeriesMath results = null;
			if (ratings != null) {
				results = ratings.reverseRate(tsm);
			}
			else if (mathExpression != null) {
				throw new RatingException("Cannot reverse rate a source rating formula");
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return results;
		}
		/* (non-Javadoc)
		 * @see hec.data.IRating#getIndParamCount()
		 */
		@Override
		public int getIndParamCount() throws RatingException {
			int count = UNDEFINED_INT;
			if (ratings != null) {
				count = ratings.getIndParamCount();
			}
			else if (mathExpression != null) {
				count = mathExpression.getVariables().getVariableCount();
			}
			else {
				throw new RatingException("Source rating has no information.");
			}
			return count;
		}
		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj) {
			return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((SourceRating)obj).getData()));
		}
		/* (non-Javadoc)
		 * @see hec.data.cwmsRating.AbstractRating#hashCode()
		 */
		@Override
		public int hashCode() {
			return getClass().getName().hashCode() + getData().hashCode();
		}
		@Override
		public void update(java.util.Observable o, Object arg) {
			synchronized(this) {
				observationTarget.setChanged();
				observationTarget.notifyObservers();
			}
		}
		/**
		 * Adds an Observer to this SourceRating. The Observer will be notified of any changes to this SourceRating
		 * @param o The Observer object to add
		 * @see java.util.Observer
		 */
		public void addObserver(Observer o) {
			synchronized(observationTarget) {
				observationTarget.addObserver(o);
			}
		}
		/**
		 * Deletes an Observer from this SourceRating. The Observer will no longer be notified of any changes to this SourceRating
		 * @param o The Observer object to delete
		 * @see java.util.Observer
		 */
		public void deleteObserver(Observer o) {
			synchronized(observationTarget) {
				observationTarget.deleteObserver(o);
			}
		}

	/**
	 * Returns the vertical datum container from the ratings if the ratings is not null.
	 * @return
	 */
	@Override
	public VerticalDatumContainer getVerticalDatumContainer() {
		VerticalDatumContainer retval = null;
		if(ratings != null) {
			retval = ratings.getVerticalDatumContainer();
		}
		return retval;
	}

	/**
	 * Sets the vertical datum container on the ratings if the ratings is not null.
	 *
	 * @param vdc
	 */
	public void setVerticalDatumContainer(VerticalDatumContainer vdc) {
		if(ratings != null) {
			ratings.setVerticalDatumContainer(vdc);
		}
	}
}
