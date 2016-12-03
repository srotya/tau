# Linea

Linea is Latin for limit, is a stream processing framework leveraged in Tau. It's based on concepts from Apache Storm however it's written from scratch without using any Apache Storm code/dependencies.

Linea also uses an Apache Storm like API to enable easy adoption and enable developer familiarity if others who would like to leverage this framework.

# Components

`Event`: Linea's version of a Storm Tuple

`Bolt`: Custom code to execute

`BoltExecutor`: Operates a collection of Bolt instances in parallel

`Router`: Responsible for Routing events to the correct bolt instance across workers

`Acker`: A special Bolt that uses GROUPBY Routing Type

`ROUTING_TYPE`: Linea's version of Storm Grouping

`Columbus`: A gossip based worker discovery service
