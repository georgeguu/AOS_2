// package bin;
import bin.Node;
import bin.Parseconfig;
import java.io.*;
import java.text.*;
import java.util.*;
import java.net.*;

// Server class
public class Server
{
    private static int nodeId;
    private static int portNum;
    
    public static void main(String[] args) throws IOException
    {
        if (args.length != 3) {
            throw new IllegalArgumentException("Please enter port#, node ID and config file path");
        }
        
        portNum = Integer.parseInt(args[0]);
        nodeId = Integer.parseInt(args[1]);
        System.out.println("Port#= "+portNum);
        System.out.println("nodeID= "+nodeId);
        
        // Parse the config.txt
        Parseconfig config = new Parseconfig(nodeId, args[2]);
        
        // Print the configuration file
        //config.loadConfig();
        config.printConfig(nodeId);
        //System.out.println("Number of nodes: "+ config.getNumOfNode());
        
    }
}
