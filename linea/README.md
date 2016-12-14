# Linea

Linea is Latin for limit, is a stream processing framework leveraged in Tau. It's based on concepts from Apache Storm however it's written from scratch without using any Apache Storm code/dependencies.

Linea also uses an Apache Storm like API to enable easy adoption and enable developer familiarity if others who would like to leverage this framework.

## Why Linea?

Linea is intended to be light-weight, think hybrid of Storm and Akka/Vert.x

Why bother?
- Storm offers the amazing XOR Ledger design pattern
- Disruptor offers state-of-art inter thread messaging and parallelism framework 
- Headless clustering design pattern is needed for truly container native deployment / dynamic scaling
- Tau needs best of all worlds to be scalable, reliable and highly performant

Distributed systems are difficult to build and even more difficult to test, the lighter a system the quicker it is for you to launch and the quicker to terminate it. The CTO of a company worked for talked about the importance of OODA Loops, and indeed for a development cycle they are important.

# Components

`Event`: Linea's version of a Storm Tuple

`Bolt`: Custom code to execute

`BoltExecutor`: Operates a collection of Bolt instances in parallel

`Router`: Responsible for Routing events to the correct bolt instance across workers

`Acker`: A special Bolt that uses GROUPBY Routing Type

`ROUTING_TYPE`: Linea's version of Storm Grouping

`Columbus`: A gossip based worker discovery service
