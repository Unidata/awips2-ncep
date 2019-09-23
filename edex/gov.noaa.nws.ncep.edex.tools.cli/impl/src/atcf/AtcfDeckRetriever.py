import os
from ufpy import ThriftClient
from dynamicserialize.dstypes.com.raytheon.uf.common.datastorage.records import StringDataRecord
from dynamicserialize.dstypes.com.raytheon.uf.common.datastorage.records import ByteDataRecord
from dynamicserialize.dstypes.gov.noaa.nws.ncep.common.dataplugin.atcf.request import RetrieveAtcfDeckRequest


class AtcfDeckRetriever:
    """ Retrieves all ATCF records for a given AtcfDeck (ID) and writes them to a file."""

    def __init__(self,deckID):
         if not deckID.endswith(".dat"):
             deckID += ".dat"
         self.deckID = deckID
         self.outdir = os.getcwd()
         self.host = os.getenv("DEFAULT_HOST", "localhost")
         self.port = os.getenv("DEFAULT_PORT", "9581")
         self.client = ThriftClient.ThriftClient(self.host, self.port)

    def setOutputDir(self, outdir):
         self.outdir = outdir
    
    def setFullpath(self, fullpath):
         self.fullpath = fullpath

    def _writeout(self,filename,deckLines):
        outname = self.outdir + str(os.sep) + filename
        f = open(outname,"wb")
        for deckLine in deckLines:
            f.write(deckLine + "\n")
        f.close()

    def getAndExportDeckLines(self, filename):
         """ Sends ThriftClient request and writes out received lines to file."""
         # if filename doesn't have ".dat" suffix, add it
         if not filename.endswith(".dat"):
             filename += ".dat"
         req = RetrieveAtcfDeckRequest()
         req.setDeckID(self.deckID)
         response = self.client.sendRequest(req)
         if not response:
             print "No data found matching criteria to export to " + filename
         else:
             self._writeout(filename,response)
             print "Extracted deck file... " + filename
             
