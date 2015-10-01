package gov.noaa.nws.ncep.edex.plugin.geomag;

/**
 * Class that represents a component exception
 * 
 * <pre>
 * * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer        Description
 * ------------ ---------- -----------     --------------------------
 * 10/07/2015   R11429     sgurung,jtravis Initial creation
 * 
 * </pre>
 * 
 * @author jtravis
 * @version 1.0
 */

public class ComponentException extends Exception {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1215252428147093628L;

    /**
	 * 
	 */
    public ComponentException() {
    }

    /**
     * @param message
     */
    public ComponentException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public ComponentException(Throwable cause) {
        super(cause);
    }

    /**
     * @param message
     * @param cause
     */
    public ComponentException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public ComponentException(String message, Throwable cause,
            boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

}
