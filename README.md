# Gossip-algorithm
Gossip Algorithm for information propagation, Push-Sum algorithm for sum computation
Gossip type algorithms can be used both for group communication and for aggregate computation. The goal of this project is to determine
the convergence of such algorithms through a simulator based on actors written in Scala. Since actors in Scala are fully asynchronous, the particular type of
Gossip implemented is the so called Asynchronous Gossip.

Refer this paper: http://www.cs.cornell.edu/johannes/papers/2003/focs2003-gossip.pdf  
Based on the input parameters to our program, main will create the particular topology.
The topology consists of the list of all the Nodes/Minions-Actors it has (we referred to each node as a minion, likewise in code minion list is the list of nodes) and its corresponding neighbors it is linked with.
Minion-actor contains the implementation of both algorithms.
There is a MinionHandler-Actor who handles Minions, for monitoring running time and checking the convergence case to shut down.
Based on the input Algorithm, MinionHandler will trigger the gossip or pushsum communication on the input topology by randomly picking one of the Minion-actor.


Full Network : Every actor is a neighboor of all other actors. Every actor can talk directly to any other actor.<br>
3D Grid: Actors form a 3D grid. The actors can only talk to the grid neighbours.<br>
Line: Actors are arranged in a line. <br>
Imperfect 3D Grid: Grid arrangement but one random other neighbour is selected from the list of all actors (4+1 neighboors).

Execution: 
project2.scala numNodes topology algorithm

Topologies: full, 3D, line, imp3D
algorithm:  gossip, push-sum
