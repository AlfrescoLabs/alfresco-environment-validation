Description
-----------
This module provides a rudimentary "environment validation" tool that helps
determine whether a server is suitable for using to host the Alfresco open
source CMS.

The tool is provided as an archive (zip) file that (when uncompressed)
contains executable scripts that will run the tool. The tool itself is
implemented as a Java (JVM 1.4+) command line application.

Please note that this tool is not exhaustive - it simply validates some of the
more common environmental problems Alfresco has seen. Administrators intending
to install Alfresco should ensure they have fully validated that their
environment is on the Alfresco Supported Stack. They should also ensure they
have reviewed, understood and applied the various installation related
information available from the Alfresco Support Portal, the Alfresco Wiki and
official product documentation.

After completing environment validation, but prior to installation,
administrators should also review the Alfresco Day Zero Configuration Guide,
to ensure they are able to complete basic configuration of Alfresco
immediately following installation.

Author
------
Andy Hunt (andy dot hunt at alfresco.com)
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

The tool must be run as the OS user that Alfresco will run as.  In particular
it will report misleading results if run as "root" (or equivalent on other
OSes) if Alfresco is not intended to be run as that user.


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


Release History
---------------
v1.0 2012-02-16 - Alfresco 4.0.0
     Fixed issue #36: Remove Postgresql Implicit cast test 
     Fixed issue #29: liblibsigar-x86_64-linux.so does not exist
     -- FAIL for DB2 and MSSqlServer
     
v1.0-SNAPSHOT 2012-02-14 - Alfresco 4.0.0
     Fixed issue #33: Update EVT to work with Alfresco 4.0.0
	 Fixed issue #34: Add RHEL 6.1
     Fixed issue #35: Update network.alfresco.com link
     -- Correctly parse Mysql versions of the format  5.1.41-3ubuntu12.7
     -- Identify different patch levels for Mysql or Mysql-ubuntu
     -- Determine OOO version correctly on linux
     -- Make windows OOO call headless
     
v0.11 released 2011-03-02 - Alfresco 3.4
     Fixed issue #9: PostgreSQL: add test to ensure implicit int -> boolean typecasts are enabled
     Fixed issue #10: Update README to clarify that the tool must be run as the OS user that will run Alfresco
     Fixed issue #11: MySQL: Test "wait_timeout" system variable, and issue warning if it's below a specific threshold
     Fixed issue #12: Update DB2 version checks to ensure 9.7 fixpack 3+ is installed
     Fixed issue #13: Prepare version for Alfresco 3.4.x
     Fixed issue #15: Add check for pdf2swf version
     Fixed issue #19: Replace handcoded 3rd party application version parsing code with regexes
     Fixed issue #20: JVM patchlevel not being checked

v0.10 released 2011-01-14 - Alfresco 3.3
     Fixed issue #1: Minimum supported JDK version is 1.6.0_21
     Fixed issue #5: Check innodb_autoinc_lock_mode=2 on MySQL
     Fixed issue #6: Only Windows 2003 32 bit is supported
     Fixed issue #7: Only Windows 2008 64 bit is supported
     Fixed issue #8: RHEL detection code is incorrectly reporting distro as SUSE

v0.9 Released 2010-12-20 - Alfresco 3.3
     First public release
