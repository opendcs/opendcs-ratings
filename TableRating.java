package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.*;
import static hec.util.TextUtil.*;
import static hec.lang.Const.UNDEFINED_DOUBLE;
import static hec.lang.Const.UNDEFINED_LONG;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.NUMBER;
import static javax.xml.xpath.XPathConstants.STRING;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.RatingValueContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.heclib.util.HecTime;
import hec.lang.Observable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
/**
 * Table-based (lookup) rating implementation.
 * 
 * @author Mike Perryman
 */
public class TableRating extends AbstractRating {
	/**
	 * The rating values
	 */
	protected RatingValue[] values = null;
	/**
	 * The rating extension values, if any
	 */
	protected RatingValue[] extensionValues = null;
	/**
	 * The combination of rating and extension values
	 */
	protected RatingValue[] effectiveValues = null;
	/**
	 * A permutation of this rating used to perform reverseRate() methods
	 */
	protected TableRating reversed = null;
	/**
	 * The rating behavior for when the value to be rated is in the range of the independent values of the rating.
	 */
	protected RatingMethod inRangeMethod = RatingMethod.LINEAR;
	/**
	 * The rating behavior for when the value to be rated sorts to a position before the first independent value of the rating.
	 */
	protected RatingMethod outRangeLowMethod = RatingMethod.ERROR;
	/**
	 * The rating behavior for when the value to be rated sorts to a position after the last independent value of the rating.
	 */
	protected RatingMethod outRangeHighMethod = RatingMethod.ERROR;
	/**
	 * Holds properties of the sequence of independent values
	 */
	protected SequenceProperties props = null;

