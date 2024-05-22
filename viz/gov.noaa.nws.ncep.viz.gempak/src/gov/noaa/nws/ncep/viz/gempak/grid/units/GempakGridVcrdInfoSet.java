package gov.noaa.nws.ncep.viz.gempak.grid.units;

import java.util.ArrayList;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElements;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;

@XmlRootElement(name = "gempakGridVcordUnitSet")
@XmlAccessorType(XmlAccessType.NONE)
public class GempakGridVcrdInfoSet implements ISerializableObject{
	/**
     * List of vcord for/from the XML.
     */
    @XmlElements( { @XmlElement(name = "vcordinfo", type = GempakGridVcrdInfo.class) })
    private ArrayList<GempakGridVcrdInfo> vcordinfo;

	public ArrayList<GempakGridVcrdInfo> getVcordinfo() {
		return vcordinfo;
	}

	public void setVcordinfo(ArrayList<GempakGridVcrdInfo> vcordinfo) {
		this.vcordinfo = vcordinfo;
	}
}

