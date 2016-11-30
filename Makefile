all: Tracker.java Client.java
	javac Tracker.java Client.java
tracker: Tracker.java
	javac Tracker.java
client: Client.java
	javac Client.java
clean:
	rm *.class
