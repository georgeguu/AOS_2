
//package bin;
import bin.Node;
import bin.Parseconfig;
import bin.Message;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.net.*;

// Server class
public class Server {
	private static int nodeId;
	private static int portNum;
	private static long timeoutSec = 5;
	public static volatile boolean ready;

	public static void main(String[] args) throws IOException {

		ConcurrentHashMap<Integer, Message> broadQueue = new ConcurrentHashMap<Integer, Message>();

		if (args.length != 3) {
			throw new IllegalArgumentException("Please enter port#, node ID and config file path");
		}

		portNum = Integer.parseInt(args[0]);
		nodeId = Integer.parseInt(args[1]);

		// Parse the config.txt
		Parseconfig config = new Parseconfig(nodeId, args[2]);

		// Process config.txt info. into myNode, shall let every process and thread can
		// access this.
		Node myNode = new Node(config.getNodeId(), config.getMyHost(), portNum, config.getRootId());
		myNode.setNeiborNodes(config.getNeighbors());
		System.out.println("Node: " + myNode.getNodeId());

		Thread serverFactory = new ServerFactory(myNode, broadQueue);
		serverFactory.start();

		// Timeout for seconds
		timeout(timeoutSec);

		if (myNode.isRoot()) {
			// myNode.setIsRoot(true);
			Thread clientT = new ClientHandler(myNode/* , broadQueue */);
			clientT.start();

		}
		while (true)
			;
	}

	private static void timeout(long second) {
		long start = System.currentTimeMillis();
		long end = start + second * 1000; // second * 1000 ms/sec
		while (System.currentTimeMillis() < end) {
			// run
		}
	}
}

// ClientHandler class 
class ServerFactory extends Thread {
	private static int portNum;
	private static int neiborhoodNum;
	Node myNode;
	ConcurrentHashMap<Integer, Message> broadQueue;

	public ServerFactory(Node myNode) {
		this.myNode = myNode;
	}

	public ServerFactory(Node myNode, ConcurrentHashMap<Integer, Message> queue) {
		this.myNode = myNode;
		this.broadQueue = queue;
	}

	@Override
	public void run() {
		try {
			ServerSocket ss = new ServerSocket(this.myNode.getPort());
			while (true) {
				Socket s = null;
				try {

					s = ss.accept();
					Thread serverT = new ServerThread(s, ss, this.myNode, broadQueue);
					serverT.start();

				} catch (Exception e) {
					s.close();
					e.printStackTrace();
				}
			}
		} catch (IOException e) {
			System.out.println("Can not open socket, port Number is not correct" + this.myNode.getPort());

		}

	}
}

class ServerThread extends Thread {
	private ObjectInputStream incomingMsg;
	private ObjectOutputStream returnMsg;
	Node myNode;
	Socket s;
	ServerSocket ss;
	ConcurrentHashMap<Integer, Message> broadQueue;

	Message newComingObj;
	Message temp;

	public ServerThread(Socket s, ServerSocket ss, Node myNode) {
		this.s = s;
		this.ss = ss;
		this.myNode = myNode;
	}

	public ServerThread(Socket s, ServerSocket ss, Node myNode, ConcurrentHashMap<Integer, Message> queue) {
		this.s = s;
		this.ss = ss;
		this.myNode = myNode;
		this.broadQueue = queue;

	}

