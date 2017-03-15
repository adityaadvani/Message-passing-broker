import java.io.IOException;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.io.*;
import java.net.*;

/**
 * Class BunnyME_Server handles the functionality of the server. It handles
 * communication of the server with bootstrap, users and other server. It uses
 * RMI to communicate with the bootstrap and other servers and uses TCP to
 * communicate with the client. The server handles all the queues and the data
 * in the network. they use a chord like DHT mechanism to distribute the queues
 * within the network and provide fault tolerance by recovering gracefully in
 * case of a node failure in the system. The servers handle the queues in their
 * range a store a backup of the queues at their predecessor and prepredecessor
 * as backups to handle node failure recovery. Server with the queue directly
 * sends out a message to the subscribed users in case of a new message but only
 * communicate with the connected users in case of any other communication.
 * Every server checks if its predecessor is alive by sending out a heart beat
 * message. It also maintains a list of all connected users and acts as the
 * point of contact for the connected users by channeling all the system
 * communication with it. This means that no server talks to a user not
 * connected to it for anything but publishing messages to subscribers. TCP
 * connections are made with the subscribers to publish to them the new messages
 * and the connection is ended as soon as the message is sent.
 * 
 * To compile the server, save the files Queue.java and
 * BootStrap2ServerInterface.java in the same directory and run the following
 * command: 'javac *.java'
 * 
 * To run the server, first make sure that the bootstrap is running and then run
 * the following command: 'java BunnyME_Server [bootstrap IP]'
 *
 * @author Aditya Advani
 * @author Ankita Sambhare
 * 
 * @version May 13, 2016
 * 
 */
