import java.awt.BorderLayout;
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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.AccessException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JList;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JTextArea;
import javax.swing.LayoutStyle.ComponentPlacement;

import java.awt.event.WindowStateListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowAdapter;


public class Pub_Sub extends JFrame implements BootStrap2ServerInterface, Serializable{

	private JPanel contentPane;
	private JTextField textField_Message;
	private JTextField textField_QueueName;
	static JButton button_subscriber_Tab;
	
	static HashMap<String,ArrayList<String>> ServiceMessageData = new HashMap<>();
	
	//rmi related variables
	static String poiServer = ""; 
	static String username = "";
	static final int port = 7394;
	static String bootstrap = "129.21.22.196"; 
	static int bootstrapPort =  7394;
	static Registry registry;

	// poiServer HeartBeat check timer task
	static Timer heartbeatTimer;
	
	//counter for notifications
	static int unreadMessageCounter = 0;
	
	
	JList list_ServicesList;
	static ArrayList<String> myPublishedServices = new ArrayList<String>();
	
	//tcp for notifications
	static int tcpPort = 6162;
	static ServerSocket ss;
	   
	
	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
//					Pub_Sub frame = new Pub_Sub();
//					System.out.println("LOGIN WINDOW POI: " + BunnyME_Client.poiServer);
//					
//					frame.setVisible(true);
				} catch (Exception e) {
//					e.printStackTrace();
				}
			}
		});
	}

	
//	public Pub_Sub()
//	{
//	}
	
	/**
	 * Create the frame.
	 * @param queueList 
	 * @throws RemoteException 
	 * @throws AccessException 
	 */
	public Pub_Sub(String poi, String user, ArrayList<String> queueList) throws AccessException, RemoteException {
				
		//parameter received form the login window
		poiServer = poi;
		username = user;
		
		System.out.println("poiServer: " + poiServer);
		System.out.println("username: " + username);
		
		//tcp listening thread for notifications
		Thread heartbeat = new Thread(new KeepAlive());
		heartbeat.start();

		
		
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
		setBounds(100, 100, 1479, 833);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);
		
		JLabel label_BunnyMe = new JLabel("Bunny ME");
		label_BunnyMe.setBounds(638, 10, 176, 49);
		label_BunnyMe.setFont(new Font("Tahoma", Font.PLAIN, 40));
		contentPane.add(label_BunnyMe);
		
		JPanel panel_Parent = new JPanel();
		panel_Parent.setBounds(0, 65, 1453, 66);
		contentPane.add(panel_Parent);
		panel_Parent.setLayout(null);
		
				
				
				JPanel panel_Child = new JPanel();
				panel_Child.setBounds(0, 127, 1453, 635);
				contentPane.add(panel_Child);
				panel_Child.setLayout(null);
				
				JPanel panel_Welcome = new JPanel();
				panel_Welcome.setLayout(null);
				panel_Welcome.setBounds(0, 0, 1452, 507);
				
				
				JLabel label = new JLabel("Welcome To Bunny ME, Please select either publisher or subscriber to proceed.");
				label.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label.setBounds(169, 102, 1107, 180);
				panel_Welcome.add(label);
				
				
				
				JPanel panel_Publisher = new JPanel();
				panel_Publisher.setBounds(0, 0, 1452, 635);
				panel_Publisher.setLayout(null);
				
				
				JPanel panel_Subscriber = new JPanel();
				panel_Subscriber.setBounds(0, 21, 1452, 614);
				
				
				JPanel panel_left = new JPanel();
				panel_left.setBounds(0, 0, 394, 635);
				panel_left.setLayout(null);
				
				JPanel panel_ServiceList = new JPanel();
				panel_ServiceList.setBounds(0, 0, 394, 498);
				
				panel_ServiceList.setLayout(null);
				
				JPanel panel_ServiceListInner = new JPanel();
				panel_ServiceListInner.setBounds(21, 10, 373, 470);
				panel_ServiceList.add(panel_ServiceListInner);
				panel_ServiceListInner.setLayout(null);
				
				list_ServicesList = new JList();
				list_ServicesList.setBounds(33, 67, 308, 387);
				panel_ServiceListInner.add(list_ServicesList);
				list_ServicesList.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				
				JLabel lblListOfServices = new JLabel("   List of Services:");
				lblListOfServices.setBounds(0, 21, 352, 37);
				panel_ServiceListInner.add(lblListOfServices);
				lblListOfServices.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				
				JPanel panel_ServiceDetails_Inner = new JPanel();
				panel_ServiceDetails_Inner.setBounds(21, 0, 373, 234);
				//panel_ServiceDetails.add(panel_ServiceDetails_Inner);
				panel_ServiceDetails_Inner.setLayout(null);
				
				JLabel lblServiceDetails = new JLabel("   Service Details:");
				lblServiceDetails.setBounds(0, 21, 333, 37);
				panel_ServiceDetails_Inner.add(lblServiceDetails);
				lblServiceDetails.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				JList list_ServiceDetails = new JList();
				list_ServiceDetails.setBounds(47, 79, 305, 142);
				panel_ServiceDetails_Inner.add(list_ServiceDetails);
				
				list_ServiceDetails.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				
				JButton btnUpdateQ = new JButton("Update Q");
				
				btnUpdateQ.setBounds(60, 569, 298, 45);
				btnUpdateQ.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				JButton button_Message = new JButton("Message");
				button_Message.setBounds(60, 519, 298, 45);
				
				button_Message.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				JPanel panel_right = new JPanel();
				panel_right.setBounds(419, 32, 1012, 582);
				
				panel_right.setLayout(null);
				
				JPanel panel_updateService = new JPanel();
				panel_updateService.setBounds(21, 21, 980, 540);
				//
				panel_updateService.setLayout(null);
				
				JLabel label_UpdateService = new JLabel("Update Service");
				label_UpdateService.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_UpdateService.setBounds(355, 0, 251, 37);
				panel_updateService.add(label_UpdateService);
				
				JPanel panel_updateService_Tabs = new JPanel();
				panel_updateService_Tabs.setBounds(0, 52, 980, 44);
				panel_updateService.add(panel_updateService_Tabs);
				panel_updateService_Tabs.setLayout(null);
