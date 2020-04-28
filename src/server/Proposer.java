package server;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import util.Constants;
import util.Logger;

/**
 * @author Shiqi Luo
 *
 */
public class Proposer {

	private ConcurrentHashMap<ProposerConnectThread, Integer> acceptors;
	private String message;
	private int port;
	private AtomicInteger promiseCount = new AtomicInteger(0);
	private AtomicInteger rejectCount = new AtomicInteger(0);
	private AtomicInteger proposeNumberDiffCount = new AtomicInteger(0);
	// As professor metioned, we don't focus fault tolerance right now
	private AtomicInteger inactiveCount = new AtomicInteger(0);
	
	private AtomicInteger acceptAnnounceCount = new AtomicInteger(0);
	private AtomicInteger acceptAnnounceFailCount = new AtomicInteger(0);
	
	private KeyValueStoreLearner keyValueStore;
	
	private AtomicBoolean justStart;
	
	private AtomicLong maxProposeNumber = new AtomicLong(0);
	/**
	 * @throws IOException 
	 * @throws UnknownHostException 
	 * 
	 */
	public Proposer(int port, KeyValueStoreLearner keyValueStore) throws UnknownHostException, IOException {
		acceptors = new ConcurrentHashMap<>();
		this.port = port+1;
		this.keyValueStore = keyValueStore;
		this.justStart = new AtomicBoolean(true);
		Logger.logServerEvent("Proposer up and started. ", Thread.currentThread().getId()+"");
	}
	
	public void sendPrepareMessage(String request) throws UnknownHostException, IOException{
		if(this.acceptors.size() == 0){
			inactiveCount = new AtomicInteger(0);
			for(int i = 16811; i < 16811 + (Constants.replicaNum * 10); i+=10){
				if(i != port){
					ProposerConnectThread acceptorConnectThread = new ProposerConnectThread(i);
					acceptorConnectThread.start();
					acceptors.put(acceptorConnectThread, 0);
				}
				
			}
			Logger.logServerEvent("init proposer, acceptor size: " + acceptors.size(), Thread.currentThread().getId()+"");
		}
		this.message = request;
		this.rejectCount = new AtomicInteger(0);
		this.promiseCount = new AtomicInteger(0);
		this.acceptAnnounceCount = new AtomicInteger(0);
		this.acceptAnnounceFailCount = new AtomicInteger(0);
		
		keyValueStore.setMaxProposeNumber(keyValueStore.getMaxProposeNumber() + 1);
		maxProposeNumber.set(keyValueStore.getMaxProposeNumber());

		for(ProposerConnectThread thread: this.acceptors.keySet()){
			if(thread.isActive() == false && thread.isAlive() == false){
				thread.start();
			}
			thread.setRunState();
		}
	}
	
	public void finishTransaction(){
		Logger.logServerEvent("finish transaction", Thread.currentThread().getId()+"");
		for(ProposerConnectThread thread: this.acceptors.keySet()){
			thread.setStopState();
		}
	}
	
	public String generatePutMessage(String key, String value){
		return "put " + key.length() + " " + key + value;
	}
	
	public String generateDelMessage(String key){
		return "delete " + key;
	}
	
	public boolean finishVote(){
		for(ProposerConnectThread thread: this.acceptors.keySet()){
			if(thread.isGetResponse() == false && thread.isActive() == true){
				return false;
			}
		}
		//Logger.logServerEvent("proposer status: accept announce-" + acceptAnnounceCount.get() + ", accept fail-" + acceptAnnounceFailCount.get() + ", inactive acceptor-" + inactiveCount.get(), Thread.currentThread().getId()+"");

		return this.acceptAnnounceCount.get() + this.acceptAnnounceFailCount.get() + this.inactiveCount.get() == Constants.replicaNum - 1;
	}
	
	public boolean checkVote(){
		Logger.logServerEvent("proposer status: accept announce-" + acceptAnnounceCount.get() + ", inactive acceptor-" + inactiveCount.get(), Thread.currentThread().getId()+"");

		if(this.acceptAnnounceCount.get() + this.inactiveCount.get() == Constants.replicaNum - 1){
			return true;
		}
		else{
			return false;
		}
	}
	
	private class ProposerConnectThread extends Thread {
		private Socket socket;
		private int port;
		private DataInputStream input = null;
		private DataOutputStream output = null;
		private boolean runState = false;
		
