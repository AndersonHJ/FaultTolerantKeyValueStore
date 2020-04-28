package Model;

import java.security.InvalidParameterException;

/**
 * Request class
 * @author Shiqi Luo
 */
public class TransactionMessage {

	OperationType operationType;
	String key = null;
	String value = null;
	
	/**
	 * Constructor of TransactionMessage class
	 * @param messageOperation
	 * @param key
	 * @param value
	 * @throws Exception
	 */
	public TransactionMessage(String messageOperation, String key, String value) throws Exception {
		this.operationType = parseOperationType(messageOperation);
		this.key = key;
		this.value = value;
	}
	
	/**
	 * Constructor of TransactionMessage class
	 * @param messageOperation
	 * @param key
	 * @throws Exception
	 */
	public TransactionMessage(String messageOperation, String key) throws Exception {
		this.operationType = parseOperationType(messageOperation);
		this.key = key;
		this.value = null;
	}
	
	/**
	 * @return the operationType
	 */
	public OperationType getOperationType() {
		return operationType;
	}

	/**
	 * @return the key
	 */
	public String getKey() {
		return key;
	}

	/**
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * Parse query string to create TransactionMessage instance
	 * @param query
	 * @return
	 * @throws Exception
	 */
	public static TransactionMessage parseQuery(String query) throws Exception {
		if(query == null || query.isEmpty()){
			throw new InvalidParameterException("Query string can't be null or empty");
		}

		String operation = "";
		String key = "";
		String value = null;
		
		if(query.startsWith("put ")){
			operation = "put";
			char[] chars = query.toCharArray();
			int index = 4;
			int keyLength = 0;
			while(chars[index] != ' '){
				if(!Character.isDigit(chars[index])){
					throw new IllegalArgumentException("Length of key length for put operation must be digit number");
				}
				
				keyLength = keyLength * 10 + (chars[index] - '0');
				index++;
			}
			index++;
			if(index + keyLength > chars.length){
				throw new IllegalArgumentException("Key length exceeded actual content length");
			}

			key = query.substring(index, index + keyLength);
			value = query.substring(index + keyLength);
		}
		else if(query.startsWith("delete ")){
			operation = "delete";
			key = query.substring(7);
		}
		else{
			throw new IllegalArgumentException(query + "Operation is not supported");
		}
		
		return new TransactionMessage(operation, key, value);
	}
	
	/**
	 * Get the OperationType from string
	 * @param messageOperation
	 * @throws Exception 
	 */
	private OperationType parseOperationType(String messageOperation) throws Exception {
		if(messageOperation.toLowerCase().equals("put")){
			return OperationType.put;
		}
		else if(messageOperation.toLowerCase().equals("delete")){
			return OperationType.delete;
		}
		else{
			throw new Exception(messageOperation + " - Unsupported operation exception.");
		}
	}


}
