import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.EventQueue;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import javax.swing.JLabel;

import java.awt.Font;

import javax.swing.JButton;

import java.awt.FlowLayout;

import javax.swing.JInternalFrame;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

import java.io.*;
import java.net.*;

public class LoginWindow extends JFrame implements BootStrap2ServerInterface, Serializable{

	private JPanel contentPane;

	
	
	private JTextField txtAditya;
	private JPasswordField passwordField;

	
	//RMI variables
	static Registry registry = null;
	static final int port = 7394;
	static String bootstrap = "129.21.22.196"; 
	static int bootstrapPort =  7394;
	static String poiServer = "";

	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
//					BunnyME_LoginWindow frame = new BunnyME_LoginWindow();
//					frame.setVisible(true);
				} catch (Exception e) {
//					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public LoginWindow() throws RemoteException {

		// Start registry
		try {

			registry = LocateRegistry.getRegistry(port);// use any no. less than
														// 55000
			registry.list();
			registry.rebind("update", this);
		} catch (Exception e) {
			registry = LocateRegistry.createRegistry(port);
			registry.rebind("update", this);
		}
		
		
		//UI components
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 1353, 789);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		
		
		
		
		JLabel label_LoginDetails = new JLabel("BunnyME");
		label_LoginDetails.setBounds(453, 96, 260, 82);
		label_LoginDetails.setForeground(new Color(0, 0, 0));
		label_LoginDetails.setBackground(new Color(175, 238, 238));
		label_LoginDetails.setFont(new Font("Tahoma", Font.PLAIN, 40));
		contentPane.add(label_LoginDetails);
		
		JLabel label_UserName = new JLabel("User Name:");
		label_UserName.setBounds(242, 259, 174, 68);
		label_UserName.setFont(new Font("Tahoma", Font.PLAIN, 30));
		contentPane.add(label_UserName);
		
		JLabel label_Password = new JLabel("Password:");
		label_Password.setBounds(242, 332, 179, 68);
		label_Password.setFont(new Font("Tahoma", Font.PLAIN, 30));
		contentPane.add(label_Password);
		
		txtAditya = new JTextField();
		txtAditya.setBounds(474, 262, 387, 46);
		txtAditya.setFont(new Font("Tahoma", Font.PLAIN, 30));
		contentPane.add(txtAditya);
		txtAditya.setColumns(10);
		
		
		passwordField = new JPasswordField();
		passwordField.setBounds(474, 347, 387, 46);
		passwordField.setToolTipText("");
		passwordField.setFont(new Font("Tahoma", Font.PLAIN, 30));
		contentPane.add(passwordField);
		
		JLabel label_LoginStatus = new JLabel("");
		label_LoginStatus.setBounds(474, 414, 760, 43);
		label_LoginStatus.setFont(new Font("Tahoma", Font.PLAIN, 30));
		contentPane.add(label_LoginStatus);
	
		
		JButton button_Login = new JButton("Login");
		button_Login.setBounds(546, 485, 174, 57);
		
		
		
		
		button_Login.setBackground(new Color(0, 255, 127));
		button_Login.setFont(new Font("Tahoma", Font.PLAIN, 30));
		contentPane.add(button_Login);
		
		JButton button_SignUp = new JButton("Sign up");
		button_SignUp.setBounds(546, 553, 174, 57);

		button_SignUp.setFont(new Font("Tahoma", Font.PLAIN, 30));
		button_SignUp.setBackground(new Color(0, 255, 127));
		contentPane.add(button_SignUp);
		
		
		
		
		
		
		
		
		button_Login.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				
				
				String username = txtAditya.getText();
				String password = passwordField.getText();
			
				
				//checking at the cleint side
				if(password.equals("") && username.equals(""))
				{
					label_LoginStatus.setText("Please Enter Username and Password");
				}
				else
				{				
					if(password.equals(""))
					{
						label_LoginStatus.setText("Please Enter Password");
					}
					else
					{
						if(username.equals(""))
						{
							label_LoginStatus.setText("Please Enter Username");
						}
						else
						{
							System.out.println("Login: " + username);
							
							
							//RMI call to authenticate the user
							try 
							{
								BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + bootstrap + ":" + bootstrapPort + "/update");
								String result = send.RemoteLogin(username, password, InetAddress.getLocalHost().getHostAddress());
								
								
								
								if(result.equals("wrong username/password"))
								{
									System.out.println("Wrong Username or Password try again");
									label_LoginStatus.setText("Wrong Username/Password! Try again.");
								}
								else
								{
									poiServer = result;
									System.out.println("serverIP: " + poiServer);
									
									
										BootStrap2ServerInterface fetch = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
										ArrayList<String> QueueList = fetch.getServices(username);
										
										
										System.out.println("Queue List Received: " + QueueList);
									
									Pub_Sub ps = new Pub_Sub(poiServer, username, QueueList);
									ps.setVisible(true);
									dispose();	
								}
								
							} 
							catch (Exception e) 
							{
								System.out.println("Connection Not Establisted!!!");
								label_LoginStatus.setText("Connection Error...");
//								e.printStackTrace();
							}
	
						}	
					}
				}
			}
		});

		
		
		
		button_SignUp.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				
				
				String username = txtAditya.getText();
				String password = passwordField.getText();
				
				//checking at the client side
				if(password.equals("") && username.equals(""))
				{
					label_LoginStatus.setText("Please Enter Username and Password");
				}
				else
				{
					if(password.equals(""))
					{
						label_LoginStatus.setText("Please Enter Password");
					}
					else
					{
						if(username.equals(""))
						{
							label_LoginStatus.setText("Please Enter Username");
						}
						else
						{
							System.out.println("Sign up: " + username);
							
							//RMI call to register the user
							try 
							{
								BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + bootstrap + ":" + bootstrapPort + "/update");
								String result = send.RemoteSignup(username, password, InetAddress.getLocalHost().getHostAddress());
								
								if(result.equals("username exists"))
								{
									System.out.println("Username Exits try again");
									label_LoginStatus.setText("Username Already Exists! Try Login.");
								}
								else
								{
									poiServer = result;
									System.out.println("serverIP: " + poiServer);
									
									
									ArrayList<String> QueueList = new ArrayList<>();
								
									Pub_Sub ps = new Pub_Sub(poiServer, username, QueueList);
									ps.setVisible(true);
									dispose();
									
								}
								
								
							} 
							catch (Exception ex) 
							{
								System.out.println("Connection Not Establisted!!!");
								label_LoginStatus.setText("Connection Error...");
	//							ex.printStackTrace();
							}
	
						}
					}
				}
				
			}
		});


		
	}

	@Override
	public void addToNetwork(String nodeIP, int IPhash) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

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
	public String printDetails() throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String removeNode(int iphash) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getCurrentLoad() throws RemoteException {
		// TODO Auto-generated method stub
		return 0;
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
	public ArrayList<String> createQueue(String username, String QName,
			String QPattern) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String addConnectedUser(String username, String hostAddress)
			throws RemoteException {
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
	public void LogOut(String username) throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<String> deleteQueue(String username, String QPattern)
			throws RemoteException {
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
	public boolean subscribe(String username, String qPattern)
			throws RemoteException {
		// TODO Auto-generated method stub
		return false;
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
	public void pushQueueData(String QName, String Message)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}


	@Override
	public String ReconnectUser(String username) throws RemoteException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void disconnect(String username, String CallerIP)
			throws RemoteException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public ArrayList<String> ShiftQueue(String username, String qPattern)
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

}
