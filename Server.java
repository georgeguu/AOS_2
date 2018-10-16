// package bin;
import bin.Node;
import bin.Parseconfig;
import bin.Message;
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;

// Server class
public class Server
{
    private static int nodeId;
    private static int portNum;
    private static long timeoutSec = 5;
    public static void main(String[] args) throws IOException
    {
        if (args.length != 3)
        {
            throw new IllegalArgumentException("Please enter port#, node ID and config file path");
        }
        
        portNum = Integer.parseInt(args[0]);
        nodeId = Integer.parseInt(args[1]);
        
        // Parse the config.txt
        Parseconfig config = new Parseconfig(nodeId, args[2]);
        
        // Process config.txt info. into myNode, shall let every process and thread can access this.
        Node myNode = new Node(config.getNodeId(), config.getMyHost(), portNum, config.getRootId());
        myNode.setNeiborNodes(config.getNeighbors());

        myNode.printConfig();

        Thread serverFactory = new ServerFactory(myNode);
        serverFactory.start();

        // Timeout for seconds
        timeout(timeoutSec);

        if(myNode.isRoot())
        {
            Thread clientT = new ClientHandler(myNode);
            clientT.start();
        }else{

        }


    }
    private static void timeout(long second){
        long start = System.currentTimeMillis();
        long end = start + second * 1000; // second * 1000 ms/sec
        while (System.currentTimeMillis() < end)
        {
            // run
        }
    }
}

// ClientHandler class 
class ServerFactory extends Thread{
    private static int portNum;
    private static int neiborhoodNum;
    Node myNode;
    public ServerFactory(Node myNode)  
    { 
        this.myNode = myNode;
    } 
    @Override
    public void run()
    {
        try{
            ServerSocket ss = new ServerSocket(this.myNode.getPort());
            while (true)  
            { 
                Socket s = null; 
                try 
                { 
                    s = ss.accept(); 
                    Thread serverT = new ServerThread(s, ss, this.myNode);
                    serverT.start();
                } 
                catch (Exception e){ 
                    s.close(); 
                    e.printStackTrace(); 
                }
            }
        }
        catch(IOException e){
            System.out.println("Can not open socket, port Number is not correct" + this.myNode.getPort());

        }      
                
    } 
}

class ServerThread extends Thread{
    private ObjectInputStream incomingMsg; 
    private ObjectOutputStream returnMsg;
    Node myNode;
    Socket s; 
    ServerSocket ss;

    Message newComingObj;
    public ServerThread(Socket s, ServerSocket ss, Node myNode)  
    { 
        this.s = s;
        this.ss = ss;
        this.myNode = myNode;
    } 
    @Override
    public void run()  
    {
        while(true)
        {
            try
            { 
                incomingMsg = new ObjectInputStream(s.getInputStream()); 
                ObjectOutputStream returnMsg = new ObjectOutputStream(s.getOutputStream());

                System.out.println("New connection");


                newComingObj = (Message) incomingMsg.readObject();
                System.out.println("Received msg from: " + newComingObj.getOrigin().getNodeId() + "\n");
                // queue.add(newComingObj);
                newComingObj.getMsgAsString();

                if(!myNode.isParentSet()){
                    Message msg = new Message(myNode, newComingObj.getOrigin(), "PACK");
                    returnMsg.writeObject(msg);
                }
                // returnMsg.writeChars("PACK");
                // returnMsg.flush();
                
                break;             
                  
            }
            catch (SocketException se)
            {
                System.exit(0);
            }catch (IOException e)
            {
                e.printStackTrace();
            }catch (ClassNotFoundException cn)
            {
                cn.printStackTrace();
            }catch (NullPointerException ne)
            {

            }
        }
        try{
            s.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        } 
                      
         
    } 
}
class ClientHandler extends Thread  
{ 
    // private RoutingTable currentTable;
    private Node targetNode;
    private Socket socket = null;
    private ObjectInputStream inputStream = null;
    private ObjectOutputStream outputStream = null;
    private boolean isConnected = false;
    Node myNode;
    // private static int neiborhoodNum;
    // ConcurrentLinkedQueue<Message> queue;
    // private sendCount;
  
    // Constructor 
    public ClientHandler(Node myNode)  
    { 
        this.myNode = myNode;
    } 
    public void sendMsg(Message msg){
        targetNode = msg.getDestination();
        try{
            // currentTable.getMsg().setDestination(targetNode);
            try {
                socket = new Socket(targetNode.getHostName(), targetNode.getPort());
                isConnected = true;
                outputStream = new ObjectOutputStream(socket.getOutputStream());
                outputStream.writeObject(msg);

                try{
                    // try{
                    inputStream = new ObjectInputStream(socket.getInputStream());
                    Message response = (Message) inputStream.readObject();
                    // System.out.println("Message: " + response.);
                    response.getMsgAsString();

                    // }
                    // String response = (String) inputStream.readObject();

                    // System.out.println("Message: " + response);

                }catch (ClassNotFoundException cn)
                {
                    cn.printStackTrace();
                }catch (NullPointerException ne)
                {
                   ne.printStackTrace();
                } 

                // getResult maybe Pack? Nack?
                // count 

            } catch (SocketException se) {
                se.printStackTrace();
                System.out.println("Connection fail, not able connect to : " + targetNode.getHostName());

            } catch (IOException e) {
                e.printStackTrace();
            }
        }catch (NullPointerException e){
            System.out.println("table is null");
        }


        return;
    }
    
    @Override
    public void run()  
    {
        for(int i = 0;i < this.myNode.getNeighbors().size(); i++){
            Node targetNode = this.myNode.getNeighbors().get(i);
            Message firstMessage = new Message(myNode, targetNode, "Explore");
            sendMsg(firstMessage);
        }

    }

} 