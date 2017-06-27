/**
 * AsdiResourceData
 * Date created: March 29, 2017
 * This code has been developed by the SIB for use in the AWIPS2 system.
 */
package gov.noaa.nws.ncep.viz.rsc.asdi.rsc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.apache.log4j.Logger;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.viz.core.catalog.DirectDbQuery;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.IDataLoader;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.attributes.ResourceAttrSet;
import gov.noaa.nws.ncep.viz.ui.display.ColorBar;

/**
 * 
 * AsdiResourceData
 * 
 * This code has been developed by the SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#  Engineer     Description
 * ------------ -------- -----------  --------------------------
 * 03/07/2017   R28579     R.Reynolds  Initial coding.
 * 
 * </pre>
 * 
 * @author RC Reynolds
 * @version 1
 * 
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "AsdiResourceData")
public class AsdiResourceData extends AbstractNatlCntrsRequestableResourceData
        implements /* IMiscResourceData, */ INatlCntrsResourceData {

    private Logger logger = Logger.getLogger(this.getClass());

    private Boolean cancelBtnPressed = false;

    protected List<String> originalAirportNames = new ArrayList<>();

    protected List<Object[]> selectedAirportsNameLonLat = new ArrayList<>();

    protected List<String> selectedAirportNames = new ArrayList<>();

    public Boolean getCancelBtnPressed() {
        return cancelBtnPressed;
    }

    public void setCancelBtnPressed(Boolean cancelBtnPressed) {
        this.cancelBtnPressed = cancelBtnPressed;
    }

    public List<Object[]> getSelectedAirportsNameLonLat() {
        return selectedAirportsNameLonLat;
    }

    public List<String> getSelectedAirportNames() {
        return selectedAirportNames;
    }

    /**
     * assigns selected airports based on those selected from list and computes
     * their lat,lon
     * 
     * @param apNames
     */
    public void setSelectedAirportNames(String[] apNames) {

        String sql = "SELECT arpt_id, lon, lat FROM mapdata.airport";

        selectedAirportNames.clear();
        String[] sp = null;
        for (int i = 0; i < apNames.length; i++) {
            sp = apNames[i].split(",");
            for (int j = 0; j < sp.length; j++) {
                selectedAirportNames.add(sp[j]);
            }

        }

        selectedAirportsNameLonLat.clear();

        try {

            List<Object[]> resultList = DirectDbQuery.executeQuery(sql, "maps",
                    DirectDbQuery.QueryLanguage.SQL);

            /*
             * next, get lon, lat for selected airports using "maps" database
             */
            for (Iterator<Object[]> it = resultList.iterator(); it.hasNext();) {
                Object[] object = (Object[]) it.next();
                String airport3LetterCode = (String) object[0];

                /*
                 * ...and finally set airport code, lon and lat together in a
                 * new list
                 */
                if (selectedAirportNames.contains(airport3LetterCode)) {
                    selectedAirportsNameLonLat.add(object);
                }

            }

        } catch (Exception ex) {
            logger.debug(
                    " Exception is thrown when trying to do DirectDbQuery.executeQuery(sql,... error="
                            + ex.getMessage());
        }

    }

    public Integer getDepartArrive() {
        return departArrive;
    }

    public void setDepartArrive(Integer departArrive) {
        this.departArrive = departArrive;
    }

    @XmlElement
    protected int timeLimitValue;

    public int getTimeLimitValue() {
        return timeLimitValue;
    }

    public void setTimeLimitValue(int timeLimitValue) {
        this.timeLimitValue = timeLimitValue;
    }

    @XmlElement
    protected String legendName;

    @XmlElement
    protected boolean allAirports;

    @XmlElement
    protected int departArrive;

    @XmlElement
    protected String selectedAirports;

    @XmlElement
    protected ColorBar colorBarHeight;

    @XmlElement
    protected ColorBar colorBarTime;

    public ColorBar getColorBarHeight() {
        return colorBarHeight;
    }

    public void setColorBarHeight(ColorBar colorBarHeight) {
        this.colorBarHeight = colorBarHeight;
    }

    public ColorBar getColorBarTime() {
        return colorBarTime;
    }

    public void setColorBarTime(ColorBar colorBarTime) {
        this.colorBarTime = colorBarTime;
    }

    /**
     * get attributes for plotting by height
     * 
     * @return
     */
    public Boolean plotByHeight() {

        ResourceAttrSet editedRscAttrSet = new ResourceAttrSet(
                this.getRscAttrSet());

        if (editedRscAttrSet.getRscAttrSetName().equals("ASDI_H"))
            return true;

        return false;
    }

    public List<String> getAirportNames() {
        return originalAirportNames;
    }

    /**
     * set list of selected airport names including comma separated lists of
     * names
     * 
     * @param aptnamesList
     */
    public void loadAirportData(List<String> aptnamesList) {

        String[] split = null;

        List<String> tmpList = new ArrayList<>();

        for (int index = 0; index < aptnamesList.size(); index++) {
            split = aptnamesList.get(index).split(",");
            for (int splitIndex = 0; splitIndex < split.length; splitIndex++) {
                tmpList.add(split[splitIndex]);
            }
        }
        setSelectedAirportNames(tmpList.toArray(new String[0]));
    }

    public boolean getAllAirports() {
        return allAirports;
    }

    public void setAllAirports(boolean allAirports) {
        this.allAirports = allAirports;
    }

    public String getLegendName() {
        return legendName;
    }

    public void setLegendName(String legendName) {
        this.legendName = legendName;
    }

    /**
     * loads list of airports from XML Default Constructor
     * 
     * @throws VizException
     */
    public AsdiResourceData() throws VizException {
        super();

        /*
         * from XML file in localization
         */
        loadAirportData(AirportNames.getAirports());

        this.nameGenerator = new AbstractNameGenerator() {
            @Override
            public String getName(AbstractVizResource<?, ?> resource) {

                if (legendName != null) {
                    return legendName;
                }
                return "ASDI";
            }
        };
    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects) {
        AsdiResource asdiResource = new AsdiResource(this, loadProperties);
        return asdiResource;
    }

    // @Override
    // public MiscRscAttrs getMiscResourceAttrs() {
    // MiscRscAttrs attrs = null;
    // return attrs;
    // }

    @Override
    public IDataLoader getDataLoader() {
        return new AsdiDataLoader(this);
    }

}
