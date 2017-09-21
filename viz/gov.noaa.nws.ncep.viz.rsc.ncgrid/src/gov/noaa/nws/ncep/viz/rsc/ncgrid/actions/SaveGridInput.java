package gov.noaa.nws.ncep.viz.rsc.ncgrid.actions;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.raytheon.uf.common.dataplugin.level.Level;
import com.raytheon.uf.common.dataplugin.level.LevelFactory;
import com.raytheon.uf.common.dataplugin.level.mapping.LevelMapper;
import com.raytheon.uf.common.parameter.Parameter;
import com.raytheon.uf.common.parameter.lookup.ParameterLookupException;
import com.raytheon.uf.common.parameter.mapping.ParameterMapper;
import com.raytheon.uf.common.status.IUFStatusHandler;
import com.raytheon.uf.common.status.UFStatus;
import com.raytheon.uf.common.status.UFStatus.Priority;
import com.raytheon.uf.common.util.mapping.MultipleMappingException;

/**
 * Validates user input values when saving blended grid.
 * 
 * <pre>
 * 
 * SOFTWARE HISTORY
 * 
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/01/2016   R6821      kbugenhagen  Initial creation
 * 
 * </pre>
 * 
 * @author kbugenhagen
 * @version 1.0
 */

public class SaveGridInput {
    protected static final transient IUFStatusHandler statusHandler = UFStatus
            .getHandler(SaveGridInput.class);

    public String gdfile = "";

    private String modelName = "";

    private String gparm = "";

    private String gvcord = "";

    private String glevel = "";

    private String gdattim = "";

    private boolean saveAll = false;

    private Level level = null;

    private Parameter parameter = null;

    /**
     * @param gdfile
     */
    public SaveGridInput(String gdfile) {
        this.gdfile = gdfile;
    }

    public SaveGridInput(String gdfile, Map<String, String> attributes) {
        this(gdfile);
        populateDefaultValues(attributes);
    }

    private void populateDefaultValues(Map<String, String> attributes) {
        glevel = attributes.containsKey("SAVE_GLEVEL") ? attributes
                .get("SAVE_GLEVEL") : "";
        gvcord = attributes.containsKey("SAVE_GVCORD") ? attributes
                .get("SAVE_GVCORD") : "";
        gparm = attributes.containsKey("SAVE_GPARM") ? attributes
                .get("SAVE_GPARM") : "";
    }

    /**
     * @return the gdfile
     */
    public String getGdfile() {
        return gdfile;
    }

    /**
     * @return the modelName
     */
    public String getModelName() {
        return modelName;
    }

    /**
     * @param modelName
     *            the modelName to set
     */
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    /**
     * @return the gparm
     */
    public String getGparm() {
        return gparm;
    }

    /**
     * @param gparm
     *            the gparm to set
     */
    public void setGparm(String gparm) {
        this.gparm = gparm;
    }

    /**
     * @return the gvcord
     */
    public String getGvcord() {
        return gvcord;
    }

    /**
     * @param gvcord
     *            the gvcord to set
     */
    public void setGvcord(String gvcord) {
        this.gvcord = gvcord;
    }

    /**
     * @return the glevel
     */
    public String getGlevel() {
        return glevel;
    }

    /**
     * @param glevel
     *            the glevel to set
     */
    public void setGlevel(String glevel) {
        this.glevel = glevel;
    }

    /**
     * @return the gdattim
     */
    public String getGdattim() {
        return gdattim;
    }

    /**
     * @param gdattim
     *            the gdattim to set
     */
    public void setGdattim(String gdattim) {
        this.gdattim = gdattim;
    }

    /**
     * 
     * @return
     */
    public boolean isSaveAll() {
        return saveAll;
    }

    /**
     * 
     * @param saveAll
     */
    public void setSaveAll(boolean saveAll) {
        this.saveAll = saveAll;
    }

    public boolean validInput() {
        boolean valid = false;

        valid = gdfile != null && !gdfile.isEmpty();
        valid = modelName != null && !modelName.isEmpty();
        valid = validateGparm();
        valid = validateGvcord();

        return valid;
    }

    private boolean validateGvcord() {
        level = null;

        String[] glevel = getGlevel().split(":");
        String masterLevel;
        Set<String> masterLevelMappings;

        try {
            masterLevelMappings = LevelMapper.getInstance()
                    .lookupBaseNamesOrEmpty(gvcord, "GEMPAK");
            if (masterLevelMappings.isEmpty()) {
                statusHandler.handle(Priority.PROBLEM,
                        "No mapping found for Gvcord: " + gvcord);
                return false;
            } else if (masterLevelMappings.size() == 1) {
                masterLevel = masterLevelMappings.iterator().next();
            } else {
                masterLevel = resolveAmbiguousLevel(masterLevelMappings, glevel);
                if (masterLevel == null) {
                    statusHandler.handle(
                            Priority.PROBLEM,
                            "Could not resolve ambiguous AWIPS2 Master Level mapping for "
                                    + gvcord
                                    + " with mappings: "
                                    + Arrays.toString(masterLevelMappings
                                            .toArray()));
                    return false;
                }
            }
            if (glevel.length == 1) {
                level = LevelFactory.getInstance().getLevel(masterLevel,
                        Double.parseDouble(glevel[0]));
            } else if (glevel.length == 2) {
                level = LevelFactory.getInstance().getLevel(masterLevel,
                        Double.parseDouble(glevel[0]),
                        Double.parseDouble(glevel[1]));
            } else {
                statusHandler.handle(Priority.PROBLEM, "");
                return false;
            }
        } catch (NumberFormatException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
            return false;
        }
        return true;
    }

    private String resolveAmbiguousLevel(Set<String> masterLevelMappings,
            String[] glevel) {
        String masterLevel = null;

        if (gvcord.equalsIgnoreCase("PRES")) {
            masterLevel = "MB";
        }

        return masterLevel;
    }

    private boolean validateGparm() {
        parameter = null;

        String gparm = getGparm().toUpperCase();
        try {
            parameter = ParameterMapper.getInstance().lookupParameter(gparm,
                    "GEMPAK");
        } catch (ParameterLookupException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
            return false;
        } catch (MultipleMappingException e) {
            statusHandler.handle(Priority.PROBLEM, e.getLocalizedMessage(), e);
            return false;
        }
        return true;
    }

    /**
     * @return the level
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return the parameter
     */
    public Parameter getParameter() {
        return parameter;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}