package hec.data.cwmsRating;

import hec.data.NotMonotonicRatingException;
import static hec.data.cwmsRating.RatingConst.SEPARATOR2;
import static hec.lang.Const.UNDEFINED_DOUBLE;
import static hec.lang.Const.UNDEFINED_LONG;
import static hec.util.TextUtil.join;
import static hec.util.TextUtil.split;
import hec.data.RatingException;
import hec.data.RatingOutOfRangeException;
import static hec.data.RatingOutOfRangeException.OutOfRangeEnum.*;
import hec.data.Units;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.RatingValueContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.lang.Observable;
import hec.util.TextUtil;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
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

	protected TableRating() {}
	
	static void setBehaviors(
			TableRatingContainer trc, 
			RatingMethod[] inRangeMethods,
			RatingMethod[] outRangeLowMethods,
			RatingMethod[] outRangeHighMethods,
			int offset) {
	
		trc.inRangeMethod = inRangeMethods[offset].name();
		trc.outRangeLowMethod = outRangeLowMethods[offset].name();
		trc.outRangeHighMethod = outRangeHighMethods[offset].name();
		if (offset < inRangeMethods.length - 1) {
			for (int i = 0; i < trc.values.length; ++i) {
				setBehaviors(trc.values[i].depTable, inRangeMethods, outRangeLowMethods, outRangeHighMethods, offset+1);
			}
		}
	}
	
	void setBehaviors(RatingTemplate template) throws RatingException {
		TableRatingContainer trc = (TableRatingContainer) getData();
		TableRating.setBehaviors(trc, template.getInRangeMethods(), template.getOutRangeLowMethods(), template.getOutRangeHighMethods(), 0);
		setData(trc);
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
				RatingMethod.fromString(trc.inRangeMethod == null ? "LINEAR" : trc.inRangeMethod),
				RatingMethod.fromString(trc.outRangeLowMethod == null ? "ERROR" : trc.outRangeLowMethod),
				RatingMethod.fromString(trc.outRangeHighMethod == null ? "ERROR" : trc.outRangeHighMethod),
				trc.officeId,
				trc.ratingSpecId,
				trc.unitsId,
				trc.effectiveDateMillis,
				trc.transitionStartDateMillis,
				trc.createDateMillis,
				trc.active,
				trc.description);
		if (trc.vdc != null) {
			vdc = trc.vdc.clone();
		}
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
	 * @param description The description of the rating        
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
				UNDEFINED_LONG,
				createDate,
				active,
				description);
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
	 * @param transitionStartDate The date to start the transition (interpolation) from the previous rating
	 * @param createDate The creation date of the rating. The creation date is the earliest date/time that the rating was loaded and usable in the system.
	 *        This may be later than the effective date 
	 * @param active Specifies whether the rating is currently active
	 * @param description The description of the rating        
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
			long transitionStartDate,
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
				transitionStartDate,
				createDate,
				active,
				description);
	}
	protected boolean eq(double v1, double v2) {
		return Math.abs(v2-v1) < 1.e-8;
	}
	protected boolean ne(double v1, double v2) {
		return !eq(v1, v2);
	}
	protected boolean gt(double v1, double v2) {
		return !eq(v1, v2) && v1 > v2;
	}
	protected boolean ge(double v1, double v2) {
		return eq(v1, v2) || v1 > v2;
	}
	protected boolean lt(double v1, double v2) {
		return !eq(v1, v2) && v1 < v2;
	}
	protected boolean le(double v1, double v2) {
		return eq(v1, v2) || v1 < v2;
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
	 * @see hec.data.cwmsRating.AbstractRating#rate(double)
	 */
	@Override
	public double rateOne2(double[] pIndVals) throws RatingException {

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
			throw new RatingException(String.format(
					"Data has %d independent parameters; rating %s requires %d", 
					pIndVals.length, ratingSpecId, getIndParamCount()));
		}
		double[] rated = new double[pIndVals.length];
		for (int i = 0; i < pIndVals.length; ++i) rated[i] = this.rate(pIndVals[i], 0);
		return rated;
	}
	
	protected double rate(double[] pIndVals, int p_offset) throws RatingException {
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
		return rate(pIndVals, dataUnits, ratingUnits, p_offset);
	}
	
	protected double rate(double[] pIndVals, String[] dataUnits, String[] ratingUnits, int p_offset) throws RatingException {

		double ind_val = pIndVals[p_offset];
		if (ind_val == UNDEFINED_DOUBLE) return UNDEFINED_DOUBLE;
		ind_val = convertUnits(ind_val, dataUnits[p_offset], ratingUnits[p_offset]);
		boolean out_range_low = false;
		boolean out_range_high = false;
		int lo = 0;
		int hi = values.length-1;
		int mid;
		double mid_ind_val;
		RatingMethod extrap_method = null; 
		double dep_val = UNDEFINED_DOUBLE;
		comps: do {
			//--------------------------------------------------- //
			// find the interpolation/extrapolation value indices //
			//--------------------------------------------------- //
			if (props.hasIncreasing() || values.length == 1) {
				if (lt(ind_val, effectiveValues[lo].getIndValue())) {
					out_range_low = true;
					hi = lo + 1;
				}
				else if (gt(ind_val, effectiveValues[hi].getIndValue())) {
					out_range_high = true;
					lo = hi - 1;
				}
				else {
					while (hi - lo > 1) {
						mid = (lo + hi) / 2;
						mid_ind_val = effectiveValues[mid].getIndValue();
						if (lt(ind_val, mid_ind_val)) hi = mid; else lo = mid;
					}
				}
			}
			else if (props.hasDecreasing()) {
				if (gt(ind_val, effectiveValues[lo].getIndValue())) {
					out_range_low = true;
					hi = lo;
					lo = hi + 1;
				}
				else if (lt(ind_val, effectiveValues[hi].getIndValue())) {
					out_range_high = true;
					lo = hi;
					hi = lo - 1;
				}
				else {
					while (hi - lo > 1) {
						mid = (lo + hi) / 2;
						mid_ind_val = effectiveValues[mid].getIndValue();
						if (gt(ind_val, mid_ind_val)) hi = mid; else lo = mid;
					}
				}
			}
			else {
				throw new RatingException("Table does not monotonically increase or decrease");
			}
			//-------------------------//
			// handle out of range low //
			//-------------------------//
			if (out_range_low) {
				switch (outRangeLowMethod) {
				case NULL:
					dep_val = UNDEFINED_DOUBLE;
//					System.out.println("rate : dep_val 1 = " + dep_val);
					break comps;
				case ERROR:
					throw new RatingOutOfRangeException(OUT_OF_RANGE_LOW);
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
						dep_val = effectiveValues[0].getDepValue();
//						System.out.println("rate : dep_val 2 = " + dep_val);
						break comps;
					}
					else {
						dep_val = effectiveValues[0].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
//						System.out.println("rate : dep_val 3 = " + dep_val);
						break comps;
					}
				case LOWER:
					if (props.hasIncreasing()) throw new RatingException("No lower value in table.");
					if (effectiveValues[0].hasDepValue()) {
						dep_val = effectiveValues[0].getDepValue();
//						System.out.println("rate : dep_val 4 = " + dep_val);
						break comps;
					}
					else {
						dep_val = effectiveValues[0].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
//						System.out.println("rate : dep_val 5 = " + dep_val);
						break comps;
					}
				case HIGHER:
					if (props.hasDecreasing()) throw new RatingException("No higher value in table.");
					if (effectiveValues[0].hasDepValue()) {
						dep_val = effectiveValues[0].getDepValue();
//						System.out.println("rate : dep_val 6 = " + dep_val);
						break comps;
					}
					else {
						dep_val = effectiveValues[0].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
//						System.out.println("rate : dep_val 7 = " + dep_val);
						break comps;
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
					dep_val = UNDEFINED_DOUBLE;
//					System.out.println("rate : dep_val 8 = " + dep_val);
					break comps;
				case ERROR:
					throw new RatingOutOfRangeException(OUT_OF_RANGE_HIGH);
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
						dep_val = effectiveValues[effectiveValues.length-1].getDepValue();
//						System.out.println("rate : dep_val 9 = " + dep_val);
						break comps;
					}
					else {
						dep_val = effectiveValues[effectiveValues.length-1].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
//						System.out.println("rate : dep_val 10 = " + dep_val);
						break comps;
					}
				case LOWER:
					if (props.hasDecreasing()) throw new RatingException("No lower value in table.");
					if (effectiveValues[effectiveValues.length-1].hasDepValue()) {
						dep_val = effectiveValues[effectiveValues.length-1].getDepValue();
//						System.out.println("rate : dep_val 11 = " + dep_val);
						break comps;
					}
					else {
						dep_val = effectiveValues[effectiveValues.length-1].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
//						System.out.println("rate : dep_val 12 = " + dep_val);
						break comps;
					}
				case HIGHER:
					if (props.hasIncreasing()) throw new RatingException("No higher value in table.");
					if (effectiveValues[effectiveValues.length-1].hasDepValue()) {
						dep_val = effectiveValues[effectiveValues.length-1].getDepValue();
//						System.out.println("rate : dep_val 13 = " + dep_val);
						break comps;
					}
					else {
						dep_val = effectiveValues[effectiveValues.length-1].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
//						System.out.println("rate : dep_val 14 = " + dep_val);
						break comps;
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
			// handle in-range and extrapolation //
			//-----------------------------------//
			double lo_ind_val = effectiveValues[lo].getIndValue();
			double hi_ind_val = effectiveValues[hi].getIndValue();
			//----------------------------------------------------------//
			// use specific rating (prevent interpolation) if ind value //
			// matches the ind value of one of the bounding ratings     //
			//----------------------------------------------------------//
			if (eq(ind_val, lo_ind_val)) {
				if (effectiveValues[lo].hasDepValue()) {
					dep_val = effectiveValues[lo].getDepValue();
				}
				else {
					dep_val = effectiveValues[lo].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
				}
//				System.out.println("rate : dep_val 15 = " + dep_val);
				break comps;
			}
			else if (eq(ind_val, hi_ind_val)) {
				if (effectiveValues[hi].hasDepValue()) {
					dep_val = effectiveValues[hi].getDepValue();
				}
				else {
					dep_val = effectiveValues[hi].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
				}
//				System.out.println("rate : dep_val 16 = " + dep_val);
				break comps;
			}
			//-----------------------------------------------------------------//
			// can't use specific rating, use in-range behavior or extrapolate //
			//-----------------------------------------------------------------//
			double lo_dep_val = effectiveValues[lo].hasDepValue() ? effectiveValues[lo].getDepValue() : effectiveValues[lo].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
			double hi_dep_val = effectiveValues[hi].hasDepValue() ? effectiveValues[hi].getDepValue() : effectiveValues[hi].getDepValues().rate(pIndVals, dataUnits, ratingUnits, p_offset+1);
			if (lo_dep_val == UNDEFINED_DOUBLE || hi_dep_val == UNDEFINED_DOUBLE) {
				return UNDEFINED_DOUBLE;
			}
			RatingMethod method = (out_range_low || out_range_high) ? extrap_method : inRangeMethod;
			switch (method) {
			case NULL:
				return UNDEFINED_DOUBLE;
			case ERROR:
				throw new RatingException("No such value in table.");
			case PREVIOUS:
				dep_val = lo_dep_val;
//				System.out.println("rate : dep_val 17 = " + dep_val);
				break comps;
			case NEXT:
				dep_val = hi_dep_val;
//				System.out.println("rate : dep_val 18 = " + dep_val);
				break comps;
			case LOWER:
				dep_val = props.hasIncreasing() ? lo_dep_val : hi_dep_val;
//				System.out.println("rate : dep_val 19 = " + dep_val);
				break comps;
			case HIGHER:
				dep_val = props.hasDecreasing() ? lo_dep_val : hi_dep_val;
//				System.out.println("rate : dep_val 20 = " + dep_val);
				break comps;
			case CLOSEST:
				dep_val = lt(Math.abs(ind_val - lo_ind_val), Math.abs(hi_ind_val - ind_val)) ? lo_dep_val : hi_dep_val;
//				System.out.println("rate : dep_val 21 = " + dep_val);
				break comps;
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
			dep_val = y;
//			System.out.println("rate : dep_val 22 = " + dep_val);
			break comps;
		} while(false);
		if (p_offset == 0 && dep_val != UNDEFINED_DOUBLE) {
			dep_val = convertUnits(dep_val, ratingUnits[ratingUnits.length-1], dataUnits[dataUnits.length-1]); 
		}
		return dep_val;
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
	 * @see hec.data.cwmsRating.AbstractRating#rate(long, double[])
	 */
	@Override
	public double rateOne2(long valTime, double[] pIndVals) throws RatingException {
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
		try {
			super._setData(rc);
			TableRatingContainer trc = (TableRatingContainer)rc;
			for (RatingValue value : values) value.deleteObserver(this);
			if(extensionValues != null)
			{
				for (RatingValue value : extensionValues) value.deleteObserver(this);
			}
			RatingValue[] values = new RatingValue[trc.values.length];
			for (int i = 0; i < trc.values.length; ++i) {
				values[i] = new RatingValue(trc.values[i]);
			}
			RatingValue[] extensionValues = null;
			if(trc.extensionValues != null)
			{
				extensionValues = new RatingValue[trc.extensionValues.length];
				for (int i = 0; i < trc.extensionValues.length; ++i) {
					extensionValues[i] = new RatingValue(trc.extensionValues[i]);
				}
			}
			else
			{
				extensionValues = null;
			}
			init(	values,
					extensionValues,
					RatingMethod.fromString(trc.inRangeMethod == null ? "LOGARITHMIC" : trc.inRangeMethod),
					RatingMethod.fromString(trc.outRangeLowMethod == null ? "ERROR" : trc.outRangeLowMethod),
					RatingMethod.fromString(trc.outRangeHighMethod == null ? "ERROR" : trc.outRangeHighMethod),
					trc.officeId,
					trc.ratingSpecId,
					trc.unitsId,
					trc.effectiveDateMillis,
					trc.transitionStartDateMillis,
					trc.createDateMillis,
					trc.active,
					trc.description);
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
		return getData().toXml(indent, indentLevel);
	}

	public String getLookupMethods() {
		return ((TableRatingContainer)getData()).getLookupMethods();
	}
	/**
	 * Sets the lookup behaviors from an XML string
	 * @param xml the XML string in the format as generated by getLookupBehaviors()
	 */
	public void setLookupMethods(String xml) {
		try {
			Document doc = new SAXBuilder().build(new StringReader(xml));
			Element elem = doc.getRootElement();
			if (elem.getName().equals("lookup-behaviors")) {
				inRangeMethod = RatingMethod.fromString(elem.getChildText("in-range"));
				outRangeLowMethod = RatingMethod.fromString(elem.getChildText("out-range-low"));
				outRangeHighMethod = RatingMethod.fromString(elem.getChildText("out-range-high"));
			}
		}
		catch (Exception e) {
		}
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
	
	/* (non-Javadoc)
	 * @see hec.data.IRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		// ratingTime is ignored
		if (ratingSpecId == null) {
			throw new RatingException("Only top level ratings can return rating extents.");
		}
		double[][] extents = new double[2][getIndParamCount()+1];
		Arrays.fill(extents[0], Double.POSITIVE_INFINITY);
		Arrays.fill(extents[1], Double.NEGATIVE_INFINITY);
		getRatingExtents(extents, 0);
		String[] dataUnits = getDataUnits();
		String[] ratingUnits = getRatingUnits();
		for (int i = 0; i < extents[0].length; ++i) {
			if (!TextUtil.equals(dataUnits[i], ratingUnits[i])) {
				extents[0][i] = convertUnits(extents[0][i], ratingUnits[i], dataUnits[i]);
				extents[1][i] = convertUnits(extents[1][i], ratingUnits[i], dataUnits[i]);
			}
		}
		return extents;
	}
	
	protected void getRatingExtents(double[][] extents, int level) throws RatingException {
		if (values == null || values.length == 0) {
			throw new RatingException("Empty rating.");
		}
		if (props.hasIncreasing) {
			if (lt(values[0].indValue, extents[0][level])) {
				extents[0][level] = values[0].indValue;
			}
			if (gt(values[values.length-1].indValue, extents[1][level])) {
				extents[1][level] = values[values.length-1].indValue;
			}
		}
		else {
			if (lt(values[values.length-1].indValue, extents[0][level])) {
				extents[0][level] = values[values.length-1].indValue;
			}
			if (gt(values[0].indValue, extents[1][level])) {
				extents[1][level] = values[0].indValue;
			}
		}
		boolean isLeaf = values[0].hasDepValue();
		for (int i = 0; i < values.length; ++i) {
			if (isLeaf) {
				if (lt(values[i].depValue, extents[0][level+1])) {
					extents[0][level+1] = values[i].depValue;
				}
				if (gt(values[i].depValue, extents[1][level+1])) {
					extents[1][level+1] = values[i].depValue;
				}
			}
			else {
				values[i].depTable.getRatingExtents(extents, level+1);
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setName(java.lang.String)
	 */
	@Override
	public void setName(String name) throws RatingException {
		if (ratingSpecId == null) {
			throw new RatingException("Only top level ratings can be named.");
		}
		super.setName(name);
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
			long transitionStartDate,
			long createDate,
			boolean active,
			String description) throws RatingException {
		//-------------------------------------//
		// validate the incoming rating values //
		//-------------------------------------//
		double[] ind_vals = null;
		if (values != null && values.length > 0) {
			getIndParamCount(values);
			ind_vals = new double[values.length];
			for (int i = 0; i < values.length; ++i) ind_vals[i] = values[i].getIndValue();
			props = new SequenceProperties(ind_vals);
			if (props.hasUndefined() || props.hasConstant() || (props.hasIncreasing() && props.hasDecreasing())) {
				throw new NotMonotonicRatingException("Specifed values do not monotonically increase or decrease, cannot use for rating.");
			}
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
				throw new NotMonotonicRatingException("Specifed extension values do not monotonically increase or decrease, cannot use for rating.");
			}
			if (extensionProps.hasIncreasing() != props.hasIncreasing()) {
				throw new RatingException("Extension values are ordered in opposite direction of rating values.");
			}
			//---------------------------------------------------------------------//
			// construct the effective values from the rating and extension values //
			//---------------------------------------------------------------------//
			double first = values[0].getIndValue();
			double last  = values[values.length-1].getIndValue();
			List<RatingValue> effective = new Vector<RatingValue>();
			if (props.hasIncreasing()) {
				int i;
				for (i = 0; i < extensionValues.length && lt(extensionValues[i].getIndValue(), first); ++i) {
					effective.add(extensionValues[i]);
				}
				for (int j = 0; j < values.length; ++j) {
					effective.add(values[j]);
				}
				for (; i < extensionValues.length && le(extensionValues[i].getIndValue(), last); ++i);
				for (; i < extensionValues.length; ++i) {
					effective.add(extensionValues[i]);
				}
			}
			else {
				int i;
				for (i = 0; i < extensionValues.length && lt(extensionValues[i].getIndValue(), first); ++i) {
					effective.add(extensionValues[i]);
				}
				for (int j = 0; j < values.length; ++j) {
					effective.add(values[j]);
				}
				for (; i < extensionValues.length && ge(extensionValues[i].getIndValue(), last); ++i);
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
		default:
			break;
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
		default:
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
		default:
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
		setTransitionStartDate(transitionStartDate);
		setCreateDate(createDate);
		setActive(active);
		setDescription(description);
		if (this.values != null) {
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

	@Override
	@Deprecated
	public RatingValue[] getValues(Integer defaultInterval)
	{
		return values;
	}
	
	/**
	 * @return The mixture of rating values and extension values used to perform lookups. 
	 */
	public RatingValue[] getEffectiveValues()
	{
		return effectiveValues;
	}
	/**
	 * @return The set of rating values.
	 */
	public RatingValue[] getRatingValues() {
		return values;
	}
	/**
	 * @return The set of extension values.
	 */
	public RatingValue[] getExtensionValues() {
		return extensionValues;
	}
	/**
	 * Sets the rating values to use to perform lookups
	 * @param values The rating values
	 * @throws RatingException
	 */
	public void setRatingValues(RatingValue[] values) throws RatingException {
		if (values == null) {
			throw new RatingException("Cannot set rating values to null");
		}
		RatingValueContainer[] rvcs = new RatingValueContainer[values.length];
		for (int i = 0; i < values.length;++i) {
			rvcs[i] = values[i].getData();
		}
		TableRatingContainer trc = (TableRatingContainer)getData();
		trc.values = rvcs;
		setData(trc);
	}
	/**
	 * Sets the extension values to use to perform lookups
	 * @param values The extension values
	 * @throws RatingException
	 */
	public void setExtensionValues(RatingValue[] values) throws RatingException {
		if (getIndParamCount() > 1) {
			throw new RatingException("Cannot set extension values for rating with more than one independent value");
		}
		RatingValueContainer[] rvcs = null;
		if (values != null) {
			rvcs = new RatingValueContainer[values.length];
			for (int i = 0; i < values.length;++i) {
				rvcs[i] = values[i].getData();
			}
		}
		TableRatingContainer trc = (TableRatingContainer)getData();
		trc.extensionValues = rvcs;
		setData(trc);
	}
	
	@Override
	public TableRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException
	{
		if (!(ratingContainer instanceof TableRatingContainer)) {
			throw new UnsupportedOperationException("Table Ratings only support Table Rating Containers.");
		}
		return new TableRating((TableRatingContainer)ratingContainer);
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((TableRating)obj).getData()));
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + getData().hashCode();
	}
}
