all: peer client

peer: BankPeer.java
	javac BankPeer.java

client: BankClient.java
	javac BankClient.java

rebuild: clean all

rebuild_peer: clean peer

rebuild_client: clean client

clean:
	rm -f *.class
