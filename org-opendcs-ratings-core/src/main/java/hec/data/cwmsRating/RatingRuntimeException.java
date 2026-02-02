package org.opendcs.ratings;

/**
 * @author Mike Perryman
 *
 */
public class RatingRuntimeException extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1599158567784780580L;

	/**
	 * 
	 */
	public RatingRuntimeException() {
	}

	/**
	 * @param message
	 */
	public RatingRuntimeException(String message) {
		super(message);
	}

	/**
	 * @param cause
	 */
	public RatingRuntimeException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message
	 * @param cause
	 */
	public RatingRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message
	 * @param cause
	 * @param enableSuppression
	 * @param writableStackTrace
	 */
	public RatingRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
