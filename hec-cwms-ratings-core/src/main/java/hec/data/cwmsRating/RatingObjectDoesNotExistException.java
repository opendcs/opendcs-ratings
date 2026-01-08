package hec.data.cwmsRating;

/**
 * Class to encapsulate rating errors arising from missing objects
 *
 * @author Mike Perryman
 */
public class RatingObjectDoesNotExistException extends RatingException {
	/**
	 * Constructor from text
	 * @param text The error message
	 */
	public RatingObjectDoesNotExistException(String text) {
		super(text);
	}
	/**
	 * Constructor from nested error
	 * @param t The nested error
	 */
	public RatingObjectDoesNotExistException(Throwable t) {
		super(t);
	}

}
