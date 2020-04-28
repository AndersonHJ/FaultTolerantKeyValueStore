package util;

/**
 * Logger class, is used to print/generate log on server/client side
 * @author Shiqi Luo
 */
public class Logger {

	/**
	 * Log information logs on server side
	 * @param message
	 * @param clientAddr
	 */
	public static void logServerInfo(String message, String clientAddr){
		System.out.println(System.currentTimeMillis() + "  Info: " + message + "\n" + "-- Client: " + clientAddr + "\n");
	}
	
	/**
	 * Log information logs on server side
	 * @param message
	 * @param threadId
	 */
	public static void logServerEvent(String message, String threadId){
		System.out.println(System.currentTimeMillis() + "  Event: " + message + "\n" + "-- Thread: " + threadId + "\n");
	}
	
	/**
	 * Log errors on server side
	 * @param error
	 * @param clientAddr
	 */
	public static void logServerError(String error, String clientAddr){
		System.out.println(System.currentTimeMillis() + "  Error: " + error + "\n" + "-- Client: " + clientAddr + "\n");
	}
	
	/**
	 * Print information on client side
	 * @param info
	 */
	public static void printClientInfo(String info){
		System.out.println(System.currentTimeMillis() + "  Message: " + info + "\n\n");
	}

}
