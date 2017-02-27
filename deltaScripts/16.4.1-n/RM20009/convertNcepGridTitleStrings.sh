#!/bin/bash
# convertNcepGridTitleStrings.sh
# 
# This bash script converts the TITLE strings in GRID attribute files (*.attr)
# from the legacy convention that the time information was put at the beginning of the string
# to the AWIPS2 convention that the time information is right justified at the end of string.
#
# For example, the TITLE string
#	TITLE = 1/-1/?~^ (~995 MB) MOISTURE CONVERGENCE, WIND (KTS), THTAE!0
# will be converted to
#	TITLE = 1/-1/(\~995 MB) MOISTURE CONVERGENCE, WIND (KTS), THTAE ?^!0
# where the '\' character (ESCAPE) is used to "escape" a special character from 
# its interpretation and treated literally.
#
# For more information, see the TITLE help file (TITLE.help) and NCEP Redmine ticket R20009.

attrFiles="/awips2/edex/data/utility/cave_static/*/*/ncep/AttributeSetGroups/ModelFcstGridContours/*.attr"
convertingScript=./convertNcepGridTitleStrings.py

for file in $attrFiles
do
	echo "converting $file ..."
	$convertingScript < $file > /tmp/myTemp.xml; mv -f /tmp/myTemp.xml $file
done

echo "INFO: the conversion has completed!"
exit 0

