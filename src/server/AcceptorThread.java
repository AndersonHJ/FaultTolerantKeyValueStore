package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import Model.OperationType;
import Model.TransactionMessage;
import util.Logger;

/**
 * @author Shiqi Luo
 *
 */
public class AcceptorThread implements Runnable {

	// TCP part
	private Socket proposer = null;
	private ServerSocket listenerSocket = null;
	private DataInputStream input = null;
	private DataOutputStream output = null;
	
	private KeyValueStoreLearner keyValueStore = null;
	
	private Long maxProposeNumber = (long) 0;
	private String curMessage = "";
	
	/**
	 * @param port
	 * @throws IOException 
	 * 
	 */
	public AcceptorThread(int port, KeyValueStoreLearner keyValueStore) throws IOException {
		this.listenerSocket = new ServerSocket(port+1);
		this.keyValueStore = keyValueStore;
	}
	
	@Override
	public void run() {
		String msg = "";
		String acceptMsg = "";
		String response = "";
		Logger.logServerEvent("Acceptor up and started. ", Thread.currentThread().getId()+"");
		
		while(true){
			try{
				this.proposer = this.listenerSocket.accept();
				this.input = new DataInputStream(proposer.getInputStream());
				this.output = new DataOutputStream(proposer.getOutputStream());
				
				// waiting for prepare message
				msg = this.input.readUTF();
				Logger.logServerEvent("acceptor get a prepare message: " + msg + ", from " + this.proposer.getRemoteSocketAddress(), 
						Thread.currentThread().getId()+"");

				String[] info = msg.split(":");
				
				maxProposeNumber = Long.parseLong(info[0]);
				
				if(maxProposeNumber >= keyValueStore.getMaxProposeNumber()){
					// send promise
					this.output.writeUTF(msg);
					Logger.logServerEvent("acceptor response promise to " + this.proposer.getRemoteSocketAddress(), Thread.currentThread().getId()+"");
				}
				else{
					// send abort promise
					this.output.writeUTF(keyValueStore.getMaxProposeNumber()+":"+info[1]);
					Logger.logServerEvent("acceptor got lower propose number from " + this.proposer.getRemoteSocketAddress(), Thread.currentThread().getId()+"");
				}

				// waiting for accept message
				acceptMsg = this.input.readUTF();
				Logger.logServerEvent("acceptor get accept message: " + acceptMsg + ", from " + this.proposer.getRemoteSocketAddress(), 
						Thread.currentThread().getId()+"");
				
				if(acceptMsg.equals("abort")){
					continue;
				}

				String[] acceptInfo = msg.split(":");
				
				maxProposeNumber = Long.parseLong(acceptInfo[0]);
				keyValueStore.setMaxProposeNumber(maxProposeNumber);
				curMessage = new String(acceptInfo[1]);
				
				TransactionMessage request = TransactionMessage.parseQuery(curMessage);
				
				while(this.keyValueStore.isWriteLock()){
					Thread.sleep(500);
				}
				
				if(!this.keyValueStore.isWriteLock()){
					this.keyValueStore.writeLock();
					// lock the resource and send back the vote-commit message
					Logger.logServerEvent("acceptor write lock resource", Thread.currentThread().getId()+"");
					
					// commit the action if received commit message

					boolean result = true;
					if(request.getOperationType().equals(OperationType.put)){
						result = this.keyValueStore.put(request.getKey(), request.getValue());
						if(result == false){
							this.output.writeUTF("abortack");
						}
						response = "put operation " + (result == true ? "successed" : "failed");
						Logger.logServerEvent("acceptor " + response, Thread.currentThread().getId()+"");
					}
					else if(request.getOperationType().equals(OperationType.delete)){
						result = this.keyValueStore.delete(request.getKey());
						if(result == false){
							this.output.writeUTF("abortack");
						}
						response = "delete operation " + (result == true ? "successed" : "failed(key is not existed)");
						Logger.logServerEvent("acceptor " + response, Thread.currentThread().getId()+"");
					}
					else{
						this.output.writeUTF("abortack");
						throw new IllegalArgumentException("operation: " + msg + " is not supported");
					}

					if(result == false){
						this.output.writeUTF("abortack");
						Logger.logServerEvent("acceptor response abort ack to " + this.proposer.getRemoteSocketAddress(), 
								Thread.currentThread().getId()+"");
					}
					else{
						this.output.writeUTF("accepted");
						Logger.logServerEvent("acceptor response commit ack to " + this.proposer.getRemoteSocketAddress(), 
								Thread.currentThread().getId()+"");
					}

					this.keyValueStore.writeUnlock();
					Logger.logServerEvent("acceptor write unlock resource", Thread.currentThread().getId()+"");
				}

			} catch(Exception e2) {
				e2.printStackTrace();
				Logger.logServerError(e2.toString(), this.proposer.getRemoteSocketAddress().toString());
				try {
					if(this.keyValueStore.isWriteLock()){
						this.keyValueStore.writeUnlock();
					}
					this.output.close();
					this.input.close();
					this.proposer.close();
				} catch (IOException | InterruptedException e3) {
					Logger.logServerError(e3.toString(), this.proposer.getRemoteSocketAddress().toString());
				}
				
			}
		}
	}
}
