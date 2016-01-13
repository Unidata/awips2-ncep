package gov.noaa.nws.ncep.viz.resources.manager;

import java.util.HashMap;

/**
 * This class facilitates the storing and retrieval of the aliases of resource
 * attributes. These aliases can be displayed in the dialog window of "Select
 * New Resource", replacing the names of resource attributes.
 * 
 * The current implementation used three levels of HashMaps to save the mappings
 * (1) from GRID's types to groups (one to many) and (2) from groups to
 * attribute sets (one to many) and (3) from attribute sets to aliases (one to
 * one).
 * 
 * The implementation can be adapted to add aliases for different Resource
 * Categories.
 * 
 * <pre>
 * SOFTWARE HISTORY
 * Date         Ticket#     Engineer    Description
 * ------------ ----------  ----------- --------------------------
 * 
 * 09/22/2015   R8824       A. Su       Initial creation.
 * 
 * </pre>
 * 
 * @author asu
 * @version 1
 */

public class AttrSetLabelsManager {

    /**
     * The top level of HashMap for the mapping from types to groups (one to
     * many).
     */
    private HashMap<String, HashMap<String, HashMap<String, String>>> gridTypeMap = new HashMap<String, HashMap<String, HashMap<String, String>>>();

    /**
     * A declaration of the second level of HashMap for the mapping from groups
     * to attribute sets (one to many).
     */
    private HashMap<String, HashMap<String, String>> groupMap;

    /**
     * A declaration of the third level of HashMap for the mapping from
     * attribute sets to aliases (one to one).
     */
    private HashMap<String, String> attrMap;

    /**
     * The singleton design pattern.
     */
    private static AttrSetLabelsManager instance = null;;

    /**
     * The singleton design pattern.
     */
    private AttrSetLabelsManager() {
    }

    /**
     * The singleton design pattern.
     * 
     * @return an instance of this class.
     */
    public static synchronized AttrSetLabelsManager getInstance() {
        if (instance == null) {
            instance = new AttrSetLabelsManager();
        }
        return instance;
    }

    /**
     * Set an alias based on type, group and attribute set.
     * 
     * @param type
     *            A type
     * @param group
     *            A group
     * @param attrSet
     *            An attribute set
     * @param alias
     *            A new alias
     */
    public void setAlias(String type, String group, String attrSet, String alias) {

        boolean isValidInput = (type != null && type.length() > 0)
                && (group != null && group.length() > 0)
                && (attrSet != null && attrSet.length() > 0)
                && (alias != null && alias.length() > 0);

        if (!isValidInput) {
            return;
        }

        groupMap = gridTypeMap.get(type);
        if (groupMap == null) {
            attrMap = new HashMap<String, String>();
            attrMap.put(attrSet, alias);

            groupMap = new HashMap<String, HashMap<String, String>>();
            groupMap.put(group, attrMap);

            gridTypeMap.put(type, groupMap);
        } else {
            HashMap<String, String> attrMap = groupMap.get(group);
            if (attrMap == null) {
                attrMap = new HashMap<String, String>();
                attrMap.put(attrSet, alias);

                groupMap.put(group, attrMap);

            } else {
                attrMap.put(attrSet, alias);
            }
        }

    }

    /**
     * Get an alias based on type, group and attribute set.
     * 
     * @param type
     *            A type
     * @param group
     *            A group
     * @param attrSet
     *            An attribute set
     * @return A saved alias or null if not found.
     */
    public String getAlias(String type, String group, String attrSet) {

        boolean isValidInput = (type != null && type.length() > 0)
                && (group != null && group.length() > 0)
                && (attrSet != null && attrSet.length() > 0);

        if (!isValidInput) {
            return null;
        }

        groupMap = gridTypeMap.get(type);
        if (groupMap == null) {
            return null;
        }

        attrMap = groupMap.get(group);
        if (attrMap == null) {
            return null;
        }

        String alias = attrMap.get(attrSet);
        return alias;
    }
}
