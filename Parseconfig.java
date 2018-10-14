package aos;

// import Node;
import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Parseconfig {
    
	private int numOfNode;
	//private Path path;
    private Path file;
    private int nodeId;
    private ArrayList<Node> hosts;
    private ArrayList<Node> neighbors;
    private Node myNode;
    
    public Parseconfig(int nodeId, String relativePath){
        this.nodeId = nodeId;
        //this.path = Paths.get(relativePath);
        this.file = Paths.get(relativePath).toAbsolutePath();
        //System.out.println(path.toString());
        
        // Load file
        loadConfig();
        
    }
    
    public int getnodeId(){
        return this.nodeId;
    }
    public int getNumOfNode() {
		return this.numOfNode;
	}
    public Node getmyNode() {
        return this.myNode;
    }   
    public ArrayList<Node> getHosts(){
        return this.hosts;
    }

	public ArrayList<Node> getNeighbors() {
		return this.neighbors;
	}
    public int getNeighborsCnt() {
        return this.neighbors.size();
    }

	private void loadConfig(){
        
        Charset charset = Charset.forName("UTF-8");
        try (BufferedReader reader = Files.newBufferedReader(file, charset)) {
            String line = null;
            int n = 0;
            
            // to get the number of nodes (numOfNode)
            // We use while loop to see if there are white spaces at the beginning of the config.txt
            //............................................................
            while ((line = reader.readLine()) != null) {
                line = line.replaceAll("#.*",""); 
                if(line.length() == 0)
                    continue;
                numOfNode = Integer.parseInt(line);
                break;
            }
            
            System.out.println("Number of nodes: "+ getNumOfNode());
            n = numOfNode;
            //............................................................
            
            hosts = new ArrayList<>(numOfNode);
            
            int currentId=1;
            
            // Load host list.
            while ((line = reader.readLine()) != null && n != 0) {
                line = line.replaceAll("#.*","");  
                if(line.length() == 0)
                    continue;
                
                String[] hostInfo = line.split("\\s+");
                
                Node host = new Node(currentId, hostInfo[0], hostInfo[1]);
				hosts.add(host);
				//String[] neighborIds = hostInfo[2].split("\\s+");
			    if( currentId == nodeId){
                    //this.myNode = hosts.get(currentId);

//                    neighbors = new ArrayList<>();
//                    Node node = hosts.get(nodeId);
//                    neighbors.add(node);
//                    for(int i = 3; i < hostInfo.length; i++){
//                        int id = Integer.parseInt(hostInfo[i]);
//                        node = hosts.get(id);
//                        neighbors.add(node);
//                    }
                 }
			    
				 currentId++;
                 n--;
            }
            if(n != 0){
                throw new IOException("Insufficent valid lines in config file.");
            }
            
            /*
            n = numOfNode;
            
            // Load neighbors
            while ((line = reader.readLine()) != null && n != 0) {
                line = line.replaceAll("#.*","");  
                if(line.length() == 0)
                    continue;
                String[] neighborIds = line.split("\\s+");
                int currentId = Integer.parseInt(neighborIds[0]);
                
                if( currentId == nodeId){
                    this.myNode = hosts.get(currentId);

                    neighbors = new ArrayList<>();
                    for(int i = 1; i < neighborIds.length; i++){
                        int id = Integer.parseInt(neighborIds[i]);
                        Node node = hosts.get(id);
                        neighbors.add(node);
                    }
                }
                n--;
            }
            if(n != 0){
                throw new IOException("Insufficent valid lines in config file.");
            }
            */
            
            if(neighbors == null){
                throw new NullPointerException("Expect adjacent neighbors for node " + nodeId);
            }
            
        } catch (IOException x) {
            System.err.format("IOException: %s%n", x);
        } catch (NullPointerException e){
            System.err.println(e.getMessage());
        }
    }
    
    public void printConfig(int nodeID){
		System.out.println(String.format("Node %d Configuration", nodeId));
        // Print hosts 
        System.out.println("Host List:");
		for(Node node : hosts){
            System.out.println(node.configToSring());
        }
        
		System.out.println("Neighbor List:");
        // Print neighbors
        for(Node node : neighbors){
            System.out.println(node.configToSring());
        }
		System.out.println("End of Configuration..........");
    }
  
}