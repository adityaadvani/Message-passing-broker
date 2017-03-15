import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Class Queue is the data structure for a queue in the Pub-Sub system. It
 * maintains the queue properties, data and the list of subscribers.
 * 
 * @author Aditya Advani
 * @author Ankita Sambhare
 * 
 * @version May 13, 2016 
 *
 */
public class Queue implements Serializable {

	// instance variables of the queue
	String publisher; // name of the user who can publish into the queue
	String name = ""; // name of the queue
	String pattern = ""; // Queue identifier
	int capacity = 0; // total number of messages that the queue can handle
	ArrayList<String> data = new ArrayList<>(); // data holder for queue
	HashMap<String, String> Subscribers = new HashMap<>(); // Map of all
															// subscribers and
															// their IP

	//default constructor
	Queue() {

	}

	//constructor for queue at the servers
	Queue(String Pub, String QName, String QPattern) {
		this.publisher = Pub;
		this.name = QName;
		this.pattern = QPattern;
		this.capacity = 50;
	}

	//constructor for queue at the bootstrap
	Queue(String Pub, String QName, String QPattern, boolean BootstrapQueue) {
		this.publisher = Pub;
		this.name = QName;
		this.pattern = QPattern;
		this.capacity = Integer.MAX_VALUE;
	}
}
