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
 * Class of TCP client
 * @author Shiqi Luo
 */
public class TCPClient {
	private Socket socket;
	private DataInputStream inputFromServer = null;
	private DataOutputStream output = null;
	
	private String address = null;
	private int port = 0;
	
	/**
	 * Constructor of TCP client
	 * @param address
	 * @param port
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public TCPClient(String address, int port) throws UnknownHostException, IOException{
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
		
		query = ClientHelper.getQuery();

		while(query != null){
			try{
				this.socket = ClientHelper.randomSelectServer();
				Logger.printClientInfo("connect with server -- " + this.socket.getRemoteSocketAddress());
				this.socket.setSoTimeout(10*1000);
				this.inputFromServer = new DataInputStream(socket.getInputStream());
				this.output = new DataOutputStream(socket.getOutputStream());
				
				output.writeUTF(query);
				response = inputFromServer.readUTF();
				Logger.printClientInfo("response from server -- " + response);

			} catch (SocketTimeoutException e) {
				Logger.printClientInfo("Socket time out. Please try again.");
			} finally {
				inputFromServer.close();
				output.close();
				socket.close();
			}

			query = ClientHelper.getQuery();
		}

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
			if(args.length != 1){
				throw new IllegalArgumentException("Only accept 1 arguments: ip address!");
			}
			TCPClient client = new TCPClient(args[0], 0);
			client.run();
		} catch (IOException | IllegalArgumentException e) {
			Logger.printClientInfo(e.getLocalizedMessage());
		}
	}

}
