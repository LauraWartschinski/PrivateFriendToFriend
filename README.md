# PrivateFriendToFriend
Prototype for a privacy-focused peer-to-peer filesharing network that provides deniability and allows for filesharing with unknown peers while only communicating with friends. For a lack of a cool name abbreviated with PFTF.
Doesn't offer any kind of full functionality for actual filesharing, but serves as a **proof of concept**.


>*This system was created by Laura Wartschinski, Enno Jertschat and Leonie Reichert in Winter 2017/18. A rough separation of the work would be as follows:*
>* *Leonie Reichert: main program structure, connection overlay, GUI (not uploaded here)*   
>* *Enno Jertschat: friend-to-friend connectivity, connection overlay*
>* *Laura Wartschinski: idea/design of concept, main friend-to-friend functionality*
>
>*Development happened on a different (private) git, so this one here is just a re-upload with some small tweaks.*


### The idea ###

PFTF is a peer to peer network with **two layers of overlay**.
I the first overlay, the *connection overlay*, every peer is connected with many other peers, just as one would usually expect.
It is used for providing basic connectivity in order to discover your **friends** on the network, which whom the *friend overlay* is created. Only friends tell each other
what files they have, and only friends share files directly. But friends forward file announcements to other friends, and do the 
same with file requests and answers to those requests. Therefore, it is never clear whether a single user provided a file himself
or just informed other people that their friend provided it, and in the same way, a request could always come from the participant who asked
or from somebody else, being merely forwarded. Therefore, the system provides strong **deniability**, while not being very efficient. Also, every
user only directly interacts with friends.

![overlay structure](https://github.com/LauraWartschinski/PrivateFriendToFriend/blob/master/doku/overlays.png)

*How the network would like like if Bob and Emily, and Bob and Alice were friends, and there was no direct connectivity between Bob and Alice (maybe because of they are to far away from each other)*

### Details ###


#### Bootstrapping / The Connection Overlay ####
In the connection overlay, every peer can establish a connecction to every other peer who is online and uses the system.

To start using the system, at least **one other peer has to be found** via the connection overlay. To find a peer to connect to,
an IP and port combination has to be entered. (A future implementation could save working combinations in a file to try.
Also, there could be an option for a peer to volunteer as a publicly discoverable peer who publishes its ip/port-combination on
a website where everyone can look them up. This should not be mandatory, as the peer would disclose that this ip
is definitely using the service to non-users.)

After connecting with one peer, information about neighbours are transmitted through the system, so that neighbours of neighbours of neighbours (etc.)
can be found and a Connection Overlay connection can be established. Right now, peers discover to everyone they discover that way. In a  real life implementation, it would be neccessary to have some kind of limit.

![example of bootstrapping](https://github.com/LauraWartschinski/PrivateFriendToFriend/blob/master/doku/bootstrap.png)

#### The Friend Overlay / Filesharing ####

Friends are users that trust each other. The friend connection is symmetrical. Friends have to exchange their public keys over a secure channel of communication that is not provided by this network (but also not very hard). Then, they can send a friend request through the network. It is sent over the Connection Overlay and flooded through the whole network. The payload is a message that is encrypted with the public key of the intented receiver, so only the receiver can decrypt it. Inside the message are the port and ip and public key of the sender who started the friend request. The receiver will check if the friend is in their list of trusted peers (by comparing their public key to their database of potential friends), and if that succeeds, they will send a message directly to the ip/port specified in the message. This includes *their* IP, port and public key. This will result in a direct acknowledgement from the friend, and afterwards, both consider each other connected as friends.


![example of three connected peers](https://github.com/LauraWartschinski/PrivateFriendToFriend/blob/master/doku/friendrequest.png)


Friends exchange files with each other and tell friends which files they can provide. 

In the local folder reserved for the user, all files are stored. Every file that is located there is advertised by name to all friends, who in turn advertise the file to their friends. If a announcement is received, the receiver stores the filename and the connection details of the announcer. If the same file (for simplicity: a file with the same name) is advertised by somebody else, this is ignored. Therefore, the knowledge about the file spreads through the network like a tree with the root at the origin peer.

(In a real life implementation for actual use, it would be useful to use a soft state protocol that updates the messages in regular intervals and assumes that files are not longer available if the last update is after a certain timeout. This might be tricky, since updates from friends might refresh the file again, so cycles have to be avoided.)

A user can send a request for a file to a friend who announced that file to them. The files will be transmitted directly, or if the friend doesn't store the file locally, the friend will first request the file from whoever announced it to *them*, and so forth, until the file is transmitted to the final recipient.

In our implementation, files are stored in the same folder as the files that are announced to the network, so they spread through the network.


![example of three connected peers](https://github.com/LauraWartschinski/PrivateFriendToFriend/blob/master/doku/threesome.png)

In this example, all peers are connected on the Connection Overlay. Bob is friends with Claire, and Emily is friends with Claire (you can see the requests for friendship in Claires logs). All peers have two announcements, from both other peers, even though they are only friends with one other peer, since the other announcement is forwarded through the network.

#### Conclusion ####

The network is, obviously, not exactly fast and efficient. In the worst case, a long chain of n friends with no interconnections (Alice-Bob-Claire-Emily), a transmission from Alice to Emily would require n transmissions of announcements, n requests, and n times a file to be delivered over the whole network. The routing complexity is in O(n), which is considerably worse than lots of other, more sensible filesharing systems, e.g. those with a DHT. Every Peer only *needs* to have one friend to be able to receive files, but it is not given that every file will be available in such case. It is possible if the whole friend overlay is a connected graph, which doesn't need to be the case, since it is up to the user who they consider to be a friend. To sum it up, this P2P network is very inefficient.

It's upsides are definitely the deniability features. Nobody knows if the person asking for a file wants it for themselves, or to transmit it somebody else. The same goes for file announcements. Everybody is only ever exchanging files with their direct friends. Maybe there could be some kind of application of such a system in the real world.

The unique structure and the implementation details that came with the double overlay structure are the main points of interest. It is possible to transmit (text) files with the programm, but it is not exactly comfortable.


## How to install ##


![example of setup](https://github.com/LauraWartschinski/PrivateFriendToFriend/blob/master/doku/setup.png)