//				panel_right.add(panel_updateService);
				
				JButton button_Add_Tab = new JButton("Add");
				
				button_Add_Tab.setBounds(0, 0, 492, 45);
				panel_updateService_Tabs.add(button_Add_Tab);
				button_Add_Tab.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				JButton button_Remove_Tab = new JButton("Remove");
				
				button_Remove_Tab.setFont(new Font("Tahoma", Font.PLAIN, 30));
				button_Remove_Tab.setBounds(488, 0, 492, 45);
				panel_updateService_Tabs.add(button_Remove_Tab);
				
				JPanel panel_updateService_Content = new JPanel();
				panel_updateService_Content.setBounds(0, 96, 980, 444);
				panel_updateService.add(panel_updateService_Content);
				panel_updateService_Content.setLayout(null);
				
				JPanel panel_RemoveQueue = new JPanel();
				panel_RemoveQueue.setBounds(33, 37, 914, 363);

				
				JButton button_Remove = new JButton("Remove");
				button_Remove.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				JLabel label_QueueRemoveMessage = new JLabel("");
				label_QueueRemoveMessage.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				JPanel panel_AddQueue = new JPanel();
				panel_AddQueue.setLayout(null);
				panel_AddQueue.setBounds(45, 48, 914, 363);
				
				
				
				JLabel label_QueueRemoveStatus = new JLabel("");
				label_QueueRemoveStatus.setFont(new Font("Tahoma", Font.PLAIN, 30));
				GroupLayout gl_panel_RemoveQueue = new GroupLayout(panel_RemoveQueue);
				gl_panel_RemoveQueue.setHorizontalGroup(
					gl_panel_RemoveQueue.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_RemoveQueue.createSequentialGroup()
							.addGroup(gl_panel_RemoveQueue.createParallelGroup(Alignment.LEADING)
								.addGroup(gl_panel_RemoveQueue.createSequentialGroup()
									.addGap(132)
									.addComponent(label_QueueRemoveMessage, GroupLayout.PREFERRED_SIZE, 702, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_panel_RemoveQueue.createSequentialGroup()
									.addGap(411)
									.addComponent(button_Remove, GroupLayout.PREFERRED_SIZE, 198, GroupLayout.PREFERRED_SIZE))
								.addGroup(gl_panel_RemoveQueue.createSequentialGroup()
									.addGap(166)
									.addComponent(label_QueueRemoveStatus, GroupLayout.PREFERRED_SIZE, 629, GroupLayout.PREFERRED_SIZE)))
							.addContainerGap(80, Short.MAX_VALUE))
				);
				gl_panel_RemoveQueue.setVerticalGroup(
					gl_panel_RemoveQueue.createParallelGroup(Alignment.LEADING)
						.addGroup(gl_panel_RemoveQueue.createSequentialGroup()
							.addGap(21)
							.addComponent(label_QueueRemoveMessage, GroupLayout.PREFERRED_SIZE, 165, GroupLayout.PREFERRED_SIZE)
							.addGap(39)
							.addComponent(label_QueueRemoveStatus, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
							.addGap(31)
							.addComponent(button_Remove, GroupLayout.PREFERRED_SIZE, 51, GroupLayout.PREFERRED_SIZE)
							.addContainerGap(10, Short.MAX_VALUE))
				);
				panel_RemoveQueue.setLayout(gl_panel_RemoveQueue);

				
				
				textField_QueueName = new JTextField();
				textField_QueueName.setFont(new Font("Tahoma", Font.PLAIN, 30));
				textField_QueueName.setColumns(10);
				textField_QueueName.setBounds(211, 128, 580, 51);
				panel_AddQueue.add(textField_QueueName);
				
				JButton button_Add = new JButton("Add");
				button_Add.setFont(new Font("Tahoma", Font.PLAIN, 30));
				button_Add.setBounds(411, 276, 198, 51);
				panel_AddQueue.add(button_Add);
				
				JLabel label_QueueName = new JLabel("Queue Name:");
				label_QueueName.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_QueueName.setBounds(112, 63, 209, 42);
				panel_AddQueue.add(label_QueueName);
				
				JLabel label_AddQueueStatus = new JLabel("");
				label_AddQueueStatus.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_AddQueueStatus.setBounds(221, 213, 570, 42);
				panel_AddQueue.add(label_AddQueueStatus);
				
				JPanel panel_SendMessage = new JPanel();
				panel_SendMessage.setBounds(49, 36, 916, 503);
				
				
				panel_SendMessage.setLayout(null);
				JLabel lblSendAMessage = new JLabel("Send a Message:");
				lblSendAMessage.setFont(new Font("Tahoma", Font.PLAIN, 30));
				lblSendAMessage.setBounds(112, 108, 284, 43);
				panel_SendMessage.add(lblSendAMessage);
				
				JButton button_SendMessage = new JButton("Send Message");
				
				button_SendMessage.setFont(new Font("Tahoma", Font.PLAIN, 30));
				button_SendMessage.setBounds(390, 345, 234, 48);
				panel_SendMessage.add(button_SendMessage);
				
				textField_Message = new JTextField();
				textField_Message.setFont(new Font("Tahoma", Font.PLAIN, 30));
				textField_Message.setBounds(213, 172, 626, 48);
				panel_SendMessage.add(textField_Message);
				textField_Message.setColumns(10);
				
				JLabel label_sendMessageStatus = new JLabel("");
				label_sendMessageStatus.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_sendMessageStatus.setBounds(266, 259, 559, 43);
				panel_SendMessage.add(label_sendMessageStatus);

				panel_Child.repaint();
				panel_Child.revalidate();

				
				JButton button_publisher_Tab = new JButton("Publisher");
				button_publisher_Tab.setBounds(0, 0, 734, 66);
				panel_Parent.add(button_publisher_Tab);
				
				
				button_publisher_Tab.setFont(new Font("Tahoma", Font.PLAIN, 30));
				button_subscriber_Tab = new JButton("Subscriber");
				button_subscriber_Tab.setBounds(733, 0, 725, 66);
				panel_Parent.add(button_subscriber_Tab);
				
				button_subscriber_Tab.setFont(new Font("Tahoma", Font.PLAIN, 30));
				
				panel_Subscriber.setLayout(null);
				
				JList list_SubscribedServices = new JList();
				list_SubscribedServices.setFont(new Font("Tahoma", Font.PLAIN, 30));
				list_SubscribedServices.setBounds(31, 110, 342, 343);
				
				
				JLabel label_SubscribedServices = new JLabel("Subscribed Services:");
				label_SubscribedServices.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_SubscribedServices.setBounds(31, 44, 342, 45);
				
				JButton button_Unsubscribe = new JButton("Unsubscribe");
				button_Unsubscribe.setFont(new Font("Tahoma", Font.PLAIN, 30));
				button_Unsubscribe.setBounds(31, 474, 342, 45);
				
				JButton button_AddNewSubscription = new JButton("Add New Subscription");
				button_AddNewSubscription.setFont(new Font("Tahoma", Font.PLAIN, 30));
				button_AddNewSubscription.setBounds(31, 540, 342, 45);
				
				
				JPanel panel_SubscribedMessges = new JPanel();
				panel_SubscribedMessges.setBounds(43, 33, 907, 485);
				
				panel_SubscribedMessges.setLayout(null);
				
				JLabel label_SubscribedMessages = new JLabel("Subscribed Messages:");
				label_SubscribedMessages.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_SubscribedMessages.setBounds(51, 21, 308, 37);
				panel_SubscribedMessges.add(label_SubscribedMessages);
				
				JList list_SubscribedMessages = new JList();
				list_SubscribedMessages.setFont(new Font("Tahoma", Font.PLAIN, 30));
				list_SubscribedMessages.setBounds(87, 79, 759, 351);
				panel_SubscribedMessges.add(list_SubscribedMessages);
				
				JPanel panel_AddNewSubscription = new JPanel();
				panel_AddNewSubscription.setBounds(21, 34, 970, 527);
				panel_AddNewSubscription.setLayout(null);
				
				JLabel label_UnsubscribedServices = new JLabel("Unsubscribed Services:");
				label_UnsubscribedServices.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_UnsubscribedServices.setBounds(53, 0, 359, 62);
				panel_AddNewSubscription.add(label_UnsubscribedServices);
				
				JList list_UnsubscribedServices = new JList();
				list_UnsubscribedServices.setFont(new Font("Tahoma", Font.PLAIN, 30));
				list_UnsubscribedServices.setBounds(33, 70, 518, 436);
				panel_AddNewSubscription.add(list_UnsubscribedServices);
				
				JButton button_Subscribe = new JButton("Subscribe");
				button_Subscribe.setFont(new Font("Tahoma", Font.PLAIN, 30));
				button_Subscribe.setBounds(633, 369, 258, 56);
				panel_AddNewSubscription.add(button_Subscribe);
				
				
				heartbeatTimer = new Timer();
				heartbeatTimer.schedule(new HeartBeatSchedule(), 5000, 5000);

				JButton btnLogout = new JButton("LogOut");
				btnLogout.setFont(new Font("Tahoma", Font.PLAIN, 30));
				btnLogout.setBounds(1215, 9, 199, 50);
				contentPane.add(btnLogout);
				
				
				JLabel label_SubscriptionStatus = new JLabel("");
				label_SubscriptionStatus.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_SubscriptionStatus.setBounds(598, 191, 320, 36);
				panel_AddNewSubscription.add(label_SubscriptionStatus);
				
				
				
				// logout user on closing the window RMI call
				addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent arg0) {

						try {
							
							
							BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
							send.LogOut(username);
							ss.close();
							
						} catch (Exception e) {
//							e.printStackTrace();
						}
						
						dispose();
					}
					@Override
					public void windowClosed(WindowEvent e) {
					}
				});

				
				
				
				JLabel label_userName = new JLabel("");
				label_userName.setFont(new Font("Tahoma", Font.PLAIN, 30));
				label_userName.setBounds(21, 10, 286, 45);
				contentPane.add(label_userName);
				
				label_userName.setText(username);
				
				
				//Initial Welcome layout
					panel_Child.add(panel_Welcome);
					

					
				//********************* Event Listeners *********************
					
					//logout 
					btnLogout.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
							
							try {
								
								//RMI call to the poi server
								BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
								send.LogOut(username);

								//navigate back to the login window
								dispose();
								LoginWindow lw = new LoginWindow();
								lw.setVisible(true);
								dispose();
								
							} catch (Exception e) {
//								e.printStackTrace();
							}
							
							
						}
					});
					
					
					// unsubscribe from a service
					button_Unsubscribe.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent arg0) {
						
							String QPattern = (String)list_SubscribedServices.getSelectedValue();
							
							int QPatternIndex = list_SubscribedServices.getSelectedIndex();
							if(QPattern == null)
							{
								//check at the client side
							}
							else
							{
								//RMI call to unsubscribe from a service
								try {
									BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
									boolean result = send.unsubscribe(username, QPattern);

									DefaultListModel dlm = (DefaultListModel) list_SubscribedServices.getModel();
									
									dlm.remove(QPatternIndex);
									list_SubscribedServices.setModel(dlm);
									
								} catch (Exception e) 
								{
//									e.printStackTrace();
								}
								
							}
							
						}
					});

					
					//add a new subscription- shows all unsubscribed services
					button_AddNewSubscription.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {

							//change panels on display
							panel_right.removeAll();
							panel_right.repaint();
							panel_right.revalidate();
							panel_right.add(panel_AddNewSubscription);
							panel_Child.repaint();
							panel_Child.revalidate();
							

							//RMI call to received these unsubscribed services and display them
							try {
								
								
								BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
								ArrayList<String> result = send.getUnsubscribedQueues(username);
								
								if(!result.isEmpty())
								{
									DefaultListModel dlm = new DefaultListModel();
									
									for(int i=0; i<result.size(); i++)
									{
										dlm.addElement(result.get(i));
									}
									list_UnsubscribedServices.setModel(dlm);
								}
								
							} catch (Exception ex) {
//								ex.printStackTrace();
							}
						}
					});
					
					
					//subscribe to a selected service
					button_Subscribe.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							
						try {
								
								//get the selected service
								String QPattern = (String) list_UnsubscribedServices.getSelectedValue();
								int QPatternIndex = list_UnsubscribedServices.getSelectedIndex();
								
								if(QPattern == null)
								{
									//check at client side
									label_SubscriptionStatus.setText("Please Select a Queue");
								}
								else
								{
									//RMI call to subscribe to the selected service
									BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
									boolean result = send.subscribe(username, QPattern);
									
									if(result)
									{
										label_SubscriptionStatus.setText("Subscribed: " + QPattern);	

										//update list of subscribed services
										try
										{
											BootStrap2ServerInterface fetchSubs = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
											ArrayList<String> resultsubs = fetchSubs.getMySubscriptions(username);
											
											if(!resultsubs.isEmpty())
											{
												DefaultListModel dlm = new DefaultListModel();
												
												for(int i=0; i<resultsubs.size(); i++)
												{
													dlm.addElement(resultsubs.get(i));
												}
												list_SubscribedServices.setModel(dlm);
											}
											
										}
										catch(Exception ex)
										{
//											ex.printStackTrace();
										}
										
										
										//remove the subscribed service from the list of unsubscribed services
										DefaultListModel dd = (DefaultListModel) list_UnsubscribedServices.getModel();
										dd.remove(QPatternIndex);
										list_UnsubscribedServices.setModel(dd);
										
									}
									else
									{
										//incase quque does not exist
										label_SubscriptionStatus.setText("Queue does not Exists");
									}
								}
								
							} catch (Exception ex) {
								label_SubscriptionStatus.setText("Connection Error!");
//								ex.printStackTrace();
							}
						}
					});
					
					
					//select the service and display relevant panels
					list_SubscribedServices.addListSelectionListener(new ListSelectionListener() {
						public void valueChanged(ListSelectionEvent arg0) {
							
							panel_right.removeAll();
							panel_right.repaint();
							panel_right.revalidate();
							
							
							panel_right.add(panel_SubscribedMessges);

							
							panel_Child.repaint();
							panel_Child.revalidate();
							
							try {
								String QPattern = (String) list_SubscribedServices.getSelectedValue();
								
								
								BootStrap2ServerInterface fetchsubsmsg = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
								ArrayList<String> resultsubmsgs = fetchsubsmsg.ShiftQueue(username, QPattern);
								
								DefaultListModel dlm = new DefaultListModel();
								
								if(resultsubmsgs != null)
								{
									for(int i=0; i < resultsubmsgs.size(); i++)
									{
										dlm.addElement(resultsubmsgs.get(i));
									}
									list_SubscribedMessages.setModel(dlm);
								}
							}
							catch(Exception ex)
							{
//								System.out.println();
							}
						}
					});
					
					
					myPublishedServices = queueList;
					
				//set all the services in the system in the list intially
				if(!queueList.isEmpty())
				{
					DefaultListModel dlm = new DefaultListModel();
					
					for(int i=0; i<queueList.size(); i++)
					{
						dlm.addElement(queueList.get(i));
					}
					list_ServicesList.setModel(dlm);
				}
				
				
				//On choosing the publisher tab, view the relevant panels
				button_publisher_Tab.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						
						//remove all earlier stuff
						panel_Child.removeAll();
						panel_Child.repaint();
						panel_Child.revalidate();
						
						panel_left.removeAll();
						panel_left.repaint();
						panel_left.revalidate();
						
						panel_right.removeAll();
						panel_right.repaint();
						panel_right.revalidate();
						
						
						//add new panel
						panel_Child.add(panel_Publisher);
						
						panel_Child.add(panel_Publisher);

						panel_Publisher.add(panel_left);
						panel_Publisher.add(panel_right);
						
						//set left panel
						panel_left.add(panel_ServiceList);
						panel_left.add(btnUpdateQ);
						panel_left.add(button_Message);
						
						
						//Set right panel
						panel_right.add(panel_SendMessage);
