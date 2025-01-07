/**
 * 
 */
package gov.noaa.nws.ncep.standalone.miscsetConverter;

/**
 * @author bhebbard
 *
 */

public enum FlagField implements FlagCommand {

    NAME() {
        @Override
        public String retrieve(FlagRecord fr) {
            String flagName = fr.getFlagName();
            return flagName;
        }
    },
    ENABLE() {
        @Override
        public String retrieve(FlagRecord fr) {
            String rawValue = fr.getEnable();
            Boolean b = rawValue.equals("1");
            return b.toString();
        }
    };

    @Override
    public abstract String retrieve(FlagRecord tr);

}
