package server;

import java.util.concurrent.ConcurrentHashMap;

import util.Logger;

/**
 * @author Shiqi Luo
 *
 */
public class KeyValueStoreLearner {

	private ConcurrentHashMap<String, String> data = null;
	private String clientId = "";
	
	private boolean isWriteLock = false;
	
	private Long maxProposeNum = (long) 0;
	/**
	 * 
	 */
	public KeyValueStoreLearner() {
		this.data = new ConcurrentHashMap<>();
		Logger.logServerEvent("Learner created. ", Thread.currentThread().getId()+"");
	}
	
	public String get(String key) {
		if(this.data.containsKey(key)){
			return this.data.get(key);
		}
		return null;
	}
	
	public boolean put(String key, String value) {
		if(value == null || key == null){
			return false;
		}

		this.data.put(key, value);
		
		return true;
	}
	
	public boolean delete(String key) {
		if(this.data.remove(key) == null){
			return false;
		}
		
		return true;
	}
	
	public synchronized boolean isWriteLock(){
		return this.isWriteLock;
	}
	
	public synchronized void writeLock() throws InterruptedException{
		if(!this.isWriteLock){
			this.isWriteLock = true;
		}
		else{
			throw new InterruptedException("Can't lock write right now, it's already locked");
		}
	}
	
	public synchronized void writeUnlock() throws InterruptedException{
		if(this.isWriteLock){
			this.isWriteLock = false;
		}
		else{
			throw new InterruptedException("Can't unlock write right now, it's already unlocked");
		}
	}
	
	public synchronized Long getMaxProposeNumber(){
		return this.maxProposeNum;
	}
	
	
	public synchronized void setMaxProposeNumber(Long newNumber){
		if(newNumber > this.maxProposeNum){
			this.maxProposeNum = newNumber;
		}
	}
}