//						panel_right.add(panel_updateService);
	//
						//update service contents
//						panel_updateService_Content.add(panel_AddQueue);
//						panel_updateService_Content.add(panel_RemoveQueue);
						
						panel_Child.repaint();
						panel_Child.revalidate();
						
						
						
					}
				});
				
				//On choosing the subscriber tab, view the relevant panels
				button_subscriber_Tab.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						unreadMessageCounter = 0;
						button_subscriber_Tab.setText("Subscriber");
						
						//remove all earlier stuff
						panel_Child.removeAll();
						panel_Child.repaint();
						panel_Child.revalidate();
						
						
						panel_left.removeAll();
						panel_left.repaint();
						panel_left.revalidate();
						
						panel_right.removeAll();
						panel_right.repaint();
						panel_right.revalidate();
						
						
						//add new panel
						
						panel_Child.add(panel_Subscriber);
						
						panel_Subscriber.add(panel_left);
						panel_Subscriber.add(panel_right);
						

					panel_left.add(list_SubscribedServices);
					panel_left.add(label_SubscribedServices);
					
					panel_left.add(button_Unsubscribe);
					
					panel_left.add(button_AddNewSubscription);

						
					panel_right.add(panel_SubscribedMessges);
					
//					panel_right.add(panel_AddNewSubscription);

					
					panel_Child.repaint();
					panel_Child.revalidate();
					
					
					//RMI call to get all the subscribed services
					try
					{
						BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
						ArrayList<String> result = send.getMySubscriptions(username);
						
						
						if(!result.isEmpty())
						{
							DefaultListModel dlm = new DefaultListModel();
							
							for(int i=0; i<result.size(); i++)
							{
								dlm.addElement(result.get(i));
							}
							list_SubscribedServices.setModel(dlm);
						}
						
					}
					catch(Exception ex)
					{
//						ex.printStackTrace();
					}
						
						
					}
				});
				
				
				//list select event handler to to view relevant data on the screen
				list_ServicesList.addListSelectionListener(new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent event) {
						if(event.getValueIsAdjusting())
						{
							String selected = list_ServicesList.getSelectedValue().toString();
							
							
							String removeText = "Are you sure you want to remove Queue: " + selected + "?";
							label_QueueRemoveMessage.setText(removeText);
							
						}
					}
				});
				
				
				
				list_ServiceDetails.addListSelectionListener(new ListSelectionListener() {
					public void valueChanged(ListSelectionEvent arg0) {
						
						
						//remove all earlier stuff
						panel_right.removeAll();
						panel_right.repaint();
						panel_right.revalidate();
						
						//add new panel
						panel_right.add(panel_SendMessage);
						panel_right.repaint();
						panel_right.revalidate();
						
					}
				});
				
				
				//update button to either create a new service or remove and existing service
				btnUpdateQ.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
					
						//remove all earlier stuff
						panel_right.removeAll();
						panel_right.repaint();
						panel_right.revalidate();
						
						//add new panel
						panel_right.add(panel_updateService);
						panel_right.repaint();
						panel_right.revalidate();
					
					}
				});
			
				
		
				
				//add tab in update service to add a new service
				button_Add_Tab.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						

						//remove all earlier stuff
						panel_updateService_Content.removeAll();
						panel_updateService_Content.repaint();
						panel_updateService_Content.revalidate();
						
						//add new panel
						panel_updateService_Content.add(panel_AddQueue);
						panel_updateService_Content.repaint();
						panel_updateService_Content.revalidate();
						
					}
				});
				
				
				//RMI call to add a new service
				button_Add.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						
						String QName = textField_QueueName.getText();
						String QPattern = username + "." + QName;
						
						
							System.out.println("poiServer:" + poiServer);
							
							//check if queue already exists
							if(myPublishedServices.contains(QPattern))
							{
								label_AddQueueStatus.setText("Queue Already Exists");
							}
							else
							{
								//create service
								try 
								{
									BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
									ArrayList<String> result = send.createQueue(username, QName, QPattern);
									
									System.out.println("Successful!!! Queue Created.");
									label_AddQueueStatus.setText("Queue created Successfull!!!");
									
									myPublishedServices = result;
									
//									System.out.println("result: " + result);
									if(!result.isEmpty())
									{
										DefaultListModel dlm = new DefaultListModel();
										
										for(int i=0; i<result.size(); i++)
										{
											dlm.addElement(result.get(i));
										}
										list_ServicesList.setModel(dlm);
										
									}
									
									
								} 
								catch (Exception ex) 
								{
									System.out.println("Connection Not Establisted!!! add new server");
									label_AddQueueStatus.setText("Connection Error...");
//									ex.printStackTrace();
								}
								
							}
					}
				});
				
				//remove an existing service tab selection
				button_Remove_Tab.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						
						//remove all earlier stuff
						panel_updateService_Content.removeAll();
						panel_updateService_Content.repaint();
						panel_updateService_Content.revalidate();
						
						//add new panel
						panel_updateService_Content.add(panel_RemoveQueue);
						panel_updateService_Content.repaint();
						panel_updateService_Content.revalidate();
						
						
						String QName = textField_QueueName.getText();
						String removeText = "Please select a service to delete:";
						label_QueueRemoveMessage.setText(removeText);
					}
				});
				
				//RMI call to remove the selected queue
				button_Remove.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						
						
						String QPattern = (String) list_ServicesList.getSelectedValue();
						if(QPattern == null)
						{
							//client side checks
							label_QueueRemoveStatus.setText("Queue not Selected!");
						}
						else
						{
							//RMI call
							try 
							{
								System.out.println("poiServer:" + poiServer);
								BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
								ArrayList<String> QList = send.deleteQueue(username,QPattern);
								
								label_QueueRemoveStatus.setText("Queue Deleted Successfull!!!");
								
								myPublishedServices = QList;
								
								DefaultListModel dlm = new DefaultListModel();
								if(!QList.isEmpty())
								{
									for(int i=0; i<QList.size(); i++)
									{
										dlm.addElement(QList.get(i));
									}
								}
								list_ServicesList.setModel(dlm);
							} 
							catch (Exception ex) 
							{
								System.out.println("Connection Not Establisted!!! add new server");
								label_QueueRemoveStatus.setText("Connection Error...");
							}
						}
					}
				});
				
				
				//change panels to send a message to a selected service
				button_Message.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						
						
						//remove all earlier stuff
						panel_right.removeAll();
						panel_right.repaint();
						panel_right.revalidate();
						
						//add new panel
						panel_right.add(panel_SendMessage);
						panel_right.repaint();
						panel_right.revalidate();
						
					}
				});

				//RMI call to send message to a selected service
				button_SendMessage.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent arg0) {
						
						if(list_ServicesList.getSelectedValue() == null)
						{
							label_sendMessageStatus.setText("Please select a service from the list!");
						}
						else
						{
						
							try 
							{
								System.out.println("poiServer:" + poiServer);
								String publishMsg = textField_Message.getText();
								
								String QPattern = (String) list_ServicesList.getSelectedValue();
								
								BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port + "/update");
								boolean result = send.publishNewMessage(username,QPattern, publishMsg);
								
								if(result)
								{
									System.out.println("Successful!!! Message Sent!");
									label_sendMessageStatus.setText("Message Sent Successfully!");
								}
								else
								{
									System.out.println("ERROR!!! Message not sent!");
									label_sendMessageStatus.setText("Message Sent Failed!");
								}
								
							} 
							catch (Exception ex) 
							{
								System.out.println("Connection Not Establisted!!!");
								label_sendMessageStatus.setText("Connection Error!");
							}
						}
					}
				});
		
	}

	
	//class to run heartbeat to the connected server node
	class HeartBeatSchedule extends TimerTask {
		@Override
		public void run() {
			int counter = 0;
			// ping the poiserver for 3 times
			// if connection failed all 3 times, get a new poiserver from the bootstrap

			while (true) 
			{
				try 
				{
					BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + poiServer + ":" + port+ "/update");
					send.HeartBeat();
					break;
				} 
				catch (Exception e) 
				{
					counter++;
					System.out.println("Heart Beat FAILURE... " + counter);
					if (counter == 3) 
					{
						break;
					}
				}
			}
			
			if (counter == 3) {
				//contact the bootstrap  server for a new poiServer
				
				try
				{
					BootStrap2ServerInterface send = (BootStrap2ServerInterface) Naming.lookup("//" + bootstrap + ":" + port + "/update");
					poiServer = send.ReconnectUser(username);
				}
				catch(Exception e)
				{
					
				}
				
			}
		}
	}
	
	//thread to keep running for incoming tcp notifications
	public static class KeepAlive implements Runnable {

		KeepAlive() 
		{
			try 
			{
				ss = new ServerSocket(tcpPort);
			} 
			catch (IOException e1) 
			{
//				e1.printStackTrace();
			}
		}

		@Override
		public void run() 
		{
			while (true) 
			{
				try 
				{
					
					Socket server = ss.accept();
			           
					BufferedReader in = new BufferedReader(new InputStreamReader(server.getInputStream()));
		            String clientSentence = in.readLine();
					
					unreadMessageCounter++;
					
					button_subscriber_Tab.setText("Subscriber \t New Msg: " + unreadMessageCounter);
					System.out.println("Subscriber \t New Msg: " + unreadMessageCounter);
					
				} 
				catch (Exception ex) 
				{
//					ex.printStackTrace();
				}
				
			}
		}
	}

	
	
	

	
	@Override
	public void addToNetwork(String nodeIP, int IPhash) throws RemoteException {
		
	}

	@Override
	public String InsertFirstNode() throws RemoteException {
		return null;
	}

	@Override
	public String InsertionNewNode(String ip, int nodehash, String startNodeIP)
			throws RemoteException {
		return null;
	}

	@Override
	public String printDetails() throws RemoteException {
		return null;
	}

	@Override
	public String removeNode(int iphash) throws RemoteException {
		return null;
	}

	@Override
	public String RemoteLogin(String username, String password, String userIP)
			throws RemoteException {
		return null;
	}

	@Override
	public String RemoteSignup(String username, String password, String userIP)
			throws RemoteException {
		return null;
	}

	@Override
	public ArrayList<String> createQueue(String username, String QName, String QPattern)
			throws RemoteException {
		return null;
	}

	@Override
	public int getCurrentLoad() throws RemoteException {
		return 0;
	}

	@Override
	public String addConnectedUser(String username, String hostAddress)
			throws RemoteException {
		return null;
	}


	@Override
	public void setPredecessor(String pre) throws RemoteException {
		
	}


	@Override
	public void setSuccessor(String suc) throws RemoteException {
		
	}


	@Override
	public String getPredecessor() throws RemoteException {
		return null;
	}


	@Override
	public String getSuccessor() throws RemoteException {
		return null;
	}


	@Override
	public int getHashValue() throws RemoteException {
		return 0;
	}


	@Override
	public void setUpperLimit(int uLimit) throws RemoteException {
		
	}


	@Override
	public int getUpperLimit() throws RemoteException {
		return 0;
	}


	@Override
	public void setLowerLimit(int lLimit) throws RemoteException {
		
	}


	@Override
	public int getLowerLimit() throws RemoteException {
		return 0;
	}


	@Override
	public void getQueues(HashMap<String, Queue> Q) throws RemoteException {
		
	}


	@Override
	public void HeartBeat() throws RemoteException {

		
	}


	@Override
	public HashMap<String, Queue> getQueuesNotInRange() throws RemoteException {
		return null;
	}


	@Override
	public void setPrePredecessor(String prePre) throws RemoteException {
		
	}


	@Override
	public String getPrePredecessor() throws RemoteException {
		return null;
	}




	@Override
	public boolean sendQueueToBackup(Queue q) throws RemoteException {
		return false;
	}


	@Override
	public void removeQueue(String QPattern) throws RemoteException {
		
	}


	@Override
	public void addQueueToListOfQueuesInSystem(String QPattern)
			throws RemoteException {
		
	}


	@Override
	public void removeQueueFromListOfQueuesInSystem(String QPattern)
			throws RemoteException {
		
	}


	@Override
	public void addUpdatedQueues(HashMap<String, Queue> BackupQueues)
			throws RemoteException {
		
	}


	@Override
	public void updateBackups() throws RemoteException {
		
	}


	@Override
	public void setHashValue(int hash) throws RemoteException {
		
	}


	@Override
	public void removeTheseQueues(HashMap<String, Queue> rmQueue)
			throws RemoteException {
		
	}


	@Override
	public HashMap<String, Queue> getQueuesInRange() throws RemoteException {
		return null;
	}


	@Override
	public void addMeToChord(String ip, int nodehash, String startNodeIP)
			throws RemoteException {
		
	}

	@Override
	public boolean publishNewMessage(String username,
			String qPattern, String publishMsg) throws RemoteException {
		return false;
	}


	
	@Override
	public void setQueuesInSystemToConnectedUsers(ArrayList<String> QIS) throws RemoteException {
		
		//Incoming RMI call for incoming messages 
		
		myPublishedServices = QIS;
		
		//add entries to the list
		if(!QIS.isEmpty())
		{
			DefaultListModel dlm = new DefaultListModel();
			
			for(int i=0; i<QIS.size(); i++)
			{
				dlm.addElement(QIS.get(i));
			}
			
			list_ServicesList.setModel(dlm);
		}
	}


	@Override
	public ArrayList<String> getQueuesInSystem(String username)
			throws RemoteException {
		return null;
	}


	@Override
	public void sendupdates() throws RemoteException {
	}


	@Override
	public ArrayList<String> getSubscriptionListOfUser(String username,
			String CallerIP, ArrayList<String> subs) throws RemoteException {
		return null;
	}


	@Override
	public ArrayList<String> getMySubscriptions(String username)
			throws RemoteException {
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
	public void pushQueueData(String QPattern, String Message)
			throws RemoteException {

		//pushing data to the queue for notifications
		
		unreadMessageCounter++;
		
		button_subscriber_Tab.setText("Subscriber \t New Msg: " + unreadMessageCounter);
		System.out.println("Subscriber \t New Msg: " + unreadMessageCounter);
		
		if(ServiceMessageData.containsKey(QPattern))
		{
			int Qsize = ServiceMessageData.get(QPattern).size();
			
			if(Qsize < 50)
			{
				// add message to the queue 
				ServiceMessageData.get(QPattern).add(Message);
			}
			else
			{
				
				ServiceMessageData.get(QPattern).remove(0);
				ServiceMessageData.get(QPattern).add(Message);
			}
		}
		else
		{
			ArrayList<String> temp = new ArrayList<String>();
			temp.add(Message);
			ServiceMessageData.put(QPattern, temp);
		}
		
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
}
