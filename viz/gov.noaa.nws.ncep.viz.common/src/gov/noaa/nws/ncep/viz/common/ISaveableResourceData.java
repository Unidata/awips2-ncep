/*
 * gov.noaa.nws.ncep.viz.common.ISaveableResourceData
 * 
 * 08 August 2016
 *
 * This code has been developed by the NCEP/SDB for use in the AWIPS2 system.
 */

package gov.noaa.nws.ncep.viz.common;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * An Interface to define the methods needed for saveable resource data.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ -----------------------------------------------------------------
 * 08/08/2016   R17954      B. Yin      Initial Creation.
 * 
 * </pre>
 * 
 * @author byin
 * 
 */
public interface ISaveableResourceData {

    public abstract boolean isResourceDataDirty();

    public abstract int promptToSaveOnCloseResourceData();

    public abstract void doSaveResourceData(IProgressMonitor monitor);
}
