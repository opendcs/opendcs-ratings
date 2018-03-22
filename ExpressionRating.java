package hec.data.cwmsRating;

import static hec.lang.Const.UNDEFINED_DOUBLE;
import static hec.util.TextUtil.replaceAll;
import hec.data.RatingException;
import hec.data.Units;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.ExpressionRatingContainer;
import hec.hecmath.computation.MathExpression;
import hec.hecmath.computation.Variable;
import hec.hecmath.computation.VariableSet;
import hec.lang.Observable;
import hec.util.TextUtil;

import java.util.Arrays;

/**
 * Class for using mathematical expressions for ratings
 * @author Mike Perryman
 */
public class ExpressionRating extends AbstractRating {
	/**
	 * The mathematical expression used to create this rating
	 */
	protected String expressionString = null;
	/**
	 * The object used to rate input values
	 */
	protected MathExpression expression = null;
	/**
	 * The variables for the "expression" field - one variable for each independent parameter
	 */
	protected Variable[] variables = null;
	
	/**
	 * Public Constructor
	 * @param expr The Mathematical expression for the rating. Independent parameters are specified by ARG1 - ARG9
	 *        (case insensitive).  The expression can be specified in infix (algebraic), prefix, postfix (RPN), or 
	 *        S-Expression (lisp) notation. Note that this constructor does't set the transition start date. 
	 * @param officeId The identifier of the office that owns this rating
	 * @param ratingSpecId The rating specification identifier
	 * @param ratingUnitsId The units identifier
	 * @param effectiveDate The effective date of the rating. The effective date is the earliest date/time for which the rating should be applied.
	 * @param createDate The creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 *        This may be later than the effective date 
	 * @param active Specifies whether the rating is currently active
	 * @param desription The description of the rating        
	 * @throws RatingException
	 */
	public ExpressionRating(
			String expr,
			String officeId,
			String ratingSpecId,
			String ratingUnitsId,
			long effectiveDate,
			long createDate,
			boolean active,
			String description) throws RatingException {
		init(expr, officeId, ratingSpecId, ratingUnitsId, effectiveDate, createDate, active, description);
	}
	/**
	 * Public Constructor from ExpressionRatingContainer
	 * @param erc The ExpressionRatingContainer to initialize from
	 * @throws RatingException
	 */
	public ExpressionRating(ExpressionRatingContainer erc) throws RatingException {
		synchronized(this) {
			super._setData(erc);
			setExpression(erc.expression);
			observationTarget = new Observable();
		}

	}
	/**
	 * Retrieves the mathematical expression for this rating
	 * @return
	 */
	public String getExpression() {
		synchronized(this) {
			return expressionString;
		}
	}
	/**
	 * Sets this rating's mathematical expression
	 * @param expr The mathematical expression
	 * @throws RatingException
	 */
	public void setExpression(String expr) throws RatingException {
		synchronized(this) {
			try {
				String expr2 = replaceAll(expr, "(^|\\W)(arg|i)([1-9])", "$1\\$$2$3", "i");
				expression = new MathExpression(expr2);
				VariableSet varset = expression.getVariables();
				String[] varnames = new String[varset.getVariableCount()];
				varset.getVariableNames().toArray(varnames);
				Arrays.sort(varnames);
				variables = new Variable[varnames.length];
				for (int i = 0; i < varnames.length; ++i) {
					variables[i] = varset.getVariable(varnames[i]);
				}
				expressionString = expr;
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		synchronized(this) {
			double[][] extents = new double[2][getIndParamCount()+1];
			Arrays.fill(extents[0], Double.NEGATIVE_INFINITY);
			Arrays.fill(extents[1], Double.POSITIVE_INFINITY);
			return extents;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(double)
	 */
	@Override
	public double rate(double pIndVal) throws RatingException {
		double[] indVals = {pIndVal};
		return rate(indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(double[])
	 */
	@Override
	public double rateOne(double... pIndVals) throws RatingException {
		return rateOne2(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(double[])
	 */
	@Override
	public double rateOne2(double[] pIndVals) throws RatingException {
		double[][] indVals = {pIndVals};
		return rate(indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rateOne(double[])
	 */
	@Override
	public double[] rate(double[] pIndVals) throws RatingException 
	{
		synchronized(this) {
			if (variables.length != 1) {
				throw new RatingException(String.format("Data has 1 independent parameter; rating %s requires %d",  ratingSpecId, this.getIndParamCount()));
			}
			try {
				String[] dataUnits = getDataUnits();
				String[] ratingUnits = getRatingUnits();
				for (int i = 0; i < ratingUnits.length; ++i) {
					if (TextUtil.equals(dataUnits[i], ratingUnits[i])) {
						dataUnits[i] = ratingUnits[i] = null;
					}
					else if(Units.canConvertBetweenUnits(dataUnits[i], ratingUnits[i])) {
					}
					else {
						String msg = String.format("Cannot convert from \"%s\" to \"%s\".", dataUnits[i], ratingUnits[i]);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg + "  Rating will be performed on unconverted values.");
					}
				}
				double[] rated = new double[pIndVals.length];
				for (int i = 0; i < pIndVals.length; ++i) {
					variables[0].setValue(convertUnits(pIndVals[i], dataUnits[0], ratingUnits[0]));
					rated[i] = convertUnits(expression.evaluate(), ratingUnits[1], dataUnits[1]);
				}
				return rated;
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] pIndVals) throws RatingException {
		synchronized(this) {
			for (int i = 1; i < pIndVals.length; ++i) {
				if (pIndVals[i].length != pIndVals[0].length) {
					throw new RatingException("Independent value sets have varying lengths.");
				}
			}
			if (pIndVals[0].length != variables.length) {
				throw new RatingException(String.format("Data has %d independent parameters; rating %s requires %d", pIndVals[0].length, this.ratingSpecId, variables.length));
			}
			try {
				String[] dataUnits = getDataUnits();
				String[] ratingUnits = getRatingUnits();
				for (int i = 0; i < ratingUnits.length; ++i) {
					if (TextUtil.equals(dataUnits[i], ratingUnits[i])) {
						dataUnits[i] = ratingUnits[i] = null;
					}
					else if(Units.canConvertBetweenUnits(dataUnits[i], ratingUnits[i])) {
					}
					else {
						String msg = String.format("Cannot convert from \"%s\" to \"%s\".", dataUnits[i], ratingUnits[i]);
						if (!allowUnsafe) throw new RatingException(msg);
						if (warnUnsafe) logger.warning(msg + "  Rating will be performed on unconverted values.");
					}
				}
				double[] rated = new double[pIndVals.length];
				for (int i = 0; i < pIndVals.length; ++i) {
					for (int j = 0; j < variables.length; ++j) {
						if (pIndVals[i][j] == UNDEFINED_DOUBLE) {
							rated[i] = UNDEFINED_DOUBLE;
							break;
						}
						variables[j].setValue(convertUnits(pIndVals[i][j], dataUnits[j], ratingUnits[j]));
					}
					if (rated[i] != UNDEFINED_DOUBLE) {
						rated[i] = convertUnits(expression.evaluate(), ratingUnits[variables.length], dataUnits[variables.length]);
					}
				}
				return rated;
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double)
	 */
	@Override
	public double rate(long pValTime, double pIndVal) throws RatingException {
		// ignores time
		return rate(pIndVal);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double[])
	 */
	@Override
	public double rateOne(long pValTime, double... pIndVals) throws RatingException {
		// ignores time
		return rateOne(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double[])
	 */
	@Override
	public double rateOne2(long pValTime, double[] pIndVals) throws RatingException {
		// ignores time
		return rateOne2(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rateOne(long, double[])
	 */
	@Override
	public double[] rate(long pValTime, double[] pIndVals) throws RatingException {
		// ignores time
		return rate(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long[], double[])
	 */
	@Override
	public double[] rate(long[] pValTimes, double[] pIndVals) throws RatingException {
		// ignores time
		return rate(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double[][])
	 */
	@Override
	public double[] rate(long pValTime, double[][] pIndVals) throws RatingException {
		return rate(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long[], double[][])
	 */
	@Override
	public double[] rate(long[] pValTimes, double[][] pIndVals) throws RatingException {
		// ignores time
		return rate(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
		throw new RatingException("Reverse rating is not supported for formula-based ratings.");
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getIndParamCount()
	 */
	@Override
	public int getIndParamCount() throws RatingException {
		synchronized(this) {
			return variables.length;
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData()
	 */
	@Override
	public ExpressionRatingContainer getData() {
		synchronized(this) {
			ExpressionRatingContainer erc = new ExpressionRatingContainer();
			super.getData(erc);
			erc.expression = expressionString;
			return erc;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setData(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer rc) throws RatingException {
		synchronized(this) {
			if (!(rc instanceof ExpressionRatingContainer)) throw new RatingException("setData() requires a ExpressionRatingContainer object.");
			try {
				super._setData(rc);
				ExpressionRatingContainer erc = (ExpressionRatingContainer)rc;
				this.setExpression(erc.expression);
				observationTarget.setChanged();
				observationTarget.notifyObservers();
			}
			catch (Throwable t) {
				if (t instanceof RatingException) throw (RatingException)t;
				throw new RatingException(t);
			}
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#toXmlString(java.lang.CharSequence, int)
	 */
	@Override
	public String toXmlString(CharSequence indent, int indentLevel) throws RatingException {
		return getData().toXml(indent, indentLevel);
	}
	/**
	 * Initialization helper for constructors and generators
	 * @param expr The Mathematical expression for the rating. Independent parameters are specified by ARG1 - ARG9
	 *        (case insensitive).  The expression can be specified in infix (algebraic), prefix, postfix (RPN), or 
	 *        S-Expression (lisp) notation.
	 * @param officeId The identifier of the office that owns this rating
	 * @param ratingSpecId The rating specification identifier
	 * @param ratingUnitsId The units identifier
	 * @param effectiveDate The effective date of the rating. The effective date is the earliest date/time for which the rating should be applied.
	 * @param createDate The creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 *        This may be later than the effective date 
	 * @param active Specifies whether the rating is currently active
	 * @param desription The description of the rating        
	 * @throws RatingException
	 */
	protected void init(
			String expr,
			String officeId,
			String ratingSpecId,
			String ratingUnitsId,
			long effectiveDate,
			long createDate,
			boolean active,
			String description) throws RatingException {
		synchronized(this) {
			if (observationTarget == null) observationTarget = new Observable();
			setOfficeId(officeId);
			setRatingSpecId(ratingSpecId);
			setRatingUnitsId(ratingUnitsId);
			setEffectiveDate(effectiveDate);
			setCreateDate(createDate);
			setActive(active);
			setDescription(description);
			setExpression(expr);
		}
	}

	@Override
	public RatingValue[] getValues(Integer defaultInterval)
	{
		return null; //TODO: Implement me;
	}
	
	@Override
	public ExpressionRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException
	{
		if (!(ratingContainer instanceof ExpressionRatingContainer))
		{
			throw new UnsupportedOperationException("Expression Ratings only support Expression Rating Containers.");
		}
		return new ExpressionRating((ExpressionRatingContainer)ratingContainer);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((ExpressionRating)obj).getData()));
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + getData().hashCode();
	}
}