public class BunnyME_Server extends UnicastRemoteObject implements
		BootStrap2ServerInterface, Serializable {

	// Synchronization variable
	static Object o = new Object(); // for queue add/remove
	static Object msgobj = new Object(); // for message add/remove

	// Chord Variables
	static int maxIDSpace = 25; // ID value range is 0 to (maxIDSpace-1)
	static int hashValue; // hashValue of current node
	static String predecessor; // IP of predecessor
	static String predecessorName; // name of predecessor
	static String successor; // IP of successor
	static String successorName; // name of successor
	static String prePredecessor; // IP of prepredecessor
	static String prePredecessorName; // name of prepredecessor
	static int lowerLimit; // lower limit of current node's ID range in network
	static int upperLimit; // upper limit of current node's ID range in network

	// chord HeartBeat check timer task
	static Timer heartbeatTimer;

	// Class Variables
	// list of all current queues in the system <QueuePattern>
	static ArrayList<String> QueuesInSystem;
	// Map of all queues on the current node <QueuePattern, Queue>
	static HashMap<String, Queue> Queues;
	// Map of all connected users on the node <username,IP>
	static HashMap<String, String> ConnectedUsers;
	// Map of all new messages for a queue <QueuePattern,list of messages for
	// the queue QueuePattern>
	static HashMap<String, ArrayList<String>> newMessages;

	// Node Attributes
	static Registry registry; // global registry reference
	static int port = 7394; // RMI port
	static int portOnClient = 6162; // TCP port
	static String NodeIP = ""; // server IP
	static String NodeName = ""; // server name

	static String BootstrapIP = ""; // BootStrap's IP

	// Global State Object
	static BunnyME_Server MyState;

	// Constructor
	BunnyME_Server() throws RemoteException {

		// start registry
		try {
			registry = LocateRegistry.getRegistry(port);
			registry.rebind("update", this);
		} catch (Exception e) {
			registry = LocateRegistry.createRegistry(port);
			registry.rebind("update", this);
		}

		// initialize maps
		ConnectedUsers = new HashMap<>();
		Queues = new HashMap<>();
		QueuesInSystem = new ArrayList<>();
		newMessages = new HashMap<>();
	}

	// main method
	public static void main(String[] args) {
		try {
			MyState = new BunnyME_Server();
		} catch (RemoteException ex) {
		}

		String BootStrapIP = args[0];
		MyState.startServer(BootStrapIP);
	}

	public void startServer(String BootStrapIP) {
		BootstrapIP = BootStrapIP;

		// keep alive indefinitely till explicitely terminated
		Thread heartbeat = new Thread(new KeepAlive());
		heartbeat.start();

		Thread BCast = new Thread(new Broadcaster());
		BCast.start();

		heartbeatTimer = new Timer();
		heartbeatTimer.schedule(new HeartBeatSchedule(), 5000, 3000);

		// identify self network attributed
		try {
			InetAddress IP = InetAddress.getLocalHost();
			NodeName = IP.getHostName();
			NodeIP = IP.getHostAddress();
			hashValue = HashOf(NodeIP);
			System.out.println("My hash Value: " + hashValue);

		} catch (UnknownHostException ex) {
		}

		try {
			System.out.println("Requesting connecting to bootstrap: "
					+ hashValue);
			BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
					.lookup("//" + BootStrapIP + ":" + port + "/update");
			send.addToNetwork(NodeIP, hashValue);
		} catch (Exception e) {
		}
	}

	// Synchronized queue add to current server
	public void addQueueToQueues(String QPattern, Queue Q) {
		synchronized (o) {
			Queues.put(QPattern, Q);
		}
	}

	// Synchronized queue remove from current server
	public void removeQueueFromQueues(String QPattern) {
		synchronized (o) {
			Queues.remove(QPattern);
		}
	}

	// method to display all stats for current server
	public void DisplayNodeStats() {
		System.out.println("\nMachine Details are as follows:");
		System.out.println("Local Machine: " + NodeName + " (" + NodeIP + ")");

		BootStrap2ServerInterface node;
		try {
			node = (BootStrap2ServerInterface) Naming.lookup("//" + predecessor
					+ ":" + port + "/update");
			predecessorName = node.getName();
			node = (BootStrap2ServerInterface) Naming.lookup("//" + successor
					+ ":" + port + "/update");
			successorName = node.getName();
			node = (BootStrap2ServerInterface) Naming.lookup("//"
					+ prePredecessor + ":" + port + "/update");
			prePredecessorName = node.getName();
		} catch (MalformedURLException e) {
		} catch (RemoteException e) {
		} catch (NotBoundException e) {
		}

		System.out.println("Local Range: LowerLimit- " + lowerLimit
				+ ", UpperLimit- " + upperLimit);
		System.out.println("Successor Machine: " + successorName + " ("
				+ successor + ")");
		System.out.println("Predecessor Machine: " + predecessorName + " ("
				+ predecessor + ")");
		System.out.println("PrePredecessor Machine: " + prePredecessorName
				+ " (" + prePredecessor + ")");

		int QueueCounter = 0;
		int UserCounter = 0;
		int QueueHash;

		System.out.println("\nManaged Queues: " + Queues.size());
		for (String qp : Queues.keySet()) {
			QueueCounter++;
			QueueHash = HashOf(qp);
			System.out.println("Hash: " + QueueHash);
			if (isWithinRange(QueueHash)) {
				System.out.println("Queue Type: Active Queue");
			} else {
				System.out.println("Queue Type: Backup Queue");
			}
			System.out.println("Queue_" + QueueCounter + " pattern: "
					+ Queues.get(qp).pattern);
			System.out.println("Queue_" + QueueCounter + " publisher: "
					+ Queues.get(qp).publisher);
			System.out.println("Queue_" + QueueCounter + " subscribers: ");
			UserCounter = 0;
			for (String user : Queues.get(qp).Subscribers.keySet()) {
				UserCounter++;
				System.out
						.println("User_" + UserCounter + " Username: " + user);
			}
			System.out.println();
		}

		System.out.println("ConnectedUsers count: " + ConnectedUsers.size());
		UserCounter = 0;
		for (String user : ConnectedUsers.keySet()) {
			UserCounter++;
			System.out.println("User_" + UserCounter + " Username: " + user);
			System.out.println("User_" + UserCounter + " IPAddress: "
					+ ConnectedUsers.get(user));
		}

	}

	@Override
	// method to add first server to the network
	public String InsertFirstNode() throws RemoteException {

		System.out.println("\n\nReceived request to connect");
		predecessor = NodeIP;
		predecessorName = NodeName;
		successor = NodeIP;
		successorName = NodeName;
		prePredecessor = NodeIP;
		prePredecessorName = NodeName;

		upperLimit = hashValue;

		if (upperLimit == (maxIDSpace - 1)) {
			// crossover point in focus
			lowerLimit = 0;
		} else {
			// crossover point not in focus
			lowerLimit = upperLimit + 1;
		}
		DisplayNodeStats();
		return null;
	}

	@Override
	// method to add new server to the network
	public void addMeToChord(String ip, int nodehash, String startNodeIP)
			throws RemoteException {

		// deserving files for new node + backup files of current node's new
		// range
		HashMap<String, Queue> MyOldQueues = new HashMap<>();
		for (String qp : Queues.keySet()) {
			if (isWithinRange(HashOf(qp))) {
				Queue q = new Queue(Queues.get(qp).publisher,
						Queues.get(qp).name, Queues.get(qp).pattern);
				for (String d : Queues.get(qp).data) {
					q.data.add(d);
				}
				MyOldQueues.put(qp, q);
			}
		}

		// within range
		System.out.println("Attempting to add new node to the network.");

		try {
			BootStrap2ServerInterface new_node = (BootStrap2ServerInterface) Naming
					.lookup("//" + ip + ":" + port + "/update");
			BootStrap2ServerInterface pred_node = (BootStrap2ServerInterface) Naming
					.lookup("//" + predecessor + ":" + port + "/update");
			BootStrap2ServerInterface succ_node = (BootStrap2ServerInterface) Naming
					.lookup("//" + successor + ":" + port + "/update");

			// setting node's links

			// set node's successor to node_x
			// set node's predecessor to node_x's predecessor
			new_node.setSuccessor(NodeIP);
			new_node.setPredecessor(predecessor);
			new_node.setHashValue(nodehash);

			// get node's range
			// get node_x's predecessor's IPhash to get node's range
			// set node's range limits
			// initialize node's fileMap
			// change node_x's lowerLimit to node's IPhash+1
			int pre_node_hash = pred_node.getHashValue();
			new_node.setLowerLimit((pre_node_hash + 1) % maxIDSpace);
			new_node.setUpperLimit(nodehash % maxIDSpace);
			// new_node.setup(BootStraps, Users, Sequencers, Queues);
			lowerLimit = (nodehash + 1) % maxIDSpace;

			// setting remaining links
			// set node_x's predecessor's successor to node
			// set node_x's predecessor to node
			pred_node.setSuccessor(ip);
			prePredecessor = predecessor;
			predecessor = ip;

			new_node.setPrePredecessor(pred_node.getPredecessor());

			pred_node = (BootStrap2ServerInterface) Naming.lookup("//"
					+ predecessor + ":" + port + "/update");
			if (!NodeIP.equals(pred_node.getPredecessor())) {
				succ_node.setPrePredecessor(predecessor);
			}

			// add active and backup queue to new node
			System.out.println("Sending to new node: " + MyOldQueues);
			new_node.getQueues(MyOldQueues);

			// remove these nodes from old predecessor if self or predecessor or
			// prepredecessor is not old predecessor
			for (String qp : MyOldQueues.keySet()) {
				if (!isWithinRange(HashOf(qp))) {
					MyOldQueues.remove(qp);
					if (!NodeIP.equals(new_node.getPredecessor())
							&& !NodeIP.equals(new_node.getPrePredecessor())) {
						removeQueueFromQueues(qp);
					}
				}
			}

			// remove new range queues from old prepredecessor
			String oldPrePredecessor = new_node.getPrePredecessor();
			if (!oldPrePredecessor.equals(NodeIP)
					&& !oldPrePredecessor.equals(predecessor)
					&& !oldPrePredecessor.equals(prePredecessor)) {
				BootStrap2ServerInterface old_pred = (BootStrap2ServerInterface) Naming
						.lookup("//" + oldPrePredecessor + ":" + port
								+ "/update");
				old_pred.removeTheseQueues(MyOldQueues);
			}

			// add successor's queues as backup to new node
			HashMap<String, Queue> succ_Queues;
			succ_Queues = succ_node.getQueuesInRange();
			new_node.getQueues(succ_Queues);

			// remove queues from successor's old predecessor
			BootStrap2ServerInterface prepred_node = (BootStrap2ServerInterface) Naming
					.lookup("//" + prePredecessor + ":" + port + "/update");
			prepred_node.removeTheseQueues(succ_Queues);

		} catch (Exception e) {
		}
	}

	@Override
	// remove queues from the server
	public void removeTheseQueues(HashMap<String, Queue> rmQueue)
			throws RemoteException {
		for (String qp : rmQueue.keySet()) {
			if (Queues.containsKey(qp)) {
				removeQueueFromQueues(qp);
			}
		}
	}

	@Override
	// called by external server to update upper limit of current server
	public void setUpperLimit(int uLimit) throws RemoteException {
		upperLimit = uLimit;
	}

	@Override
	// called by external server to get upper limit of current server
	public int getUpperLimit() throws RemoteException {
		return upperLimit;
	}

	@Override
	// called by external server to update lower limit of current server
	public void setLowerLimit(int lLimit) throws RemoteException {
		lowerLimit = lLimit;
	}

	@Override
	// called by external server to get the lower limit of current server
	public int getLowerLimit() throws RemoteException {
		return lowerLimit;
	}

	@Override
	// called by external server to get the name of current server
	public String getName() throws RemoteException {
		return NodeName;
	}

	@Override
	// called by external server to get hash value of current server
	public int getHashValue() throws RemoteException {
		return hashValue;
	}

	@Override
	// called by bootstrap to update hash value of current server
	public void setHashValue(int hash) throws RemoteException {
		hashValue = hash;
	}

	@Override
	// called by external server to update predecessor of current server
	public void setPredecessor(String pre) throws RemoteException {
		predecessor = pre;
	}

	@Override
	// called by external server to update successor of current server
	public void setSuccessor(String suc) throws RemoteException {
		successor = suc;
	}

	@Override
	// called by external server to update prepredecessor of current server
	public void setPrePredecessor(String prePre) throws RemoteException {
		prePredecessor = prePre;
	}

	@Override
	// called by external server to get predecessor of current server
	public String getPrePredecessor() throws RemoteException {
		return prePredecessor;
	}

	@Override
	// called by external server to get prepredecessor of current server
	public String getPredecessor() throws RemoteException {
		return predecessor;
	}

	@Override
	// called by external server to get successor of current server
	public String getSuccessor() throws RemoteException {
		return successor;
	}

	@Override
	// add queues to the current server
	public void getQueues(HashMap<String, Queue> Q) throws RemoteException {
		for (String QPattern : Q.keySet()) {
			addQueueToQueues(QPattern, Q.get(QPattern));
		}
	}

	@Override
	// method to add new server into the network
	public String InsertionNewNode(String ip, int nodehash, String startNodeIP)
			throws RemoteException {

		System.out.println("\n\nReceived request to connect " + "(" + ip + ")"
				+ " to the system at " + NodeName);

		try {
			BootStrap2ServerInterface node = (BootStrap2ServerInterface) Naming
					.lookup("//" + startNodeIP + ":" + port + "/update");
			node.addMeToChord(ip, nodehash, startNodeIP);

		} catch (Exception e) {
		}
		DisplayNodeStats();
		return null;
	}

	// method to remove a failed predecessor from the network
	public void removeNode() {

		int removenodeHash = lowerLimit - 1;
		if (removenodeHash == -1) {
			removenodeHash = maxIDSpace - 1;
		}

		System.out.println("removenode hash: " + removenodeHash);

		try {

			// updating self predecessor to prepredecessor
			predecessor = prePredecessor;

			// updating self prepredecessor
			BootStrap2ServerInterface pred_node = (BootStrap2ServerInterface) Naming
					.lookup("//" + predecessor + ":" + port + "/update");

			prePredecessor = pred_node.getPredecessor();
			lowerLimit = (pred_node.getHashValue() + 1);
			lowerLimit = lowerLimit % maxIDSpace;
			if (lowerLimit == maxIDSpace) {
				lowerLimit = 0;
			}

			// updating prepredecessor's successor to self
			pred_node.setSuccessor(NodeIP);

			// updating successor's prepredecessor to self predecessor
			BootStrap2ServerInterface succ_node = (BootStrap2ServerInterface) Naming
					.lookup("//" + successor + ":" + port + "/update");

			succ_node.setPrePredecessor(predecessor);

			// get all queues from pred_node that are out of its range
			HashMap<String, Queue> QMap = pred_node.getQueuesNotInRange();

			// from the returned queues, update the queues that are in your
			// range, ignore rest of the queues
			for (String QPattern : QMap.keySet()) {
				int QHash = HashOf(QPattern);
				if (isWithinRange(QHash)) {
					addQueueToQueues(QPattern, QMap.get(QPattern));
				}
			}
		} catch (Exception e) {
		}

		// remove node from bootstrap
		try {
			BootStrap2ServerInterface node = (BootStrap2ServerInterface) Naming
					.lookup("//" + BootstrapIP + ":" + port + "/update");
			String result = node.removeNode(removenodeHash);

			updateBackups();

			BootStrap2ServerInterface succ_node = (BootStrap2ServerInterface) Naming
					.lookup("//" + successor + ":" + port + "/update");
			succ_node.updateBackups();

		} catch (Exception e) {
		}

	}

	@Override
	// method to update predecessor and prepredecessor
	public void updateBackups() throws RemoteException {
		try {
			HashMap<String, Queue> QueuesToSend = new HashMap<>();
			for (String qp : Queues.keySet()) {
				if (isWithinRange(HashOf(qp))) {
					QueuesToSend.put(qp, Queues.get(qp));
				}
			}

			BootStrap2ServerInterface send1 = (BootStrap2ServerInterface) Naming
					.lookup("//" + predecessor + ":" + port + "/update");
			send1.addUpdatedQueues(QueuesToSend);

			BootStrap2ServerInterface send2 = (BootStrap2ServerInterface) Naming
					.lookup("//" + prePredecessor + ":" + port + "/update");
			send2.addUpdatedQueues(QueuesToSend);

		} catch (Exception ex) {
		}
	}

	@Override
	// method to add backup queues to self
	public void addUpdatedQueues(HashMap<String, Queue> BackupQueues)
			throws RemoteException {
		for (String QPattern : BackupQueues.keySet()) {
			addQueueToQueues(QPattern, BackupQueues.get(QPattern));
		}
		DisplayNodeStats();
	}

	@Override
	// method returns all queues from the server that are not in its range
	public HashMap<String, Queue> getQueuesNotInRange() throws RemoteException {
		HashMap<String, Queue> Q = new HashMap<>();
		for (String QPattern : Queues.keySet()) {
			int QHash = HashOf(QPattern);
			if (!isWithinRange(QHash)) {
				Q.put(QPattern, Queues.get(QPattern));
			}
		}
		return Q;
	}

	@Override
	// method returns all queues from the server that are in its range
	public HashMap<String, Queue> getQueuesInRange() throws RemoteException {
		HashMap<String, Queue> send_backup = new HashMap<>();
		for (String qp : Queues.keySet()) {
			if (isWithinRange(HashOf(qp))) {
				send_backup.put(qp, Queues.get(qp));
			}
		}
		return send_backup;
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

	// returns true if the number is within the servers range
	public boolean isWithinRange(int n) {
		boolean crossover = false;
		if (lowerLimit > upperLimit) {
			crossover = true;
		}
		if (crossover) {
			// if crossover detected
			if (n >= lowerLimit || n <= upperLimit) {
				// if node addition is within current node's range
				return Boolean.TRUE;
			}
		} else {
			// if no crossover
			if (n >= lowerLimit && n <= upperLimit) {
				// if node addition is within current node's range
				return Boolean.TRUE;
			}
		}
		return Boolean.FALSE;
	}

	@Override
	// method to remove queue from the system
	public ArrayList<String> deleteQueue(String username, String QPattern)
			throws RemoteException {
		boolean exists = false;
		int QPatternHash = HashOf(QPattern);
		ArrayList<String> userPublications = new ArrayList<>();

		if (!isWithinRange(QPatternHash)) {
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				userPublications = send.deleteQueue(username, QPattern);

			} catch (Exception ex) {
			}

			DisplayNodeStats();
			return userPublications;
		} else {
			for (String qp : Queues.keySet()) {
				if (qp.equals(QPattern)) {
					exists = true;
					break;
				}
			}

			if (exists) {
				removeQueueFromQueues(QPattern);
				try {
					BootStrap2ServerInterface send1 = (BootStrap2ServerInterface) Naming
							.lookup("//" + predecessor + ":" + port + "/update");
					send1.removeQueue(QPattern);

					BootStrap2ServerInterface send2 = (BootStrap2ServerInterface) Naming
							.lookup("//" + prePredecessor + ":" + port
									+ "/update");
					send2.removeQueue(QPattern);
				} catch (Exception ex) {
				}

				removeQueueFromListOfQueuesInSystem(QPattern);

				DisplayNodeStats();
				return getServices(username);
			} else {
				DisplayNodeStats();
				return getServices(username);
			}
		}
	}

	@Override
	// wrapper to shift queue data
	public ArrayList<String> ShiftQueue(String username, String QPattern)
			throws RemoteException {
		ArrayList<String> a = new ArrayList<>();
		try {
			BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
					.lookup("//" + predecessor + ":" + port + "/update");
			a = send.shiftDataQueue(username, QPattern, NodeIP);

		} catch (Exception ex) {
		}
		return a;
	}

	@Override
	// method to shift queue data
	public ArrayList<String> shiftDataQueue(String username, String QPattern,
			String CallerIP) throws RemoteException {
		ArrayList<String> d = new ArrayList<>();

		if (isWithinRange(HashOf(QPattern))) {
			return Queues.get(QPattern).data;
		}

		if (NodeIP.equals(CallerIP)) {

		} else {
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				d = send.shiftDataQueue(username, QPattern, CallerIP);

			} catch (Exception ex) {
			}
		}
		return d;
	}

	@Override
	// create a new queue in the system
	public ArrayList<String> createQueue(String username, String QName,
			String QPattern) throws RemoteException {

		boolean exists = false;
		ArrayList<String> userPublications = new ArrayList<>();

		int QPatternHash = HashOf(QPattern);

		if (!isWithinRange(QPatternHash)) {
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				userPublications = send.createQueue(username, QName, QPattern);

			} catch (Exception ex) {
			}

			DisplayNodeStats();
			return userPublications;
		} else {

			for (String qp : Queues.keySet()) {
				if (qp.equals(QPattern)) {
					exists = true;
					break;
				}
			}

			boolean createdQueueSuccessfully = false;
			if (!exists) {
				createdQueueSuccessfully = true;
				boolean replyFromBackup = false;
				Queue q = new Queue(username, QName, QPattern);
				addQueueToQueues(QPattern, q);

				if (!predecessor.equals(NodeIP)) {
					replyFromBackup = false;
					try {
						BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
								.lookup("//" + predecessor + ":" + port
										+ "/update");
						replyFromBackup = send.sendQueueToBackup(q);
						createdQueueSuccessfully = (createdQueueSuccessfully && replyFromBackup);
					} catch (Exception ex) {
					}
				}

				if (!prePredecessor.equals(NodeIP)) {
					replyFromBackup = false;
					try {
						BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
								.lookup("//" + prePredecessor + ":" + port
										+ "/update");
						replyFromBackup = send.sendQueueToBackup(q);
						createdQueueSuccessfully = (createdQueueSuccessfully && replyFromBackup);
					} catch (Exception ex) {
					}
				}

				if (!createdQueueSuccessfully) {
					if (!predecessor.equals(NodeIP)) {
						try {
							BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
									.lookup("//" + predecessor + ":" + port
											+ "/update");
							send.removeQueue(QPattern);
						} catch (Exception ex) {
						}
					}

					if (!prePredecessor.equals(NodeIP)) {
						try {
							BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
									.lookup("//" + prePredecessor + ":" + port
											+ "/update");
							send.removeQueue(QPattern);
						} catch (Exception ex) {
						}
					}

					if (Queues.containsKey(q.pattern)) {
						removeQueueFromQueues(q.pattern);
					}
				}
			}

			if (createdQueueSuccessfully) {
				addQueueToListOfQueuesInSystem(QPattern);
			}

			DisplayNodeStats();
			return getServices(username);
		}
	}

	@Override
	// add queue to predecessor and prepredecessor
	public boolean sendQueueToBackup(Queue q) throws RemoteException {
		addQueueToQueues(q.pattern, q);
		DisplayNodeStats();
		return true;

	}

	@Override
	// remove queue from predecessor and prepredecessor
	public void removeQueue(String QPattern) throws RemoteException {
		if (Queues.containsKey(QPattern)) {
			removeQueueFromQueues(QPattern);
			DisplayNodeStats();
		}

	}

	@Override
	// add queue to list of all queues in the system
	public void addQueueToListOfQueuesInSystem(String QPattern)
			throws RemoteException {

		if (!QueuesInSystem.contains(QPattern)) {
			QueuesInSystem.add(QPattern);

			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				send.addQueueToListOfQueuesInSystem(QPattern);
			} catch (Exception ex) {
			}
		}
	}

	@Override
	// remove queue from list of all queues in the system
	public void removeQueueFromListOfQueuesInSystem(String QPattern)
			throws RemoteException {
		if (QueuesInSystem.contains(QPattern)) {
			QueuesInSystem.remove(QPattern);

			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				send.removeQueueFromListOfQueuesInSystem(QPattern);
			} catch (Exception ex) {
			}
		}
	}

	@Override
	// unused in application, created for framework
	public void sendupdates() throws RemoteException {
		if (!ConnectedUsers.isEmpty()) {
			System.out.println(ConnectedUsers);
			System.out
					.println("Queues Have been updated, sending update to connected users");
			for (String user : ConnectedUsers.keySet()) {
				try {
					System.out.println("sending update to " + user + " at "
							+ ConnectedUsers.get(user));
					BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
							.lookup("//" + ConnectedUsers.get(user) + ":"
									+ port + "/update");
					send.setQueuesInSystemToConnectedUsers(QueuesInSystem);
					System.out.println("sent update to " + user);
				} catch (Exception ex) {
				}
			}
		}
	}

	// implemented at user, called when new message needs to be published from
	// server to user
	// unused in application, created for framework
	public void pushQueueData(String QPattern, String Message)
			throws RemoteException {

	}

	@Override
	// called by user to get their subscription list
	public ArrayList<String> getMySubscriptions(String username)
			throws RemoteException {
		ArrayList<String> subscriptions = new ArrayList<>();
		try {
			BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
					.lookup("//" + predecessor + ":" + port + "/update");
			subscriptions = send.getSubscriptionListOfUser(username, NodeIP,
					subscriptions);
		} catch (Exception ex) {
		}
		return subscriptions;

	}

	@Override
	// server-server message callback to gather list of subscriptions for a
	// particular user
	public ArrayList<String> getSubscriptionListOfUser(String username,
			String CallerIP, ArrayList<String> subs) throws RemoteException {
		if (NodeIP.equals(CallerIP)) {
			for (String qp : Queues.keySet()) {
				if (isWithinRange(HashOf(qp))) {
					if (Queues.get(qp).Subscribers.containsKey(username)) {
						subs.add(qp);
					}
				}
			}
			return subs;
		} else {
			for (String qp : Queues.keySet()) {
				if (isWithinRange(HashOf(qp))) {
					if (Queues.get(qp).Subscribers.containsKey(username)) {
						subs.add(qp);
					}
				}
			}
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				subs = send.getSubscriptionListOfUser(username, CallerIP, subs);
			} catch (Exception ex) {
				// System.out
				// .println("Connection Not Establisted!!! add queue to list");
				// ex.printStackTrace();
			}
			return subs;
		}
	}

	@Override
	// unused in application, created for framework
	public void setQueuesInSystemToConnectedUsers(ArrayList<String> QIS)
			throws RemoteException {
		// should be called from server remotely on connected user, send current
		// queues in system
	}

	// method to refresh list of queues in the system
	public static void QueueRegenerate() {
		try {
			QueuesInSystem = MyState.RegenerateQueuesInSystem();
		} catch (RemoteException e) {
		}
	}

	@Override
	// callback method to refresh list of queues in the system
	public ArrayList<String> RegenerateQueuesInSystem() throws RemoteException {
		ArrayList<String> QIS = new ArrayList<>();
		try {
			BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
					.lookup("//" + predecessor + ":" + port + "/update");
			QIS = send.Regenerate(NodeIP, QIS);
		} catch (Exception ex) {
		}
		return QIS;

	}

	@Override
	// server-server message callback to gather list of subscriptions for a
	// particular user
	public ArrayList<String> Regenerate(String CallerIP, ArrayList<String> QIS)
			throws RemoteException {
		if (NodeIP.equals(CallerIP)) {
			for (String qp : Queues.keySet()) {
				if (isWithinRange(HashOf(qp))) {
					if (!QIS.contains(qp)) {
						QIS.add(qp);
					}
				}
			}
			return QIS;
		} else {
			for (String qp : Queues.keySet()) {
				if (isWithinRange(HashOf(qp))) {
					if (!QIS.contains(qp)) {
						QIS.add(qp);
					}
				}
			}
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				QIS = send.Regenerate(CallerIP, QIS);
			} catch (Exception ex) {
			}
			return QIS;
		}
	}

	/**
	 * Nested class implementing runnable to create a thread to keep the server
	 * running at all times till specifically stopped.
	 */
	public static class KeepAlive implements Runnable {

		KeepAlive() {
		}

		@Override
		public void run() {
			while (true) {
				try {
					Thread.sleep(1500);
				} catch (InterruptedException ex) {
				}
				try {
					QueueRegenerate();
				} catch (Exception e) {

				}
			}
		}
	}

	/**
	 * Nested class implementing runnable to create a thread to publish new
	 * messages to all subscribed and connected users of a queue as and when the
	 * messages are published by a user to that queue. Method uses TCP to
	 * publish the messages to the subscribers.
	 */
	public static class Broadcaster implements Runnable {
		Broadcaster() {

		}

		public void run() {
			while (true) {
				synchronized (msgobj) {
					try {
						ArrayList<String> messages;
						if (!newMessages.isEmpty()) {
							for (String QPattern : newMessages.keySet()) {
								messages = newMessages.get(QPattern);
								for (int i = 0; i < messages.size(); i++) {
									for (String subs : Queues.get(QPattern).Subscribers
											.keySet()) {
										try {
											MyState.sendMessage(Queues
													.get(QPattern).Subscribers
													.get(subs), portOnClient,
													QPattern, messages.get(i));

										} catch (Exception ex) {
										}
									}
								}
							}
						}
					} catch (Exception e) {
					}
					newMessages = new HashMap<>();
				}
				try {
					Thread.sleep(2000);
				} catch (InterruptedException e) {
				}
			}
		}
	}

	// method called to initiate a thread to send data to the subscriber
	public void sendMessage(String userIP, int userPort, String QPattern,
			String Message) {
		Thread msg = new Thread(new sendmessagetoclient(userIP, userPort,
				QPattern, Message));
		msg.start();
	}

	/**
	 * Nested class implements runnable to create a new thread every time a new
	 * message is to be sent to the connected subscriber. The thread creates a
	 * new TCP connection with the subscriber, forwards the message and ends the
	 * connection.
	 */
	public static class sendmessagetoclient implements Runnable {
		String IP = "";
		int p = 0;
		String Pattern = "";
		String Msg = "";

		sendmessagetoclient() {
		}

		sendmessagetoclient(String ClientIP, int ClientPort, String QPattern,
				String Message) {
			this.IP = ClientIP;
			this.p = ClientPort;
			this.Pattern = QPattern;
			this.Msg = Message;
		}

		public void run() {
			try {
				InetAddress ip = InetAddress.getByName(IP);
				String host = ip.getHostName();
				Socket client = new Socket(host, p);
				OutputStream outToServer = client.getOutputStream();
				DataOutputStream out = new DataOutputStream(outToServer);
				out.writeUTF(Pattern + "###" + Msg);
				client.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block

			}
		}
	}

	/**
	 * Inner class extending timer task to send heart beat and check if
	 * predecessor is alive.
	 */
	public class HeartBeatSchedule extends TimerTask {
		@Override
		public void run() {
			int counter = 0;
			// run only if total number of servers in the system is greater than
			// 1
			if (!NodeIP.equals(predecessor)) {
				while (true) {
					try {
						BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
								.lookup("//" + predecessor + ":" + port
										+ "/update");
						send.HeartBeat();
						break;
					} catch (Exception e) {
						counter++;
						System.out.println("Heart Beat FAILURE... " + counter);
						if (counter == 3) {
							break;
						}
					}
				}

				if (counter == 3) {
					removeNode();
				}
			}
		}

	}

	@Override
	public void HeartBeat() throws RemoteException {
		// called by remote server
		// run in try catch
		// will go to catch at caller if this server is disconnected
	}

	@Override
	// remote method to display the stats of the current server
	public String printDetails() throws RemoteException {
		DisplayNodeStats();
		return null;
	}

	@Override
	// returns the current user load (connected users count) at the current
	// server
	public int getCurrentLoad() throws RemoteException {

		if (ConnectedUsers.isEmpty()) {
			return 0;
		} else {
			return ConnectedUsers.size();
		}

	}

	@Override
	// called by bootstrap to add a new user to the current server
	public String addConnectedUser(String username, String hostAddress) {

		ConnectedUsers.put(username, hostAddress);
		DisplayNodeStats();

		return null;
	}

	@Override
	// Sreturns a list of all active queues currently in the system
	public ArrayList<String> getQueuesInSystem(String username)
			throws RemoteException {
		return QueuesInSystem;
	}

	@Override
	// returns a list of all the queues that the current user is unsubscribed to
	public ArrayList<String> getUnsubscribedQueues(String username)
			throws RemoteException {
		ArrayList<String> AllQueues = new ArrayList<>();
		ArrayList<String> MySubscriptions = new ArrayList<>();
		ArrayList<String> MyPublications = new ArrayList<>();

		AllQueues = getQueuesInSystem(username);
		MySubscriptions = getMySubscriptions(username);
		MyPublications = getServices(username);

		for (String qp : MySubscriptions) {
			if (AllQueues.contains(qp)) {
				AllQueues.remove((String) qp);
			}
		}

		for (String qp : MyPublications) {
			if (AllQueues.contains(qp)) {
				AllQueues.remove((String) qp);
			}
		}
		return AllQueues;

	}

	@Override
	// called by the user to subscribe to a new queue
	public boolean subscribe(String username, String QPattern)
			throws RemoteException {
		return subscribeMe(username, ConnectedUsers.get(username), QPattern);
	}

	@Override
	// adds user as a subscriber to the queue. called at backup queues
	public void addSubscriber(String username, String userIP, String QPattern)
			throws RemoteException {
		if (Queues.containsKey(QPattern)) {
			if (!Queues.get(QPattern).Subscribers.containsKey(username)) {
				Queues.get(QPattern).Subscribers.put(username, userIP);

			}
		}

	}

	@Override
	// callback method to add user as subscriber to the queue and in the backup
	// queues.
	public boolean subscribeMe(String username, String userIP, String QPattern)
			throws RemoteException {

		if (isWithinRange(HashOf(QPattern))) {

			if (!Queues.get(QPattern).Subscribers.containsKey(username)) {
				Queues.get(QPattern).Subscribers.put(username, userIP);
			} else {
				return false;
			}

			boolean Success = true;

			// send to backups
			try {

				BootStrap2ServerInterface send1 = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				send1.addSubscriber(username, userIP, QPattern);

				BootStrap2ServerInterface send2 = (BootStrap2ServerInterface) Naming
						.lookup("//" + prePredecessor + ":" + port + "/update");
				send2.addSubscriber(username, userIP, QPattern);

			} catch (Exception ex) {
				Success = false;
			}
			DisplayNodeStats();

			return Success;
		} else {
			boolean result = false;
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				result = send.subscribeMe(username, userIP, QPattern);

			} catch (Exception ex) {
			}

			return result;
		}
	}

	@Override
	// called by a subscribed user to unsubscribe from a queue
	public boolean unsubscribe(String username, String QPattern)
			throws RemoteException {
		return unsubscribeMe(username, ConnectedUsers.get(username), QPattern);
	}

	@Override
	// method removes user from a queue's subscribed users list. called at
	// backup queues.
	public void removeSubscriber(String username, String userIP, String QPattern)
			throws RemoteException {
		if (Queues.containsKey(QPattern)) {
			if (Queues.get(QPattern).Subscribers.containsKey(username)) {
				Queues.get(QPattern).Subscribers.remove(username);

			}
		}

	}

	@Override
	// callback method to unsubscribe a user from the queue and its backups.
	public boolean unsubscribeMe(String username, String userIP, String QPattern)
			throws RemoteException {

		if (isWithinRange(HashOf(QPattern))) {

			if (Queues.get(QPattern).Subscribers.containsKey(username)) {
				Queues.get(QPattern).Subscribers.remove(username);
			} else {
				return false;
			}

			boolean Success = true;

			// send to backups
			try {

				BootStrap2ServerInterface send1 = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				send1.removeSubscriber(username, userIP, QPattern);

				BootStrap2ServerInterface send2 = (BootStrap2ServerInterface) Naming
						.lookup("//" + prePredecessor + ":" + port + "/update");
				send2.removeSubscriber(username, userIP, QPattern);

			} catch (Exception ex) {
				Success = false;
			}

			DisplayNodeStats();

			return Success;
		} else {
			boolean result = false;
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				result = send.unsubscribeMe(username, userIP, QPattern);

			} catch (Exception ex) {
			}

			return result;

		}
	}

	@Override
	// remove a user from the list of connected users in case of user connection
	// failure
	public void disconnect(String username, String CallerIP)
			throws RemoteException {
		if (ConnectedUsers.containsKey(username)) {
			try {
				LogOut(username);
			} catch (RemoteException e) {
			}
		} else {
			if (!NodeIP.equals(CallerIP)) {
				try {
					BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
							.lookup("//" + predecessor + ":" + port + "/update");
					send.disconnect(username, CallerIP);

				} catch (Exception ex) {
				}
			}
		}
	}

	@Override
	// called by user to publish a new message to their queue
	public boolean publishNewMessage(String username, String qPattern,
			String publishMsg) throws RemoteException {

		int QPatternHash = HashOf(qPattern);

		if (isWithinRange(QPatternHash)) {

			Queues.get(qPattern).data.add(publishMsg);
			if (Queues.get(qPattern).data.size() > Queues.get(qPattern).capacity) {
				Queues.get(qPattern).data.remove(0);
			}
			synchronized (msgobj) {
				if (!newMessages.containsKey(qPattern)) {
					newMessages.put(qPattern, new ArrayList<String>());
				}
				newMessages.get(qPattern).add(publishMsg);
			}
			boolean Success = true;

			// send to backups
			try {

				BootStrap2ServerInterface send1 = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				send1.addData(qPattern, publishMsg);

				BootStrap2ServerInterface send2 = (BootStrap2ServerInterface) Naming
						.lookup("//" + prePredecessor + ":" + port + "/update");
				send2.addData(qPattern, publishMsg);

			} catch (Exception ex) {
				Success = false;
			}

			DisplayNodeStats();

			return Success;
		} else {
			boolean result = false;
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				result = send.publishNewMessage(username, qPattern, publishMsg);

			} catch (Exception ex) {
			}
			return result;
		}
	}

	@Override
	// add data to the queue. called at backups
	public void addData(String QPattern, String QMessage)
			throws RemoteException {
		Queues.get(QPattern).data.add(QMessage);
		if (Queues.get(QPattern).data.size() > Queues.get(QPattern).capacity) {
			Queues.get(QPattern).data.remove(0);
		}
		DisplayNodeStats();
	}

	@Override
	// method returns the list of all queues created by the calling user
	public ArrayList<String> getServices(String username)
			throws RemoteException {
		ArrayList<String> publications = new ArrayList<>();
		try {
			BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
					.lookup("//" + predecessor + ":" + port + "/update");
			publications = send.getPublicationsOf(username, NodeIP,
					publications);
		} catch (Exception ex) {
		}
		return publications;
	}

	@Override
	// callback method to get the publication list of a user
	public ArrayList<String> getPublicationsOf(String username,
			String CallerIP, ArrayList<String> pubs) throws RemoteException {
		if (NodeIP.equals(CallerIP)) {
			for (String qp : Queues.keySet()) {
				if (isWithinRange(HashOf(qp))) {
					if (Queues.get(qp).publisher.equals(username)) {
						pubs.add(qp);
					}
				}
			}
			return pubs;
		} else {
			for (String qp : Queues.keySet()) {
				if (isWithinRange(HashOf(qp))) {
					if (Queues.get(qp).publisher.equals(username)) {
						pubs.add(qp);
					}
				}
			}
			try {
				BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
						.lookup("//" + predecessor + ":" + port + "/update");
				pubs = send.getPublicationsOf(username, CallerIP, pubs);
			} catch (Exception ex) {
			}
			return pubs;
		}
	}

	@Override
	// method to log out a user from the system
	public void LogOut(String username) throws RemoteException {
		ConnectedUsers.remove(username);
		try {
			BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming
					.lookup("//" + BootstrapIP + ":" + port + "/update");
			send.logOutUser(username);
		} catch (Exception ex) {
		}
		DisplayNodeStats();
	}

	@Override
	// method used to tell bootstrap that the server is alive
	public void imAlive() throws RemoteException {
	}

	@Override
	public String RemoteLogin(String username, String password, String userIP)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String RemoteSignup(String username, String password, String userIP)
			throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void addToNetwork(String nodeIP, int IPhash) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public String removeNode(int iphash) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void logOutUser(String username) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void addDataToQueue(String QPattern, String Message)
			throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void createQueueAtBootStrap(String username, String QName,
			String QPattern) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteQueueAtBootStrap(String QPattern) throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public void autoheal() throws RemoteException {
		// TODO Auto-generated method stub

	}

	@Override
	public String ReconnectUser(String username) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

}
