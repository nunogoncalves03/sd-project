# TupleSpaces

Distributed Systems Project 2024

**Group A09**

**Difficulty level**: I am Death incarnate!

### Code Identification

In all source files (namely in the *groupId*s of the POMs), replace **GXX** with your group identifier. The group
identifier consists of either A or T followed by the group number - always two digits. This change is important for
code dependency management, to ensure your code runs using the correct components and not someone else's.

### Team Members

| Number | Name             | User                                 | Email                                        |
| ------ | ---------------- | ------------------------------------ | -------------------------------------------- |
| 102802 | Fábio Mata       | <https://github.com/fmata97>         | <mailto:fabio.mata@tecnico.ulisboa.pt>       |
| 103392 | Nuno Gonçalves   | <https://github.com/nunogoncalves03> | <mailto:nunomrgoncalves@tecnico.ulisboa.pt>  |
| 103363 | Alexandre Vudvud | <https://github.com/vudvud>          | <mailto:alexandre.vudvud@tecnico.ulisboa.pt> |

## Getting Started

The overall system is made up of several modules. The different types of servers are located in _ServerX_ (where X denotes stage 1, 2 or 3).
The clients is in _Client_.
The definition of messages and services is in _Contract_. The future naming server
is in _NamingServer_.

See the [Project Statement](https://github.com/tecnico-distsys/TupleSpaces) for a complete domain and system description.

### Prerequisites

The Project is configured with Java 17 (which is only compatible with Maven >= 3.8), but if you want to use Java 11 you
can too -- just downgrade the version in the POMs.

To confirm that you have them installed and which versions they are, run in the terminal:

```s
javac -version
mvn -version
```

### Installation and configuration

To compile and install all modules:

```
mvn clean install
```

#### NameServer

To setup the python virtual environment:

```
python -m venv .venv
source .venv/bin/activate
python -m pip install grpcio
python -m pip install grpcio-tools
```

To create NameServer gRPC related classes:
```
cd Contract/
mvn install
mvn exec:exec
```

To run the NameServer:
```
python server.py [-debug]
```

#### Server

**NOTE**: the default arguments provided to the program are defined in `pom.xml` as follows:

```
<dns_host>: localhost
<dns_port>: 5001
<sv_host>: localhost
<sv_port>: 2001
<qualifier>: A
<service>: TupleSpaces
```

To run the Server without debugging:

```
mvn compile exec:java
```
or
```
mvn compile exec:java -Dexec.args="<dns_host> <dns_port> <sv_host> <sv_port> <qualifier> <service>"
```
To run with debugging:
```
mvn compile exec:java -Dexec.args="<dns_host> <dns_port> <sv_host> <sv_port> <qualifier> <service> -debug"
```

#### Client

**NOTE**: the default arguments provided to the program are defined in `pom.xml` as follows:

```
<dns_host>: localhost
<dns_port>: 5001
<service>: TupleSpaces
```

To run the client without debugging:

```
mvn compile exec:java
```
or
```
mvn compile exec:java -Dexec.args="<dns_host> <dns_port> <service>"
```

To run the client with debugging:
```
mvn compile exec:java -Dexec.args="<dns_host> <dns_port> <service> -debug"
```


## Built With

-   [Maven](https://maven.apache.org/) - Build and dependency management tool;
-   [gRPC](https://grpc.io/) - RPC framework.
