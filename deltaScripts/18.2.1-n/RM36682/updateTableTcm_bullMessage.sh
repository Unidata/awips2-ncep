#!/bin/bash
#
# updateTableNctext_inputfile.sh
###############################################################
#   
#   SOFTWARE HISTORY
#   
#   Date            Ticket#       Engineer       Description
#   ------------    ----------    -----------    ------------
#   01/25/2019      7717          K. Sunil       Update bullmessage column to remove 8000 char limitation

#  Purpose:  Run SQL script to change bullmessage column data type in table tcm
#  Database: metadata 
#  Schema:   awips
#  Table:    tcm
#  SQL:      alter table tcm alter column bullmessage set data type varchar;
#
#  Note:     This should affect one column in the table
#

#  Expected output:  "SUCCESS!   UPDATE 1" 

##############################################################

# The psql command

PSQL="/awips2/psql/bin/psql"

# Database to use

DB="metadata"

# Run as user

USER="awipsadmin"

if [ ! -x $PSQL ] 

    then

        echo "${PSQL} Not Found, or is not executable: Please edit script"

        exit -1

fi 

PSQL_RUN_COMMAND="${PSQL} -h dx1f -X -U $USER --set ON_ERROR_STOP=on --set AUTOCOMMIT=on $DB "

RESULT=$($PSQL_RUN_COMMAND <<ENDSQL

alter table tcm alter column bullmessage set data type varchar

ENDSQL

)

if [ $? -eq 0 ] 
    then
        echo "SUCCESS!  " $RESULT
        exit 0
    else
        echo "FAILURE:  " $RESULT
        exit -1
fi
