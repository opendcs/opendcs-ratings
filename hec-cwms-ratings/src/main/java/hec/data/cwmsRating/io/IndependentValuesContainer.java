/*
 * Copyright (c) 2021. Hydrologic Engineering Center (HEC).
 * United States Army Corps of Engineers
 * All Rights Reserved. HEC PROPRIETARY/CONFIDENTIAL.
 * Source may not be released without written approval from HEC
 *
 */

package hec.data.cwmsRating.io;

/**
 * Container to hold times and values to be rated
 *
 * @author Mike Perryman
 */
public class IndependentValuesContainer {
	/**
	 * The times of the values to be rated
	 */
	public long[] valTimes = null;
	/**
	 * The values to be rated
	 */
	public double[][] indVals = null;
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
					IndependentValuesContainer other = (IndependentValuesContainer)obj;
					if (valTimes == null) {
						if (other.valTimes != null) break;
					}
					else {
						if (other.valTimes == null) break;
						if (other.valTimes.length != valTimes.length) break;
						for (int i = 0; i < valTimes.length; ++i) {
							if (other.valTimes[i] != valTimes[i]) break test;
						}
					}
					if (indVals == null) {
						if (other.indVals != null) break;
					}
					else {
						if (other.indVals == null) break;
						if (other.indVals.length != indVals.length) break;
						for (int i = 0; i < indVals.length; ++i) {
							if (indVals[i] == null) {
								if (other.indVals[i] != null) break test;
							}
							else {
								if (other.indVals[i] == null) break test;
								if (other.indVals[i].length != indVals[i].length) break test;
								for (int j = 0; j < indVals[i].length; ++j) {
									if (other.indVals[i][j] != indVals[i][j]) break test;
								}
							}
						}
					}
					result = true;
				} while (false);
		}
		return result;
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int hashCode = getClass().getName().hashCode();
		if (valTimes != null) {
			for (int i = 0; i < valTimes.length; ++i)
			hashCode += (i+1) * (int)valTimes[i];
		}
		if (indVals != null) {
			hashCode += 3 * indVals.length;
			for (int i = 0; i < indVals.length; ++i) {
				if (indVals[i] == null) {
					hashCode += 5 * (i+1);
				}
				else {
					hashCode += 7 * (i+1) * indVals[i].length;
					for (int j = 0; j < indVals[i].length; ++j) {
						hashCode += 9 * (j+1) * (int)(indVals[i][j] * 1000);
					}
				}
			}
		}
		return hashCode;
	}
}
