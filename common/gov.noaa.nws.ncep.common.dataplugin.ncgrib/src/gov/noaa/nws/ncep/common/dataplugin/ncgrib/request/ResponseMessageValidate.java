package gov.noaa.nws.ncep.common.dataplugin.ncgrib.request;

import com.raytheon.uf.common.message.response.AbstractResponseMessage;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

/**
 * Represents a validation response from EDEX. Fields are provided to specify
 * validation result (success or failure) and for a result message. Modified
 * from com.raytheon.edex.msg.ResponseMessageValidate: Added annotations for use
 * with ThriftClient and removed script field.
 * <P>
 * A static convenience method
 * {@link #generateValidateResponse(boolean, String, String, Exception)
 * generateValidateResponse } is included to facilitate message creation.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date             PR#             Engineer            Description
 * -----------      ----------      ------------        --------------------------
 * 19Jul2006                        MW Fegan            Initial Creation
 * 10Aug2006        Task 19         MW Fegan            Updated JavaDoc.
 * 
 * </PRE>
 * 
 * :q
 * 
 * @author mfegan
 * 
 */
@DynamicSerialize
public class ResponseMessageValidate extends AbstractResponseMessage {
    @DynamicSerializeElement
    private Boolean result;

    @DynamicSerializeElement
    private String message;

    /**
     * Constructor. Basic no argument constructor.
     */
    public ResponseMessageValidate() {
        super();
    }

    /**
     * Constructor. Creates a Validate Response Message for the specified
     * {@code result}, {@code message } and {@code script}.
     * 
     * @param result
     *            true if the validation was successful
     * @param message
     *            plain text validation result message
     * @param script
     *            the script being validated
     */
    public ResponseMessageValidate(boolean result, String message) {
        super();
        this.result = result;
        this.message = message;
    }

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message
     *            the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the result
     */
    public Boolean getResult() {
        return result;
    }

    /**
     * @param result
     *            the result to set
     */
    public void setResult(Boolean result) {
        this.result = result;
    }

}
