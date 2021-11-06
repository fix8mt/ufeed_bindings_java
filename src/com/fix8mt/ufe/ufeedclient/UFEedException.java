package com.fix8mt.ufe.ufeedclient;

/**
 * UFEedClient exception class. Can be thrown by UFEedClient methods
 */
public class UFEedException extends Exception {

	/**
	 * Constructs exception
	 * @param text exception message
	 */
	public UFEedException(String text) {
		super(text);
	}
}

