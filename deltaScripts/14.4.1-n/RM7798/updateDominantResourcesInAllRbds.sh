#!/bin/bash
# This script will repair (if needed) SPF/RBD XML files saved by users during a
# period when a bug in the system caused certain RBDs to be written incorrectly. 

# Specifically, the bug would cause the "dominantResourceName" attribute
# of timeMatcher elements to be written with a fixed cycle time for GRID
# and NTRANS resources, even if "Save Source Timestamp As:" option was set
# to LATEST during Save RBD.  This script will update these fixed cycle times
# (such as "(150511_06)") to "(LATEST)" in the timeMatcher dominantResourceName
# IF the resource name with the fixed cycle time does not match any displayed
# resource in the RBD, but does match such a resource with "(LATEST)".
#
# (See NCEP Redmine Issues #4983 and #7798 for more information.)

IFS=$'\n'
files=`ls /awips2/edex/data/utility/cave_static/*/*/ncep/SPFs/*/*/*.xml`

if [ $? -ne 0 ]; then
echo "No RBDs found"
exit 1
fi

MY_DIR=`dirname $0`

for f in $files; do
    echo Checking file $f
    python $MY_DIR/updateDominantResourcesInOneRbd.py $f
done

echo "INFO: the update has completed successfully!"
exit 0
