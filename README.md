# Message-passing-broker
Framework for a distributed, indirect message passing broker system that overcomes the challenges of load balancing, failure handling, security and network traffic in a publisher-subscriber distributed system. The system design follows the Observer design pattern.

BunnyME is a distributed Publisher-Subscriber service. 
1. Overview
2. Architecture
3. Contributions
4. Run & Compile

## Overview:
Its architecture is divided into 2 parts.
first is the Message Broker consisting of a single bootstrap and a bunch of servers and the second is the client or the user that can connect to the broker and act as a publisher and/or subscriber.
To achieve this, the system uses ‘Queues’ as a medium of communication amongst users. These Queues are distributed all over the system within the DHT network of servers along with backups. Publishers create new Queues as services and the system makes them available to subscribe to for other users in the system. Once subscribed, the user will instantaneously receive all the messages that are published by the publisher user of that queue. The subscriber may at any point of time decide to unsubscribe to any queue. The publisher may at any point of time decide to make any Queue or service unavailable to its subscribers. And any user may at any point of time decide to log out of the system.

## Architecture:
### The Messaging Broker:
The BunnyME Messaging Broker is again split into two logical parts, first is the bootstrap and second is the DHT made up of multiple servers.
The bootstrap acts as an interface between the users and the servers before the user is a part of the system. User Authentication takes place at the bootstrap and then the user is handed off to one of the servers in the DHT structure connected to the bootstrap. The server that the user will be assigned to is selected by the ‘choose-two’ process to make sure that the selection is not completely random and that there is a sense of load balancing in the system with respect to the number of users being handled by each server. Two servers are selected at random and compared for load, based on this, the server with lesser load is assigned the user.
The bootstrap also acts as an interface between the DHT and a server trying to become a part of the network. When the server receives the request from a server to join the network, the bootstrap looks for the closest neighbour for the new server and assigns the add server job to that server to optimise the server addition process. To do this, the bootstrap looks at the positional metric, which in our case is the hash value of IP, associated with every server and the new server, and based on this the server is added. The bootstrap also maintains a record of all the user to ever be registered into the system apart from maintaining a list of all currently connected users. After the server and the users are a part of the system, the bootstrap plays little to no role in their functionalities and communications. The server contacts the bootstrap again only if it detects that its neighbour has failed.

### The User:
The user, once connected to the system via the bootstrap communicates only with the Server it is connected to. All system communications such as creation and deletion of its services (publishing queues), subscription and unsubscription requests, etc. take place only via this Server. The user is unaware of any other server and assumes that the whole broker resides at the server itself. After a user is subscribed to a service (publishing queue), the server where the queue resides directly connects to the user when a new message is to be published and disconnects as soon as the message is published at the subscriber. The user may contact the bootstrap if the server it is connected to fails. In this case the bootstrap will assign the user a new load balance selected server and connect the user to it. User has a GUI for ease of use

## Contributions:
### Distributiveness-
We have made the system distributed by creating a network of servers connected to each other in a CHORD-like DHT where every server has pointers to one successor server, one predecessor server and one to the predecessor’s predecessor or as we regard it in our system, prepredecessor. The Message Queues are distributed all over this network of servers.
### Transparency-
We have designed the system in such a way that the user does not and need not know where the Queue that it creates to publish or connects by subscribing resides. The user is under the impression that all the Queues and users reside at the same server that it is connected to. The user also does not need to know where or who the subscribers are. the user simply communicates its message over to the server that it is connected to and the server takes care of the rest. If a user has subscribed to a queue that does not reside at the same server to which it is connected, the server where the queue is directly establishes a connection to the subscriber, publishes the message and disconnects without the user knowing who sent the message.
### Fault tolerance-
If in the DHT network, a server fails, its predecessor detects it and makes the necessary changes to the DHT network structure and takes over its queues by requesting for backups of those queues making sure the system stabilizes gracefully even in case a server fails. Also when a server fails, the users connected to it are connected with a new server by the bootstrap making sure there is no hindrance to their experience of services.
### Availability-
The system has a good availability as the users are always connected to a new server if the server that they’re connected to fails. Also, the queues are always available to the users as there are backups of the queues in the system to take over in case of failures making sure the user always gets the service that it expects.

## Run and compile:
The bootstrap always runs at glados.cs.rit.edu
The servers/users may be on any other machine
### Bootstrap:
To compile the bootstrap, add BunnyME_BootStrap.java, Queue.java and BootStrap2ServerInterface.java in the same directory and run the command: 'javac *.java'. 
To run the bootstrap, run the following command: ‘java BunnyME_BootStrap’

### Server:
To compile the server, add BunnyME_Server.java, Queue.java and BootStrap2ServerInterface.java in the same directory and run the command: 'javac *.java'.
To run the server, run the following command: ‘java BunnyMe_Server [bootstrap IP]’
### Client:
To compile the user/client add BunnyME_Client.java, LoginWindow.java, Pub_Sub.java, Queue.java and BootStrap2ServerInterface.java in the same directory and run the command: 'javac *.java'.
To run the user/client, run the command: ‘java BunnyME_Client’
