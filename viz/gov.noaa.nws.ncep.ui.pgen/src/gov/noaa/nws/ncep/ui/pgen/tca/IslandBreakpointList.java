/*
 * IslandBerakpointList
 * 
 * Date created 03 NOVEMBER 2009
 *
 * This code has been developed by the NCEP/SIB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.ui.pgen.tca;

import java.util.List;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import com.raytheon.uf.common.serialization.ISerializableObject;

/**
 * This class contains a list of many island breakpoints
 * @author sgilbert
 *
 */
@XmlRootElement(name="islandBreakpoints")
@XmlAccessorType(XmlAccessType.NONE)
public class IslandBreakpointList implements ISerializableObject {


	@XmlElement(name="island")
	private List<IslandBreakpoint> islands;

	/**
	 * @return the islands
	 */
	public List<IslandBreakpoint> getIslands() {
		return islands;
	}

	/**
	 * @param islands the islands to set
	 */
	public void setIslands(List<IslandBreakpoint> islands) {
		this.islands = islands;
	}

	
}
