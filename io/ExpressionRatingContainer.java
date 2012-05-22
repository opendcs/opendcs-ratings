/**
 * 
 */
package hec.data.cwmsRating.io;

/**
 * Container for ExpressionRating data
 * @author Mike Perryman
 */
public class ExpressionRatingContainer extends AbstractRatingContainer {
	/**
	 * The mathematical expression for the rating
	 */
	public String expression = null;
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.io.AbstractRatingContainer#clone(hec.data.cwmsRating.io.AbstractRatingContainer)
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof ExpressionRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a ExpressionRatingContainer.");
		}
		super.clone(other);
		ExpressionRatingContainer rec = (ExpressionRatingContainer)other;
		rec.expression = expression;
	}
}
