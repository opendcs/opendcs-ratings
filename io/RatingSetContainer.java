package hec.data.cwmsRating.io;

/**
 * Data container class for RatingSet
 * @author Mike Perryman
 */
public class RatingSetContainer {
	/**
	 * Contains the rating specification
	 */
	public RatingSpecContainer ratingSpecContainer = null;
	/**
	 * Contains the individual ratings
	 */
	public AbstractRatingContainer[] abstractRatingContainers = null;
	/**
	 * Fills another RatingSetContainer object with data from this one
	 * @param other The RatingSetContainer object to fill
	 */
	public void clone(RatingSetContainer other) {
		if (ratingSpecContainer == null) {
			other.ratingSpecContainer = null;
		}
		else {
			if (other.ratingSpecContainer == null) other.ratingSpecContainer = new RatingSpecContainer();
			ratingSpecContainer.clone(other.ratingSpecContainer);
		}
		if (abstractRatingContainers == null) {
			other.abstractRatingContainers = null;
		}
		else {
			other.abstractRatingContainers = new AbstractRatingContainer[abstractRatingContainers.length];
			for (int i = 0; i < abstractRatingContainers.length; ++i) {
				other.abstractRatingContainers[i] = new TableRatingContainer();
				abstractRatingContainers[i].clone(other.abstractRatingContainers[i]);
			}
		}
	}
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		try {
			return String.format(
					"%s - %d ratings",
					ratingSpecContainer == null ? "<NULL>" : ratingSpecContainer.toString(),
					abstractRatingContainers == null ? 0 : abstractRatingContainers.length);	
		}
		catch (Throwable t) {
			return ((Object)this).toString();
		}
	}

}
