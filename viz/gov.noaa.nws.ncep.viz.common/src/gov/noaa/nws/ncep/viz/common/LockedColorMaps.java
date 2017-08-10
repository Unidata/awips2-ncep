package gov.noaa.nws.ncep.viz.common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * This class reads lockedColorMaps.tbl to get the list of color map file names
 * that needs to be locked.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#    Engineer    Description
 * ------------ ---------- ----------- --------------------------
 * 03/2012      #621       S. Gurung   Initial creation.
 * 05/20/2016   R18398     S. Russell  Updated the constructor and the method
 *                                     readTable to not force methods in
 *                                     ColorMapUtil to use the deprecated
 *                                     LocalizationFile.getFile()
 * 
 * </pre>
 * 
 * @author sgurung
 * @version 1.0
 * 
 */
public class LockedColorMaps {

    private List<String> cmapList;

    public LockedColorMaps(InputStream instream) throws FileNotFoundException,
            IOException {
        readTable(instream);
    }

    /**
     * Read an InputStream into an ArrayList of Strings used to hold a color map
     * 
     * @param instream
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void readTable(InputStream instream) throws FileNotFoundException,
            IOException {

        Reader reader = new InputStreamReader(instream);
        BufferedReader br = new BufferedReader(reader);
        String lineStr = null;
        cmapList = new ArrayList<>();

        while ((lineStr = br.readLine()) != null) {

            if (lineStr.startsWith("!")) {
                continue;
            }
            cmapList.add(lineStr.trim());
        }

        br.close();
        instream.close();
    }

    public boolean isLocked(String cmap) {
        if (cmapList.contains(cmap) || cmapList.contains(cmap + ".cmap"))
            return true;
        else
            return false;
    }

    public String getColorMap(int index) {
        return cmapList.get(index);
    }

    public List<String> getColorMapList() {
        return cmapList;
    }

}
