## What is Nucleus?
Nucleus is a single-node / homegeneous scaling unit of Tau. 

In simple terms, Nucleus is all of Tau condensed into a single application. 

Nucleus's design is fundamentally different to allow single-node / homogeneous deployments.

Homogenous deployment: meaning you can simply add more instances with the same component to scale up.

## Why Nucleus?

Tau's distributed mode (dengine) leverages Storm and the complexity of development and debugging reached a tipping point where testing and validation of complex correlations wasn't automatable.

Nucleus has 2 primary objectives:

1. Allow users to deploy Tau without needing any underlying components (Storm, Kafka or Zookeeper)
2. Reduce technical debt for development and testing

### The good

- Nucleus's new single node design makes it fault tolerant
- Single node has been tested to be produce correct and reliable results.

### The bad

- Nucleus's current design requires substantial engineering to enable CP (consistency, partitioning) compliant, since WAL replication will need to be designed from scratch for RocksDB.
- WAL design is inherently IO bound therefore performance is disk constrained (on a 2013 dual-core Macbook Pro, received ~44K EPS)

## Deployments
Here's a sample deployment setup for Nucleus:
https://github.com/srotya/tau/tree/master/install/configs

## Features
- Self contained system
- Allows events to be streamed over HTTP (with performance penalty)

## Design

The system was originally designed to have implementation framework decoupled from the core (Wraith). This was one to ensure framework portability as well as to develop a homogenenous deployment framework in the future. 
Dengine (built on Storm) has several moving parts making deployments and management extremely complex, since all of the dependencies must be running before the system can be deployed.

Nucleus transforms the original 9 deployment parts to just 4:

1. Nucleus Server
2. API Server
3. UI Server
4. MySQL

where the first 2 are Dropwizard applications.

Nucleus replaces Kafka, Storm, Zookeeper and 2 Storm topologies with a single Dropwizard application in Tau.

### Internals

There are 2 critical concepts in Nucleus:

#### AbstractProcessor
An abstract processor is equivalent to a Storm bolt for Tau, except each AbstractProcessor has a WAL along with the disruptor ring buffer. 
Any component can send events to an AbstractProcessor by calling one of 2 methods: `processEventWaled` or `processEventNonWaled`.

#### WAL
Write Ahead Log, is used to ensure Fault-Tolerant processing by first publishing an event to the WAL before it can be published to the ring buffer for processing. 
Therefore in the event of a process failure, the WAL is used to replay unprocessed events. 

Events are removed from the WAL by the encapsulating AbstractProcessor when they are deemed fully processed, which usually means the results have been forwarded to the next processor in the chain or the event has been fully consumed. 
Currently RocksDB is used as a WAL for Nucleus.

All Tau components like RulesEngine, AlertEngine etc. have been wrapped in their own AbstractProcessors and then wired together, which allows for tight coupling between modules.
