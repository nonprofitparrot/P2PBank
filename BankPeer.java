import java.util.*;
import java.io.*;
import java.net.*;

public class BankPeer{
	
	public int port;
	public ServerSocket peerSocket;
	
	//public ArrayList<BankPeer> peers = new Arraylist<BankPeer>();
	//public Hashtable<String, BankPeer> memberToPeer = new Hashtable<String, BankPeer>();
	
	public HashMap<String, BankMember> members = new HashMap<String, BankMember>();
	
	public static void main (String args[])throws Exception{
		
		BankPeer peer = new BankPeer();
		Socket socket = null;
		
		peer.port = Integer.parseInt(args[0]); //should add error handling
		peer.peerSocket = new ServerSocket(peer.port, 20);	
		peer.peerSocket.setReuseAddress(true);
		
		System.out.println("Started up Bank Peer on port "+Integer.toString(peer.port));
		
		while ( (socket = peer.peerSocket.accept()) != null ) {
			
			System.out.println( "Accepted an incoming connection" );
			// creates a SessionThread for each client that connects
			new Session(socket, peer).start();
			
		}
		
	
		peer.peerSocket.close();
		
		
	}
	
	synchronized boolean addMember(String n){
		
		if (members.containsKey(n)){
		
			return false;
			
		}else{
			
			//memberToPeer.put(name,);
			
			members.put(n, new BankMember(n));
			return true;
			
		}
		
	}
	
	synchronized boolean hasMember(String n){
		
		if (members.containsKey(n)){
		
			return true;
			
		}else return false;
		
	}
	
	public static class BankMember{
		
		public String name;
		private int balance;
		private boolean inSession;
		
		public BankMember(String n){
			
			name = n;
			balance = 0;
			
		}
		
		synchronized int credit(int m){
			
			balance += m;
			return balance;
			
		}
		
		synchronized int debit(int m){
			
			balance -= m;
			return balance;
			
		}
		
		synchronized int getBal(){
			
			return balance;
			
		}
		/* Operations are thread safe and shouldn't need a lock
		synchronized boolean sessionLock(){
			
			if(inSession == true){
				return false;
			}else{
				inSession = true;
				return true;
			}
			
		}
		synchronized boolean sessionUnlock(){
			
			if(inSession == false){
				return false;
			}else{
				inSession = false;
				return true;
			}
			
		}
		*/
	}
	
	public static class Session extends Thread{
	
		private Socket socket;
		private BankPeer peer;
		private BufferedReader	fromClient;
		private PrintWriter 	toClient;
		private boolean inSession; //true while user has started accessing a named account
		private String name; //users account name
		private BankMember mem = null;
		
		
		public Session( Socket sock, BankPeer beer ) {
			
			socket = sock;
			peer = beer;
			
		}
		
		public void run(){
			
			String s;
		
			try{
				
				fromClient = new BufferedReader(new InputStreamReader(socket.getInputStream()));
				toClient = new PrintWriter( new OutputStreamWriter(socket.getOutputStream()), true );
				
				toClient.println("Connected to Bank Server.");

				while ( (s = fromClient.readLine()) != null ) {
						
					doCommand(s);
				
				}
				
				socket.close();
				
			}catch(Exception e){}
		} 
		
		private void doCommand(String cmd)throws Exception{
			
			String s = null;
			int i = 0;
			
			if(inSession == false){ //user is not in named session
				
				if(cmd.startsWith("open")){//
					
					s = cmd.substring(5);
					if(peer.addMember(s)){
						toClient.println("Opened new account: "+s);
						return;
					}else{ 
						toClient.println("There is already a bank member by this name, try starting an account session.");	
						return;
					}
			
				
				}else if(cmd.startsWith("start")){//
				
					s = cmd.substring(6);
					if(peer.hasMember(s)){
						mem = peer.members.get(s);
						inSession = true;
						name = s;
						toClient.println("Starting account session: "+s);
						return;
					}else{ 
						toClient.println("There is no bank memeber by that name, try opening one.");	
						return;
					}

				}else if(cmd.startsWith("exit")){
					
					toClient.println("Disconnecting from Bank");
					socket = null;
					fromClient = null;
					toClient = null;
					
				}else{ 
					toClient.println("you do not have permission to use this command, try starting an account session");	
					return;
				}
				
				
			}else{ //user is in a named session
				
				if(cmd.startsWith("credit")){//
				
					i = Integer.parseInt(cmd.substring(7));
					mem.credit(i);
					toClient.println("Added $"+Integer.toString(i)+" to account "+name+".");
					return;
					
				}else if(cmd.startsWith("debit")){//
					
					i = Integer.parseInt(cmd.substring(6));
					if(mem.getBal() < i){
						
						toClient.println("There are not enough funds in the account");
						return;
						
					}else{
						
						mem.debit(i);
						toClient.println("Removed $"+Integer.toString(i)+" to account "+name+".");
						return;
						
					}
					
				}else if(cmd.startsWith("balance")){//
				
					i = mem.getBal();
					toClient.println("Balance: "+name+" $"+Integer.toString(i));
					return;
					
				
				}else if(cmd.startsWith("finish")){
									
					mem = null;
					inSession = false;
					name = null;
					toClient.println("Ending account session.");
					return;

				}else{
					
					toClient.println("You do no have permission to use this command, try finishing your current session");
					
				}
			
			}
			
			return;
			
		}
	}	
}
