package gov.noaa.nws.ncep.viz.rsc.ntrans.rsc;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.eclipse.swt.graphics.RGB;

import com.raytheon.uf.common.time.DataTime;
import com.raytheon.uf.viz.core.exception.VizException;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;

import gov.noaa.nws.ncep.viz.common.RGBColorAdapter;
import gov.noaa.nws.ncep.viz.common.display.NcDisplayType;
import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.resources.manager.ResourceDefnsMngr;
import gov.noaa.nws.ncep.viz.resources.misc.IMiscResourceData;

/**
 * NtransResourceData - Resource Data for Display of NTRANS Metafiles.
 *
 * This code has been developed by the SIB for use in the AWIPS2 system.
 *
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 02 Apr 2013  838        B. Hebbard  Initial creation.
 * 25 Apr 2013  838        G. Hull     don't override getAvailableDataTimes(). rm legend as a parameter
 * 2017/02/22   R5940      B. Hebbard  Override resolveLatestCycleTime() to deal 
 *                                     with metafile name too and add helper 
 *                                     method getLatestMatchingGroup.
 *
 * </pre>
 *
 * @author bhebbard
 * @version 1.0
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NC-NtransResourceData")
public class NtransResourceData extends AbstractNatlCntrsRequestableResourceData
        implements IMiscResourceData, INatlCntrsResourceData {

    @XmlElement
    @XmlJavaTypeAdapter(RGBColorAdapter.class)
    protected RGB color; // resource legend color only

    @XmlElement
    protected String modelName;

    @XmlElement
    protected String metafileName;

    @XmlElement
    protected String productName;

    // ------------------------------------------------------------

    /**
     * Create an NTRANS Metafile display resource.
     *
     * @throws VizException
     */
    public NtransResourceData() throws VizException {
        super();
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String model) {
        this.modelName = model;
    }

    public String getMetafileName() {
        return metafileName;
    }

    public void setMetafileName(String metafile) {
        this.metafileName = metafile;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String group) {
        this.productName = group;
    }

    @Override
    public NcDisplayType[] getSupportedDisplayTypes() {
        return new NcDisplayType[] { NcDisplayType.NTRANS_DISPLAY };
    }

    @Override
    public MiscRscAttrs getMiscResourceAttrs() {
        MiscRscAttrs attrs = new MiscRscAttrs(3);

        attrs.addAttr(new MiscResourceAttr("hour0Enable", "Initial (0-hour)",
                EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("hour0Color", "",
                EditElement.COLOR_SELECTOR, 2));
        attrs.addAttr(new MiscResourceAttr("hour0LineWidth", "Line Width",
                EditElement.SPINNER, 3));
        attrs.addAttr(new MiscResourceAttr("hour1Enable", "1-hour",
                EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("hour1Color", "",
                EditElement.COLOR_SELECTOR, 2));
        attrs.addAttr(new MiscResourceAttr("hour1LineWidth", "Line Width",
                EditElement.SPINNER, 3));
        attrs.addAttr(new MiscResourceAttr("hour2Enable", "2-hour",
                EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("hour2Color", "",
                EditElement.COLOR_SELECTOR, 2));
        attrs.addAttr(new MiscResourceAttr("hour2LineWidth", "Line Width",
                EditElement.SPINNER, 3));
        attrs.addAttr(new MiscResourceAttr("outlookEnable", "Outlook",
                EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("outlookColor", "",
                EditElement.COLOR_SELECTOR, 2));
        attrs.addAttr(new MiscResourceAttr("outlookLineWidth", "Line Width",
                EditElement.SPINNER, 3));

        attrs.addAttr(
                new MiscResourceAttr(null, null, EditElement.SEPARATOR, 1));

        attrs.addAttr(new MiscResourceAttr("hour0sequenceIdEnable",
                "Sequence ID (0-hour)", EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("timeEnable", "Time",
                EditElement.CHECK_BOX, 1));

        attrs.addAttr(new MiscResourceAttr("motionEnable", "Direction/speed",
                EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("flightLevelEnable", "Flight Level",
                EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("intensityEnable", "Intensity",
                EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("hour1sequenceIdEnable",
                "Sequence ID (1-hour)", EditElement.CHECK_BOX, 1));
        attrs.addAttr(new MiscResourceAttr("hour2sequenceIdEnable",
                "Sequence ID (2-hour)", EditElement.CHECK_BOX, 1));

        return attrs;
    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties,
            com.raytheon.uf.common.dataplugin.PluginDataObject[] objects)
                    throws VizException {
        return new NtransResource(this, loadProperties);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData#
     * resolveLatestCycleTime()
     */
    @Override
    public void resolveLatestCycleTime() {
        if (getResourceName().isLatestCycleTime()) {

            // Get resource definitions manager...

            ResourceDefnsMngr rscDefnsMngr;
            try {
                rscDefnsMngr = ResourceDefnsMngr.getInstance();
            } catch (VizException e) {
                return;
            }

            // ...and ask it which groups (NTRANS metafile/product pairs) are
            // available

            String[] rscGroups;
            try {
                rscGroups = rscDefnsMngr
                        .getResourceSubTypes(resourceName.getRscType());
            } catch (VizException e) {
                return;
            }

            // Determine the latest such group that is 'similar' to the original
            // one, and update the ResourceName accordingly...

            String latestMatchingGroupInDb = getLatestMatchingGroup(
                    resourceName.getRscGroup(), rscGroups);
            resourceName.setRscGroup(latestMatchingGroupInDb);

            // ...and, importantly, update the metadataMap to reflect the newer
            // NTRANS metafile
            this.getMetadataMap().get("metafileName")
                    .setConstraintValue(latestMatchingGroupInDb.split("_")[0]);

            // Now determine the available cycle time(s), and set the latest one

            DataTime[] availableTimes = null;
            try {
                availableTimes = getAvailableTimes();
            } catch (VizException e) {
                statusHandler.error(
                        "Error getting Available Times: " + e.getMessage());
                return;
            }
            long cycleTimeMs = 0;
            for (DataTime dt : availableTimes) {
                if (dt.getRefTime().getTime() > cycleTimeMs) {
                    cycleTimeMs = dt.getRefTime().getTime();
                    resourceName.setCycleTime(dt);
                }
            }
        }
    }

    private String getLatestMatchingGroup(String originalGroupName,
            String[] availableGroupNames) {

        // Find the group name in availableGroupNames that contains the
        // latest time substring AND otherwise matches originalGroupName

        // As with the NTRANS decoder, we need to recognize time (date,
        // and sometimes hour) substrings in any of these formats:
        // YYYYMMDD
        // YYYYMMDDHH
        // YYYYMMDD_HH
        // YYMMDD
        // YYMMDD_HH
        final Pattern p1 = Pattern
                .compile("(^.*?)((\\d\\d){3,4})_?(\\d\\d)?(.*?$)");
        Matcher m1 = p1.matcher(originalGroupName);

        String latestMatch = originalGroupName;
        // If we don't find any time substring, just return the original name
        if (m1.find()) {
            // Otherwise, build a second pattern, to match everything from the
            // original EXCEPT (i.e., before and after) the date...
            String prefix = m1.group(1);
            // String timeString = m1.group(2); // ignored
            String postfix = m1.group(5);
            String patternString2 = "^" + prefix + "((\\d\\d){3,4})_?(\\d\\d)?"
                    + postfix + "$";
            patternString2 = patternString2.replaceAll("\\+", "\\\\+");
            Pattern p2 = Pattern.compile(patternString2);
            // ...and find latest (lex >) name matching that second pattern:
            for (String s : availableGroupNames) {
                Matcher m2 = p2.matcher(s);
                if (m2.find() && s.compareTo(latestMatch) > 0) {
                    latestMatch = s;
                }
            }
        }
        return latestMatch;
    }

    /*
     * public ArrayList<DataTime> generateFrameTimes( ) { ArrayList<DataTime>
     * times = new ArrayList<DataTime>(); times.add( new DataTime(
     * Calendar.getInstance().getTime() ) ); return times; }
     */

}
