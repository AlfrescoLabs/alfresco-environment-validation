Description
-----------
This module provides a rudimentary "environment validation" tool for servers
on which Alfresco is intended to be installed.  This is packaged as an
executable JAR file that runs a series of tests to determine whether the
environment is suitable for installation of an Alfresco Enterprise 3.3.x
server.

Please note that this tool is not exhaustive - it simply validates some of the
more common environmental problems Alfresco has seen.


Author
------
Peter Monks (reverse moc.ocserfla@sknomp)


Pre-requisites
--------------
JVM 1.4+ (note: some JDBC drivers require JDK 1.5+)


Running the Validator
---------------------

usage: evt[.sh|.cmd] [-?|--help] [-v] [-V|-vv]
            -t databaseType -h databaseHost [-r databasePort]
            [-d databaseName] -l databaseLogin [-p databasePassword]

where:      -?|--help        - display this help
            -v               - produce verbose output
            -V|-vv           - produce super-verbose output (stack traces)
            databaseType     - the type of database.  May be one of:
                               mysql, postgresql, oracle, mssqlserver, db2
            databaseHost     - the hostname of the database server
            databasePort     - the port the database is listening on (optional -
                               defaults to default for the database type)
            databaseName     - the name of the Alfresco database (optional -
                               defaults to 'alfresco')
            databaseLogin    - the login Alfresco will use to connect to the
                               database
            databasePassword - the password for that user (optional)


Licensing
---------
The Alfresco Environment Validation Tool is licensed under the GNU Public
License, Version 2 (GPL v2) with the following exceptions that are licensed
separately by their respective parties:

* Hyperic SIGAR API, released under the GNU Public License, Version 2 (GPL v2)
  - see gpl-2.0.txt
  Source code for Hyperic is available from https://github.com/hyperic/sigar
  
* MySQL Connector/J JDBC driver, released under the GNU Public License,
  Version 2 (GPL v2) - see gpl-2.0.txt
  Source code for the MySQL Connector/J JDBC driver is available from
  http://dev.mysql.com/downloads/connector/j/
  
* PostgreSQL JDBC driver, released under the BSD License - see
  http://jdbc.postgresql.org/license.html
  
* jTDS JDBC driver, released under the GNU Lesser Public License Version 3
  (LGPL v3) - see lgpl-3.0.txt
  Source code for the jTDS JDBC driver is available from
  http://sourceforge.net/projects/jtds/files/jtds/1.2.4/
  
* Oracle JDBC driver, released under the Oracle Technology Network License
  Agreement - see http://www.oracle.com/technetwork/licenses/distribution-license-152002.html
  
* DB2 JDBC driver, released under the IBM International Program License
  Agreement - see http://www14.software.ibm.com/cgi-bin/weblap/lap.pl?la_formnum=&li_formnum=L-SRAN-7PW37R&title=IBM+Data+Server+Driver+for+JDBC+and+SQLJ+(JCC+Driver)&l=en

Source code for the Alfresco Environment Validation Tool itself is available
from http://code.google.com/p/alfresco-environment-validation/source/checkout
