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
package org.opendcs.ratings.io;

/**
 * Container for source ratings for virtual and transitional ratings
 * @author Mike Perryman
 */
public class SourceRatingContainer {
	
	/**
	 * Rating expression, used by virtual ratings 
	 */
	public String mathExpression = null;
	/**
	 * Rating container, used by virtual and transitional ratings.
	 */
	public RatingSetContainer rsc = null;
	/**
	 * Units of rating or math expression in normal rating order
	 */
	public String[] units = null;

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		boolean result = obj == this;
		if (!result) {
			test:
				do {
					if (obj == null || obj.getClass() != getClass()) break;
					SourceRatingContainer other = (SourceRatingContainer)obj;
					if ((other.mathExpression == null) != (mathExpression == null)) break;
					if (mathExpression != null) {
						if (!other.mathExpression.equals(mathExpression)) break;
					}
					if ((other.rsc == null) != (rsc == null)) break;
					if (rsc != null) {
						if (!other.rsc.equals(rsc)) break;
					}
					if ((other.units == null) != (units == null)) {
						if (units != null) {
							if (other.units.length != units.length) break;
							for (int i = 0; i < units.length; ++i) {
								if ((other.units[i] == null) != (units[i] == null)) break test;
								if (units[i] != null) {
									if (!other.units[i].equals(units[i])) break test;
								}
							}
						}
					}
					result = true;
				} while(false);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode()
				+ 3 * (mathExpression == null ? 1 : mathExpression.hashCode())
				+ 5 * (rsc == null ? 1 : rsc.hashCode());
		if (units == null) {
			hashCode += 7;
		}
		else {
			hashCode += 9 * units.length;
			for (int i = 0; i < units.length; ++i) {
				hashCode += 11 * (units[i] == null ? i+1 : units[i].hashCode());
			}
		}
		return hashCode;
	}

	/**
	 * Clone this object into another
	 * @param other The other SourceRatingContainer object to clone into
	 */
	public void clone(SourceRatingContainer other)  {
		
		other.mathExpression = mathExpression;
		if (units != null) {
			other.units = new String[units.length];
			for (int i = 0; i < units.length; ++i) other.units[i] = units[i];
		}
		other.rsc = rsc == null ? null : rsc.clone();
	}
	
	public SourceRatingContainer clone() {
		SourceRatingContainer src = new SourceRatingContainer();
		this.clone(src);
		return src;
	}
}
