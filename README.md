Purpose
=======

Implement a multi purpose Database Connection Pool system using HikariCP for OSGI systems.  Use standard naming for OSGI services, and handle config changes gracefully.

Apache Karaf container is the only one to be tested heavily so far.  

Once the projects are compiled, you can install in Apache Karaf via the following install script:

> install -s mvn:org.javassist/javassist/3.18.1-GA
> install -s wrap:mvn:org.postgresql/postgresql/9.2-1004-jdbc41
> install mvn:com.zaxxer/HikariCPPostgreSQLFragment/1.0.0
> install mvn:com.zaxxer/HikariCPMSSqlFragment/1.0.0
> install -s mvn:com.zaxxer/HikariCP/2.2.5
> install -s mvn:com.progbits.db/OsgiDatabase/1.1.0

I do not include the install script for MSSql here.  You will need to install in your environment before the fragment will work.
