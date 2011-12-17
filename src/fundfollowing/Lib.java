package fundfollowing;

/**
 * Thrown when an assertion fails.
 */
@SuppressWarnings("serial")
class AssertionFailureError extends Error {
    AssertionFailureError() {
	super();
    }

    AssertionFailureError(String message) {
	super(message);
    }
}

public class Lib {

    /**
     * Asserts that <i>expression</i> is <tt>true</tt>. If not, then
     * exits with an error message.
     *
     * @param	expression	the expression to assert.
     */     
    public static void assertTrue(boolean expression) {
	if (!expression)
	    throw new AssertionFailureError();
    }

    /**
     * Asserts that <i>expression</i> is <tt>true</tt>. If not, then
     * exits with the specified error message.
     *
     * @param	expression	the expression to assert.
     * @param	message		the error message.
     */     
    public static void assertTrue(boolean expression, String message) {
	if (!expression)
	    throw new AssertionFailureError(message);
    }

    /**
     * Asserts that this call is never made. Same as <tt>assertTrue(false)</tt>.
     */
    public static void assertNotReached() {
	assertTrue(false);
    }
    
    /**
     * Asserts that this call is never made, with the specified error message.
     * Same as <tt>assertTrue(false, message)</tt>.
     *
     * @param	message	the error message.
     */
    public static void assertNotReached(String message) {
	assertTrue(false, message);
    }
	
    
	public static long convertSecondToMillis(double d) {
		return (long) (1000 * d);
	}
}
