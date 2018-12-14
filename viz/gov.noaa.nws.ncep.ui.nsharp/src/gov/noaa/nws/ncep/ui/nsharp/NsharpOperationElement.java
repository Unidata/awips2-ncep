package gov.noaa.nws.ncep.ui.nsharp;

import gov.noaa.nws.ncep.ui.nsharp.NsharpConstants.ActState;

/**
 * 
 * This code has been developed by the NCEP-SIB for use in the AWIPS2 system.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * 
 * Date          Ticket#  Engineer   Description
 * ------------- -------- ---------- ---------------------------------------
 * Jun 26, 2013           Chin Chen  Initial coding
 * Dec 14, 2018  6872     bsteffen   Abstract to allow more accurate times.
 * 
 * </pre>
 * 
 * @author Chin Chen
 */
public abstract class NsharpOperationElement
        implements Comparable<NsharpOperationElement> {

    private ActState actionState;

    /**
     * Create an element in the ACTIVE stte.
     */
    public NsharpOperationElement() {
        this(ActState.ACTIVE);
    }

    public NsharpOperationElement(ActState actionState) {
        this.actionState = actionState;
    }

    public abstract String getDescription();

    public ActState getActionState() {
        return actionState;
    }

    public void setActionState(ActState actionState) {
        this.actionState = actionState;
    }

    @Override
    public String toString() {
        return getDescription() + ": " + actionState;
    }

    @Override
    public int compareTo(NsharpOperationElement o) {
        return this.getDescription().compareTo(o.getDescription());
    }

}
