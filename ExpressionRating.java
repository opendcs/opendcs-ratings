package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.activeXpath;
import static hec.data.cwmsRating.RatingConst.createDateXpath;
import static hec.data.cwmsRating.RatingConst.descriptionXpath;
import static hec.data.cwmsRating.RatingConst.effectiveDateXpath;
import static hec.data.cwmsRating.RatingConst.formulaXpath;
import static hec.data.cwmsRating.RatingConst.inRangeMethodXpath;
import static hec.data.cwmsRating.RatingConst.indParamNodesXpath;
import static hec.data.cwmsRating.RatingConst.indParamPosXpath;
import static hec.data.cwmsRating.RatingConst.indParamsNodeXpath;
import static hec.data.cwmsRating.RatingConst.initXmlParsing;
import static hec.data.cwmsRating.RatingConst.officeIdXpath;
import static hec.data.cwmsRating.RatingConst.outRangeHighMethodXpath;
import static hec.data.cwmsRating.RatingConst.outRangeLowMethodXpath;
import static hec.data.cwmsRating.RatingConst.ratingSpecIdXpath;
import static hec.data.cwmsRating.RatingConst.unitsIdXpath;
import static hec.lang.Const.UNDEFINED_DOUBLE;
import static hec.util.TextUtil.replaceAll;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static javax.xml.xpath.XPathConstants.STRING;
import hec.data.RatingException;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.ExpressionRatingContainer;
import hec.heclib.util.HecTime;
import hec.hecmath.computation.MathExpression;
import hec.hecmath.computation.Variable;
import hec.hecmath.computation.VariableSet;
import hec.lang.Observable;

