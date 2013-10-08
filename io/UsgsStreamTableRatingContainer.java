package hec.data.cwmsRating.io;


/**
 * Data container class for UsgsStreamTableRating data
 * @author Mike Perryman
 */
public class UsgsStreamTableRatingContainer extends TableRatingContainer {

	/**
	 * The dated shift adjustments
	 */
	public RatingSetContainer shifts = null;
	/**
	 * The logarithmic interpolation offsets
	 */
	public TableRatingContainer offsets = null;
	
	/* (non-Javadoc)
	 * @see hec.data.cwmsRating.RatingTableContainer#clone(hec.data.cwmsRating.RatingContainer)
	 */
	@Override
	public void clone(AbstractRatingContainer other) {
		if (!(other instanceof UsgsStreamTableRatingContainer)) {
			throw new IllegalArgumentException("Clone-to object must be a UsgsStreamTableRatingContainer.");
		}
		super.clone(other);
		UsgsStreamTableRatingContainer ustrc = (UsgsStreamTableRatingContainer)other;
		if (shifts == null) {
			ustrc.shifts = null;
		}
		else {
			if (ustrc.shifts == null) ustrc.shifts = new RatingSetContainer();
			shifts.clone(ustrc.shifts);
		}
		if (offsets == null) {
			ustrc.offsets = null;
		}
		else {
			if (ustrc.offsets == null) ustrc.offsets = new TableRatingContainer();
			offsets.clone(ustrc.offsets);
		}
	}
	
	@Override
	public AbstractRatingContainer getInstance()
	{
		return new UsgsStreamTableRatingContainer();
	}
}
