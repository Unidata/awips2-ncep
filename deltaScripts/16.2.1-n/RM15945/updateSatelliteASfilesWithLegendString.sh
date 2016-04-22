#!/bin/bash
#
# add basic legendString in satellite AS files
###############################################################
# Example for GINI GOES15 and channel IR                      #
#---------------------------------------                      #
#  As it appears in file IR.attr: legendString={RD} {channel} #
#  As it is drawn in CAVE legend: GINI_GOES15 IR              #
###############################################################
#   
#
#    
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    04/06/16        #15945        RReynolds      Initial Creation.
#    
# 
#
ATTR_DIR="/awips2/edex/data/utility/cave_static/*/*/ncep/ResourceDefns/SATELLITE/*/*.attr"

if [ ! -f $ATTR_DIR ]
then
    echo "No Attribute Files Found"
else
    for fname in $(find $ATTR_DIR | grep .attr) ; do sed -i '$ a legendString={RD} {channel}' $fname ; done
fi