import java.util.Arrays;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

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
	 * Static Generator from XML DOM nodes
	 * @param templateNode The node containing the rating template information
	 * @param ratingNode The node containing the rating information
	 * @return A new RatingTable object initialized with the data from the XML nodes
	 * @throws RatingException
	 */
	public static ExpressionRating fromXml(Node templateNode, Node ratingNode) throws RatingException {
		try {
			initXmlParsing();
			Node indParamsNode = (Node)indParamsNodeXpath.evaluate(templateNode, NODE);
			NodeList indParamNodes = (NodeList)indParamNodesXpath.evaluate(indParamsNode, NODESET);
			int indParamCount = indParamNodes.getLength();
			if (indParamCount == 0) {
				throw new RatingException("Rating template has no independent parameters.");
			}
			RatingMethod[] inRangeMethods = new RatingMethod[indParamCount];
			RatingMethod[] outRangeLowMethods = new RatingMethod[indParamCount];
			RatingMethod[] outRangeHighMethods = new RatingMethod[indParamCount];
			for (int i = 0; i < indParamCount; ++i) {
				Node indParamNode = indParamNodes.item(i);
				if ((Double)indParamPosXpath.evaluate(indParamNode, NUMBER) != i+1) {
					throw new RatingException("Rating template has independent parameters out of order");
				}
				inRangeMethods[i] = RatingMethod.fromString((String)inRangeMethodXpath.evaluate(indParamNode, STRING));
				outRangeLowMethods[i] = RatingMethod.fromString((String)outRangeLowMethodXpath.evaluate(indParamNode, STRING));
				outRangeHighMethods[i] = RatingMethod.fromString((String)outRangeHighMethodXpath.evaluate(indParamNode, STRING));
			}
			String officeId = (String)officeIdXpath.evaluate(ratingNode, STRING);
			String ratingSpecId = (String)ratingSpecIdXpath.evaluate(ratingNode, STRING);
			String ratingUnitsId = (String)unitsIdXpath.evaluate(ratingNode, STRING);
			String effectiveDateStr = (String)effectiveDateXpath.evaluate(ratingNode, STRING);
			String createDateStr = (String)createDateXpath.evaluate(ratingNode, STRING);
			String activeStr = (String)activeXpath.evaluate(ratingNode, STRING); 
			String description = (String)descriptionXpath.evaluate(ratingNode, STRING);
			String expression = (String)formulaXpath.evaluate(ratingNode, STRING);
			HecTime t = new HecTime(HecTime.SECOND_GRANULARITY);
			t.set(effectiveDateStr);
			long effectiveDate = t.getTimeInMillis();
			t.set(createDateStr);
			long createDate = t.getTimeInMillis();
			boolean active = activeStr.equalsIgnoreCase("true");
			return new ExpressionRating(
					expression,
					officeId,
					ratingSpecId,
					ratingUnitsId,
					effectiveDate,
					createDate,
					active,
					description);
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}

	/**
	 * Public Constructor
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
		init(	erc.expression, 
				erc.officeId, 
				erc.ratingSpecId, 
				erc.unitsId, 
				erc.effectiveDateMillis, 
				erc.createDateMillis, 
				erc.active, 
				erc.description);
	}
	/**
	 * Retrieves the mathematical expression for this rating
	 * @return
	 */
	public String getExpression() {
		return expressionString;
	}
	/**
	 * Sets this rating's mathematical expression
	 * @param expr The mathematical expression
	 * @throws RatingException
	 */
	public void setExpression(String expr) throws RatingException {
		try {
			expression = new MathExpression(replaceAll(expr, "(^|\\W)(arg)([1-9])(\\W|$)", "$1\\$$3$4", "i"));
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
		double[][] indVals = {pIndVals};
		return rate(indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rateOne(double[])
	 */
	@Override
	public double[] rate(double[] pIndVals) throws RatingException {
		if (variables.length != 1) {
			throw new RatingException(String.format("Data has 1 independent parameter; rating %s requires %d",  ratingSpecId, this.getIndParamCount()));
		}
		try {
			double[] rated = new double[pIndVals.length];
			for (int i = 0; i < pIndVals.length; ++i) {
				variables[0].setValue(pIndVals[i]);
				rated[i] = expression.evaluate();
			}
			return rated;
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] pIndVals) throws RatingException {
		
		for (int i = 1; i < pIndVals.length; ++i) {
			if (pIndVals[i].length != pIndVals[0].length) {
				throw new RatingException("Independent value sets have varying lengths.");
			}
		}
		if (pIndVals[0].length != variables.length) {
			throw new RatingException(String.format("Data has %d independent parameters; rating %s requires %d", pIndVals[0].length, this.ratingSpecId, variables.length));
		}
		try {
			double[] rated = new double[pIndVals.length];
			for (int i = 0; i < pIndVals.length; ++i) {
				for (int j = 0; j < variables.length; ++j) {
					if (pIndVals[i][j] == UNDEFINED_DOUBLE) {
						rated[i] = UNDEFINED_DOUBLE;
						break;
					}
					variables[j].setValue(pIndVals[i][j]);
				}
				if (rated[i] != UNDEFINED_DOUBLE) {
					rated[i] = expression.evaluate();
				}
			}
			return rated;
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
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
		return variables.length;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData()
	 */
	@Override
	public AbstractRatingContainer getData() {
		ExpressionRatingContainer erc = new ExpressionRatingContainer();
		super.getData(erc);
		erc.expression = expressionString;
		return erc;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setData(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer rc) throws RatingException {
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

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#toXmlString(java.lang.CharSequence, int)
	 */
	@Override
	public String toXmlString(CharSequence indent, int indentLevel) throws RatingException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indentLevel; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		sb.append(String.format("%s<rating office-id=\"%s\">\n", prefix, officeId))
		  .append(String.format("%s%s<rating-spec-id>%s</rating-spec-id>\n", prefix, indent, ratingSpecId))
		  .append(String.format("%s%s<units-id>%s</units-id>\n", prefix, indent, ratingUnitsId));
		HecTime t = new HecTime(HecTime.SECOND_GRANULARITY);
		t.setTimeInMillis(effectiveDate);
		sb.append(String.format("%s%s<effective-date>%s</effective-date>\n", prefix, indent, t.getXMLDateTime(0).replaceAll("Z", "")));
		t.setTimeInMillis(createDate);
		sb.append(String.format("%s%s<create-date>%s</create-date>\n", prefix, indent, t.getXMLDateTime(0).replaceAll("Z", "")))
		  .append(String.format("%s%s<active>%s</active>\n", prefix, indent, active ? "true" : "false"));
		if (description == null || description.length() == 0) {
			sb.append(String.format("%s%s<description/>\n", prefix, indent));
		}
		else {
			sb.append(String.format("%s%s<description>%s</description>\n", prefix, indent, description));
		}
		sb.append(String.format("%s%s<formula>%s</formula>\n", prefix, indent, expressionString));
		return sb.toString();
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