	/**
	 * Generator from XML DOM nodes
	 * @param templateNode The node containing the rating template information
	 * @param ratingNode The node containing the rating information
	 * @return A new RatingTable object initialized with the data from the XML nodes
	 * @throws RatingException
	 */
	public static TableRating fromXml(Node templateNode, Node ratingNode) throws RatingException {
		try {
			initXmlParsing();
			//-------------------------//
			// parse the template node //
			//-------------------------//
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
			//-----------------------//
			// parse the rating node //
			//-----------------------//
			String officeId = (String)officeIdXpath.evaluate(ratingNode, STRING);
			String ratingSpecId = (String)ratingSpecIdXpath.evaluate(ratingNode, STRING);
			String unitsId = (String)unitsIdXpath.evaluate(ratingNode, STRING);
			String effectiveDateStr = (String)effectiveDateXpath.evaluate(ratingNode, STRING);
			String createDateStr = (String)createDateXpath.evaluate(ratingNode, STRING);
			String activeStr = (String)activeXpath.evaluate(ratingNode, STRING); 
			String description = (String)descriptionXpath.evaluate(ratingNode, STRING);
			HecTime t = new HecTime(HecTime.SECOND_GRANULARITY);
			t.set(effectiveDateStr);
			long effectiveDate = t.getTimeInMillis();
			t.set(createDateStr);
			long createDate = t.getTimeInMillis();
			boolean active = activeStr.equalsIgnoreCase("true");
			//-------------------------//
			// parse the rating points //
			//-------------------------//
			NodeList pointGroupNodes = (NodeList)ratingPointGroupNodesXpath.evaluate(ratingNode, NODESET);
			int pointGroupCount = pointGroupNodes.getLength();
			if (pointGroupCount == 0) {
				throw new RatingException("Rating has no independent parameters.");
			}
			if (pointGroupCount > 1 && indParamCount == 1) {
				throw new RatingException("Multiple point groups is not allowed with a single independent parameter.");
			}
			double[] otherIndParamVals = new double[indParamCount-1];
			double[] lastIndParamVals = new double[indParamCount-1];
			for (int i = 0; i < indParamCount - 1; ++i) lastIndParamVals[i] = UNDEFINED_DOUBLE;
			List<List<RatingValue>> vals = new ArrayList<List<RatingValue>>(indParamCount);
			for (int i = 0; i < indParamCount; ++i) vals.add(new ArrayList<RatingValue>());
			for (int i = 0; i < pointGroupCount; ++i) {
				Node pointGroupNode = pointGroupNodes.item(i);
				NodeList otherIndParamNodes = (NodeList)otherIndParamNodesXpath.evaluate(pointGroupNode, NODESET);
				if (otherIndParamNodes.getLength() != indParamCount - 1) {
					throw new RatingException(String.format("Point group %d has incorrect number of independent parameters.", i+1));
				}
				for (int j = 0; j < indParamCount-1; ++j) {
					Node otherIndParamNode = otherIndParamNodes.item(j);
					if ((Double)indParamPosXpath.evaluate(otherIndParamNode, NUMBER) != j+1) {
						throw new RatingException("Point group has independent parameters out of order");
					}
					otherIndParamVals[j] = (Double)otherIndValXPath.evaluate(otherIndParamNode, NUMBER);
					if (lastIndParamVals[j] != UNDEFINED_DOUBLE && otherIndParamVals[j] != lastIndParamVals[j]) {
						for (int k = indParamCount-2; k >=j; --k) {
							List<RatingValue> list = vals.get(k+1);
							TableRating rt = new TableRating(
									list.toArray(new RatingValue[list.size()]),
									null,
									inRangeMethods[k+1],
									outRangeLowMethods[k+1],
									outRangeHighMethods[k+1]);
							RatingValue rv = new RatingValue(lastIndParamVals[k], rt); 
							list = vals.get(k);
							list.add(rv);
							for (int m = k+1; m < indParamCount;++m) {
								vals.get(m).clear();
								if (m < indParamCount - 1) lastIndParamVals[m] = UNDEFINED_DOUBLE;
							}
						}
					}
					lastIndParamVals[j] = otherIndParamVals[j];
				}
				NodeList pointNodes = (NodeList)pointNodesXpath.evaluate(pointGroupNode, NODESET);
				for (RatingValue rv : RatingValue.fromXml(pointNodes)) vals.get(indParamCount-1).add(rv);
			}
			for (int i = indParamCount-2; i >=0; --i) {
				List<RatingValue> list = vals.get(i+1);
				TableRating rt = new TableRating(
						list.toArray(new RatingValue[list.size()]),
						null,
						inRangeMethods[i+1],
						outRangeLowMethods[i+1],
						outRangeHighMethods[i+1]);
				RatingValue rv = new RatingValue(lastIndParamVals[i], rt); 
				list = vals.get(i);
				list.add(rv);
			}
			//-----------------------------------//
			// parse the rating extension points //
			//-----------------------------------//
			List<List<RatingValue>> extVals = null;
			NodeList extPointGroupNodes = (NodeList)extensionPointGroupNodesXpath.evaluate(ratingNode, NODESET);
			int extPointGroupCount = extPointGroupNodes.getLength();
			if (extPointGroupCount > 0) {
				if (extPointGroupCount > 1 && indParamCount == 1) {
					throw new RatingException("Multiple extension point groups is not allowed with a single independent parameter.");
				}
				for (int i = 0; i < indParamCount - 1; ++i) lastIndParamVals[i] = UNDEFINED_DOUBLE;
				extVals = new ArrayList<List<RatingValue>>(indParamCount);
				for (int i = 0; i < indParamCount; ++i) extVals.add(new ArrayList<RatingValue>());
				for (int i = 0; i < extPointGroupCount; ++i) {
					Node pointGroupNode = extPointGroupNodes.item(i);
					NodeList otherIndParamNodes = (NodeList)otherIndParamNodesXpath.evaluate(pointGroupNode, NODESET);
					if (otherIndParamNodes.getLength() != indParamCount - 1) {
						throw new RatingException(String.format("Extension point group %d has incorrect number of independent parameters.", i+1));
					}
					for (int j = 0; j < indParamCount-1; ++j) {
						Node otherIndParamNode = otherIndParamNodes.item(j);
						if ((Double)indParamPosXpath.evaluate(otherIndParamNode, NUMBER) != j+1) {
							throw new RatingException("Extension point group has independent parameters out of order");
						}
						otherIndParamVals[j] = (Double)otherIndValXPath.evaluate(otherIndParamNode, NUMBER);
						if (lastIndParamVals[j] != UNDEFINED_DOUBLE && otherIndParamVals[j] != lastIndParamVals[j]) {
							for (int k = indParamCount-2; k >=j; --k) {
								List<RatingValue> list = extVals.get(k+1);
								TableRating rt = new TableRating(
										list.toArray(new RatingValue[list.size()]),
										null,
										inRangeMethods[k+1],
										outRangeLowMethods[k+1],
										outRangeHighMethods[k+1]);
								RatingValue rv = new RatingValue(lastIndParamVals[k], rt); 
								list = extVals.get(k);
								list.add(rv);
								for (int m = k+1; m < indParamCount;++m) {
									extVals.get(m).clear();
									if (m < indParamCount - 1) lastIndParamVals[m] = UNDEFINED_DOUBLE;
								}
							}
						}
						lastIndParamVals[j] = otherIndParamVals[j];
					}
					NodeList pointNodes = (NodeList)pointNodesXpath.evaluate(pointGroupNode, NODESET);
					for (RatingValue rv : RatingValue.fromXml(pointNodes)) extVals.get(indParamCount-1).add(rv);
				}
				for (int i = indParamCount-2; i >=0; --i) {
					List<RatingValue> list = extVals.get(i+1);
					TableRating rt = new TableRating(
							list.toArray(new RatingValue[list.size()]),
							null,
							inRangeMethods[i+1],
							outRangeLowMethods[i+1],
							outRangeHighMethods[i+1]);
					RatingValue rv = new RatingValue(lastIndParamVals[i], rt); 
					list = extVals.get(i);
					list.add(rv);
				}
			}
			return new TableRating(
					vals.get(0).toArray(new RatingValue[vals.get(0).size()]),
					extVals == null ? null : extVals.get(0).toArray(new RatingValue[extVals.get(0).size()]),
					inRangeMethods[0],
					outRangeLowMethods[0],
					outRangeHighMethods[0],
					officeId,
					ratingSpecId,
					unitsId,
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
	 * Protected Constructor for nested rating tables 
	 * @param values The table of values that comprise the rating.
	 * @param extensionValues The rating extension values
	 * @param inRangeMethod The prescribed behavior for when the value to rate falls within the range of independent values in the rating table
	 * @param outRangeLowMethod The prescribed behavior for when the value to rate would sort before the first independent value in the rating table
	 * @param outRangeHighMethod The prescribed behavior for when the value to rate would sort after the last independent value in the rating table
	 */
	protected TableRating(
			RatingValue[] values,
			RatingValue[] extensionValues,
			RatingMethod inRangeMethod,
			RatingMethod outRangeLowMethod,
			RatingMethod outRangeHighMethod) throws RatingException {
		init(	values,
				extensionValues,
				inRangeMethod,
				outRangeLowMethod,
				outRangeHighMethod,
				null,
				null,
				null,
				UNDEFINED_LONG,
				UNDEFINED_LONG,
				true,
				null);
	}
	/**
	 * Public Constructor from TableRatingContainer
	 * @param trc The TableRatingContainer object to construct from
	 * @throws RatingException
	 */
	public TableRating(TableRatingContainer trc) throws RatingException {
		RatingValue[] values = null;
		RatingValue[] extensionValues = null;
		if (trc.values != null) {
			values = new RatingValue[trc.values.length];
			for (int i = 0; i < trc.values.length; ++i) {
				values[i] = new RatingValue(trc.values[i]);
			}
		}
		if(trc.extensionValues != null) {
			extensionValues = new RatingValue[trc.extensionValues.length];
			for (int i = 0; i < trc.extensionValues.length; ++i) {
				extensionValues[i] = new RatingValue(trc.extensionValues[i]);
			}
		}
		init(	values,
				extensionValues,
				RatingMethod.fromString(trc.inRangeMethod),
				RatingMethod.fromString(trc.outRangeLowMethod),
				RatingMethod.fromString(trc.outRangeHighMethod),
				trc.officeId,
				trc.ratingSpecId,
				trc.unitsId,
				trc.effectiveDateMillis,
				trc.createDateMillis,
				trc.active,
				trc.description);
	}
	/**
	 * Public Constructor 
	 * @param values The table of values that comprise the rating.
	 * @param extensionValues The rating extension values
	 * @param inRangeMethod The prescribed behavior for when the value to rate falls within the range of independent values in the rating table
	 * @param outRangeLowMethod The prescribed behavior for when the value to rate would sort before the first independent value in the rating table
	 * @param outRangeHighMethod The prescribed behavior for when the value to rate would sort after the last independent value in the rating table
	 * @param officeId The identifier of the office that owns this rating
	 * @param ratingSpecId The rating specification identifier
	 * @param unitsId The units identifier
	 * @param effectiveDate The effective date of the rating. The effective date is the earliest date/time for which the rating should be applied.
	 * @param createDate The creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 *        This may be later than the effective date 
	 * @param active Specifies whether the rating is currently active
	 * @param desription The description of the rating        
	 * @throws RatingException
	 */
	public TableRating(
			RatingValue[] values,
			RatingValue[] extensionValues,
			RatingMethod inRangeMethod,
			RatingMethod outRangeLowMethod,
			RatingMethod outRangeHighMethod,
			String officeId,
			String ratingSpecId,
			String unitsId,
			long effectiveDate,
			long createDate,
			boolean active,
			String description) throws RatingException {
		init(	values,
				extensionValues,
				inRangeMethod,
				outRangeLowMethod,
				outRangeHighMethod,
				officeId,
				ratingSpecId,
				unitsId,
				effectiveDate,
				createDate,
				active,
				description);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(double)
	 */
	@Override
	public double rate(double pIndVal) throws RatingException {

		double[] ind_vals = {pIndVal};
		return rate(ind_vals, 0);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(double)
	 */
	@Override
	public double rateOne(double... pIndVals) throws RatingException {

		return rate(pIndVals, 0);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rateOne(double[])
	 */
	@Override
	public double[] rate(double[] pIndVals) throws RatingException {
		
		double rated[] = new double[pIndVals.length];
		for (int i = 0; i < pIndVals.length; ++i) rated[i] = this.rate(pIndVals[i]);
		return rated;
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
		if (pIndVals[0].length != getIndParamCount()) {
			throw new RatingException(String.format("Data has %d independent parameters; rating %s requires %d", pIndVals.length, this.ratingSpecId, this.getIndParamCount()));
		}
		double[] rated = new double[pIndVals.length];
		for (int i = 0; i < pIndVals.length; ++i) rated[i] = this.rate(pIndVals[i], 0);
		return rated;
	}
	
	protected double rate(double[] pIndVals, int p_offset) throws RatingException {

		double ind_val = pIndVals[p_offset];
		boolean out_range_low = false;
		boolean out_range_high = false;
		int lo = 0;
		int hi = values.length-1;
		int mid;
		double mid_ind_val;
		RatingMethod extrap_method = null;
		//--------------------------------------------------- //
		// find the interpolation/extrapolation value indices //
		//--------------------------------------------------- //
		if (props.hasIncreasing()) {
			if (ind_val < effectiveValues[lo].getIndValue()) {
				out_range_low = true;
			}
			else if (ind_val > effectiveValues[hi].getIndValue()) {
				out_range_high = true;
			}
			else {
				while (hi - lo > 1) {
					mid = (lo + hi) / 2;
					mid_ind_val = effectiveValues[mid].getIndValue();
					if (ind_val < mid_ind_val) hi = mid; else lo = mid;
				}
			}
		}
		else {
			if (ind_val > effectiveValues[lo].getIndValue()) {
				out_range_low = true;
			}
			else if (ind_val < effectiveValues[hi].getIndValue()) {
				out_range_high = true;
			}
			else {
				while (hi - lo > 1) {
					mid = (lo + hi) / 2;
					mid_ind_val = effectiveValues[mid].getIndValue();
					if (ind_val > mid_ind_val) hi = mid; else lo = mid;
				}
			}
		}
		//-------------------------//
		// handle out of range low //
		//-------------------------//
		if (out_range_low) {
			switch (outRangeLowMethod) {
			case NULL:
				return UNDEFINED_DOUBLE;
			case ERROR:
				throw new RatingException("Value is out of range low.");
			case LINEAR:
			case LOGARITHMIC:
			case LIN_LOG:
			case LOG_LIN:
				extrap_method = outRangeLowMethod;
				break;
			case PREVIOUS:
				throw new RatingException("No previous value in table.");
			case NEXT:
			case NEAREST:
			case CLOSEST:
				if (effectiveValues[0].hasDepValue()) {
					return effectiveValues[0].getDepValue();
				}
				else {
					return effectiveValues[0].getDepValues().rate(pIndVals, p_offset+1);
				}
			case LOWER:
				if (props.hasIncreasing()) throw new RatingException("No lower value in table.");
				if (effectiveValues[0].hasDepValue()) {
					return effectiveValues[0].getDepValue();
				}
				else {
					return effectiveValues[0].getDepValues().rate(pIndVals, p_offset+1);
				}
			case HIGHER:
				if (props.hasDecreasing()) throw new RatingException("No higher value in table.");
				if (effectiveValues[0].hasDepValue()) {
					return effectiveValues[0].getDepValue();
				}
				else {
					return effectiveValues[0].getDepValues().rate(pIndVals, p_offset+1);
				}
			default:
				throw new RatingException(
						"Unexpected behavior specified : "
								+ outRangeLowMethod
								+ " : "
								+ outRangeLowMethod.description());
			}
		}
		//--------------------------//
		// handle out of range high //
		//--------------------------//
		else if (out_range_high) {
			switch (outRangeHighMethod) {
			case NULL:
				return UNDEFINED_DOUBLE;
			case ERROR:
				throw new RatingException("Value is out of range high.");
			case LINEAR:
			case LOGARITHMIC:
			case LIN_LOG:
			case LOG_LIN:
				extrap_method = outRangeHighMethod;
				break;
			case NEXT:
				throw new RatingException("No next value in table.");
			case PREVIOUS:
			case NEAREST:
			case CLOSEST:
				if (effectiveValues[effectiveValues.length-1].hasDepValue()) {
					return effectiveValues[effectiveValues.length-1].getDepValue();
				}
				else {
					return effectiveValues[effectiveValues.length-1].getDepValues().rate(pIndVals, p_offset+1);
				}
			case LOWER:
				if (props.hasDecreasing()) throw new RatingException("No lower value in table.");
				if (effectiveValues[effectiveValues.length-1].hasDepValue()) {
					return effectiveValues[effectiveValues.length-1].getDepValue();
				}
				else {
					return effectiveValues[effectiveValues.length-1].getDepValues().rate(pIndVals, p_offset+1);
				}
			case HIGHER:
				if (props.hasIncreasing()) throw new RatingException("No higher value in table.");
				if (effectiveValues[effectiveValues.length-1].hasDepValue()) {
					return effectiveValues[effectiveValues.length-1].getDepValue();
				}
				else {
					return effectiveValues[effectiveValues.length-1].getDepValues().rate(pIndVals, p_offset+1);
				}
			default:
				throw new RatingException(
						"Unexpected behavior specified : "
								+ outRangeHighMethod
								+ " : "
								+ outRangeHighMethod.description());
			}
		}
		//-----------------------------------//
		// handle in range and extrapolation //
		//-----------------------------------//
		double lo_ind_val = effectiveValues[lo].getIndValue();
		double hi_ind_val = effectiveValues[hi].getIndValue();
		double lo_dep_val = effectiveValues[lo].hasDepValue() ? effectiveValues[lo].getDepValue() : effectiveValues[lo].getDepValues().rate(pIndVals, p_offset+1);
		double hi_dep_val = effectiveValues[hi].hasDepValue() ? effectiveValues[hi].getDepValue() : effectiveValues[hi].getDepValues().rate(pIndVals, p_offset+1);
		if (ind_val == lo_ind_val) return lo_dep_val;
		if (ind_val == hi_ind_val) return hi_dep_val;
		RatingMethod method = (out_range_low || out_range_high) ? extrap_method : inRangeMethod;
		switch (method) {
		case NULL:
			return UNDEFINED_DOUBLE;
		case ERROR:
			throw new RatingException("No such value in table.");
		case PREVIOUS:
			return lo_dep_val;
		case NEXT:
			return hi_dep_val;
		case LOWER:
			return props.hasIncreasing() ? lo_dep_val : hi_dep_val;
		case HIGHER:
			return props.hasDecreasing() ? lo_dep_val : hi_dep_val;
		case CLOSEST:
			return Math.abs(ind_val - lo_ind_val) < Math.abs(hi_ind_val - ind_val) ? lo_dep_val : hi_dep_val;
		default:
			break;
		}
		//---------------------------//
		// interpolate / extrapolate //
		//---------------------------//
		boolean ind_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LIN_LOG;
		boolean dep_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LOG_LIN;
		double x  = ind_val;
		double x1 = lo_ind_val;
		double x2 = hi_ind_val;
		double y1 = lo_dep_val;
		double y2 = hi_dep_val;
		if (ind_log) {
			x  = Math.log10(x);
			x1 = Math.log10(x1);
			x2 = Math.log10(x2);
			if (Double.isNaN(x) || Double.isInfinite(x)   
					|| Double.isNaN(x1) || Double.isInfinite(x1) 
					|| Double.isNaN(x2) || Double.isInfinite(x2))  {
				//-------------------------------------------------//
				// fall back from LOGARITHMIC or LOG_LIN to LINEAR //
				//-------------------------------------------------//
				x = ind_val;
				x1 = lo_ind_val;
				x2 = hi_ind_val;
				dep_log = false;
			}
		}
		if (dep_log) {
			y1 = Math.log10(y1);
			y2 = Math.log10(y2);
			if (Double.isNaN(y1) || Double.isInfinite(y1) || Double.isNaN(y2) || Double.isInfinite(y2))  {
				//-------------------------------------------------//
				// fall back from LOGARITHMIC or LIN_LOG to LINEAR //
				//-------------------------------------------------//
				x = ind_val;
				x1 = lo_ind_val;
				x2 = hi_ind_val;
				y1 = lo_dep_val;
				y2 = hi_dep_val;
				dep_log = false;
			}
		}
		double y = y1 + ((x - x1) / (x2 - x1)) * (y2 - y1);
		if (dep_log) y = Math.pow(10, y);
		return y;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double)
	 */
	@Override
	public double rate(long valTime, double pIndVal) throws RatingException {
		// ignores time
		return rate(pIndVal);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double[])
	 */
	@Override
	public double rateOne(long valTime, double... pIndVals) throws RatingException {
		// ignores time
		return rateOne(pIndVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rateOne(long, double[])
	 */
	@Override
	public double[] rate(long valTime, double[] pIndVals) throws RatingException {
		// ignores time
		return rate(pIndVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rateOne(long[], double[])
	 */
	@Override
	public double[] rate(long[] valTimes, double[] pIndVals) throws RatingException {
		// ignores times
		return rate(pIndVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double[][])
	 */
	@Override
	public double[] rate(long valTime, double[][] pIndVals) throws RatingException {
		// ignores time
		return rate(pIndVals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#rate(long[], double[][])
	 */
	@Override
	public double[] rate(long[] valTimes, double[][] pIndVals) throws RatingException {
		// ignores times
		return rate(pIndVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#reverseRate(long[], double[])
	 */
	@Override
	public double[] reverseRate(long[] valTimes, double[] depVals) throws RatingException {
		long[] times = null;
		if (valTimes == null) {
			times = new long[depVals.length];
			Arrays.fill(times, defaultValueTime);
		}
		else {
			times = valTimes;
		}
		if (times.length != depVals.length) {
			throw new RatingException("Different numbers of times and values.");
		}
		double[] indVals = new double[times.length];
		for (int i = 0; i < times.length; ++i) {
			indVals[i] = reverseRate(times[i], depVals[i]);
		}
		return indVals;
	}
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getData()
	 */
	@Override
	public AbstractRatingContainer getData() {
		TableRatingContainer trc = new TableRatingContainer();
		getData(trc);
		return trc;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setData(hec.data.cwmsRating.RatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer rc) throws RatingException {
		if (!(rc instanceof TableRatingContainer)) throw new RatingException("setData() requires a TableRatingContainer object.");
		TableRatingContainer backup = (TableRatingContainer)getData();
		try {
			super._setData(rc);
			TableRatingContainer trc = (TableRatingContainer)rc;
			for (RatingValue value : values) value.deleteObserver(this);
			RatingValue[] values = new RatingValue[trc.values.length];
			for (int i = 0; i < trc.values.length; ++i) {
				values[i] = new RatingValue(trc.values[i]);
			}
			for (RatingValue extensionValue : extensionValues) extensionValue.deleteObserver(this);
			RatingValue[] extensionValues = new RatingValue[trc.extensionValues.length];
			for (int i = 0; i < trc.extensionValues.length; ++i) {
				extensionValues[i] = new RatingValue(trc.extensionValues[i]);
			}
			init(	values,
					extensionValues,
					RatingMethod.fromString(trc.inRangeMethod),
					RatingMethod.fromString(trc.outRangeLowMethod),
					RatingMethod.fromString(trc.outRangeHighMethod),
					trc.officeId,
					trc.ratingSpecId,
					trc.unitsId,
					trc.effectiveDateMillis,
					trc.createDateMillis,
					trc.active,
					trc.description);
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
		catch (Throwable t) {
			setData(backup);
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
		sb.append(toXmlString(indent, indentLevel+1, values, false, null))
		  .append(toXmlString(indent, indentLevel+1, extensionValues, true, null))
		  .append(String.format("%s</rating>\n", prefix));
		return sb.toString();
	}
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#reverseRate(long, double)
	 */
	@Override
	public double reverseRate(long valTime, double depVal) throws RatingException {
		if (reversed == null) reverse();
		return reversed.rate(valTime, depVal);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getIndParamCount()
	 */
	@Override
	public int getIndParamCount() throws RatingException {
		return ratingSpecId == null ? (values == null ? 0 : getIndParamCount(values)) : getRatingParameters().length - 1;
	}
	/**
	 * Fills the specified TableRatingContainer object with information from this rating.
	 * @param trc The TableRatingObject to fill
	 */
	protected void getData(TableRatingContainer trc) {
		super.getData(trc);
		if (values != null) {
			trc.values = new RatingValueContainer[values.length];
			for (int i = 0; i < values.length; ++i) {
				trc.values[i] = values[i].getData();
			}
		}
		if (extensionValues != null) {
			trc.extensionValues = new RatingValueContainer[extensionValues.length];
			for (int i = 0; i < extensionValues.length; ++i) {
				trc.extensionValues[i] = extensionValues[i].getData();
			}
		}
		trc.inRangeMethod = inRangeMethod.toString();
		trc.outRangeLowMethod = outRangeLowMethod.toString();
		trc.outRangeHighMethod = outRangeHighMethod.toString();
	}
	/**
	 * Retreives an XML representation of this rating
	 * @param indent The string to use for each indentation level
	 * @param indentLevel The base indentation level
	 * @param values The values to output
	 * @param isExtension Flag specifying whether the "values" parameter represents a rating extension
	 * @param indValues A list of "upstream" independent values - non-null if this is a multiple independent parameter rating
	 * @return The XML text
	 * @throws RatingException
	 */
	protected String toXmlString(CharSequence indent, int indentLevel, RatingValue[] values, boolean isExtension, List<Double>indValues) throws RatingException {
		StringBuilder sb = new StringBuilder();
		if (values != null) {
			if (getIndParamCount(values) == 1) {
				for (int i = 0; i < indentLevel; ++i) sb.append(indent);
				String prefix = sb.toString();
				sb.delete(0, sb.length());
				sb.append(prefix).append(isExtension ? "<extension-points>\n" : "<rating-points>\n");
				if (indValues != null && indValues.size() > 0) {
					for (int i = 0; i < indValues.size(); ++i) {
						sb.append(String.format("%s%s<other-ind position=\"%d\" value=\"%s\"/>\n", prefix, indent, i+1, RatingValue.format(indValues.get(i))));
					}
					indValues.remove(indValues.size()-1);
				}
				for (RatingValue rv : values) {
					sb.append(String.format("%s%s<point>\n", prefix, indent))
					  .append(String.format("%s%s%s<ind>%s</ind>\n", prefix, indent, indent, RatingValue.format(rv.getIndValue())))
					  .append(String.format("%s%s%s<dep>%s</dep>\n", prefix, indent, indent, RatingValue.format(rv.getDepValue())));
					if (rv.hasNote()) {
						sb.append(String.format("%s%s%s<note>%s</note>\n", prefix, indent, indent, rv.getNote()));
					}
					sb.append(String.format("%s%s</point>\n", prefix, indent));
				}
				sb.append(prefix).append(isExtension ? "</extension-points>\n" : "</rating-points>\n");
				
			}
			else {
				for (RatingValue rv : values) {
					sb.append(rv.toXmlString(indent, indentLevel, isExtension, indValues));
				}
			}
		}
		return sb.toString();
	}
	/**
	 * Retrieves the independent parameter count of an array of rating values
	 * @param values The rating values to analyze
	 * @return The number of independent parameters for the specified rating values
	 * @throws RatingException
	 */
	protected static int getIndParamCount(RatingValue[] values) throws RatingException {
		if (values == null || values.length == 0) {
			throw new RatingException("No rating values!");
		}
		int indParamCount = values[0].hasDepValue() ? 1 : 1 + values[0].getDepValues().getIndParamCount();
		for (int i = 1; i < values.length; ++i) {
			int thisIndParamCount = values[i].hasDepValue() ? 1 : 1 + values[i].getDepValues().getIndParamCount();
			if (thisIndParamCount != indParamCount) {
				throw new RatingException("Rating values have inconsistent independent parameter counts.");
			}
		}
		return indParamCount;
	}
	/**
	 * Generates the reversed permutation of this rating - used for reverseRate() methods
	 * @throws RatingException
	 */
	protected void reverse() throws RatingException {
		if (getIndParamCount() > 1) {
			throw new RatingException("Cannot reverse a TableRating with more than one independent parameter");
		}
		TableRatingContainer trc = (TableRatingContainer)getData();
		if (trc.values != null) {
			double temp;
			for (RatingValueContainer rvc : trc.values) {
				temp = rvc.indValue;
				rvc.indValue = rvc.depValue;
				rvc.depValue = temp;
			}
		}
		String[] units = split(trc.unitsId, SEPARATOR2, "L");
		trc.unitsId = join(SEPARATOR2, units[1], units[0]);
		if (trc.inRangeMethod.equals(RatingMethod.LIN_LOG.toString())) {
			trc.inRangeMethod = RatingMethod.LOG_LIN.toString();
		}
		else if (trc.inRangeMethod.equals(RatingMethod.LOG_LIN.toString())) {
			trc.inRangeMethod = RatingMethod.LIN_LOG.toString();
		}
		if (trc.outRangeLowMethod.equals(RatingMethod.LIN_LOG.toString())) {
			trc.outRangeLowMethod = RatingMethod.LOG_LIN.toString();
		}
		else if (trc.outRangeLowMethod.equals(RatingMethod.LOG_LIN.toString())) {
			trc.outRangeLowMethod = RatingMethod.LIN_LOG.toString();
		}
		if (trc.outRangeHighMethod.equals(RatingMethod.LIN_LOG.toString())) {
			trc.outRangeHighMethod = RatingMethod.LOG_LIN.toString();
		}
		else if (trc.outRangeHighMethod.equals(RatingMethod.LOG_LIN.toString())) {
			trc.outRangeHighMethod = RatingMethod.LIN_LOG.toString();
		}
		reversed = new TableRating(trc);
	}
	/**
	 * Initialization method used by constructors
	 * @param values The table of values that comprise the rating.
	 * @param extensionValues The rating extension values
	 * @param inRangeMethod The prescribed behavior for when the value to rate falls within the range of independent values in the rating table
	 * @param outRangeLowMethod The prescribed behavior for when the value to rate would sort before the first independent value in the rating table
	 * @param outRangeHighMethod The prescribed behavior for when the value to rate would sort after the last independent value in the rating table
	 * @param officeId The identifier of the office that owns this rating
	 * @param ratingSpecId The rating specification identifier
	 * @param unitsId The units identifier
	 * @param effectiveDate The effective date of the rating. The effective date is the earliest date/time for which the rating should be applied.
	 * @param createDate The creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 *        This may be later than the effective date 
	 * @param active Specifies whether the rating is currently active
	 * @param desription The description of the rating        
	 * @throws RatingException
	 */
	protected void init(
			RatingValue[] values,
			RatingValue[] extensionValues,
			RatingMethod inRangeMethod,
			RatingMethod outRangeLowMethod,
			RatingMethod outRangeHighMethod,
			String officeId,
			String ratingSpecId,
			String ratingUnitsId,
			long effectiveDate,
			long createDate,
			boolean active,
			String description) throws RatingException {
		//-------------------------------------//
		// validate the incoming rating values //
		//-------------------------------------//
		getIndParamCount(values);
		double[] ind_vals = new double[values.length];
		for (int i = 0; i < values.length; ++i) ind_vals[i] = values[i].getIndValue();
		props = new SequenceProperties(ind_vals);
		if (props.hasUndefined() || props.hasConstant() || (props.hasIncreasing() && props.hasDecreasing())) {
			throw new RatingException("Specifed values do not monotonically increase or decrease, cannot use for rating.");
		}
		//-----------------------------------------------//
		// validate the incoming rating extension values //
		//-----------------------------------------------//
		RatingValue[] effectiveValues = null;
		if (extensionValues != null) {
			if (ratingSpecId == null) {
				throw new RatingException("Only top-level table ratings can have extension values.");
			}
			if (getIndParamCount(extensionValues) != getIndParamCount(extensionValues)) {
				throw new RatingException("Rating extension has different number of parameters than rating.");
			}
			ind_vals = new double[extensionValues.length];
			for (int i = 0; i < extensionValues.length; ++i) ind_vals[i] = extensionValues[i].getIndValue();
			SequenceProperties extensionProps = new SequenceProperties(ind_vals);
			if (extensionProps.hasUndefined() || extensionProps.hasConstant() || (extensionProps.hasIncreasing() && extensionProps.hasDecreasing())) {
				throw new RatingException("Specifed extension values do not monotonically increase or decrease, cannot use for rating.");
			}
			if (extensionProps.hasIncreasing() != props.hasIncreasing()) {
				throw new RatingException("Extension values are ordered in opposite direction of rating values.");
			}
			//---------------------------------------------------------------------//
			// construct the effective values from the rating and extension values //
			//---------------------------------------------------------------------//
			double first = values[0].getIndValue();
			double last  = values[values.length].getIndValue();
			List<RatingValue> effective = new Vector<RatingValue>();
			if (props.hasIncreasing()) {
				int i;
				for (i = 0; i < extensionValues.length && extensionValues[i].getIndValue() < first; ++i) {
					effective.add(extensionValues[i]);
				}
				for (int j = 0; j < values.length; ++j) {
					effective.add(values[j]);
				}
				for (; i < extensionValues.length && extensionValues[i].getIndValue() <= last; ++i);
				for (; i < extensionValues.length; ++i) {
					effective.add(extensionValues[i]);
				}
			}
			else {
				int i;
				for (i = 0; i < extensionValues.length && extensionValues[i].getIndValue() > first; ++i) {
					effective.add(extensionValues[i]);
				}
				for (int j = 0; j < values.length; ++j) {
					effective.add(values[j]);
				}
				for (; i < extensionValues.length && extensionValues[i].getIndValue() >= last; ++i);
				for (; i < extensionValues.length; ++i) {
					effective.add(extensionValues[i]);
				}
			}
			effectiveValues = effective.toArray(new RatingValue[effective.size()]);
		}
		//----------------------------------//
		// validate the specified behaviors //
		//----------------------------------//
		switch (inRangeMethod) {
		case NEAREST:
			throw new RatingException(inRangeMethod + " is not a valid in range method.");
		}
		switch (outRangeLowMethod) {
		case PREVIOUS:
			throw new RatingException("Out of range low method " + outRangeLowMethod + " cannot be used in this context");
		case LOWER:
			if (props.hasIncreasing()) {
				throw new RatingException("Out of range low method " + outRangeLowMethod + " cannot be used in this context");
			}
			break;
		case HIGHER:
			if (props.hasDecreasing()) {
				throw new RatingException("Out of range low method " + outRangeLowMethod + " cannot be used in this context");
			}
			break;
		}
		switch (outRangeHighMethod) {
		case NEXT:
			throw new RatingException("Out of range high method " + outRangeHighMethod + " cannot be used in this context");
		case LOWER:
			if (props.hasDecreasing()) {
				throw new RatingException("Out of range high method " + outRangeHighMethod + " cannot be used in this context");
			}
			break;
		case HIGHER:
			if (props.hasIncreasing()) {
				throw new RatingException("Out of range high method " + outRangeHighMethod + " cannot be used in this context");
			}
			break;
		}
		//-----------------------//
		// finish initialization //
		//-----------------------//
		if (observationTarget == null) observationTarget = new Observable();
		this.values = values;
		this.extensionValues = extensionValues;
		this.effectiveValues = effectiveValues == null ? values : effectiveValues;
		this.reversed = null;
		this.inRangeMethod = inRangeMethod;
		this.outRangeLowMethod = outRangeLowMethod;
		this.outRangeHighMethod = outRangeHighMethod;
		setOfficeId(officeId);
		setRatingSpecId(ratingSpecId);
		setRatingUnitsId(ratingUnitsId);
		setEffectiveDate(effectiveDate);
		setCreateDate(createDate);
		setActive(active);
		setDescription(description);
		for (RatingValue value : this.values) {
			value.addObserver(this);
		}
		if (this.extensionValues != null) {
			for (RatingValue extensionValue : this.extensionValues) {
				extensionValue.addObserver(this);
			}
		}
	}
}
