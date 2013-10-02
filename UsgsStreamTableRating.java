package hec.data.cwmsRating;

import static hec.data.cwmsRating.RatingConst.activeXpath;
import static hec.data.cwmsRating.RatingConst.createDateXpath;
import static hec.data.cwmsRating.RatingConst.effectiveDateXpath;
import static hec.data.cwmsRating.RatingConst.initXmlParsing;
import static hec.data.cwmsRating.RatingConst.offsetNodeXpath;
import static hec.data.cwmsRating.RatingConst.pointNodesXpath;
import static hec.data.cwmsRating.RatingConst.shiftNodesXpath;
import static hec.lang.Const.UNDEFINED_DOUBLE;
import static hec.lang.Const.UNDEFINED_TIME;
import static javax.xml.xpath.XPathConstants.NODE;
import static javax.xml.xpath.XPathConstants.NODESET;
import static javax.xml.xpath.XPathConstants.STRING;
import hec.data.RatingException;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import hec.heclib.util.HecTime;

import java.util.Arrays;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * TableRating sub-type for USGS-style stream ratings, which may include dated shifts and log interpolation offsets
 * @author Mike Perryman
 */
public class UsgsStreamTableRating extends TableRating {
	/**
	 * The time series of shift ratings
	 */
	protected RatingSet shifts = null;
	/**
	 * The log interpolation offsets
	 */
	protected TableRating offsets = null;

	/**
	 * Generator from XML DOM nodes
	 * @param templateNode The node containing the rating template information
	 * @param ratingNode The node containing the rating information
	 * @return A new RatingTable object initialized with the data from the XML nodes
	 * @throws RatingException
	 */
	public static UsgsStreamTableRating fromXml(Node templateNode, Node ratingNode) throws RatingException {
		try {
			initXmlParsing();
			TableRating rt = TableRating.fromXml(templateNode, ratingNode);
			NodeList shiftNodes = (NodeList)shiftNodesXpath.evaluate(ratingNode, NODESET);
			TableRating[] shifts = shiftNodes.getLength() == 0 ? null : new TableRating[shiftNodes.getLength()];
			if (shifts != null) {
				HecTime t = new HecTime(HecTime.SECOND_GRANULARITY);
				for (int i = 0; i < shifts.length; ++i) {
					Node shiftNode = shiftNodes.item(i);
					NodeList pointNodes = (NodeList)pointNodesXpath.evaluate(shiftNode, NODESET);
					int pointCount = pointNodes.getLength();
					if (pointCount == 0) continue;
					String effectiveDateStr = (String)effectiveDateXpath.evaluate(shiftNode, STRING);
					if (effectiveDateStr.length() == 0) {
						throw new RatingException("Shift set does not have an effective date.");
					}
					String createDateStr = (String)createDateXpath.evaluate(shiftNode, STRING);
					if (createDateStr.length() == 0) {
						throw new RatingException("Shift set does not have an create date.");
					}
					String activeStr = (String)activeXpath.evaluate(shiftNode, STRING);
					t.set(effectiveDateStr);
					long effectiveDate = t.getTimeInMillis();
					t.set(createDateStr);
					long createDate = t.getTimeInMillis();
					boolean active = activeStr.equalsIgnoreCase("true");
					shifts[i] = new TableRating(
							RatingValue.fromXml(pointNodes),
							null,
							RatingMethod.LINEAR,
							RatingMethod.NEAREST,
							RatingMethod.NEAREST);
					shifts[i].setEffectiveDate(effectiveDate);
					shifts[i].setCreateDate(createDate);
					shifts[i].setActive(active);
				}
			}
			Node offsetsNode = (Node)offsetNodeXpath.evaluate(ratingNode, NODE);
			TableRating offsets = null;
			if (offsetsNode != null) {
				NodeList offsetNodes = (NodeList)pointNodesXpath.evaluate(offsetsNode, NODESET);
				if (offsetNodes.getLength() > 0) {
					offsets = new TableRating(
							RatingValue.fromXml(offsetNodes),
							null,
							RatingMethod.PREVIOUS,
							RatingMethod.NEXT,
							RatingMethod.PREVIOUS);
				}
			}
			UsgsStreamTableRating usrt = new UsgsStreamTableRating(
					rt.values,
					rt.extensionValues,
					rt.inRangeMethod,
					rt.outRangeLowMethod,
					rt.outRangeHighMethod,
					rt.officeId,
					rt.ratingSpecId,
					rt.ratingUnitsId,
					rt.effectiveDate,
					rt.createDate,
					shifts == null ? null : new UsgsStageShiftsRatingSet(rt.effectiveDate, shifts),
					offsets,
					rt.active,
					rt.description);
			return usrt;
		}
		catch (Throwable t) {
			if (t instanceof RatingException) throw (RatingException)t;
			throw new RatingException(t);
		}
	}

