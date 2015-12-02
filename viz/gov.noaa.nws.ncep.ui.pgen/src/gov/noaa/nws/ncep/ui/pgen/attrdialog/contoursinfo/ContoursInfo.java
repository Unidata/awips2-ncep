package gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import gov.noaa.nws.ncep.ui.pgen.attrdialog.contoursinfo.ContourFiles;

/**
 * Contour information dialog.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer     Description
 * ------------ ----------  -----------  --------------------------
 * 08/01/2015   8213        P.           CAVE>PGEN 
 *                          Chowdhuri     - Refinements to contoursInfo.xml
 * </pre>
 * 
 * @author pchowdhuri
 * @version 1
 */

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "contoursInfo")
public class ContoursInfo {

  @XmlAttribute
  private String name;
  @XmlAttribute
  private String parm;

  public ContoursInfo()	{ }

  @XmlElement(name = "level", required=false, nillable=false, type = ContourLevel.class)
  private ArrayList<ContourLevel> levels;

  @XmlElement(name = "fhrs", required=false, nillable=false, type = FcstHrs.class)
  private FcstHrs fhrs;
  
  @XmlElement(name = "buttons", required=false, nillable=false, type = ContourButtons.class)
  private ContourButtons buttons;

  @XmlElement(name = "markers", required=false, nillable=false, type = ContourMarkers.class)
  private ContourMarkers markers;

  @XmlElement(name = "lines", required=false, nillable=false, type = ContourLines.class)
  private ContourLines lines;

  @XmlElement(name = "cfiles", required=false, nillable=false, type = ContourFiles.class)
  private ContourFiles cFiles;
  
  public List<ContourLevel> getLevels() {
	return levels;
  }
  public void setLevels(List<ContourLevel> levels) {
	this.levels = (ArrayList<ContourLevel>)levels;
  }
  public ContourLines getLines() {
    return lines;
  }
  public void setLines(ContourLines lines) {
    this.lines = lines;
  }
  
  public String getName() {
	return name;
  }
  
  public void setName(String name) {
	this.name = name;
  }
  
  public String getParm() {
	return parm;
  }
  
  public void setParm(String parm) {
	this.parm = parm;
  }
  
  public FcstHrs getFhrs() {
	return fhrs;
  }
  
  public void setFhrs(FcstHrs fhrs) {
    this.fhrs = fhrs;
  }
  
  public ContourButtons getButtons() {
	return buttons;
  }
  
  public void setButtons(ContourButtons buttons) {
    this.buttons = buttons;
  }
  
  public ContourMarkers getMarkers() {
    return markers;
  }
  
  public void setMarkers(ContourMarkers markers) {
    this.markers = markers;
  }

  public ContourFiles getCfiles() {
	return cFiles;
  }
  
  public void setCfiles(ContourFiles cFiles) {
	this.cFiles = cFiles;
  }

}
