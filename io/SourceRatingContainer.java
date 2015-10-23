/**
 * 
 */
package hec.data.cwmsRating.io;

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

	/**
	 * Clone this object into another
	 * @param other The other SourceRatingContainer object to clone into
	 */
	public void clone(SourceRatingContainer other)  {
		
		other.mathExpression = mathExpression;
		if (rsc == null) {
			other.rsc = null;
		}
		else {
			other.rsc = new RatingSetContainer();
			rsc.clone(other.rsc);
		}
	}
	
	public SourceRatingContainer clone() {
		SourceRatingContainer src = new SourceRatingContainer();
		this.clone(src);
		return src;
	}
}
