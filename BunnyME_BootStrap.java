import java.io.*;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

/**
 * Class BunnyME_BootStrap handles the functionality of the bootstrap of the
 * system. The bootstrap is the initial point of contact between the user and
 * the system before the user being connected to a server. Once the user is
 * connected to the server, they do not communicate again till the server that
 * the user to fails. In that case the user is assigned a new load balanced
 * server. The same is for the servers. they communicate with the bootstrap once
 * when they want to be added to the network, once added to the network, the
 * servers communicate back to the bootstrap only when they detect a failed
 * predecessor server. The bootstrap also maintains a record of all the users
 * registered in the system and the users currently active in the system.
 * 
 * To compile the bootstrap, save the files Queue.java and
 * BootStrap2ServerInterface.java in the same directory and run the following
 * command: 'javac *.java'
 * 
 * The bootstrap runs at glados.cs.rit.edu
 * To run the bootstrap, run the following command: 'java BunnyME_BootStrap'
 * 
 * @author Aditya Advani
 * @author Ankita Sambhare
 * 
 * @version May 13, 2016
 *
 */
public class BunnyME_BootStrap extends UnicastRemoteObject implements
		BootStrap2ServerInterface, Serializable {

	// Global variables
	// synchronization object
	static Object o = new Object();
	// port of RMI operations
	static final int port = 7394;
	// Map of all connected servers(HashValue(IP),IP)
	static HashMap<Integer, String> BootStraps;
	// global registry reference
	static Registry registry = null;
	// Map of all connected users (Username, IP)
	static HashMap<String, String> ConnectedUsers;
	// Map of all registered users(username,password)
	static HashMap<String, String> Users;
	// Map of all the queues currently present in the system(QueuePattern,Queue)
	static HashMap<String, Queue> Queues;
	// Max allowed number of servers that can be connected to the system at the
	// same time
	static int maxIDSpace = 25;
	// List of all used up hash spaces by servers in the system
	ArrayList<Integer> BootstrapHash = new ArrayList<>();

	// Constructor
	public BunnyME_BootStrap() throws RemoteException {
		// Start registry
		try {
			registry = LocateRegistry.getRegistry(port);
			registry.rebind("update", this);
		} catch (Exception e) {
			registry = LocateRegistry.createRegistry(port);
			registry.rebind("update", this);
		}

		// initialize maps and lists
		BootStraps = new HashMap<>();
		Users = new HashMap<>();
		Queues = new HashMap<>();
		ConnectedUsers = new HashMap<>();

		// default user
		Users.put("Aditya", "Ankita");
	}

	@Override
	// method adds data to queue at bootstrap
	public void addDataToQueue(String QPattern, String Message)
			throws RemoteException {
		synchronized (o) {
			if (Queues.containsKey(QPattern)) {
				Queues.get(QPattern).data.add(Message);
				if (Queues.get(QPattern).data.size() > Queues.get(QPattern).capacity) {
					Queues.get(QPattern).data.remove(0);
				}
			}
		}
	}

	@Override
	// method creates a queue at bootstrap with high storage capacity
	public void createQueueAtBootStrap(String username, String QName,
			String QPattern) throws RemoteException {
		synchronized (o) {
			if (!Queues.containsKey(QPattern)) {
				Queues.put(QPattern, new Queue(username, QName, QPattern, true));
			}
		}
	}

	@Override
	// method deletes a queue from bootstrap
	public void deleteQueueAtBootStrap(String QPattern) throws RemoteException {
		synchronized (o) {
			if (Queues.containsKey(QPattern)) {
				Queues.remove(QPattern);
			}
		}
	}

	// hash function takes string as input
	public static int HashOf(String s) {
		char ch;
		int hash = 0;
		for (int i = 0; i < s.length() - 1; i++) {
			ch = s.charAt(i);
			hash += (int) ch;
		}
		hash %= maxIDSpace;
		return hash;
	}

	@Override
	// method to LogOut a user from the system
	public void logOutUser(String username) throws RemoteException {
		ConnectedUsers.remove(username);
	}

	@Override
	// remote method called by user to login into the system
	public String RemoteLogin(String username, String password, String userIP)
			throws RemoteException {

		System.out.println("login request from " + username);
		String result = login(username, password, userIP);
		System.out.println("returning from remote login : " + result);
		return result;
	}

	// method for login returns the IP of the server to connect to
	public String login(String username, String password, String userIP) {
		if (Users.containsKey(username)) {
			if (Users.get(username).equals(password)
					&& !ConnectedUsers.containsKey(username)) {
				try {

					System.out.println("User loggedin");
					String reply = LoadBalancedBootStrapIP(username, userIP);
					System.out.println(reply);
					ConnectedUsers.put(username, userIP);

					try {
						BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
								.lookup("//" + reply + ":" + port + "/update");
						String result = send.addConnectedUser(username, userIP);
					} catch (Exception e) {
					}

					return reply;
				} catch (RemoteException ex) {
				}
				return "";
			} else {
				return "wrong username/password";
			}
		} else {
			return "wrong username/password";
		}

	}

	@Override
	// method to reconnect the user to a new server if the old server fails
	public String ReconnectUser(String username) throws RemoteException {

		System.out.println("Trying to reconnect user!");

		String userIP = ConnectedUsers.get(username);
		String reply = "";
		try {
			System.out.println("User loggedin");
			reply = LoadBalancedBootStrapIP(username, userIP);
			System.out.println(reply);

			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + reply + ":" + port + "/update");
				String result = send.addConnectedUser(username, userIP);
			} catch (Exception e) {
			}
		} catch (RemoteException ex) {
		}
		return reply;
	}

	@Override
	// remote method called by user to get registered into the system
	public String RemoteSignup(String username, String password, String userIP)
			throws RemoteException {

		System.out.println("signup request from " + username);
		String result = signup(username, password, userIP);
		System.out.println("returning from remote signup : " + result);
		return result;
	}

	// method for signup returns the IP of the server to connect to
	public String signup(String username, String password, String userIP) {
		if (Users.containsKey(username)) {
			return "username exists";
		} else {
			String ip;
			Users.put(username, password);
			ConnectedUsers.put(username, userIP);

			try {
				System.out.println("user added");
				String reply = LoadBalancedBootStrapIP(username, userIP);
				System.out.println(reply);

				try {
					BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
							.lookup("//" + reply + ":" + port + "/update");
					String result = send.addConnectedUser(username, userIP);
				} catch (Exception e) {
				}

				return reply;
			} catch (RemoteException ex) {
			}
		}
		return "";
	}

	// method returns load balanced server ip to the caller
	public String LoadBalancedBootStrapIP(String username, String userIP)
			throws RemoteException {
		System.out.println("Checking load balancing");

		if (BootStraps.size() == 1) {
			// single server available for assigning the load

			String assignedIP = "";
			for (int i : BootStraps.keySet()) {
				assignedIP = BootStraps.get(i);

			}

			System.out.println("assigned IP: " + assignedIP);
			return assignedIP;

		} else {

			// create an arraylist of the hash values of the bootstrap servers
			ArrayList<Integer> BootstrapsHash = new ArrayList<>();
			for (int i : BootStraps.keySet()) {
				BootstrapsHash.add(i);
			}
			System.out.println("BootstrapHash: " + BootstrapsHash);

			// get two randomly selected hash values for selecting the
			// corresponding server

			int IndexOfRandomServer1, IndexOfRandomServer2;

			int max = BootstrapHash.size() - 1;
			int min = 0;

			Random r = new Random();

			IndexOfRandomServer1 = r.nextInt(max - min + 1) + min;

			System.out.println("IndexOfRandomServer1: " + IndexOfRandomServer1);
			do {
				IndexOfRandomServer2 = r.nextInt(max - min + 1) + min;

			} while (IndexOfRandomServer1 == IndexOfRandomServer2);

			System.out.println("IndexOfRandomServer2: " + IndexOfRandomServer2);

			int resultfrom1 = -1;
			int resultfrom2 = -1;

			int firstServerHash = BootstrapHash.get(IndexOfRandomServer1);
			String firstServerIP = BootStraps.get(firstServerHash);

			int secondServerHash = BootstrapHash.get(IndexOfRandomServer2);
			String secondServerIP = BootStraps.get(secondServerHash);

			try {

				BootStrap2ServerInterface send1 = (BootStrap2ServerInterface) Naming
						.lookup("//" + firstServerIP + ":" + port + "/update");
				resultfrom1 = send1.getCurrentLoad();

				BootStrap2ServerInterface send2 = (BootStrap2ServerInterface) Naming
						.lookup("//" + secondServerIP + ":" + port + "/update");
				resultfrom2 = send2.getCurrentLoad();

			} catch (Exception ex) {
			}

			// compare load of currently connected users
			if (resultfrom1 < resultfrom2) {
				return firstServerIP;
			} else {
				return secondServerIP;
			}

		}

	}

	// get IP of server to be used for optimally connecting new server
	public int getNodeToAssign(int n) {

		Collections.sort(BootstrapHash);

		for (int i = 0; i < BootstrapHash.size() - 1; i++) {
			int firsthash = BootstrapHash.get(i);
			int secondhash = BootstrapHash.get(i + 1);

			if (n < secondhash && n > firsthash) {
				return secondhash;
			}

		}

		int firsthash = BootstrapHash.get(BootstrapHash.size() - 1);
		int secondhash = BootstrapHash.get(0);

		System.out.println("n: " + n);
		System.out.println("firsthash: " + firsthash);
		System.out.println("secondhash: " + secondhash);

		if (n < secondhash || n > firsthash) {

			System.out.println("secondhash: " + secondhash);
			return secondhash;
		} else
			return -1;

	}

	@Override
	// method to add a new node to the network
	public void addToNetwork(String nodeIP, int IPhash) throws RemoteException {
		System.out.println("\nReceived request to add " + nodeIP
				+ " to the network.");
		try {
			boolean firstNode = false;
			if (BootStraps.isEmpty()) {
				firstNode = true;
			}

			if (firstNode) {
				// first node in the network
				System.out.println("Attempting to add " + nodeIP
						+ " to the existing network.");

				try {
					BootStrap2ServerInterface node = (BootStrap2ServerInterface) Naming
							.lookup("//" + nodeIP + ":" + port + "/update");
					String result = node.InsertFirstNode();

				} catch (Exception e) {
				}

				BootStraps.put(IPhash, nodeIP);
				BootstrapHash.add(IPhash);

				System.out.println("Hash Assigned: " + IPhash);
				System.out.println(nodeIP + " is now a part of the network.");

			} else {
				// insert new node in existing network
				System.out.println("Insert a node to the network");

				int hash = IPhash;

				// hash value collision handling
				if (!BootStraps.isEmpty()) {
					while (BootStraps.containsKey(hash)) {
						hash += hash;
						hash = hash % maxIDSpace;
						if (hash == IPhash) {
							hash++;
						}
					}
				}

				System.out.println("Hash Assigned: " + hash);

				// find the insertion node
				int startNodeHash = getNodeToAssign(hash);

				String startNodeIP = BootStraps.get(startNodeHash);

				try {
					BootStrap2ServerInterface node = (BootStrap2ServerInterface) Naming
							.lookup("//" + nodeIP + ":" + port + "/update");
					String result = node.InsertionNewNode(nodeIP, hash,
							startNodeIP);

				} catch (Exception e) {
				}

				BootStraps.put(hash, nodeIP);
				BootstrapHash.add(hash);
				System.out.println(nodeIP + " is now a part of the network.");
			}
		} catch (Exception ex) {
		}

		for (int i : BootStraps.keySet()) {
			try {
				BootStrap2ServerInterface node = (BootStrap2ServerInterface) Naming
						.lookup("//" + BootStraps.get(i) + ":" + port
								+ "/update");
				String result = node.printDetails();

			} catch (Exception e) {
			}

		}

	}

	// main method
	public static void main(String args[]) {

		try {
			BunnyME_BootStrap b = new BunnyME_BootStrap();
		} catch (RemoteException ex) {
		}

		System.out.println("Server Started...");
		String NodeIP = "";
		try {
			InetAddress IP = InetAddress.getLocalHost();
			NodeIP = IP.getHostAddress();
		} catch (UnknownHostException ex) {
		}
		System.out.println("Public IP for connection: " + NodeIP);

		Thread ka = new Thread(new KeepAlive());
		ka.start();

	}

	// nested class implements a run method to keep the bootstrap alive
	public static class KeepAlive implements Runnable {

		public KeepAlive() {
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(5000);
				} catch (InterruptedException ex) {
				}
			}
		}
	}

	// object serialization stream used for persistence
	public static byte[] serialize(Object obj) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		ObjectOutputStream os = null;
		try {
			os = new ObjectOutputStream(out);
		} catch (IOException e) {
		}
		try {
			os.writeObject(obj);
		} catch (IOException e) {
		}
		return out.toByteArray();
	}

	// object de-serialization stream used for persistence
	public static Object deserialize(byte[] data) {
		ByteArrayInputStream in = new ByteArrayInputStream(data);
		ObjectInputStream is = null;
		try {
			is = new ObjectInputStream(in);
		} catch (IOException e) {
		}
		try {
			return is.readObject();
		} catch (ClassNotFoundException | IOException e) {
		}
		return is;
	}

	@Override
	// method to remove a failed server from the system
	public String removeNode(int iphash) throws RemoteException {

		System.out.println(BootStraps);
		BootStraps.remove(iphash);
		BootstrapHash.remove((Integer) iphash);
		//
		System.out.println(BootStraps);

		for (int i : BootStraps.keySet()) {
			try {
				BootStrap2ServerInterface node = (BootStrap2ServerInterface) Naming
						.lookup("//" + BootStraps.get(i) + ":" + port
								+ "/update");
				String result = node.printDetails();

			} catch (Exception e) {
			}

		}

		return null;
	}

	@Override
	// prints current stats at every server
	public String printDetails() throws RemoteException {

		for (int i : BootStraps.keySet()) {
			try {
				BootStrap2ServerInterface node = (BootStrap2ServerInterface) Naming
						.lookup("//" + BootStraps.get(i) + ":" + port
								+ "/update");
				String result = node.printDetails();
			} catch (Exception e) {
			}
		}
		return null;
	}

	//
	//
	// unused methods from the common interface
	//
	//

	@Override
	public String InsertFirstNode() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String InsertionNewNode(String ip, int nodehash, String startNodeIP)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> createQueue(String username, String QName,
			String QPattern) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCurrentLoad() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String addConnectedUser(String username, String hostAddress) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addMeToChord(String ip, int nodehash, String startNodeIP)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setPredecessor(String pre) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSuccessor(String suc) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getPredecessor() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getSuccessor() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getHashValue() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setUpperLimit(int uLimit) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getUpperLimit() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void setLowerLimit(int lLimit) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public int getLowerLimit() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void getQueues(HashMap<String, Queue> Q) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void HeartBeat() throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public HashMap<String, Queue> getQueuesNotInRange() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setPrePredecessor(String prePre) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getPrePredecessor() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean sendQueueToBackup(Queue q) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeQueue(String QPattern) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<String> deleteQueue(String username, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addQueueToListOfQueuesInSystem(String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeQueueFromListOfQueuesInSystem(String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addUpdatedQueues(HashMap<String, Queue> BackupQueues)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateBackups() throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void setHashValue(int hash) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public String getName() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void removeTheseQueues(HashMap<String, Queue> rmQueue)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public HashMap<String, Queue> getQueuesInRange() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean publishNewMessage(String username, String qPattern,
			String publishMsg) throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void setQueuesInSystemToConnectedUsers(ArrayList<String> QIS)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<String> getQueuesInSystem(String username)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendupdates() throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<String> getSubscriptionListOfUser(String username,
			String CallerIP, ArrayList<String> subs) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getMySubscriptions(String username)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addData(String QPattern, String QMessage)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<String> getPublicationsOf(String username,
			String CallerIP, ArrayList<String> pubs) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> getServices(String username)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void LogOut(String username) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void autoheal() throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void imAlive() throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public ArrayList<String> getUnsubscribedQueues(String username)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> ShiftQueue(String username, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> shiftDataQueue(String username, String QPattern,
			String CallerIP) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addSubscriber(String username, String userIP, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean subscribeMe(String username, String userIP, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean subscribe(String username, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unsubscribe(String username, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void removeSubscriber(String username, String userIP, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean unsubscribeMe(String username, String userIP, String QPattern)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public ArrayList<String> RegenerateQueuesInSystem() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<String> Regenerate(String CallerIP, ArrayList<String> QIS)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void pushQueueData(String QPattern, String Message)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void disconnect(String username, String CallerIP)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

}
