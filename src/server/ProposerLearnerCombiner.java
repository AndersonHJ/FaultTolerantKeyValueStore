package server;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;

import Model.OperationType;
import Model.Request;
import util.Logger;

/**
 * Key value store thread using TCP protocol
 * @author Shiqi Luo
 */
public class ProposerLearnerCombiner implements Runnable {
	// TCP part
	private Socket client = null;
	private DataInputStream input = null;
	private DataOutputStream output = null;
	private Proposer proposer;
	
	private KeyValueStoreLearner keyValueStore = null;

	/**
	 * Constructor of Key value TCP thread
	 * @param service
	 * @param port
	 * @throws IOException
	 */
	public ProposerLearnerCombiner(Socket client, KeyValueStoreLearner keyValueStoreLearner, Proposer proposer) throws IOException {
		this.client = client;
		this.keyValueStore = keyValueStoreLearner;
		this.proposer = proposer;
	}
	
	@Override
	public void run() {
		String query = "";
		String response = "";
		
		try{
			
			this.input = new DataInputStream(new BufferedInputStream(this.client.getInputStream()));
			this.output = new DataOutputStream(client.getOutputStream());
			
			query = this.input.readUTF();
			Logger.logServerInfo("get query:[" + query + "]", this.client.getRemoteSocketAddress().toString());

			Request request = Request.parseQuery(query);
			
			if(request.getOperationType().equals(OperationType.get)){
				String value = this.keyValueStore.get(request.getKey());
				response = ((value == null) ? "Don't have the value of key -> \"" + request.getKey() + "\"": value);
				this.output.writeUTF(response);
			}
			else if(request.getOperationType().equals(OperationType.put)){
				proposer.sendPrepareMessage(proposer.generatePutMessage(request.getKey(), request.getValue()));
				
				while(!proposer.finishVote()){
					;
				}
				
				boolean voteResult = proposer.checkVote();
				if(voteResult == true){
					this.keyValueStore.writeLock();
					boolean result = this.keyValueStore.put(request.getKey(), request.getValue());
					this.keyValueStore.writeUnlock();
					response = "put operation " + (result == true ? "successed" : "failed");
				}
				else{
					response = "put operation failed because of vote abort.";
				}
				this.proposer.finishTransaction();
				this.output.writeUTF(response);
				
			}
			else if(request.getOperationType().equals(OperationType.delete)){
				proposer.sendPrepareMessage(proposer.generateDelMessage(request.getKey()));
				
				while(!proposer.finishVote()){
					;
				}
				
				boolean voteResult = proposer.checkVote();
				if(voteResult == true){
					this.keyValueStore.writeLock();
					boolean result = this.keyValueStore.delete(request.getKey());
					this.keyValueStore.writeUnlock();
					response = "delete operation " + (result == true ? "successed" : "failed(key is not existed)");
				}
				else{
					response = "delete operation failed.";
				}
				this.proposer.finishTransaction();
				this.output.writeUTF(response);
			}
			
			Logger.logServerInfo("response:[" + response + "]", this.client.getRemoteSocketAddress().toString());
			
			this.client.close();

		} catch (Exception e2) {
			e2.printStackTrace();
			Logger.logServerError(e2.toString(), this.client.getRemoteSocketAddress().toString());
			try {
				this.output.writeUTF(e2.toString());
				this.output.close();
				this.input.close();
			} catch (IOException e3) {
				e3.printStackTrace();
			}
		}
		
		try {
			this.client.close();
		} catch (IOException e) {
			Logger.logServerError(e.toString(), this.client.getRemoteSocketAddress().toString());
		}
	}
}
