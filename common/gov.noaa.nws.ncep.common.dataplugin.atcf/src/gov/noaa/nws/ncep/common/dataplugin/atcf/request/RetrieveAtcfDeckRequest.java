package gov.noaa.nws.ncep.common.dataplugin.atcf.request;

import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;
import com.raytheon.uf.common.serialization.comm.IServerRequest;

/**
 * 
 * Request for an ATCF "deck" file for the given dataURI
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 04/22/2013              sgilbert     Initial creation (as RetrieveActivityRequest)
 * 08/24/2016   R22939     bhebbard     Adapted to ATCF "deck" file retrieval
 * 
 * </pre>
 * 
 * @author bhebbard
 * @version 1.0
 */
@DynamicSerialize
public class RetrieveAtcfDeckRequest implements IServerRequest {

    @DynamicSerializeElement
    private String deckID;  //  String identifying the deck to export; e.g. "bal142016"

    public RetrieveAtcfDeckRequest() {
    }

    public RetrieveAtcfDeckRequest(String deckID) {
        super();
        this.deckID = deckID;
    }

    public String getDeckID() {
        return deckID;
    }

    public void setDeckID(String deckID) {
        this.deckID = deckID;
    }

}
