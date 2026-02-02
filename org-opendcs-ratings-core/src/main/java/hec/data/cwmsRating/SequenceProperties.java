/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package org.opendcs.ratings;

import hec.lang.Const;

/**
 * Holds a group of properties about a sequence of values
 *  
 * @author Mike Perryman
 *
 */
public class SequenceProperties {
	/**
	 * Flag specifying whether the sequence has any undefined values (stand-in for NULLs or missing values)
	 */
	protected boolean hasUndefined = false;
	/**
	 * Flag specifying whether the sequence has any sub-sequences in which values increase
	 */
	protected boolean hasIncreasing = false;
	/**
	 * Flag specifying whether the sequence has any sub-sequences in which values decrease
	 */
	protected boolean hasDecreasing = false;
	/**
	 * Flag specifying whether the sequence has any sub-sequences in which values remain constant
	 */
	protected boolean hasConstant = false;
	
	protected SequenceProperties() {throw new UnsupportedOperationException();}
	
	/**
	 * Public Constructor
	 * 
	 * @param vals The sequence of values to generate properties for
	 */
	public SequenceProperties(double[] vals) {
		
		for (int i = 0; i < vals.length; ++i) {
			if (Double.isNaN(vals[i]) || Double.isInfinite(vals[i]) || Const.isUndefined(vals[i])) this.hasUndefined = true;
			if (i > 0) {
				if      (vals[i] > vals[i-1]) this.hasIncreasing = true;
				else if (vals[i] < vals[i-1]) this.hasDecreasing = true;
				else                          this.hasConstant   = true;
			}
		}
	}
	/**
	 * Returns whether the sequence includes an undefined value (includes UNDEFINED_DOUBLE, NaN, +/-Infinity)
	 * 
	 * @return whether the sequence includes an undefined value
	 */
	public boolean hasUndefined()       {return this.hasUndefined;}
	/**
	 * Returns whether the sequence includes a sub-sequence values that increase with increasing position.
	 * 
	 * @return whether the sequence includes a sub-sequence values that increase with increasing position
	 */
	public boolean hasIncreasing() {return this.hasIncreasing;}
	/**
	 * Returns whether the sequence includes a sub-sequence values that decrease with increasing position.
	 * 
	 * @return whether the sequence includes a sub-sequence values that decrease with increasing position
	 */
	public boolean hasDecreasing() {return this.hasDecreasing;}
	/**
	 * Returns whether the sequence includes a sub-sequence values with the same magnitude.
	 * 
	 * @return whether the sequence includes a sub-sequence values with the same magnitude
	 */
	public boolean hasConstant()   {return this.hasConstant;}
}
