package bin;
import java.util.ArrayList;
import java.lang.Boolean;

public class Node implements java.io.Serializable{
    private int nodeId;
    private int port;
    private Boolean isRoot;
    private String host;

    private Boolean isParent;

    private ArrayList<Node> neighbors;
    private Node parent;
    private ArrayList<Node> children;

    public Node(int index, String hostName, int port, int rootId){
        this.nodeId = index;
        this.host = hostName;
        this.port = port;
        this.isRoot = (rootId == this.nodeId) ? true : false;

        this.isParent = false;
    }
    
    public String configToSring(){
        return String.format("%d %s %s", nodeId, host, port);
    }

    public int getNodeId(){
        return this.nodeId;
    }
    
    public String getHostName(){
        return this.host;
    }
    
    public int getPort(){
        return this.port;
    }

    public ArrayList<Node> getNeighbors() {
        return this.neighbors;
    }

    public int getNeighborsCnt() {
        return this.neighbors.size();
    }

    public Boolean isRoot(){
        return this.isRoot;
    }

    public Boolean isParentSet(){
        return this.isParent;
    }

    public void setNeiborNodes(ArrayList<Node> neighbors){
        this.neighbors = neighbors;
    }

    public void printConfig(){
        System.out.println(String.format("-------Node %d Configuration-----", this.nodeId));
        // Print hosts 
        // System.out.println("-----Host List-----");
        // for(Node node : hosts){
        //     System.out.println(node.configToSring());
        // }
        
        System.out.println("-----Neighbor List-----");
        // Print neighbors
        for(Node node : this.neighbors){
            System.out.println(node.configToSring());
        }

        System.out.println("-----End of Configuration-----");
    }
}   
