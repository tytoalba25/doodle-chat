# torCHAT
Channel based messaging application. Built for CPSC 559.

Written by:
	Spencer Briere
	Braden Ariss

=========
Compiling
=========
Run make in the directory to build everything.
Just the Tracker/Client can be built by running either:
	make tracker, or
	make client
To clean the directory and remove .class files run:
	make clean

=======
Running
=======
To run the Tracker, use:
	java Tracker <port>
Where <port> is where which port any clients should use to communicate with the Tracker (suggested 5555)

Due to the formatting of XML backups, in order to run multiple trackers they must be run in seperate folders

To run the Client, use:
	java Client <tracker-address>
Where <tracker-address> is the address where the Client should find the Tracker in the form <ip>:<port>

=====
Usage
=====
When the Tracker launches it will display where it can be contacted by any Clients. After this it will simply display any connections and its request/response.

The Client has many more interactions than the Tracker. First it will request a display name to be used. From there the Client can talk to the Tracker in order to:
	Get a list of available channels
		list
	Create a channel
		create <channel-name>
	Join a channel
		join <channel-name>
	Close the client
		/quit

When the Client joins a channel it can start communicating with any other Clients in the channel by simply typing a message and hitting return.
