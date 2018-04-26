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

#### the Friend Overlay ####

Friends are users that trust each other. The friend connection is symmetrical. Friends have to exchange their public keys over a secure channel of communication that is not provided by this network (but also not very hard). Friends exchange files with each other and tell friends which files they can provide.

