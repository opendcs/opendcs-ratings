package hec.data.cwmsRating.io;

import static hec.lang.Const.UNDEFINED_DOUBLE;

/**
 * Data container class for RatingValue
 *
 * @author Mike Perryman
 */
public class RatingValueContainer {
	/**
	 * The independent value
	 */
	public double indValue = UNDEFINED_DOUBLE;
	/**
	 * The dependent value - should be UNDEFINED_DOUBLE if depTable is non-null
	 */
	public double depValue = UNDEFINED_DOUBLE;
	/**
	 * The rating value note, if any
	 */
	public String note = null;
	/**
	 * The dependent rating table - should be null if depValue != UNDEFINED_DOUBLE
	 */
	public TableRatingContainer depTable = null;
	/**
	 * Fills a specified RatingValueContainer object with information from this one
	 * @param other The RatingValueContainer object to fill
	 */
	public void clone(RatingValueContainer other) {
		other.indValue = indValue;
		other.depValue = depValue;
		other.note     = note;
		if (depTable == null) {
			other.depTable = null;
		}
		else {
			depTable.clone(other.depTable);
		}
	}
}
