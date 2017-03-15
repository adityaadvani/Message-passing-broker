import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;

import java.awt.Font;
import java.awt.Color;

import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JButton;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.Serializable;
import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JPasswordField;


public class BunnyME_Client implements Serializable{

	
	private JFrame frame;
	private JTextField txtAditya;
	private JPasswordField passwordField;

	//RMI related variables
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
					try 
					{
						//start a login window frame
						LoginWindow lw = new LoginWindow();
						lw.setVisible(true);
						
					} 
					catch (Exception e) 
					{
						e.printStackTrace();
					}
					
			}
		});
	}

	/**
	 * Create the application.
	 */
	public BunnyME_Client() throws RemoteException
	{
		initialize();
	}

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		frame = new JFrame();
		frame.getContentPane().setBackground(new Color(175, 238, 238));
		frame.setBounds(100, 100, 1384, 727);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		
			
		
	}


}
