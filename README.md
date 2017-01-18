## Nichrome
Currently Nichrome supports two engines: ALPS and MLN. ALPS is an interactive synthesis engine for inductive logic programs (e.g. Datalog), and MLN is an engine for solving a system of mixed hard and soft constraints, which are widely used applications from MLNs (Markov Logic Networks) and PSLs (Probabilistic Soft Logic) areas.

## Dependencies

Nichrome depends on the following environment and library:

- Java 1.6 or later.
- PostgreSQL 9.3.4 or later.
- A weighted partial MaxSAT solver. Any MaxSAT solver using input and output format specified by the [MaxSAT evaluation](http://maxsat.ia.udl.cat/requirements/) suffices. We recommend [MiFuMaX](http://sat.inesc-id.pt/~mikolas/sw/mifumax/). 
- [LBX](http://logos.ucd.ie/web/doku.php?id=lbx).
- (_Optional_) [MCSLs](http://logos.ucd.ie/web/doku.php?id=mcsls).
- (_Optional_) [Gurobi](http://www.gurobi.com/).
- [Z3](https://github.com/Z3Prover/z3).

Note: if you are only using the ALPS engine, Z3 should be sufficient.

## Build
`cd main;  ant`

## Setup
To use Nichrome MLN engine, we need to first install and configure the PostgreSQL database, and provide a configuration file to help Nichrome find the software it depends on.

1. Install PostgreSQL.
Download and unpack the source of PostgreSQL. Let PG_DIST be the path where the source is unpaked and PG_INSTALL be the path where you want PostgreSQL to be installed, then build and install PostgreSQL as follows:

		cd PG_DIST
		./configure –prefix=PG_INSTALL
		make; make install 
		cd PG_DIST/contrib/intarray 
		make; make install				
		cd PG_DIST/contrib/intagg 
		make; make install
Then configure PostgreSQL as follows:

		cd PG_INSTALL/bin
		./initdb -D ../data
		./pg_ctl -D ../data/ -l ../log  start
		./createuser -s -P Nichrome
		# Provide a password you like, e.g., Nichrome
		./createdb Nichromedb

2. Define a configuration file.
Below is an example configuration file, which we will refer to as "Nichrome.conf".

		db_url = jdbc:postgresql://localhost:5432/Nichromedb

		# Database username; must be a superuser
		db_username = Nichrome

		# The password for db_username
		db_password = Nichrome

		# The working directory; Nichrome may write sizable temporary data here
		dir_working = <work-dir>

		maxsat = <path of the MaxSAT solver>
		lbx = <path of lbx>
		mcsls = <path of mcsls>


## Examples
TBD