	/**
	 * Public Constructor 
	 * @param values The table of values that comprise the rating.
	 * @param in_range_method The prescribed behavior for when the value to rate falls within the range of independent values in the rating table
	 * @param out_range_low_method The prescribed behavior for when the value to rate would sort before the first independent value in the rating table
	 * @param out_range_high_method The prescribed behavior for when the value to rate would sort after the last independent value in the rating table
	 * @param effectiveDate The effective date of the rating. The effective date is the earliest date/time for which the rating should be applied.
	 * @param createDate The creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 *        This may be later than the effective date
	 * @param shifts The rating shifts
	 * @param offsets The logarithmic interpolation offsets 
	 * @param active Specifies whether the rating is currently active
	 * @param desription The description of the rating        
	 * @throws RatingException
	 */
	public UsgsStreamTableRating(
			RatingValue[] values,
			RatingValue[] extensionValues,
			RatingMethod in_range_method,
			RatingMethod out_range_low_method,
			RatingMethod out_range_high_method,
			String officeId,
			String ratingSpecId,
			String unitsId,
			long effectiveDate,
			long createDate,
			RatingSet shifts,
			TableRating offsets,
			boolean active,
			String description) throws RatingException {
		super(values,
				extensionValues,
				in_range_method,
				out_range_low_method,
				out_range_high_method,
				officeId,
				ratingSpecId,
				unitsId,
				effectiveDate,
				createDate,
				active,
				description);
		if (getIndParamCount() > 1) {
			throw new RatingException("UsgsStreamTableRating objects allow only one indendent parameter.");
		}
		setShifts(shifts);
		setOffsets(offsets);
	}

