import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Interface BootStrap2ServerInterface is a common interface for remote object.
 * this interface must be extended by the servers, bootstrap and clients. it
 * contains all the methods used for RMI calls.
 * 
 * @author Aditya Advani
 * @author Ankita Sambhare
 * 
 * @version May 13, 2016
 * 
 */
public interface BootStrap2ServerInterface extends Remote {

	public void addToNetwork(String nodeIP, int IPhash) throws RemoteException;

	public String InsertFirstNode() throws RemoteException;

	public String InsertionNewNode(String ip, int nodehash, String startNodeIP)
			throws RemoteException;

	public String printDetails() throws RemoteException;

	public String removeNode(int iphash) throws RemoteException;

	public int getCurrentLoad() throws RemoteException;

	public String RemoteLogin(String username, String password, String userIP)
			throws RemoteException;

	public String RemoteSignup(String username, String password, String userIP)
			throws RemoteException;

	public ArrayList<String> createQueue(String username, String QName,
			String QPattern) throws RemoteException;

	public String addConnectedUser(String username, String hostAddress)
			throws RemoteException;

	void addMeToChord(String ip, int nodehash, String startNodeIP)
			throws RemoteException;

	void setPredecessor(String pre) throws RemoteException;

	void setSuccessor(String suc) throws RemoteException;

	String getPredecessor() throws RemoteException;

	String getSuccessor() throws RemoteException;

	public int getHashValue() throws RemoteException;

	void setUpperLimit(int uLimit) throws RemoteException;

	int getUpperLimit() throws RemoteException;

	void setLowerLimit(int lLimit) throws RemoteException;

	int getLowerLimit() throws RemoteException;

	public void getQueues(HashMap<String, Queue> Q) throws RemoteException;

	void HeartBeat() throws RemoteException;

	HashMap<String, Queue> getQueuesNotInRange() throws RemoteException;

	void setPrePredecessor(String prePre) throws RemoteException;

	String getPrePredecessor() throws RemoteException;

	boolean sendQueueToBackup(Queue q) throws RemoteException;

	void removeQueue(String QPattern) throws RemoteException;

	ArrayList<String> deleteQueue(String username, String QPattern)
			throws RemoteException;

	void addQueueToListOfQueuesInSystem(String QPattern) throws RemoteException;

	void removeQueueFromListOfQueuesInSystem(String QPattern)
			throws RemoteException;

	void addUpdatedQueues(HashMap<String, Queue> BackupQueues)
			throws RemoteException;

	void updateBackups() throws RemoteException;

	void setHashValue(int hash) throws RemoteException;

	String getName() throws RemoteException;

	void removeTheseQueues(HashMap<String, Queue> rmQueue)
			throws RemoteException;

	HashMap<String, Queue> getQueuesInRange() throws RemoteException;

	boolean publishNewMessage(String username, String qPattern,
			String publishMsg) throws RemoteException;

	void setQueuesInSystemToConnectedUsers(ArrayList<String> QIS)
			throws RemoteException;

	ArrayList<String> getQueuesInSystem(String username) throws RemoteException;

	void sendupdates() throws RemoteException;

	ArrayList<String> getSubscriptionListOfUser(String username,
			String CallerIP, ArrayList<String> subs) throws RemoteException;

	ArrayList<String> getMySubscriptions(String username)
			throws RemoteException;

	void addData(String QPattern, String QMessage) throws RemoteException;

	ArrayList<String> getPublicationsOf(String username, String CallerIP,
			ArrayList<String> pubs) throws RemoteException;

	ArrayList<String> getServices(String username) throws RemoteException;

	void LogOut(String username) throws RemoteException;

	void logOutUser(String username) throws RemoteException;

	void addDataToQueue(String QPattern, String Message) throws RemoteException;

	ArrayList<String> ShiftQueue(String username, String QPattern)
			throws RemoteException;

	ArrayList<String> shiftDataQueue(String username, String QPattern,
			String CallerIP) throws RemoteException;

	void createQueueAtBootStrap(String username, String QName, String QPattern)
			throws RemoteException;

	void deleteQueueAtBootStrap(String QPattern) throws RemoteException;

	void autoheal() throws RemoteException;

	void imAlive() throws RemoteException;

	ArrayList<String> getUnsubscribedQueues(String username)
			throws RemoteException;

	void addSubscriber(String username, String userIP, String QPattern)
			throws RemoteException;

	boolean subscribeMe(String username, String userIP, String QPattern)
			throws RemoteException;

	boolean subscribe(String username, String QPattern) throws RemoteException;

	boolean unsubscribe(String username, String QPattern)
			throws RemoteException;

	void removeSubscriber(String username, String userIP, String QPattern)
			throws RemoteException;

	boolean unsubscribeMe(String username, String userIP, String QPattern)
			throws RemoteException;

	ArrayList<String> RegenerateQueuesInSystem() throws RemoteException;

	ArrayList<String> Regenerate(String CallerIP, ArrayList<String> QIS)
			throws RemoteException;

	public void pushQueueData(String QPattern, String Message)
			throws RemoteException;

	String ReconnectUser(String username) throws RemoteException;

	void disconnect(String username, String CallerIP) throws RemoteException;

}