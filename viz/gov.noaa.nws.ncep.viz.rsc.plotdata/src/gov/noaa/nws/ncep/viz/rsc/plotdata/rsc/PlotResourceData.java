
package gov.noaa.nws.ncep.viz.rsc.plotdata.rsc;

import java.util.ArrayList;
import java.util.HashSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import com.raytheon.uf.common.dataplugin.PluginDataObject;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.viz.core.rsc.AbstractNameGenerator;
import com.raytheon.uf.viz.core.rsc.AbstractVizResource;
import com.raytheon.uf.viz.core.rsc.LoadProperties;
import com.raytheon.viz.pointdata.def.AbstractConditionalFilterManager;
import com.raytheon.viz.pointdata.def.ConditionalFilter;
import com.raytheon.viz.pointdata.rsc.retrieve.AbstractDbPlotInfoRetriever;

import gov.noaa.nws.ncep.viz.resources.AbstractNatlCntrsRequestableResourceData;
import gov.noaa.nws.ncep.viz.resources.INatlCntrsResourceData;
import gov.noaa.nws.ncep.viz.rsc.plotdata.plotModels.elements.PlotModel;

/**
 * Resource data for plots
 *
 * <pre>
 *
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * Feb 17, 2009            njensen     Initial creation
 * Apr 09, 2010   259      ghull       move legendColor to Abstract
 * 07/26/2010    T285      qzhou       Added bufrua. Added Man. level to legend on screen
 * 08/11/2010    #273      ghull       remove getResourceName(), add isSurfaceOnly(),
 *                                     get plotModel cat from resourceName, get plotModelName
 *                                     from bundle file.
 * 10/13/2010    #307      ghull       create FcstPlotResource for bufrmos
 * 03/07/2011     migration ghull     use AbstractDbPlotInfoRetriever
 * 03/04/2011              ghull       change plotModel category to plugin name
 * 05/12/2011    #441      ghull       remove upper/lower limit
 * 09/03/2011              ghull       rmove reportTypeKey; add ncscd 
 * 09/14/2011    #457      sgurung     Renamed h5 to nc
 * 09/22/2011    #459      ghull       added modelsounding, ncairep and ncpirep
 * 10/14/2011              ghull       added ncpafm, nctaf
 * 10/18/2011              sgurung     Modified setReportType() to set constrainttype as ConstraintType.IN 
 * 10/19/2011              ghull       add TafPlotResource
 * 11/01/2011    #482      ghull       added plotDensity, comment out unimplemented plugins
 * 04/09/2012    #615      sgurung     Added conditionalFilterName and conditionalFilter
 * 02/05/2012    #606      ghull       rm reportType as member variable
 * 10/19/2012    #896      sgurung     Use NcPlotResource2
 * 11/04/2012    #944      ghull       rm FcsPlotResource
 * 12/19/2012    #947      ghull       save ConditionalFilter object to the RBD.
 * 04/15/2013    #864      ghull       rm isForecastResource()
 * Sep 05, 2013  2316      bsteffen    Unify pirep and ncpirep.
 * 06/09/2015	#8585	   jhuber	   Remove ncscd plugins.
 * 12/10/2019   72281      K Sunil     Slightly different getDefaultConditionalFilter call. 
 *                                       Removed old commented out code.
 *
 * </pre>
 *
 * @author njensen
 */