	@Override
	public void run() {
		// while (true) {
		try {
			returnMsg = new ObjectOutputStream(s.getOutputStream());
			incomingMsg = new ObjectInputStream(s.getInputStream());
			// ObjectOutputStream returnMsg = new ObjectOutputStream(s.getOutputStream());

			newComingObj = (Message) incomingMsg.readObject();

			if ((String) newComingObj.getType().intern() != ("SpanningBroad").intern()
					&& (String) newComingObj.getType().intern() != ("Broadcast").intern()
					&& (String) newComingObj.getType().intern() != ("Convergecast").intern()) {

				System.out.println("Received msg from: " + newComingObj.getOrigin().getNodeId() + ", Message: "+ newComingObj.getType());

				if (!myNode.isParent() && !myNode.isRoot()) {
					System.out.println("Replied received msg to: " + newComingObj.getOrigin().getNodeId() + ", Message: PACK");

					myNode.setParent(newComingObj.getOrigin());
					myNode.popParentFromNeighbors(newComingObj.getOrigin());

					Message msg = new Message(myNode, newComingObj.getOrigin(), "PACK");
					returnMsg.writeObject(msg);

					Thread clientH = new ClientHandler(myNode);
					clientH.start();

					// Broadcast to neighbors;
				} else if ((String) newComingObj.getType().intern() != ("ACK").intern()) {
					System.out.println("Replied received msg to: " + newComingObj.getOrigin().getNodeId() + ", Message: NACK");
					Message msg = new Message(myNode, newComingObj.getOrigin(), "NACK");
					returnMsg.writeObject(msg);
				}

				if ((String) newComingObj.getType().intern() == ("ACK").intern()) {
					myNode.incrementAck();
					System.out.println("Recieve an ACK");
				}

				// Send ACK back to the root
				if (myNode.getAckCnt() == myNode.getChildrenCnt() && myNode.getChildrenCnt() != 0) {
					if (!myNode.isRoot()) {
						System.out.println("Ready to send a ACK to parent");
						Thread AckT = new AckThread(myNode);
						AckT.start();
					} else {
						// root node realized spanning tree is done.
						myNode.setIsTreeFinish(true);

						System.out.println("---------------This node can start to broadcast------------ ");

						Message m = new Message(myNode, myNode, null, "Broadcast");
						Thread BroadcastT = new Broadcast(myNode, m);
						broadQueue.put(myNode.getNodeId(), m.clone());
						BroadcastT.start();

						if (this.myNode.getSentMessage() < 5
								&& this.myNode.getSentMessage() == this.myNode.getReceiveMessage()) {
							this.myNode.incrementSentMessage();
						}

					}
				}

				// Now root node is broadcasting completion of spanning tree to other nodes
				if (myNode.isRoot() && myNode.getIsTreeFinish()) {

					System.out.println("---------------Start to broadcast spanning tree completion------------ ");
					Thread BroadT = new BroadThread(myNode);
					BroadT.start();

				}

			} else if ((String) newComingObj.getType().intern() == ("SpanningBroad").intern()) {

				myNode.setIsTreeFinish(true);

				System.out.println("Received msg from: " + newComingObj.getOrigin().getNodeId() + ", Message: "	+ newComingObj.getType());
				System.out.println("---------------Start to send spanning tree completion------------ ");

				// checks if current node is not a leaf node
				if (myNode.getChildrenCnt() != 0) {
					Thread BroadT = new BroadThread(myNode);
					BroadT.start();
				}

				// if (myNode.getIsTreeFinish()) {
				System.out.println("---------------This node can start to broadcast------------ ");

				Message m = new Message(myNode, myNode, null, "Broadcast");
				broadQueue.put(myNode.getNodeId(), m.clone());
				Thread BroadcastT = new Broadcast(myNode, m);
				BroadcastT.start();

				this.myNode.incrementSentMessage();

				// }

			} else if ((String) newComingObj.getType().intern() == ("Broadcast").intern()) {
				//System.out.println("Received msg from: " + newComingObj.getOrigin().getNodeId() + ", Message: "	+ newComingObj.getType() + ", Source " + newComingObj.getSource().getNodeId());
				// Message temp;
				// puts message into the queue

				Message temp1 = newComingObj.clone();
				temp1.resetAckCounter();
				broadQueue.put(temp1.getSource().getNodeId(), temp1);

				// System.out.println(broadQueue.toString());
				//System.out.println("HashMap size: " + broadQueue.size());

				if (myNode.getChildrenCnt() == 0) {

					Message temp2 = broadQueue.get(newComingObj.getSource().getNodeId()).clone();
					Thread convergeT = new Convergecast(myNode, temp2.clone());
					convergeT.start();
					broadQueue.remove(newComingObj.getSource().getNodeId());
					//System.out.println("HashMap size: " + broadQueue.size());

				} else {

					Thread BroadcastT = new Broadcast(myNode, newComingObj.clone());
					BroadcastT.start();
				}

			} else if ((String) newComingObj.getType().intern() == ("Convergecast").intern()) {

				//System.out.println("Received " + newComingObj.getType() + " message from "	+ newComingObj.getOrigin().getNodeId() + " for source " + newComingObj.getSource().getNodeId());

				// Increments the counter for # of ack receive for a broadcast
				// System.out.println("HashMap size: " + broadQueue.size());

				//System.out.println("Hashmap source: "	+ broadQueue.get(newComingObj.getSource().getNodeId()).getSource().getNodeId());
				Message temp = (Message) broadQueue.get(newComingObj.getSource().getNodeId());
				temp.ackCounterPP();

				// System.out.println("ack counter size: " + temp.getackCounter());
				//System.out.println("HashMap size: " + broadQueue.size());
				// broadQueue.replace(newComingObj.getSource(), temp);

				// all the converge cast message are returned to source node
				if (myNode.getNodeId() == temp.getSource().getNodeId()) {

					if ((myNode.isRoot())) // If it is a root
					{
						if (temp.getackCounter() == myNode.getChildrenCnt()) {
							System.out.println(	":):):):):):):):) Broadcast done for " + myNode.getNodeId() + " :):):):):):):):)");
							System.out.println(" Broadcast op number for node " + temp.getSource().getNodeId()
									+ " is " + this.myNode.getReceiveMessage());
							broadQueue.remove(temp.getSource().getNodeId());
							this.myNode.incrementReceiveMessage();

							if (this.myNode.getReceiveMessage() == this.myNode.getSentMessage()
									&& this.myNode.getSentMessage() < 5) {
								Message m = new Message(myNode, myNode, null, "Broadcast");
								Thread BroadcastT = new Broadcast(myNode, m);
								broadQueue.put(myNode.getNodeId(), m.clone());
								BroadcastT.start();
								this.myNode.incrementSentMessage();
							}

							// System.out.println("HashMap size: " + broadQueue.size());
						}
					} else if (!myNode.isRoot() && (myNode.getChildrenCnt() > 0)) // If it is an intermediate node
					{
						if (temp.getackCounter() == (myNode.getChildrenCnt() + 1)) {
							System.out.println(
									":):):):):):):):) Broadcast done for " + myNode.getNodeId() + " :):):):):):):):)");
							System.out.println(" Broadcast op number for node " + temp.getSource().getNodeId()
									+ " is " + this.myNode.getReceiveMessage());
							broadQueue.remove(temp.getSource().getNodeId());
							this.myNode.incrementReceiveMessage();

							if (this.myNode.getReceiveMessage() == this.myNode.getSentMessage()
									&& this.myNode.getSentMessage() < 5) {
								Message m = new Message(myNode, myNode, null, "Broadcast");
								Thread BroadcastT = new Broadcast(myNode, m);
								broadQueue.put(myNode.getNodeId(), m.clone());
								BroadcastT.start();
								this.myNode.incrementSentMessage();
							}
							// System.out.println("HashMap size: " + broadQueue.size());
						}
					} else // else it is a leaf node
					{
						if (temp.getackCounter() == 1) {
							System.out.println(
									":):):):):):):):) Broadcast done for " + myNode.getNodeId() + " :):):):):):):):)");
							System.out.println(" Broadcast op number for node " + temp.getSource().getNodeId()
									+ " is " + this.myNode.getReceiveMessage());
							broadQueue.remove(temp.getSource().getNodeId());
							this.myNode.incrementReceiveMessage();

							if (this.myNode.getReceiveMessage() == this.myNode.getSentMessage()
									&& this.myNode.getSentMessage() < 5) {
								Message m = new Message(myNode, myNode, null, "Broadcast");
								Thread BroadcastT = new Broadcast(myNode, m);
								broadQueue.put(myNode.getNodeId(), m.clone());
								BroadcastT.start();
								this.myNode.incrementSentMessage();
							}

						}
					}

				} else { // current node is not source node for the broadcasted message

					if ((myNode.isRoot())) // If it is a root
					{
						if (temp.getackCounter() == myNode.getChildrenCnt() - 1) {
							//System.out.println("Sending msg to: " + temp.getOrigin().getNodeId() + ", Message: "+ temp.getType() + " For source Node" + temp.getSource().getNodeId());

							Thread convergeT = new Convergecast(myNode, temp.clone());
							convergeT.start();
							broadQueue.remove(temp.getSource().getNodeId());
							//System.out.println("HashMap size: " + broadQueue.size());
						}
					} else if (!myNode.isRoot() && myNode.getChildrenCnt() > 0) // If it is an intermediate node
					{
						if (temp.getackCounter() == (myNode.getChildrenCnt() + 1) - 1) {

							//System.out.println("Sending msg to: " + temp.getOrigin().getNodeId() + ", Message: "+ temp.getType() + " For source Node" + temp.getSource().getNodeId());

							Thread convergeT = new Convergecast(myNode, temp.clone());
							convergeT.start();
							broadQueue.remove(temp.getSource().getNodeId());
							//System.out.println("HashMap size: " + broadQueue.size());
						} else {
							System.out.println("Waiting on the received convergecast");

						}
					} else // else it is a leaf node
					{
						if (temp.getackCounter() == 0) {
							//System.out.println("Sending msg to: " + temp.getOrigin().getNodeId() + ", Message: "+ temp.getType() + " For source Node" + temp.getSource().getNodeId());

							Thread convergeT = new Convergecast(myNode, temp.clone());
							convergeT.start();
							broadQueue.remove(temp.getSource().getNodeId());
							//System.out.println("HashMap size: " + broadQueue.size());
						}
					}

				}

				// System.out.println("Hash map status " + temp.getackCounter());

			}

			returnMsg.close();
			incomingMsg.close();
			s.close();

			// break;

		} catch (SocketException se) {
			System.out.println("Error establishing socket connection!");
			System.exit(0);
			// System.out.println("Error establishing socket connection!");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException cn) {
			cn.printStackTrace();
		} catch (NullPointerException ne) {

		}
		// }

	}
}

