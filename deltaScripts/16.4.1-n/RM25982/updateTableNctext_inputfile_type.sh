#!/bin/bash
#
# updateTableNctext_inputfile.sh
###############################################################
#   
#   SOFTWARE HISTORY
#   
#   Date            Ticket#       Engineer       Description
#   ------------    ----------    -----------    ------------
#   12/12/2016      RM25982       J. Beck        Initial Creation
#  
#  Purpose:  Run a SQL script to change the filetype to RFTS, for fileext taf
#  Database: metadata 
#  Schema:   awips
#  Table:    nctext_inputfile_type
#  SQL:      update awips.nctext_inputfile_type set filetype='RFTS' where fileext='taf'
#
#  Note:     This should affect one row in the table
#
#  Expected output:  "SUCCESS!   UPDATE 1" 
##############################################################

# The psql command
PSQL="/awips2/psql/bin/psql"

# Database to use
DB="metadata"

# Run as user
USER="awips"

if [ ! -x $PSQL ] 
    then
        echo "${PSQL} Not Found, or is not executable: Please edit script"
        exit -1
fi 

PSQL_RUN_COMMAND="${PSQL} -X -U $USER --set ON_ERROR_STOP=on --set AUTOCOMMIT=on $DB"


RESULT=$($PSQL_RUN_COMMAND <<ENDSQL
update awips.nctext_inputfile_type set filetype='RFTS' where fileext='taf'
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


