package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import util.Logger;

/**
 * @author Shiqi Luo
 *
 */
public class KeyValueMultithreadService {

	private Socket client = null;
	private ServerSocket tcpServerSocket = null;
	private int port;

	private KeyValueStoreLearner keyValueStore = null;
	private Thread acceptor;
	
	private Proposer proposer;
	/**
	 * 
	 */
	public KeyValueMultithreadService(int port) throws IOException {
		this.tcpServerSocket = new ServerSocket(port);
		this.keyValueStore = new KeyValueStoreLearner();
		this.acceptor = new Thread(new AcceptorThread(port, this.keyValueStore));
		this.acceptor.start();
		this.port = port;
		this.proposer = new Proposer(port, this.keyValueStore);
	}
	
	public void runService() {
		
		while(true){
			try{
				this.client = this.tcpServerSocket.accept();

				Thread clientHandler = new Thread(
						new ProposerLearnerCombiner(this.client, this.keyValueStore, this.proposer));
				clientHandler.start();
				
			} catch(IOException e) {
				e.printStackTrace();
				Logger.logServerError(e.getMessage(), this.client.getRemoteSocketAddress().toString());
			}
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			if(args.length != 1){
				throw new IllegalArgumentException("Only accept 1 argument: port");
			}
			KeyValueMultithreadService tcpServer = new KeyValueMultithreadService(Integer.valueOf(args[0]));
			tcpServer.runService();
			
		} catch (IllegalArgumentException | IOException e) {
			Logger.logServerError(e.getMessage(), "null");
		}
	}

}

