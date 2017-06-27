package gov.noaa.nws.ncep.edex.plugin.nctext.common;
/**
 * 
 * NctextTafStn 
 * 
 * This java class store taf data station id. It creates nctext_tafstn table
 * which is a child table of nctext table.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#     Engineer      Description
 * -------      -------     --------      -----------
 * 02/21/2017   R28184      Chin Chen     Initial coding for fixing slow loading time for Observed TAF Data Products issue
 *
 * </pre>
 * 
 * @author Chin Chen
 * @version 1.0
 */
import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

import com.raytheon.uf.common.serialization.ISerializableObject;
import com.raytheon.uf.common.serialization.annotations.DynamicSerialize;
import com.raytheon.uf.common.serialization.annotations.DynamicSerializeElement;

@Entity
@Table(name="nctext_tafstn")
@DynamicSerialize
public class NctextTafStn implements Serializable, ISerializableObject {
    private static final long serialVersionUID = 1L;
	
    @Id
    @GeneratedValue
    private Integer id = null;
	
    @Column
    @DynamicSerializeElement
    private String stnId;

    public NctextTafStn() {
        super();
    }

    public String getStnId() {
        return stnId;
    }

    public void setStnId(String stnId) {
        this.stnId = stnId;
    }
	
	
}
