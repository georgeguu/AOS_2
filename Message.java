package bin;
import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.io.Serializable;


public class Message implements Serializable{

	private Node source;
	private Node origin;
    private Node destination;
    private byte[] type;
    private int ackCounter;
    
    
    public Message(Node newOrg, Node newDst, String str)
    {   
        this.origin = newOrg;
        this.destination = newDst;
        this.type = str.getBytes();

    }
    
    @Override
    public Message clone(){
    	  try {
    	        return (Message) super.clone();
    	      }catch (CloneNotSupportedException e) {
    	         return new Message(this.source,this.origin,this.destination, this.type, this.ackCounter);
    	     }
    	}
    
    /*
     * Overloaded constructor for broadcast/convergecast(Part 2)
     * */
    public Message(Node newSrc, Node newOrg, Node newDst, String str)
    {
    	this.source = newSrc;
    	this.origin = newOrg;
        this.destination = newDst;
        this.type = str.getBytes();
        this.ackCounter = 0;
    }
    
    public Message(Node newSrc, Node newOrg, Node newDst, byte[] type, int ackCount)
    {
    	this.source = newSrc;
    	this.origin = newOrg;
        this.destination = newDst;
        this.type = type;
        this.ackCounter = ackCount;
    }
    
    public void resetAckCounter() {
    	this.ackCounter=0;
    }
    
    
    public int getackCounter()
    {
    	return this.ackCounter;
    }
    
    public void ackCounterPP()
    {
    	this.ackCounter= this.ackCounter + 1;    	
    }
    
    public Node getSource()
    {
    	return this.source;
    }

    public Node getOrigin(){
        return this.origin;
    }

    public Node getDestination(){
        return this.destination;
    }

    public String getType(){
        return (new String(this.type));
    }

    public void getMsgAsString(){
        System.out.println("origin:"+origin.getNodeId()+" to "+destination.getNodeId()+",Msg:" + new String(this.type));
        // System.out.println("origin:"+origin.getNodeId()+" to "+destination.getNodeId());

        return;
    }

}

    
