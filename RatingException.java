package hec.data.cwmsRating;

/**
 * Class to encapsulate rating errors
 *
 * @author Mike Perryman
 */
public class RatingException extends Exception {

	private static final long serialVersionUID = -1634053108648613361L;
	/**
	 * Constructor from text
	 * @param text The error message
	 */
	public RatingException(String text) {super(text);}
	/**
	 * Constructor from nested error
	 * @param t The nested error
	 */
	public RatingException(Throwable t) {super(t);}

}
