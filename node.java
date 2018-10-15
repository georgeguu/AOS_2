package bin;

public class Node{
    private int nodeId;
    private String host;
    private String port;
    
    public Node(int index, String hostName, String port){
        this.nodeId = index;
        this.host = hostName;
        this.port = port;
    }
    
    public String configToSring(){
        return String.format("%d %s %s", nodeId, host, port);
    }

    public int getNodeId(){
        return nodeId;
    }
    
    public String getHostName(){
        return host;
    }
    
    public int getPort(){
        return Integer.parseInt(port);
    }
}