class ClientHandler extends Thread {

	private Node targetNode;
	private Socket socket = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	private Node myNode;

	// Constructor
	Message response;

	public ClientHandler(Node myNode) {
		this.myNode = myNode;
	}

	public void sendMsg(Message msg) {
		targetNode = msg.getDestination();

		try {
			socket = new Socket(targetNode.getHostName(), targetNode.getPort());
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream(socket.getInputStream());

			outputStream.writeObject(msg);
			response = (Message) inputStream.readObject();

			outputStream.close();
			inputStream.close();
			socket.close();

			if ((String) response.getType().intern() == ("PACK").intern()) {

				// add to children
				System.out.println("Received Pack from: " + response.getOrigin().getNodeId());
				myNode.addToChildren(response.getOrigin());

			} else if (response.getType().intern() == "NACK".intern())
				System.out.println("Received Pack from: " + response.getOrigin().getNodeId());

			myNode.addAckToQueue(response);

		} catch (ClassNotFoundException cn) {
			cn.printStackTrace();
		} catch (NullPointerException ne) {
			ne.printStackTrace();
		} catch (SocketException se) {
			se.printStackTrace();
			System.out.println("Connection fail, not able connect to : " + targetNode.getHostName());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {
		for (int i = 0; i < this.myNode.getNeighbors().size(); i++) {
			System.out.println("Send Explore to:" + myNode.getNeighbors().get(i).getNodeId());

			Node targetNode = this.myNode.getNeighbors().get(i);
			Message firstMessage = new Message(myNode, targetNode, "Explore");
			sendMsg(firstMessage);
		}

		if (myNode.getQueueSize() == myNode.getNeighborsCnt()) {
			System.out.println("********End*******");
			this.myNode.printConfig();
			// System.exit(0);
			// return;
		}

		if (myNode.getChildrenCnt() == 0) {
			System.out.println("---------------this node is the leaf");
			Thread AckT = new AckThread(myNode);
			AckT.start();
		}

	}

}

class AckThread extends Thread {

	private Node targetNode;
	private Socket socket = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;

	Node myNode;
	Message msg;

	// Constructor
	// Message response;
	public AckThread(Node myNode) {
		this.myNode = myNode;
	}

	public AckThread(Node myNode, Message replyBroad) {
		this.myNode = myNode;
		this.msg = replyBroad;
	}

	public void sendMsg(Message msg) {
		targetNode = msg.getDestination();

		try {
			socket = new Socket(targetNode.getHostName(), targetNode.getPort());
			// isConnected = true;
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream(socket.getInputStream());
			outputStream.writeObject(msg);

			outputStream.close();
			inputStream.close();

			socket.close();

		} catch (SocketException se) {
			se.printStackTrace();
			System.out.println("Connection fail, not able connect to : " + targetNode.getHostName());

		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void run() {

		//System.out.println("Sending ACK to: " + myNode.getParent().getNodeId());

		Node targetNode = this.myNode.getParent();
		Message ackMessage = new Message(myNode, targetNode, "ACK");
		sendMsg(ackMessage);

	}

}

class BroadThread extends Thread {

	private Node targetNode;
	private Socket socket = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	private boolean isConnected = false;
	Node myNode;

	// Constructor
	// Message response;
	public BroadThread(Node myNode) {
		this.myNode = myNode;
	}

	public void sendMsg(Message msg) {
		targetNode = msg.getDestination();

		try {
			socket = new Socket(targetNode.getHostName(), targetNode.getPort());
			isConnected = true;
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream(socket.getInputStream());
			outputStream.writeObject(msg);

			outputStream.close();
			inputStream.close();

			socket.close();

		} catch (SocketException se) {
			se.printStackTrace();
			System.out.println("Connection fail, not able connect to : " + targetNode.getHostName());

		} catch (IOException e) {
			e.printStackTrace();
		}

		// outputStream.close();
		return;
	}

	@Override
	public void run() {

		for (int i = 0; i < this.myNode.getChildrenCnt(); i++) {
			System.out.println("Send spinning tree broadcast meg to: " + myNode.getChildren().get(i).getNodeId());

			Node targetNode = this.myNode.getChildren().get(i);
			Message msg = new Message(myNode, targetNode, "SpanningBroad");
			sendMsg(msg);

		}

	}

}

class Broadcast extends Thread {
	private Node targetNode;
	private Socket socket = null;
	private ObjectOutputStream outputStream = null;
	private ObjectInputStream inputStream = null;
	private Node myNode;
	private Message message;

	public Broadcast(Node myNode, Message msg) {
		this.myNode = myNode;
		this.message = msg;
	}

	public void sendMsg(Message msg) {
		targetNode = msg.getDestination();

		try {
			socket = new Socket(targetNode.getHostName(), targetNode.getPort());
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream(socket.getInputStream());
			outputStream.writeObject(msg);
			inputStream.close();
			outputStream.close();

			socket.close();
		} catch (SocketException se) {
			System.out.println("Connection fail, not able connect to : " + targetNode.getHostName());
			se.printStackTrace();

		} catch (IOException e) {
			System.out.println("Connection fail 1, not able connect to : " + targetNode.getHostName());
			e.printStackTrace();
		}

	}

	@Override
	public void run() {

		/*
		 * try {
		 * 
		 * Thread.sleep(500); } catch (InterruptedException e) {
		 * 
		 * e.printStackTrace(); }
		 */
		// Broadcasts to all the children except the message sender node
		for (int i = 0; i < this.myNode.getChildrenCnt(); i++) {

			if (this.myNode.getChildren().get(i).getNodeId() != message.getOrigin().getNodeId()) {
				//System.out.println("Sending broadcast type msg to: " + myNode.getChildren().get(i).getNodeId()	+ ". Source " + message.getSource().getNodeId());

				Node targetNode = this.myNode.getChildren().get(i);
				Message msg = new Message(message.getSource(), myNode, targetNode, "Broadcast");
				sendMsg(msg);
			}

		}

		// Broadcasts to the parent except the message sender node
		if ((this.myNode.isParent()) && (this.myNode.getParent().getNodeId() != message.getOrigin().getNodeId())) {
			//System.out.println("Sending broadcast type msg to: " + myNode.getParent().getNodeId() + ". Source "	+ message.getSource().getNodeId());

			Node targetNode = this.myNode.getParent();
			Message msg = new Message(message.getSource(), myNode, targetNode, "Broadcast");
			sendMsg(msg);
		}

	}
}

class Convergecast extends Thread {
	private Node targetNode;
	private Socket socket = null;
	private ObjectInputStream inputStream = null;
	private ObjectOutputStream outputStream = null;
	Node myNode;

	Message message;

	public Convergecast(Node myNode, Message msg) {
		this.myNode = myNode;
		this.message = msg;
	}

	public void sendMsg(Message msg) {
		targetNode = msg.getDestination();

		try {
			socket = new Socket(targetNode.getHostName(), targetNode.getPort());
			outputStream = new ObjectOutputStream(socket.getOutputStream());
			inputStream = new ObjectInputStream(socket.getInputStream());
			outputStream.writeObject(msg);

			inputStream.close();
			outputStream.close();
			socket.close();

		} catch (SocketException se) {
			se.printStackTrace();
			System.out.println("Connection fail, not able connect to : " + targetNode.getHostName());

		} catch (IOException e) {
			e.printStackTrace();
			System.out.println("Connection fail, not able connect to : " + targetNode.getHostName());
		}

	}

	@Override
	public void run() {

		/*
		 * try {
		 * 
		 * Thread.sleep(500); } catch (InterruptedException e) {
		 * 
		 * e.printStackTrace(); }
		 */

		//System.out.println("Sending Convergecast msg to : " + message.getOrigin().getNodeId() + ". For source "	+ message.getSource().getNodeId());

		Node targetNode = message.getOrigin();
		Message msg = new Message(message.getSource(), myNode, targetNode, "Convergecast");
		sendMsg(msg);

	}
}
