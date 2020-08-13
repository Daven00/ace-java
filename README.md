# README #

This is a mavenized Java 1.8 project. You should be able to compile and run these classes if you manage to figure out how Maven works (which is non-trivial due to the lack of good introductory manuals for Maven).

### What is this repository for? ###

* This is a Java library for the functions necessary to run a client, resource
  server, and authorization server as specified in [draft-ietf-ace-oauth-authz](https://datatracker.ietf.org/doc/draft-ietf-ace-oauth-authz). 
*  The library also implements the [DTLS](https://datatracker.ietf.org/doc/draft-ietf-ace-dtls-authorize) and [OSCORE](https://datatracker.ietf.org/doc/draft-ietf-ace-oscore-profile/) profiles. 
*  The base libraries do not include network functionality, since they are supposed to be protocol agnostic. However we provide [CoAP](https://tools.ietf.org/html/rfc7252) client and server support as an example of a protocol specific adaptation based on [Californium](https://www.eclipse.org/californium).
* Since this is a Java library, it is not intended for resource constrained devices, rather it is intended to be used on the "other end", by the resource rich nodes
  talking to the resource constrained ones. We are planning to release code for constrained devices as well in the future.
* Version: beta


### How do I get set up? ###

* Just clone the repo, do the Maven fu and you are good to go
* Configuration: You need to set up a MySQL database to run the Junit tests. 
  To run in production you need to configure everything, starting with
  your resource servers (out of scope here), the access control policies for
  the authorization server (KissPDP has a demo format backed in the database, check the
  test resources), the discovery of AS (out of scope again). If you don't
  know where to start you probably shouldn't use this in production settings.
* Dependencies: Lots, check the .pom file
* Database configuration:  Set up a MySQL or Postgresql database, for running 
  the Junit tests create a file 'db.pwd' with the root password of your test 
  database at the root directory of this library (just the password nothing else). 
  If you want an alternative database you have to change the dependencies to 
  include another JDBC and double check if SQLConnector uses a compatible syntax.
* How to run tests: Run the Test* class files in the src/test folders. The CoAP 
  tests will auto start a CoAP server (as normal program not as Junit test)
  first (you don't need to do anything). For all tests to work you also have to replace
  [JCE Unlimited Strength Policy](http://www.oracle.com/technetwork/java/javase/downloads/jce8-download-2133166.html) files.
* Deployment instructions: TBD. You should be able to set up the code for testing
  by just using the maven pom.xml and configuring the database (as explained 
  above).

### Contribution guidelines ###

* Writing tests: Yes please! I'd be happy to support you if you have ideas about 
  which tests would be needed.
* Code review: Yes please! Please use the Issue tracker and/or Pull requests.
* Other guidelines: Follow the [Code Conventions for Java](http://www.oracle.com/technetwork/java/codeconvtoc-136057.html), don't add 
  new dependencies unless there is a really good reason.

### Who do I talk to? ###

* This code is owned by RISE and released as Open Source under the [BSD 3 license](https://opensource.org/licenses/BSD-3-Clause).
* If you have questions or suggestions, please contact
   * ludwig dot seitz at combitech dot se
   * marco dot tiloca at ri dot se

### Acknowledgments ###
This code is maintained in the framework of the [CelticNext](https://www.celticnext.eu/) project CRITISEC, with funding from [Vinnova](http://www.vinnova.se/sv/).
The PostgresSQL adapter code and many useful debug comments were supplied by Sebastian Echeverria from the [SEI lab](https://www.sei.cmu.edu) at CMU.
