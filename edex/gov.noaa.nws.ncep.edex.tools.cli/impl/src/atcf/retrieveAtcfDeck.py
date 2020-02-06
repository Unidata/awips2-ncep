#!/awips2/python/bin/python3
#
##
# This script is used to extract ATCF "deck" file products from EDEX.
# 
# Users can override the default EDEX server and port name by specifying them
# in the $DEFAULT_HOST and $DEFAULT_PORT shell environment variables.
# 
#    10/24/2016      R22939         bhebbard       Initial Creation.
#
#
##
#

import os
import logging

from ufpy import UsageArgumentParser
import lib.CommHandler as CH
import AtcfDeckRetriever

logger = None
def __initLogger():
    global logger
    logger = logging.getLogger("retrieveAtcfDeck")
    logger.setLevel(logging.DEBUG)
    ch = logging.StreamHandler()
    ch.setLevel(logging.INFO)
    # Uncomment line below to enable debug-level logging
    #ch.setLevel(logging.DEBUG)
    formatter = logging.Formatter("%(asctime)s %(name)s %(levelname)s:  %(message)s", "%H:%M:%S")
    ch.setFormatter(formatter)
    logger.addHandler(ch)
    
#
#  Parses command line input and store in "options".
#
def __parseCommandLine():
    print("Starting __parseCommandLine")
    parser = UsageArgumentParser.UsageArgumentParser(prog='retrieveAtcfDeck',description="Retrieve ATCF 'deck' files from EDEX.")
    bgroup = parser.add_argument_group(title='batch',description='For running in scripts and/or batch mode.')

    bgroup.add_argument("-n", action="store", dest="name", 
                      help="AtcfDeck Name being requested",
                      required=False, metavar="     name")
    logger.info("Calling parser")
    options = parser.parse_args()
    #logger.info("Done with parser; options are:  ", str(options))
       
    logger.debug("Command-line arguments: " + str(options))
    return options

#
#  Main program.
#
def main():
    __initLogger()
    logger.info("Starting retrieveAtcfDeck.")
    options = __parseCommandLine()
    adr = AtcfDeckRetriever.AtcfDeckRetriever(options.name)
    adr.getAndExportDeckLines(options.name)

if __name__ == '__main__':
    main()

