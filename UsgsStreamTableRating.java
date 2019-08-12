package hec.data.cwmsRating;

import static hec.lang.Const.UNDEFINED_DOUBLE;
import static hec.lang.Const.UNDEFINED_TIME;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import hec.data.DataSetException;
import hec.data.RatingException;
import hec.data.RoundingException;
import hec.data.UsgsRounder;
import hec.data.cwmsRating.RatingConst.RatingMethod;
import hec.data.cwmsRating.io.AbstractRatingContainer;
import hec.data.cwmsRating.io.RatingSetContainer;
import hec.data.cwmsRating.io.RatingSpecContainer;
import hec.data.cwmsRating.io.RatingValueContainer;
import hec.data.cwmsRating.io.TableRatingContainer;
import hec.data.cwmsRating.io.UsgsStreamTableRatingContainer;
import hec.data.location.LocationTemplate;
import hec.data.rating.IRatingSpecification;
import hec.util.TextUtil;

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
	 * The rounder for shift values
	 */
	protected UsgsRounder shiftRounder = null;

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
		RatingValue[] values = urc.values == null ? null : new RatingValue[urc.values.length];
		RatingValue[] extensionValues = null;
		if (values != null) {
			for (int i = 0; i < values.length; ++i) values[i] = new RatingValue(urc.values[i]);
		}
		if (urc.extensionValues != null) {
			extensionValues = new RatingValue[urc.extensionValues.length];
			for (int i = 0; i < extensionValues.length; ++i) extensionValues[i] = new RatingValue(urc.extensionValues[i]);
		}
		super.init(values,
				extensionValues,
				RatingMethod.fromString(urc.inRangeMethod == null ? "LOGARITHMIC" : urc.inRangeMethod),
				RatingMethod.fromString(urc.outRangeLowMethod == null ? "ERROR" : urc.outRangeLowMethod),
				RatingMethod.fromString(urc.outRangeHighMethod == null ? "ERROR" : urc.outRangeHighMethod),
				urc.officeId,
				urc.ratingSpecId,
				urc.unitsId,
				urc.effectiveDateMillis,
				urc.transitionStartDateMillis,
				urc.createDateMillis,
				urc.active,
				urc.description);
		
		if (urc.offsets != null) {
			this.offsets = new TableRating(urc.offsets);
			if (urc.unitsId != null && urc.unitsId.length() > 0) {
				String heightUnit = TextUtil.split(urc.unitsId, ";")[0];
				this.offsets.ratingUnitsId = String.format("%s;%s", heightUnit, heightUnit);
			}
			this.offsets.addObserver(this);
		}
		if (urc.shifts != null) {
			setShifts(new RatingSet(urc.shifts));
			if (urc.unitsId != null && urc.unitsId.length() > 0) {
				String heightUnit = TextUtil.split(urc.unitsId, ";")[0];
				for (AbstractRating shift : getShifts().getRatings()) {
					shift.ratingUnitsId = String.format("%s;%s", heightUnit, heightUnit);
				}
			}
			this.shifts.addObserver(this);
		}
	}
	/**
	 * Public constructor from XML text
	 * @param xmlText The XML text to initialize from
	 * @throws RatingException
	 */
	public UsgsStreamTableRating(String xmlText) throws RatingException {
		setData(new UsgsStreamTableRatingContainer(xmlText));
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
		synchronized(this) {
			if (valTimes.length != indVals.length) {
				throw new RatingException("Different numbers of values and times.");
			}
			double[] Y = new double[indVals.length];
			for (int i = 0; i < indVals.length; ++i) {
				if (valTimes[i] == UNDEFINED_TIME && shifts != null && shifts.getRatingCount() > 0) {
					throw new RatingException("Value time is undefined in the presence of dated shifts - cannot rate.");
				}
//				System.out.println("usgs-rate : height = " + indVals[i]);
				double shift = getShiftFromUnshifted(valTimes[i], indVals[i]);
				double ind_val = indVals[i] + shift;
//				System.out.println("usgs-rate : height = " + ind_val);
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
				if (lt(ind_val, effectiveValues[lo].getIndValue())) {
					out_range_low = true;
				}
				else if (gt(ind_val, effectiveValues[hi].getIndValue())) {
					out_range_high = true;
				}
				else {
					while (hi - lo > 1) {
						mid = (lo + hi) / 2;
						mid_ind_val = effectiveValues[mid].getIndValue();
						if (lt(ind_val, mid_ind_val)) {
							hi = mid; 
						}
						else {
							lo = mid;
						}
					}
				}
				//-------------------------//
				// handle out of range low //
				//-------------------------//
				if (out_range_low) {
					switch (outRangeLowMethod) {
					case NULL:
						Y[i] = UNDEFINED_DOUBLE;
//						System.out.println("usgs-rate : dep_val 1 = " + Y[i]);
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
//						System.out.println("usgs-rate : dep_val 2 = " + Y[i]);
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
//						System.out.println("usgs-rate : dep_val 3 = " + Y[i]);
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
						Y[i] =  effectiveValues[effectiveValues.length - 1].getDepValue();
//						System.out.println("usgs-rate : dep_val 4 = " + Y[i]);
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
//					System.out.println("usgs-rate : dep_val 5 = " + Y[i]);
					continue;
				case ERROR:
					throw new RatingException("No such value in table.");
				default:
					break;
				}
				double lo_dep_val = effectiveValues[lo].getDepValue();
				double hi_dep_val = effectiveValues[hi].getDepValue();
				if (eq(ind_val, lo_ind_val)) {
					Y[i] = lo_dep_val;
//					System.out.println("usgs-rate : dep_val 6 = " + Y[i]);
					continue;
				}
				if (eq(ind_val, hi_ind_val)) {
					Y[i] = hi_dep_val;
//					System.out.println("usgs-rate : dep_val = 7 " + Y[i]);
					continue;
				}
				switch (method) {
				case PREVIOUS:
				case LOWER:
					Y[i] = lo_dep_val;
//					System.out.println("usgs-rate : dep_val = 8 " + Y[i]);
					continue;
				case NEXT:
				case HIGHER:
					Y[i] = hi_dep_val;
//					System.out.println("usgs-rate : dep_val 9 = " + Y[i]);
					continue;
				case CLOSEST:
					Y[i] =  lt(Math.abs(ind_val - lo_ind_val), Math.abs(hi_ind_val - ind_val)) ? lo_dep_val : hi_dep_val;
//					System.out.println("usgs-rate : dep_val 10 = " + Y[i]);
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
					double offset = getOffset(Math.min(x, x1));
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
//				System.out.println("usgs-rate : dep_val 11 = " + Y[i]);
			}
			return Y;
		}
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
		synchronized(this) {
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
				if (lt(flow, effectiveValues[lo].getDepValue())) {
					out_range_low = true;
				}
				else if (gt(flow, effectiveValues[hi].getDepValue())) {
					out_range_high = true;
				}
				else {
					while (hi - lo > 1) {
						mid = (lo + hi) / 2;
						midFlow = effectiveValues[mid].getDepValue();
						if (lt(flow, midFlow)) hi = mid; else lo = mid;
					}
				}
			}
			else {
				if (gt(flow, effectiveValues[lo].getDepValue())) {
					out_range_low = true;
				}
				else if (lt(flow, effectiveValues[hi].getDepValue())) {
					out_range_high = true;
				}
				else {
					while (hi - lo > 1) {
						mid = (lo + hi) / 2;
						midFlow = effectiveValues[mid].getDepValue();
						if (gt(flow, midFlow)) hi = mid; else lo = mid;
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
				shifted = lt(Math.abs(flow - loFlow), Math.abs(hiHeight - hiFlow)) ? loHeight : hiHeight;
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
	}
	/**
	 * Retrieves the current shifts, can return null
	 * @return the shifts
	 * @throws RatingException 
	 */
	public RatingSet getShifts() throws RatingException {
		synchronized(this) {
			RatingSet retval = null;
			if(this.shifts != null)
			{
				retval = new RatingSet(this.shifts.getData());
				TableRating tr = (TableRating)retval.getRatings()[0];
				if (tr.effectiveDate == effectiveDate) {
					TableRatingContainer trc = (TableRatingContainer)tr.getData();
					if (trc.values.length == 1 && trc.values[0].indValue == 0 && trc.values[0].depValue == 0) {
						//----------------------------------------------------//
						// remove the zero shift at the rating effective date //
						//----------------------------------------------------//
						retval.removeRating(effectiveDate);
					}
				}
			}
			return retval;
		}
	}
	/**
	 * Sets the current shifts
	 * @param shifts the shifts to set
	 * @throws RatingException 
	 */
	public void setShifts(RatingSet shifts) throws RatingException {
		synchronized(this) {
			if (shifts != null) {
				RatingSetContainer rsc = shifts.getData();
				TableRatingContainer trc = (TableRatingContainer)rsc.abstractRatingContainers[0];
				if (trc.values == null || trc.values.length == 0) {
					//-----------------------//
					// shifts without values //
					//-----------------------//
					this.shifts = shifts;
				}
				else {
					//---------------//
					// normal shifts //
					//---------------//
					RatingSet _shifts = new RatingSet(rsc);
					AbstractRating[] arcs = _shifts.getRatings();
					TableRatingContainer trc1 = (TableRatingContainer) arcs[0].getData();
					if (trc1.effectiveDateMillis < effectiveDate) {
						throw new RatingException("Effective date of first shift pre-dates base rating");
					}
					else if (trc1.effectiveDateMillis > effectiveDate) {
						//--------------------------------------------------------------------------//
						// add a zero shift at the rating effective date for interpolation purposes //
						//--------------------------------------------------------------------------//
						TableRatingContainer trc0 = new TableRatingContainer();
						trc1.clone(trc0);
						trc0.effectiveDateMillis = this.effectiveDate;
						RatingValueContainer rvc = new RatingValueContainer();
						rvc.indValue = 0;
						rvc.depValue = 0;
						trc0.values = new RatingValueContainer[] {rvc};
						_shifts.addRating(new TableRating(trc0));
						this.shifts = _shifts;
					}
					else {
						this.shifts = shifts;
					}
				}
			}
			else {
				this.shifts = shifts;
			}
		}
	}
	/**
	 * Retrieves the log interpolation offsets
	 * @return the offsets
	 */
	public TableRating getOffsets() {
		synchronized(this) {
			return offsets;
		}
	}
	/**
	 * Sets the log interpolation offsets
	 * @param offsets the offsets to set
	 */
	public void setOffsets(TableRating offsets) {
		synchronized(this) {
			this.offsets = offsets;
		}
	}
	/**
	 * Returns the effective date used to rate a value at the specified time. This will either be the 
	 * effective date of the base rating or the effective date of the latest active shift on or before
	 * the specified time.
	 * @param valTime The time of the value to rate
	 * @return The effective date used to rate a value at the specified time
	 */
	public long getLatestEffectiveDate(long valTime) {
		synchronized(this) {
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
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#getData()
	 */
	@Override
	public UsgsStreamTableRatingContainer getData() {
		synchronized(this) {
			UsgsStreamTableRatingContainer ustrc = new UsgsStreamTableRatingContainer();
			getData(ustrc);
			if (shifts != null)
				try {
					ustrc.shifts = getShifts().getData();
				}
				catch (RatingException e) {
					throw new UnsupportedOperationException(e);
				}
			if (offsets != null) ustrc.offsets = (TableRatingContainer)offsets.getData();
			return ustrc;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setRatingTime(long)
	 */
	@Override
	public void setRatingTime(long ratingTime) {
		synchronized(this) {
			super.setRatingTime(ratingTime);
			if (shifts != null) shifts.setRatingTime(ratingTime);
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#resetRatingTime()
	 */
	@Override
	public void resetRatingTime() {
		synchronized(this) {
			super.resetRatingTime();
			if (shifts != null) shifts.resetRatingTime();
		}
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.ICwmsRating#setData(hec.data.cwmsRating.RatingContainer)
	 */
	@Override
	public void setData(AbstractRatingContainer rc) throws RatingException {
		synchronized(this) {
			if (!(rc instanceof UsgsStreamTableRatingContainer)) throw new RatingException("setData() requires a UsgsStreamTableRatingContainer object.");
			UsgsStreamTableRatingContainer ustrc = (UsgsStreamTableRatingContainer)rc;
			for (RatingValue value : values) value.deleteObserver(this);
			if(extensionValues != null)
			{
				for (RatingValue value : extensionValues) value.deleteObserver(this);
			}
			RatingValue[] values = new RatingValue[ustrc.values.length];
			RatingValue[] extensionValues = null;
			for (int i = 0; i < ustrc.values.length; ++i) {
				values[i] = new RatingValue(ustrc.values[i]);
			}
			if(ustrc.extensionValues != null)
			{
				extensionValues = new RatingValue[ustrc.extensionValues.length];
				for (int i = 0; i < ustrc.extensionValues.length; ++i) {
					extensionValues[i] = new RatingValue(ustrc.extensionValues[i]);
				}
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
					ustrc.transitionStartDateMillis,
					ustrc.createDateMillis,
					ustrc.active,
					ustrc.description);
			
			setShifts(ustrc.shifts == null ? null : new RatingSet(ustrc.shifts));
			setOffsets(ustrc.offsets == null ? null : new TableRating(ustrc.offsets));
			
			observationTarget.setChanged();
			observationTarget.notifyObservers();
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTable#toXmlString(java.lang.CharSequence, int)
	 */
	@Override
	public String toXmlString(CharSequence indent, int indentLevel) throws RatingException {
		UsgsStreamTableRating clone = new UsgsStreamTableRating((UsgsStreamTableRatingContainer) getData());
		if (clone.shifts != null) {
			try {
				clone.shifts.removeRating(this.effectiveDate);
			}
			catch (RatingException e) {
			}
		}
		return clone.getData().toXml(indent, indentLevel);
	}
	/**
	 * Retrieves the stage shift for an unshifted stage at a specified time
	 * @param valTime The time to get the shift for
	 * @param height The unshifted stage to get the shift for 
	 * @return The stage shift
	 * @throws RatingException
	 */
	protected double getShiftFromUnshifted(long valTime, double height) throws RatingException {
		synchronized(this) {
			double shift = 0;
//			System.out.println("getShift : height = " + height);
			try {
				if (this.shiftRounder == null) {
					if (ratingSpec == null) {
						shiftRounder = new UsgsRounder("2223456782");
					}
					else {
						shiftRounder = ratingSpec.getIndRoundingSpecs()[0];
					}
				}
			if (shifts != null && shifts.getActiveRatingCount() > 0) {
				shift = shifts.rate(height, valTime);
//					System.out.println("getShift : shift  = " + shift);
					shift = shiftRounder.round(shift, true);
				}
			}
			catch (RoundingException e) {
				throw new RatingException(e);
			}
//			System.out.println("getShift : shift  = " + shift);
			return shift;
		}
	}
	/**
	 * Retrieves the stage shift for an shifted stage at a specified time
	 * @param valTime The time to get the shift for
	 * @param height The shifted stage to get the shift for 
	 * @return The stage shift
	 * @throws RatingException
	 */
	protected double getShiftFromShifted(long valTime, double height) throws RatingException {
		synchronized(this) {
			double shift = 0;
			if (shifts != null && shifts.getActiveRatingCount() > 0) {
				double shift1 = getShiftFromUnshifted(valTime, height);
				double unshifted = height - shift1;
				double shift2 = getShiftFromUnshifted(valTime, unshifted);
				double mean = (shift1 + shift2) / 2;
				double diff = Math.abs(shift2 - shift1);
				int i = 0;
				int limit = 100;
				for (i = 0; i < limit && diff * 1E8 > Math.abs(mean); ++i) {
					shift1 = shift2;
					unshifted = height - shift1;
					shift2 = getShiftFromUnshifted(valTime, unshifted);
					mean = (shift1 + shift2) / 2;
					diff = Math.abs(shift2 - shift1);
				}
				shift = mean;
				if (i == limit) {
					logger.warning("Could not converge on shift for shifted value " + height + " in " + limit + " iterations.");
				}
			}
			return shift;
		}
	}
	/**
	 * Retrieves the log interpolation offset for a specified stage
	 * @param indVal The stage to retrieve the offset for
	 * @return The log interpolation offset
	 * @throws RatingException
	 */
	protected double getOffset(double indVal) throws RatingException {
		synchronized(this) {
			double offset = 0.;
			if (offsets != null && offsets.values != null && offsets.values.length > 0) {
				TableRatingContainer trc = (TableRatingContainer)offsets.getData();
				if (trc.values.length == 1) {
					offset = trc.values[0].depValue;
				}
				else {
					if (offsets.ratingUnitsId == null) {
						offsets.ratingUnitsId = ratingUnitsId;
					}
					offset = offsets.rate(indVal);
				}
			}
//			System.out.println("getOffset : offset = " + offset);
			return offset;
		}
	}
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#getRatingExtents()
	 */
	@Override
	public double[][] getRatingExtents() throws RatingException {
		return getRatingExtents(getRatingTime());
	}

	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.TableRating#getRatingExtents(long)
	 */
	@Override
	public double[][] getRatingExtents(long ratingTime) throws RatingException {
		synchronized(this) {
			double[][] extents = super.getRatingExtents(ratingTime);
			extents[0][0] -= this.getShiftFromShifted(ratingTime, extents[0][0]);
			extents[1][0] -= this.getShiftFromShifted(ratingTime, extents[1][0]);
			return extents;
		}
	}

	@Override
	public UsgsStreamTableRating getInstance(AbstractRatingContainer ratingContainer) throws RatingException
	{
		if (!(ratingContainer instanceof UsgsStreamTableRatingContainer))
		{
			throw new UnsupportedOperationException("USGS Stream Table Ratings only support USGS Stream Table Rating Containers.");
		}
		return new UsgsStreamTableRating((UsgsStreamTableRatingContainer)ratingContainer);
	}

	private RatingSetContainer createShiftsRatingSetContainer(Date shiftDate, List<RatingValueContainer> stageShiftValues, boolean shiftActive) throws RatingException
	{
		synchronized(this) {
			RatingSetContainer retvalShifts = new RatingSetContainer();
			RatingSpecContainer ratingSpecContainer = new RatingSpecContainer(); 
			retvalShifts.ratingSpecContainer = ratingSpecContainer;
			String locationRefId = null;
			try 
			{
				IRatingSpecification ratingSpecification = this.getRatingSpecification();
				LocationTemplate locationRef = ratingSpecification.getLocationRef();
				if (locationRef != null)
				{
					locationRefId = locationRef.getLocationId();
				}
			}
			catch(DataSetException e) 
			{
				throw new RatingException(e);
			}
			ratingSpecContainer.locationId = locationRefId;
			ratingSpecContainer.inRangeMethod = "LINEAR";
			ratingSpecContainer.outRangeLowMethod = "NEAREST";
			ratingSpecContainer.outRangeHighMethod = "NEAREST";
			ratingSpecContainer.indParams = new String[1];
			ratingSpecContainer.indParams[0] = "Stage";
			ratingSpecContainer.depParam = "Stage-Shift";
			ratingSpecContainer.parametersId = String.format("%s;%s", ratingSpecContainer.indParams[0], ratingSpecContainer.depParam);
			ratingSpecContainer.templateVersion = "Standard";
			ratingSpecContainer.templateId = String.format("%s.%s", ratingSpecContainer.parametersId, ratingSpecContainer.templateVersion);
			ratingSpecContainer.inRangeMethods = new String[1];
			ratingSpecContainer.inRangeMethods[0] = "LINEAR";
			ratingSpecContainer.outRangeLowMethods = new String[1];
			ratingSpecContainer.outRangeLowMethods[0] = "NEAREST";
			ratingSpecContainer.outRangeHighMethods = new String[1];
			ratingSpecContainer.outRangeHighMethods[0] = "NEAREST";
			ratingSpecContainer.indRoundingSpecs = new String[1];
			ratingSpecContainer.indRoundingSpecs[0] = "4444444449";
			ratingSpecContainer.depRoundingSpec = "4444444449";
			retvalShifts.abstractRatingContainers = new TableRatingContainer[1];
			
			String unitsId = this.getRatingUnitsId();
			String shiftUnit = TextUtil.split(TextUtil.split(unitsId, RatingConst.SEPARATOR2)[0], RatingConst.SEPARATOR3)[0];
			
			TableRatingContainer shiftTableRatingContainer = new TableRatingContainer();
			retvalShifts.abstractRatingContainers[0] = shiftTableRatingContainer;
			
			shiftTableRatingContainer.ratingSpecId = String.format("%s.%s.%s", ratingSpecContainer.locationId, retvalShifts.ratingSpecContainer.templateId, "Production");
			shiftTableRatingContainer.unitsId = String.format("%s;%s", shiftUnit, shiftUnit);
			shiftTableRatingContainer.effectiveDateMillis = shiftDate.getTime();
			shiftTableRatingContainer.createDateMillis = UNDEFINED_TIME;//System.currentTimeMillis();
			shiftTableRatingContainer.active = shiftActive;
			shiftTableRatingContainer.values = new RatingValueContainer[stageShiftValues.size()];
			stageShiftValues.toArray(shiftTableRatingContainer.values);
			shiftTableRatingContainer.inRangeMethod = "LINEAR";
			shiftTableRatingContainer.outRangeLowMethod = "NEAREST";
			shiftTableRatingContainer.outRangeHighMethod = "NEAREST";
			return retvalShifts;
		}
	}

	private TableRating addShift(RatingSet shiftsRef, Date shiftDate, List<RatingValueContainer> stageShiftValues, boolean shiftActive) throws RatingException
	{
		synchronized(this) {
			String unitsId = this.getRatingUnitsId();
			String shiftUnit = TextUtil.split(TextUtil.split(unitsId, RatingConst.SEPARATOR2)[0], RatingConst.SEPARATOR3)[0];
			String locationRefId = null;
			try 
			{
				IRatingSpecification ratingSpecification = this.getRatingSpecification();
				LocationTemplate locationRef = ratingSpecification.getLocationRef();
				if (locationRef != null)
				{
					locationRefId = locationRef.getLocationId();
				}
			}
			catch(DataSetException e) 
			{
				throw new RatingException(e);
			}
			String locationId = locationRefId;
			RatingSpec ratingSpec = shiftsRef.getRatingSpec();
			String templateId = ratingSpec.getTemplateId();
			
			TableRatingContainer shiftTableRatingContainer = new TableRatingContainer();
			shiftTableRatingContainer.ratingSpecId = String.format("%s.%s.%s", locationId, templateId, "Production");
			shiftTableRatingContainer.unitsId = String.format("%s;%s", shiftUnit, shiftUnit);
			shiftTableRatingContainer.effectiveDateMillis = shiftDate.getTime();
			shiftTableRatingContainer.createDateMillis = UNDEFINED_TIME;//System.currentTimeMillis();
			shiftTableRatingContainer.active = shiftActive;
			shiftTableRatingContainer.values = new RatingValueContainer[stageShiftValues.size()];
			stageShiftValues.toArray(shiftTableRatingContainer.values);
			shiftTableRatingContainer.inRangeMethod = "LINEAR";
			shiftTableRatingContainer.outRangeLowMethod = "NEAREST";
			shiftTableRatingContainer.outRangeHighMethod = "NEAREST";
			TableRating shiftTableRating = new TableRating(shiftTableRatingContainer);
			shiftsRef.addRating(shiftTableRating);
			return shiftTableRating;
		}
	}


	public TableRating addShift(Date shiftDate, List<RatingValueContainer> stageShiftValues, boolean shiftActive) throws RatingException
	{
		synchronized(this) {
			TableRating retval = null;
			RatingSet shiftsRef = getShifts();
			long shiftDateTime = shiftDate.getTime();
			//check if this rating already has a shift at the date time.
			if (shiftsRef != null && shiftsRef.getRatingsMap().containsKey(shiftDateTime))
			{
				//it already exists.
				StringBuilder sb = new StringBuilder();
				DateFormat gmtDateFormat = new SimpleDateFormat("HHmm ddMMMyyyy z");
				gmtDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
				String strShiftDateTime =  gmtDateFormat.format(effectiveDate);			
				sb.append(this.getRatingSpecId()).append(" already contains a shift at: ").append(strShiftDateTime);			
				throw new RatingException(sb.toString());
			}
			
			//does this rating have any shifts?
			RatingSetContainer shiftsRatingSetContainer = null;
			if (shiftsRef == null)
			{
				//create a new shifts rating set container
				shiftsRatingSetContainer = createShiftsRatingSetContainer(shiftDate,stageShiftValues,shiftActive);
				shiftsRef = new RatingSet(shiftsRatingSetContainer);
				setShifts(shiftsRef);
			}
			else
			{
				addShift(shiftsRef,shiftDate, stageShiftValues,shiftActive);
			}
			retval = (TableRating) shiftsRef.getRating(shiftDateTime);
			return retval;
		}
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		return obj == this || (obj != null && obj.getClass() == getClass() && getData().equals(((UsgsStreamTableRating)obj).getData()));
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#hashCode()
	 */
	@Override
	public int hashCode() {
		return getClass().getName().hashCode() + getData().hashCode();
	}
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.AbstractRating#setEffectiveDate(long)
	 */
	@Override
	public void setEffectiveDate(long effectiveDate) throws RatingException {
		synchronized(this) {
			if (shifts != null) {
				for (long shiftEffectiveDate : shifts.getEffectiveDates()) {
					if (effectiveDate > shiftEffectiveDate) {
						throw new RatingException("Effective date is later than existing shift effective date");
					}
				}
			}
			super.setEffectiveDate(effectiveDate);
		}
	}
	
}
