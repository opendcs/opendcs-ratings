
package org.opendcs.ratings;

/**
 * Class to encapsulate rating errors
 *
 * @author Mike Perryman
 */
public class RatingException extends Exception {

	private static final long serialVersionUID = -1634053108648613361L;
	/**
	 * Constructor from message
	 * @param message 
	 */
	public RatingException(String message) {super(message);}
	/**
	 * Constructor from cause
	 * @param cause the cause
	 */
	public RatingException(Throwable cause) {super(cause);}
	/**
	 * Constructor from message and cause
	 * @param message
	 * @param cause
	 */
	public RatingException(String message, Throwable cause) {super(message, cause);}

}
