package util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.Random;

/**
 * ClientHelper class for client side 
 * @author Shiqi Luo
 */
public class ClientHelper {
	
	/**
	 * @return Query string based on customer's selection
	 * @throws IOException
	 */
	public static String getQuery() throws IOException {
		BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
		
		String query = "";

		while(true){
			System.out.print("Select operation: \n"
					+ "1. get\n"
					+ "2. put\n"
					+ "3. delete\n"
					+ "4. exit\n"
					+ "\n--> ");
	
			String line = input.readLine();
		
			if(line.length() == 1 
				&& Character.isDigit(line.charAt(0))
				&& (line.charAt(0) - '0') >= 1
				&& (line.charAt(0) - '0') <= 4){
	
				int selection = Integer.parseInt(line);
				
				if(selection == 4){
					return null;
				}
				
				System.out.print("Key: ");
				String key = input.readLine();
				
				if(selection == 1){
					query = "get " + key;
				}
				else if(selection == 2){
					System.out.print("Value: ");
					String value = input.readLine();
					query = "put " + key.length() + " " + key + value;
				}
				else{
					query = "delete " + key;
				}
		
				return query;
			}
			else{
				printInstruction();
			}
		}
	}

	/**
	 * Print user instruction to show how to run the program
	 */
	public static void printInstruction(){
		String message = "--------------------------- Instruction ------------------------\n\n"
						+ "Only server host ip address and port are accepted as arguments to run program\n"
						+ "e.g. java client/TCPClient <ipaddress> <port>\n\n"
						+ "Please select one of the operation below by input the operation number\n\n"
						+ "1. get    --> get the value for a specified key from key value store\n\n"
						+ "2. put    --> put a key with the value to the key value store\n\n"
						+ "3. delete --> delete a key from key value store\n\n"
						+ "4. exit   --> exit the client program\n\n";
		
		System.out.println(message);
	}
	
	public static Socket randomSelectServer(){
		while(true){
			try{
				Random random = new Random();
				int port = 10 * (random.nextInt() % Constants.replicaNum);
				Socket res = new Socket("localhost", 16810 + port);
				
				return res;
			} catch (Exception e){
				Logger.logServerInfo("connecting to server...", "");
			}
		}
	}
}