	public UsgsStreamTableRating(UsgsStreamTableRatingContainer urc) throws RatingException {
		RatingValue[] values = new RatingValue[urc.values.length];
		RatingValue[] extensionValues = null;
		for (int i = 0; i < values.length; ++i) values[i] = new RatingValue(urc.values[i]);
		if (urc.extensionValues != null) {
			extensionValues = new RatingValue[urc.extensionValues.length];
			for (int i = 0; i < extensionValues.length; ++i) extensionValues[i] = new RatingValue(urc.extensionValues[i]);
		}
		super.init(values,
				extensionValues,
				RatingMethod.fromString(urc.inRangeMethod),
				RatingMethod.fromString(urc.outRangeLowMethod),
				RatingMethod.fromString(urc.outRangeHighMethod),
				urc.officeId,
				urc.ratingSpecId,
				urc.unitsId,
				urc.effectiveDateMillis,
				urc.createDateMillis,
				urc.active,
				urc.description);
		
		if (urc.offsets != null) {
			this.offsets = new TableRating(urc.offsets);
			this.offsets.addObserver(this);
		}
		if (urc.shifts != null) {
			this.shifts = new RatingSet(urc.shifts);
			this.shifts.addObserver(this);
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(double)
	 */
	@Override
	public double rate(double indVal) throws RatingException {
		return rate(defaultValueTime, indVal);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(double[])
	 */
	@Override
	public double rateOne(double... indVals) throws RatingException {
		if (indVals.length != 1) {
			throw new RatingException("UsgsStreamTableRating objects allow only one indendent parameter.");
		}
		return rate(indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rateOne(double[])
	 */
	@Override
	public double[] rate(double[] indVals) throws RatingException {
		return rate(defaultValueTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(double[][])
	 */
	@Override
	public double[] rate(double[][] indVals) throws RatingException {
		return rate(defaultValueTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(double[], int)
	 */
	@Override
	protected double rate(double[] indVals, int offset) throws RatingException {
		if (indVals.length != 1) {
			throw new RatingException("UsgsStreamTableRating objects allow only one indendent parameter.");
		}
		return rateOne(defaultValueTime, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(long, double)
	 */
	@Override
	public double rate(long valTime, double indVal) throws RatingException {
		long[] valTimes = {valTime};
		double[] indVals = {indVal};
		return rate(valTimes, indVals)[0];
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(long, double[])
	 */
	@Override
	public double rateOne(long valTime, double... indVals) throws RatingException {
		if (indVals.length != 1) {
			throw new RatingException("UsgsStreamTableRating objects allow only one indendent parameter.");
		}
		return rate(valTime, indVals[0]);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rateOne(long, double[])
	 */
	@Override
	public double[] rate(long valTime, double[] indVals) throws RatingException {
		if (shifts != null && shifts.getRatingCount() > 1) {
			if (valTime == UNDEFINED_TIME) {
				throw new RatingException("Value times must be specified or default time must be set when shifts are present.");
			}
		}
		long[] valTimes = new long[indVals.length]; 
		Arrays.fill(valTimes, valTime);
		return rate(valTimes, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rateOne(long[], double[])
	 */
	@Override
	public double[] rate(long[] valTimes, double[] indVals) throws RatingException {
		if (valTimes.length != indVals.length) {
			throw new RatingException("Different numbers of values and times.");
		}
		double[] Y = new double[indVals.length];
		for (int i = 0; i < indVals.length; ++i) {
			if (valTimes[i] == UNDEFINED_TIME && shifts != null && shifts.getRatingCount() > 0) {
				throw new RatingException("Value time is undefined in the presence of dated shifts - cannot rate.");
			}
			double shift = getShiftFromUnshifted(valTimes[i], indVals[i]);
			double ind_val = indVals[i] + shift;
			boolean out_range_low = false;
			boolean out_range_high = false;
			int lo = 0;
			int hi = effectiveValues.length-1;
			int mid;
			double mid_ind_val;
			RatingMethod extrap_method = null;
			//--------------------------------------------------- //
			// find the interpolation/extrapolation value indices //
			//--------------------------------------------------- //
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
			//-------------------------//
			// handle out of range low //
			//-------------------------//
			if (out_range_low) {
				switch (outRangeLowMethod) {
				case NULL:
					Y[i] = UNDEFINED_DOUBLE;
					continue;
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
				case HIGHER:
				case CLOSEST:
					Y[i] = effectiveValues[0].getDepValue();
					continue;
				case LOWER:
					throw new RatingException("No lower value in table.");
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
					Y[i] = UNDEFINED_DOUBLE;
					continue;
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
				case LOWER:
					Y[i] =  effectiveValues[effectiveValues.length].getDepValue();
					continue;
				case HIGHER:
					throw new RatingException("No higher value in table.");
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
			RatingMethod method = (out_range_low || out_range_high) ? extrap_method : inRangeMethod;
			switch (method) {
			case NULL:
				Y[i] = UNDEFINED_DOUBLE;
				continue;
			case ERROR:
				throw new RatingException("No such value in table.");
			default:
				break;
			}
			double lo_dep_val = effectiveValues[lo].getDepValue();
			double hi_dep_val = effectiveValues[hi].getDepValue();
			if (ind_val == lo_ind_val) {
				Y[i] = lo_dep_val;
				continue;
			}
			if (ind_val == hi_ind_val) {
				Y[i] = hi_dep_val;
				continue;
			}
			switch (method) {
			case PREVIOUS:
			case LOWER:
				Y[i] = lo_dep_val;
				continue;
			case NEXT:
			case HIGHER:
				Y[i] = hi_dep_val;
				continue;
			case CLOSEST:
				Y[i] =  Math.abs(ind_val - lo_ind_val) < Math.abs(hi_ind_val - ind_val) ? lo_dep_val : hi_dep_val;
				continue;
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
				double offset = getOffset(Math.min(x2, indVals[i]));
				x  = Math.log10(x - offset);
				x1 = Math.log10(x1 - offset);
				x2 = Math.log10(x2 - offset);
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
			Y[i] = y;
		}
		return Y;
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(long, double[][])
	 */
	@Override
	public double[] rate(long valTime, double[][] indVals) throws RatingException {
		if (shifts != null && shifts.getRatingCount() > 1) {
			if (valTime == UNDEFINED_TIME) {
				throw new RatingException("Value times must be specified or default time must be set when shifts are present.");
			}
		}
		long[] valTimes = new long[indVals.length]; 
		Arrays.fill(valTimes, valTime);
		return rate(valTimes, indVals);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#rate(long[], double[][])
	 */
	@Override
	public double[] rate(long[] valTimes, double[][] indVals) throws RatingException {
		double[] vals = new double[indVals.length];
		for (int i = 1; i < indVals.length; ++i) {
			if (indVals[i].length != 1) {
				throw new RatingException("UsgsStreamTableRating objects allow only one indendent parameter.");
			}
			vals[i] = indVals[i][0];
		}
		return rate(valTimes, vals);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.TableRating#reverseRate(long, double)
	 */
	@Override
	public double reverseRate(long valTime, double depVal) throws RatingException {
		double flow = depVal;
		boolean out_range_low = false;
		boolean out_range_high = false;
		int lo = 0;
		int hi = values.length-1;
		int mid;
		double midFlow;
		double shifted = UNDEFINED_DOUBLE;
		RatingMethod extrap_method = null;
		//--------------------------------------------------- //
		// find the interpolation/extrapolation value indices //
		//--------------------------------------------------- //
		if (props.hasIncreasing()) {
			if (flow < effectiveValues[lo].getDepValue()) {
				out_range_low = true;
			}
			else if (flow > effectiveValues[hi].getDepValue()) {
				out_range_high = true;
			}
			else {
				while (hi - lo > 1) {
					mid = (lo + hi) / 2;
					midFlow = effectiveValues[mid].getDepValue();
					if (flow < midFlow) hi = mid; else lo = mid;
				}
			}
		}
		else {
			if (flow > effectiveValues[lo].getDepValue()) {
				out_range_low = true;
			}
			else if (flow < effectiveValues[hi].getDepValue()) {
				out_range_high = true;
			}
			else {
				while (hi - lo > 1) {
					mid = (lo + hi) / 2;
					midFlow = effectiveValues[mid].getDepValue();
					if (flow > midFlow) hi = mid; else lo = mid;
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
				flow = effectiveValues[0].getDepValue();
				break;
			case LOWER:
				if (props.hasIncreasing()) throw new RatingException("No lower value in table.");
				flow = effectiveValues[0].getDepValue();
				break;
			case HIGHER:
				if (props.hasDecreasing()) throw new RatingException("No higher value in table.");
				flow = effectiveValues[0].getDepValue();
				break;
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
				flow = effectiveValues[effectiveValues.length-1].getDepValue();
				break;
			case LOWER:
				if (props.hasDecreasing()) throw new RatingException("No lower value in table.");
				flow = effectiveValues[effectiveValues.length-1].getDepValue();
				break;
			case HIGHER:
				if (props.hasIncreasing()) throw new RatingException("No higher value in table.");
				flow = effectiveValues[effectiveValues.length-1].getDepValue();
				break;
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
		double loHeight = effectiveValues[lo].getIndValue();
		double hiHeight = effectiveValues[hi].getIndValue();
		double loFlow = effectiveValues[lo].getDepValue();
		double hiFlow = effectiveValues[hi].getDepValue();
		RatingMethod method = (out_range_low || out_range_high) ? extrap_method : inRangeMethod;
		switch (method) {
		case NULL:
			return UNDEFINED_DOUBLE;
		case ERROR:
			throw new RatingException("No such value in table.");
		case PREVIOUS:
			shifted = loHeight;
		case NEXT:
			shifted = hiHeight;
		case LOWER:
			shifted = props.hasIncreasing() ? loHeight : hiHeight;
		case HIGHER:
			shifted = props.hasIncreasing() ? hiHeight : loHeight;
		case CLOSEST:
			shifted = Math.abs(flow - loFlow) < Math.abs(hiHeight - hiFlow) ? loHeight : hiHeight;
		default:
			break;
		}
		//---------------------------//
		// interpolate / extrapolate //
		//---------------------------//
		if (shifted == UNDEFINED_DOUBLE) {
			boolean ind_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LIN_LOG;
			boolean dep_log = method == RatingMethod.LOGARITHMIC || method == RatingMethod.LOG_LIN;
			double y  = flow;
			double x1 = loHeight;
			double x2 = hiHeight;
			double y1 = loFlow;
			double y2 = hiFlow;
			double offset = 0.;
			if (ind_log) {
				if (offsets != null && offsets.values != null && offsets.values.length > 0) {
					if (offsets.values.length == 1) {
						offset = offsets.values[0].getDepValue();
					}
					else {
						offset = offsets.rate(loHeight);
					}
				}
				x1 = Math.log10(x1 - offset);
				x2 = Math.log10(x2 - offset);
				if (Double.isNaN(x1) || Double.isInfinite(x1) || Double.isNaN(x2) || Double.isInfinite(x2))  {
					//-------------------------------------------------//
					// fall back from LOGARITHMIC or LOG_LIN to LINEAR //
					//-------------------------------------------------//
					x1 = loHeight;
					x2 = hiHeight;
					ind_log = false;
					dep_log = false;
				}
			}
			if (dep_log) {
				y = Math.log10(y);
				y1 = Math.log10(y1);
				y2 = Math.log10(y2);
				if (Double.isNaN(y)  || Double.isInfinite(y)  || 
					Double.isNaN(y1) || Double.isInfinite(y1) || 
					Double.isNaN(y2) || Double.isInfinite(y2))  {
					//-------------------------------------------------//
					// fall back from LOGARITHMIC or LIN_LOG to LINEAR //
					//-------------------------------------------------//
					y  = flow;
					x1 = loHeight;
					x2 = hiHeight;
					y1 = loFlow;
					y2 = hiFlow;
					ind_log = false;
				}
			}
			shifted = x1 + ((y - y1) / (y2 - y1)) * (x2 - x1);
			if (ind_log) shifted = Math.pow(10, shifted) + offset;
		}
		double shift = getShiftFromShifted(valTime, shifted);
		double unshifted = shifted - shift;
		return unshifted;
	}
	/**
	 * Retrieves the current shifts
	 * @return the shifts
	 */
	public RatingSet getShifts() {
		return shifts;
	}
	/**
	 * Sets the current shifts
	 * @param shifts the shifts to set
	 */
	public void setShifts(RatingSet shifts) {
		this.shifts = shifts;
	}
	/**
	 * Retrieves the log interpolation offsets
	 * @return the offsets
	 */
	public TableRating getOffsets() {
		return offsets;
	}
	/**
	 * Sets the log interpolation offsets
	 * @param offsets the offsets to set
	 */
	public void setOffsets(TableRating offsets) {
		this.offsets = offsets;
	}
	/**
	 * Returns the effective date used to rate a value at the specified time. This will either be the 
	 * effective date of the base rating or the effective date of the latest active shift on or before
	 * the specified time.
	 * @param valTime The time of the value to rate
	 * @return The effective date used to rate a value at the specified time
	 */
	public long getLatestEffectiveDate(long valTime) {
		long latestEffectiveDate = effectiveDate;
		if (shifts != null) {
			for (AbstractRating rating : shifts.getRatings()) {
				if (rating.isActive()) {
					long shiftEffectiveDate = rating.getEffectiveDate();
					if (shiftEffectiveDate <= valTime && shiftEffectiveDate > latestEffectiveDate) {
						latestEffectiveDate = rating.getEffectiveDate();
					}
				}
			}
		}
		return latestEffectiveDate;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getData()
	 */
	@Override
	public AbstractRatingContainer getData() {
		UsgsStreamTableRatingContainer ustrc = new UsgsStreamTableRatingContainer();
		getData(ustrc);
		if (shifts != null) ustrc.shifts = shifts.getData();
		if (offsets != null) ustrc.offsets = (TableRatingContainer)offsets.getData();
		return ustrc;
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		super.setRatingTime(ratingTime);
		if (shifts != null) shifts.setRatingTime(ratingTime);
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#resetRatingTime()
	 */
	@Override
	public void resetRatingTime() {
		super.resetRatingTime();
		if (shifts != null) shifts.resetRatingTime();
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setData(hec.data.cwmsRating.RatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer rc) throws RatingException {
		if (!(rc instanceof UsgsStreamTableRatingContainer)) throw new RatingException("setData() requires a UsgsStreamTableRatingContainer object.");
		UsgsStreamTableRatingContainer ustrc = (UsgsStreamTableRatingContainer)rc;
		for (RatingValue value : values) value.deleteObserver(this);
		RatingValue[] values = new RatingValue[ustrc.values.length];
		for (int i = 0; i < ustrc.values.length; ++i) {
			values[i] = new RatingValue(ustrc.values[i]);
		}
		for (RatingValue extensionValue : extensionValues) extensionValue.deleteObserver(this);
		RatingValue[] extensionValues = new RatingValue[ustrc.extensionValues.length];
		for (int i = 0; i < ustrc.extensionValues.length; ++i) {
			extensionValues[i] = new RatingValue(ustrc.extensionValues[i]);
		}
		init(	values,
				extensionValues,
				RatingMethod.fromString(ustrc.inRangeMethod),
				RatingMethod.fromString(ustrc.outRangeLowMethod),
				RatingMethod.fromString(ustrc.outRangeHighMethod),
				ustrc.officeId,
				ustrc.ratingSpecId,
				ustrc.unitsId,
				ustrc.effectiveDateMillis,
				ustrc.createDateMillis,
				ustrc.active,
				ustrc.description);
		if (ustrc.shifts != null) {
			shifts = new RatingSet(ustrc.shifts);
		}
		if (ustrc.offsets != null) {
			offsets = new TableRating(ustrc.offsets);
		}
		observationTarget.setChanged();
		observationTarget.notifyObservers();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#toXmlString(java.lang.CharSequence, int)
	 */
	@Override
	public String toXmlString(CharSequence indent, int indentLevel) throws RatingException {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < indentLevel; ++i) sb.append(indent);
		String prefix = sb.toString();
		sb.delete(0, sb.length());
		sb.append(String.format("%s<usgs-stream-rating office-id=\"%s\">\n", prefix, officeId))
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
		if (this.shifts != null) {
			AbstractRating[] shifts = this.shifts.getRatings();
			for (int i = 0; i < shifts.length; ++i) {
				sb.append(String.format("%s%s<height-shifts>\n", prefix, indent));
				if (!(shifts[i] instanceof TableRating)) {
					throw new RatingException("Unexpected rating type for shifts.");
				}
				t.setTimeInMillis(shifts[i].getEffectiveDate());
				sb.append(String.format("%s%s%s<effective-date>%s</effective-date>\n", prefix, indent, indent, t.getXMLDateTime(0).replaceAll("Z","")));
				t.setTimeInMillis(shifts[i].getCreateDate());
				sb.append(String.format("%s%s%s<create-date>%s</create-date>\n", prefix, indent, indent, t.getXMLDateTime(0).replaceAll("Z","")))
				  .append(String.format("%s%s%s<active>%s</active>\n", prefix, indent, indent, shifts[i].isActive() ? "true" : "false"));
				for (RatingValue rv : ((TableRating)shifts[i]).values) {
					sb.append(String.format("%s%s%s<point>\n", prefix, indent, indent))
					  .append(String.format("%s%s%s%s<ind>%s</ind>\n", prefix, indent, indent, indent, RatingValue.format(rv.getIndValue())))
					  .append(String.format("%s%s%s%s<dep>%s</dep>\n", prefix, indent, indent, indent, RatingValue.format(rv.getDepValue())));
					if (rv.hasNote()) sb.append(String.format("%s%s%s%s<note>%s</note>\n", prefix, indent, indent, indent, rv.getNote()));
					sb.append(String.format("%s%s%s</point>\n", prefix, indent, indent));
				}
				sb.append(String.format("%s%s</height-shifts>\n", prefix, indent));
			}
		}
		if (this.offsets != null && offsets.values != null && offsets.values.length > 0) {
			sb.append(String.format("%s%s<height-offsets>\n", prefix, indent));
			for (RatingValue rv : offsets.values) {
				sb.append(String.format("%s%s%s<point>\n", prefix, indent, indent))
				  .append(String.format("%s%s%s%s<ind>%s</ind>\n", prefix, indent, indent, indent, RatingValue.format(rv.getIndValue())))
				  .append(String.format("%s%s%s%s<dep>%s</dep>\n", prefix, indent, indent, indent, RatingValue.format(rv.getDepValue())));
				if (rv.hasNote()) sb.append(String.format("%s%s%s%s<note>%s</note>\n", prefix, indent, indent, indent, rv.getNote()));
				sb.append(String.format("%s%s%s</point>\n", prefix, indent, indent));
			}
			sb.append(String.format("%s%s</height-offsets>\n", prefix, indent));
		}
		sb.append(toXmlString(indent, indentLevel+1, values, false, null))
		  .append(toXmlString(indent, indentLevel+1, extensionValues, true, null))
		  .append(String.format("%s</usgs-stream-rating>\n", prefix));
		return sb.toString();
	}
	/**
	 * Retrieves the stage shift for an unshifted stage at a specified time
	 * @param valTime The time to get the shift for
	 * @param height The unshifted stage to get the shift for 
	 * @return The stage shift
	 * @throws RatingException
	 */
	protected double getShiftFromUnshifted(long valTime, double height) throws RatingException {
		double shift = 0;
		if (shifts != null) {
			switch (shifts.getRatingCount()) {
			case 0 :
				shift = 0;
				break;
			case 1 :
				shift = ((TableRatingContainer)shifts.getData().abstractRatingContainers[0]).values[0].depValue;
				break;
			default :
				shift = shifts.rate(height, valTime);
			}
		}
		return shift;
	}
	/**
	 * Retrieves the stage shift for an shifted stage at a specified time
	 * @param valTime The time to get the shift for
	 * @param height The shifted stage to get the shift for 
	 * @return The stage shift
	 * @throws RatingException
	 */
	protected double getShiftFromShifted(long valTime, double height) throws RatingException {
		double shift = 0;
		if (shifts != null) {
			switch (shifts.getRatingCount()) {
			case 0 :
				shift = 0;
				break;
			case 1 :
				shift = ((TableRatingContainer)shifts.getData().abstractRatingContainers[0]).values[0].depValue;
				break;
			default :
				RatingSetContainer rsc = shifts.getData();
				int hi = -1, lo = -1;
				for (int i = 0; i < rsc.abstractRatingContainers.length; ++i) {
					AbstractRatingContainer rc = rsc.abstractRatingContainers[i];
					if (rc.active && rc.createDateMillis <= ratingTime) {
						if (rc.effectiveDateMillis > valTime) {
							hi = i;
							break;
						}
						else {
							lo = i;
						}
					}
				}
				if (hi > -1) {
					long hiTime = rsc.abstractRatingContainers[hi].effectiveDateMillis;
					long loTime;
					double hiShift = 0, loShift = 0;
					if (!(rsc.abstractRatingContainers[hi] instanceof TableRatingContainer)) {
						throw new RatingException("Shift rating is not a TableRating object.");
					}
					TableRatingContainer trc = (TableRatingContainer)rsc.abstractRatingContainers[hi];
					if (height - trc.values[0].depValue <= trc.values[0].indValue) {
						hiShift = trc.values[0].depValue;
					}
					else if (height - trc.values[trc.values.length-1].depValue >= trc.values[trc.values.length-1].indValue) {
						hiShift = trc.values[trc.values.length-1].depValue;
					}
					else {
						for (int i = 1; i < trc.values.length; ++i) {
							if (height - trc.values[i].depValue <= trc.values[i].indValue) {
								int j = i == trc.values.length-1 ? i-1 : i;
								double s0 = trc.values[j].depValue;
								double s1 = trc.values[j+1].depValue;
								double h0 = trc.values[j].indValue;
								double h1 = trc.values[j+1].indValue;
								double dsdh = (s1-s0)/(h1-h0);
								hiShift = height-(height-s0+h0*dsdh)/(1+dsdh);
								break;
							}
						}
					}
					if (lo == -1) {
						loTime = getEffectiveDate();
						loShift = 0.;
					}
					else {
						loTime = rsc.abstractRatingContainers[lo].effectiveDateMillis;
						if (!(rsc.abstractRatingContainers[lo] instanceof TableRatingContainer)) {
							throw new RatingException("Shift rating is not a TableRating object.");
						}
						trc = (TableRatingContainer)rsc.abstractRatingContainers[lo];
						if (height - trc.values[0].depValue <= trc.values[0].indValue) {
							loShift = trc.values[0].depValue;
						}
						else if (height - trc.values[trc.values.length-1].depValue >= trc.values[trc.values.length-1].indValue) {
							loShift = trc.values[trc.values.length-1].depValue;
						}
						else {
							for (int i = 1; i < trc.values.length; ++i) {
								if (height - trc.values[i].depValue <= trc.values[i].indValue) {
									int j = i == trc.values.length-1 ? i-1 : i;
									double s0 = trc.values[j].depValue;
									double s1 = trc.values[j+1].depValue;
									double h0 = trc.values[j].indValue;
									double h1 = trc.values[j+1].indValue;
									double dsdh = (s1-s0)/(h1-h0);
									loShift = height-(height-s0+h0*dsdh)/(1+dsdh);
									break;
								}
							}
						}
					}
					if (valTime == loTime) {
						shift = loShift;
					}
					else if (valTime == hiTime) {
						shift = hiShift;
					}
					else {
						shift = loShift + (double)(valTime - loTime) / (double)(hiTime - loTime) * (hiShift - loShift);
					}
				}
			}
		}
		return shift;
	}
	/**
	 * Retrieves the log interpolation offset for a specified stage
	 * @param indVal The stage to retrieve the offset for
	 * @return The log interpolation offset
	 * @throws RatingException
	 */
	protected double getOffset(double indVal) throws RatingException {
		double offset = 0.;
		if (offsets != null || offsets.values != null || offsets.values.length > 0) {
			TableRatingContainer trc = (TableRatingContainer)offsets.getData();
			if (trc.values.length == 1) {
				offset = trc.values[0].depValue;
			}
			else {
				offset = offsets.rate(indVal);
			}
		}
		return offset;
	}
}
