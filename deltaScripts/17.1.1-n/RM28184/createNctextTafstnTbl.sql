-- createNctextTafstnTbl.sql
--###############################################################
--#   
--#   SOFTWARE HISTORY
--#   
--#   Date            Ticket#       Engineer       Description
--#   ------------    ----------    -----------    ------------
--#   02/23/2017      RM28184       Chin Chen      Initial Creation
--#  
--#  Purpose:  SQL script to add nctext_tafstn table to database
--#  Database: metadata 
--#  Schema:   awips
--#  Note: use awipsadmin as user to run this script
--##############################################################
SET SCHEMA 'awips';
CREATE TABLE nctext_tafstn
(
  id integer NOT NULL,
  stnid character varying(255),
  parentid integer NOT NULL,
  CONSTRAINT nctext_tafstn_pkey PRIMARY KEY (id),
  CONSTRAINT fk_tafstn_to_nctext FOREIGN KEY (parentid)
      REFERENCES nctext (id) MATCH SIMPLE
      ON UPDATE NO ACTION ON DELETE NO ACTION
)
WITH (
  OIDS=FALSE
);