		private boolean getResponse = false;
		
		private boolean isActive = true;

		public ProposerConnectThread(int port) {
			this.port = port;
		}
		
		public void setRunState(){
			runState = true;
		}
		
		public void setStopState(){
			runState = false;
			this.getResponse = false;
		}

		public boolean isGetResponse(){
			return this.getResponse;
		}
		
		public boolean isActive(){
			return this.isActive;
		}

		@Override
		public void run() {
			while(true){
				while(this.runState == false || this.getResponse == true){
					try {
						sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				try {
					try{
						//acceptAnnounceFailCount = new AtomicInteger(0);
						this.socket = new Socket("localhost", port);
						this.socket.setSoTimeout(4 * 1000);
					} catch (ConnectException ce){
						if(isActive == true){
							inactiveCount.getAndIncrement();
							isActive = false;
						}
						continue;
					}

					if(isActive == false){
						inactiveCount.getAndDecrement();
						isActive = true;
					}
					this.input = new DataInputStream(socket.getInputStream());
					this.output = new DataOutputStream(socket.getOutputStream());
					// send prepare message
					this.output.writeUTF(keyValueStore.getMaxProposeNumber()+":"+message);
					
					Logger.logServerEvent("proposer send a prepare message: " + message + ", to " + this.socket.getRemoteSocketAddress(), 
							Thread.currentThread().getId()+"");
				
					// waiting for promise response
					String msg = this.input.readUTF();
					Logger.logServerEvent("proposer get promise response: " + msg + ", from " + this.socket.getRemoteSocketAddress(),
							Thread.currentThread().getId()+"");

					String[] msg_value = msg.split(":");
					long proposeNum = Long.parseLong(msg_value[0]);

					synchronized (maxProposeNumber) {
						if(proposeNum >= maxProposeNumber.get()){
							if(proposeNum > maxProposeNumber.get()){
								maxProposeNumber.set(proposeNum);
								proposeNumberDiffCount.getAndIncrement();
							}
							promiseCount.getAndIncrement();
						}
						else{
							rejectCount.getAndIncrement();
						}
					}

					while(promiseCount.get() + rejectCount.get() + inactiveCount.get() < Constants.replicaNum - 1){
						sleep(100);
					}
					Logger.logServerEvent("promise result: promise-" + promiseCount + ", reject-" + rejectCount, Thread.currentThread().getId()+"");
					
					
					if(proposeNumberDiffCount.get() > 0){
						if(justStart.get() == false){
							maxProposeNumber.getAndIncrement();
						}
						else{
							synchronized (message) {
								message = msg_value[1];
							}
						}
					}
					
					// send accept message
					if(promiseCount.get() + 1 > (Constants.replicaNum - 1) / 2){
						this.output.writeUTF(maxProposeNumber.get()+":"+message);
						Logger.logServerEvent("proposer sent accept request to " + this.socket.getRemoteSocketAddress(),
								Thread.currentThread().getId()+"");
					}
					else{
						this.output.writeUTF("abort");
						Logger.logServerEvent("proposer sent abort request to " + this.socket.getRemoteSocketAddress(),
								Thread.currentThread().getId()+"");
						this.output.close();
						this.input.close();
						this.socket.close();
						getResponse = true;
						runState = false;
						acceptAnnounceFailCount.getAndIncrement();
						continue;
					}

					// waiting accepted response
					String ackMsg = this.input.readUTF();
					Logger.logServerEvent("proposer get accepted response: " + ackMsg + " from " + this.socket.getRemoteSocketAddress(),
							Thread.currentThread().getId()+"");

					getResponse = true;

					if(!ackMsg.equals("accepted")){
						runState = false;
						acceptAnnounceFailCount.incrementAndGet();
					}
					else{
						keyValueStore.setMaxProposeNumber(maxProposeNumber.get());
						acceptAnnounceCount.getAndIncrement();
					}
					
					if(justStart.get() == true){
						justStart.set(false);
					}
					this.output.close();
					this.input.close();
					this.socket.close();
				} catch (InterruptedException | IOException e2) {
					e2.printStackTrace();
					try {
						this.output.close();
						this.input.close();
					} catch (IOException e3) {
						e3.printStackTrace();
					}
					Logger.logServerError(e2.toString(), this.socket.getRemoteSocketAddress().toString());
				}
			}
		}

	}
}