@XmlAccessorType(XmlAccessType.NONE)
@XmlType(name = "NC-PlotResourceData")
public class PlotResourceData extends AbstractNatlCntrsRequestableResourceData
        implements INatlCntrsResourceData {

    private static final IUFStatusHandler statusHandler = UFStatus
            .getHandler(PlotResourceData.class);

    protected int pixelSizeHint = 80;

    @XmlElement
    protected String legendString = null;

    @XmlElement
    protected String spiFile = null;

    /*
     * if the plotModel has not been edited then these values will be from the
     * plotModelName. Otherwise this will contain the edited plotModel values.
     */
    @XmlElement
    protected PlotModel plotModel = null;

    /*
     * For upper air plots, levelKey is an attribute, for non-upper air plots
     * this will be set to 'Surface'
     */
    @XmlElement
    protected String levelKey = null;

    @XmlElement
    protected Integer plotDensity = 10;

    @XmlElement
    protected boolean plotMissingData = false;

    @XmlElement
    protected AbstractDbPlotInfoRetriever plotInfoRetriever;

    @XmlElement
    protected ConditionalFilter conditionalFilter = null;

    private static HashSet<String> pluginNames = new HashSet<String>();

    private static ArrayList<String> sfcPlugins = new ArrayList<String>();

    static {
        pluginNames.add("obs");
        pluginNames.add("sfcobs");
        pluginNames.add("ncuair");
        pluginNames.add("airep");
        pluginNames.add("pirep");
        pluginNames.add("nctaf");
        pluginNames.add("ncpafm");
        pluginNames.add("modelsounding");
        pluginNames.add("bufrmosLAMP");
        pluginNames.add("bufrmosAVN");
        pluginNames.add("bufrmosETA");
        pluginNames.add("bufrmosGFS");
        pluginNames.add("bufrmosNAM");
        pluginNames.add("bufrmosHPC");
        pluginNames.add("bufrmosMRF");

        /*
         * We could key off of levelKey.equals("Surface") but for airep and
         * pirep levelKey won't be surface
         */
        sfcPlugins.add("obs");
        sfcPlugins.add("sfcobs");
        sfcPlugins.add("airep");
        sfcPlugins.add("pirep");
        sfcPlugins.add("nctaf");
        sfcPlugins.add("ncpafm");
        sfcPlugins.add("bufrmosLAMP");
        sfcPlugins.add("bufrmosAVN");
        sfcPlugins.add("bufrmosETA");
        sfcPlugins.add("bufrmosGFS");
        sfcPlugins.add("bufrmosNAM");
        sfcPlugins.add("bufrmosHPC");
        sfcPlugins.add("bufrmosMRF");
    }

    public PlotResourceData() {
        super();
        this.nameGenerator = new AbstractNameGenerator() {
            @Override
            public String getName(AbstractVizResource<?, ?> resource) {
                if (isSurfaceOnly()) {
                    return (legendString != null ? legendString : "Plot Data");
                } else {
                    return (legendString != null
                            ? legendString + " " + getLevelKey() + " mb"
                            : "Plot Data");
                }
            }
        };
    }

    @Override
    protected AbstractVizResource<?, ?> constructResource(
            LoadProperties loadProperties, PluginDataObject[] objects) {

        String pluginName = this.metadataMap.get("pluginName")
                .getConstraintValue();

        if (pluginNames.contains(pluginName)) {
            return new NcPlotResource2(this, loadProperties);
        } else {
            statusHandler.warn(
                    "Plugin " + pluginName + " not supported by PlotResource2");
            return null;
        }
    }

    public String getLegendString() {
        return legendString;
    }

    public void setLegendString(String legendString) {
        this.legendString = legendString;
    }

    public int getPixelSizeHint() {
        return pixelSizeHint;
    }

    public String getSpiFile() {
        return spiFile;
    }

    public void setSpiFile(String spiFile) {
        this.spiFile = spiFile;
    }

    public void setPlotModel(PlotModel pm) {
        plotModel = pm;
    }

    public PlotModel getPlotModel() {

        return new PlotModel(plotModel);
    }

    public boolean isPlotMissingData() {
        return plotMissingData;
    }

    public void setPlotMissingData(boolean plotMissingData) {
        this.plotMissingData = plotMissingData;
    }

    public AbstractDbPlotInfoRetriever getPlotInfoRetriever() {
        return plotInfoRetriever;
    }

    public void setPlotInfoRetriever(
            AbstractDbPlotInfoRetriever plotInfoRetriever) {
        this.plotInfoRetriever = plotInfoRetriever;
    }

    public String getLevelKey() {
        return levelKey;
    }

    public void setLevelKey(String levelKey) {
        this.levelKey = levelKey;
    }

    public Integer getPlotDensity() {
        return plotDensity;
    }

    public void setPlotDensity(Integer plotDensity) {
        this.plotDensity = plotDensity;
    }

    public boolean isSurfaceOnly() {
        return sfcPlugins.contains(getPluginName());
    }

    public ConditionalFilter getConditionalFilter() {
        /*
         * Note that now the conditionalFilter is set directly from either the
         * refd xml file or from the xml in the RBD. if the conditionalFilter
         * has not been set yet (from xml file or from the conditionalFilterName
         * attribute) then get it from the manager
         */
        if (conditionalFilter == null) {
            conditionalFilter = AbstractConditionalFilterManager
                    .getDefaultConditionalFilter(getPluginName());
            return conditionalFilter;
        }
        return new ConditionalFilter(conditionalFilter);
    }

    public void setConditionalFilter(ConditionalFilter conds) {
        this.conditionalFilter = conds;
    }

    @Override
    public boolean equals(Object obj) {
        if (!super.equals(obj)) {
            return false;
        }

        if (obj instanceof PlotResourceData == false) {
            return false;
        }

        PlotResourceData other = (PlotResourceData) obj;

        if (this.spiFile != null && other.spiFile == null) {
            return false;
        } else if (this.spiFile == null && other.spiFile != null) {
            return false;
        } else if (this.spiFile != null
                && this.spiFile.equals(other.spiFile) == false) {
            return false;
        }

        if (this.levelKey != null && other.levelKey == null) {
            return false;
        } else if (this.levelKey == null && other.levelKey != null) {
            return false;
        } else if (this.levelKey != null
                && this.levelKey.equals(other.levelKey) == false) {
            return false;
        }

        if (this.plotDensity != null && other.plotDensity == null) {
            return false;
        } else if (this.plotDensity == null && other.plotDensity != null) {
            return false;
        } else if (this.plotDensity != null
                && this.plotDensity.equals(other.plotDensity) == false) {
            return false;
        }

        if (this.plotInfoRetriever != null && other.plotInfoRetriever == null) {
            return false;
        } else if (this.plotInfoRetriever == null
                && other.plotInfoRetriever != null) {
            return false;
        } else if (this.plotInfoRetriever != null && this.plotInfoRetriever
                .equals(other.plotInfoRetriever) == false) {
            return false;
        }

        return (this.pixelSizeHint == other.pixelSizeHint);
    }
}
