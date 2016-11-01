all: Tracker.java Client.java TestClient.java
	javac Tracker.java Client.java TestClient.java
tracker: Tracker.java
	javac Tracker.java
client: Client.java
	javac Client.java
testclient: TestClient.java
	javac TestClient.java
clean:
	rm *.class
