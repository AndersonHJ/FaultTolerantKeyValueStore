package client;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import util.ClientHelper;
import util.Logger;

/**
 * Class of DFS client
 * @author Shiqi Luo
 */
public class DFSClient {
	private Socket socket;
	private DataInputStream inputFromServer = null;
	private DataOutputStream output = null;
	
	private String address = null;
	private int port = 0;
	private String data = "";
	
	/**
	 * Constructor of TCP client
	 * @param address
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public DFSClient(String address, int port) throws UnknownHostException, IOException{
		this.address = ""+address;
		this.port = port;
	}
	
	/**
	 * Run method of SocketClient, listen to command line message, 
	 * send it to server and print response string
	 * @throws IOException
	 */
	public void run() throws IOException {
		String response = null;
		String query = "";
		
		//query = ClientHelper.getQuery();

		//while(query != null){
			try{
				this.socket = new Socket(this.address, this.port);
				Logger.printClientInfo("connect with server -- " + this.socket.getRemoteSocketAddress());
				this.socket.setSoTimeout(10*1000);
				this.inputFromServer = new DataInputStream(socket.getInputStream());
				this.output = new DataOutputStream(socket.getOutputStream());
				
				//output.writeUTF("READ 12 13 8 \r\n\r\ntest.txt");
				output.writeUTF("new_txn -1 0 0\r\n\r\n\r\n");
				byte[] bs = new byte[2000];
				inputFromServer.read(bs);
				response = new String(bs);
				Logger.printClientInfo("response from server -- " + response);

			} catch (SocketTimeoutException e) {
				Logger.printClientInfo("Socket time out. Please try again.");
			} 
			// query = ClientHelper.getQuery();
		//}

		// close streams and sockets after received response
		output.close();
		inputFromServer.close();
		socket.close();
	}

	/**
	 * Main function
	 * @param args
	 */
	public static void main(String[] args) {
		try {
//			if(args.length != 2){
//				throw new IllegalArgumentException("Only accept 1 arguments: ip address!");
//			}
			//String data = args[2] + " " + args[3] + " " + args[4] + " " + args[5] + " " + args[6];
			DFSClient client = new DFSClient(args[0], Integer.parseInt(args[1]));
			client.run();
		} catch (IOException | IllegalArgumentException e) {
			e.printStackTrace();
			Logger.printClientInfo(e.getLocalizedMessage());
		}
	}

}
