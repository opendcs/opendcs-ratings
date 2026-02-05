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
	 * @param message The exception message
	 */
	public RatingRuntimeException(String message) {
		super(message);
	}

	/**
	 * @param cause The upstream cause of the exception
	 */
	public RatingRuntimeException(Throwable cause) {
		super(cause);
	}

	/**
	 * @param message The exception message
	 * @param cause The upstream cause of the exception
	 */
	public RatingRuntimeException(String message, Throwable cause) {
		super(message, cause);
	}

	/**
	 * @param message The exception message
	 * @param cause The upstream cause of the exception
	 * @param enableSuppression whether to enable suppression of the exception
	 * @param writableStackTrace whether the stack trace should be writable
	 */
	public RatingRuntimeException(String message, Throwable cause, boolean enableSuppression,
			boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

}
