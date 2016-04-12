#!/bin/bash
#
# reinitialize mcidas table and clean out hdf5 data on dx2 only
#   
#
#    
#     SOFTWARE HISTORY
#    
#    Date            Ticket#       Engineer       Description
#    ------------    ----------    -----------    --------------------------
#    02/11/2016      #15228        RReynolds      Initial Creation.
#    03/08/2016      #15899        RReynolds      Added $DBHOST
#    
# 
#

#HNAME="nco" 
HNAME="dx2" 
DBHOST="dx1f"
PSQL="/awips2/psql/bin/psql" 
MCIDAS_HDF5=/awips2/edex/data/hdf5/mcidas/

if [ $(hostname|sed 's/-/ /g'|awk '{print $1}') == $HNAME ];then

        echo "found hostname" $HNAME

        initValue=$(${PSQL} -h $DBHOST -t -U awips -d metadata -q -c "UPDATE plugin_info SET initialized=false WHERE name='mcidas' RETURNING initialized;" )

        # trim white spaces
        initValue=$( echo "${initValue}"| sed -e 's/^ *//' -e 's/ *$//' );

        #echo "Mcidas table initialization state is " $initValue

        #case insensitive comparison
        if [ "${initValue,,}" = "f" ];then
                echo "mcidas table initialization state has been reset to: "$initValue 
                # clean out directories and files
                rm -rf $MCIDAS_HDF5
        else
                echo "failed to reset mcidas table initialization state because initvalue is:" $initValue 
        fi
else

        echo $HNAME "not found, couldn't reset table... This can only be run on "$HNAME 

fi
