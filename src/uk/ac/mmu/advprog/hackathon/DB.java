package uk.ac.mmu.advprog.hackathon;

import java.io.StringWriter;
import java.io.Writer;
import java.sql.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.json.*;
import org.w3c.dom.*;

/**
 * Handles database access from within your web service
 * @author Arslan Tabusam!
 */
public class DB implements AutoCloseable {
	
	//allows us to easily change the database used
	private static final String JDBC_CONNECTION_STRING = "jdbc:sqlite:./data/AMI.db";
	
	//allows us to re-use the connection between queries if desired
	private Connection connection = null;
	
	/**
	 * Creates an instance of the DB object and connects to the database
	 */
	public DB() {
		try {
			connection = DriverManager.getConnection(JDBC_CONNECTION_STRING);
		}
		catch (SQLException sqle) {
			error(sqle);
		}
	}
	
	/**
	 * Returns the number of entries in the database, by counting rows
	 * @return The number of entries in the database, or -1 if empty
	 */
	public int getNumberOfEntries() {
		int result = -1;
		try {
			Statement s = connection.createStatement();
			ResultSet results = s.executeQuery("SELECT COUNT(*) AS count FROM ami_data");
			while(results.next()) { //will only execute once, because SELECT COUNT(*) returns just 1 number
				result = results.getInt(results.findColumn("count"));
			}
		}
		catch (SQLException sqle) {
			error(sqle);
			
		}
		return result;
	}
	
	/**
	 * Returns the last signal displayed ignoring OFF, BLNK and NR signals as last signal
	 * @param a string parameter containing signal id
	 * @return The last signal displayed, or nothing(blank) if empty
	 */
	public String getLastSignal(String signal_id) {
		String result = "";
		try {
			PreparedStatement s = connection.prepareStatement("SELECT signal_value AS value FROM ami_data "
					+ "WHERE signal_id LIKE ? "
					+ "AND NOT signal_value = \"OFF\" "
					+ "AND NOT signal_value = \"NR\" "
					+ "AND NOT signal_value = \"BLNK\" "
					+ "ORDER BY datetime DESC "
					+ "LIMIT 1");
			s.setString(1, signal_id);
			ResultSet results = s.executeQuery();
			
			while(results.next()) { //will only execute once, because the last signal is only one
				result = results.getString(results.findColumn("value"));
			}
		}
		catch (SQLException sqle) {
			error(sqle);
			
		}
		return result;
	}
	
	/**
	 * Returns most frequently displayed signals on a specific motorway in xml-format
	 * @param string parameter containing signal id
	 * @return most frequent signals, empty xml header if the there is an error
	 */
	public String getMostFrequentSignalsOnMotorway(String signal_id) {
		String result = "";
		
		//if the signal inputted is null or empty it will make the signal 0 which will make the result empty
		if(signal_id == null || signal_id.equals("")) {
			signal_id = "0";
		}
		
		try {
		//parametised query to match user input to the results obtained from sql query
		PreparedStatement s = connection.prepareStatement("SELECT COUNT(signal_value) AS frequency, signal_value FROM ami_data "
				+ "WHERE signal_id LIKE ? "
				+ "GROUP BY signal_value "
				+ "ORDER BY frequency DESC ");
		s.setString(1, signal_id + "%");
		ResultSet results = s.executeQuery();
		
		//creates a new document in order to build xml tree graph
		DocumentBuilderFactory documentFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder documentBuilder = documentFactory.newDocumentBuilder();
		Document document = documentBuilder.newDocument();
		
		//root element of xml tree graph 
		Element root = document.createElement("SignalTrends");
			while(results.next()) { //executes until the end of the result set
				
				Element signal = document.createElement("Signal");
				Element value = document.createElement("Value");
				Element frequency = document.createElement("Frequency");
				
				//populates the elements of xml graph
				value.setTextContent(results.getString("signal_value"));
				frequency.setTextContent(results.getString(results.findColumn("frequency")));
				signal.appendChild(value); signal.appendChild(frequency);
				root.appendChild(signal);
			}
			
			//this chunk helps turning the XML data to string
			Transformer transformer = TransformerFactory.newInstance().newTransformer();
			Writer output = new StringWriter();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.transform(new DOMSource(root), new StreamResult(output));
		    
			result = output.toString();
		}
		catch (SQLException | TransformerException | TransformerFactoryConfigurationError | ParserConfigurationException sqle) {
			sqle.printStackTrace();
		} 
		return result;
	}
	
	/**
	 * Returns a list of signal groups in a jsonarray format
	 * @return (json) array of signal groups
	 */
	public JSONArray getSignalGroups() {
		JSONArray sigGroups = new JSONArray();
		try {
			Statement s = connection.createStatement();
			ResultSet results = s.executeQuery("SELECT DISTINCT signal_group FROM ami_data");
			
			while(results.next()) { 
				//populates jsonarray with signal groups from sql query results
				sigGroups.put(results.getString("signal_group"));
			}
		}
		catch (SQLException sqle) {
			error(sqle);
			
		}
		return sigGroups;
	}
	
	/**
	 * Returns a string of signals displayed on a strecth of motorway in a specific date and time
	 * @param string parameter containing the input for a signal group
	 * @param string parameter consisting of time input
	 * @return list of objects containing signals displayes on a motorway af specific time 
	 */
	public String getsignalsByGroupTime(String group, String time) {
		JSONArray signalsByGroupTime = new JSONArray();
		
		try {
			//parametised query inserting matching 1 parameters twice inside the same query
			PreparedStatement s = connection.prepareStatement("SELECT datetime, signal_id, signal_value FROM ami_data WHERE signal_group = ? AND datetime < ? AND (datetime, signal_id) IN (SELECT MAX(datetime) AS datetime, signal_id FROM ami_data WHERE signal_group = ? AND datetime < ? GROUP BY signal_id) GROUP BY signal_id");
			s.setString(1, group);
			s.setString(2, time);
			s.setString(3, group);
			s.setString(4, time);
			
			ResultSet results = s.executeQuery();
			
			JSONObject signalGroup = new JSONObject();
			while(results.next()) { //will only execute once, because the last signal is only one
					
					//creates and populates json tree graph
					JSONObject signal = new JSONObject(); 
					signal.put("data-set", results.getString(results.findColumn("datetime")));
					signal.put("value", results.getString(results.findColumn("signal_value")));
					signalGroup.put(results.getString(results.findColumn("signal_id")), signal);
					signalsByGroupTime.put(signalGroup);
			}
		}
		catch (SQLException sqle) {
			error(sqle);
		}
		return signalsByGroupTime.toString();
	}
	
	/**
	 * Closes the connection to the database, required by AutoCloseable interface.
	 */
	@Override
	public void close() {
		try {
			if ( !connection.isClosed() ) {
				connection.close();
			}
		}
		catch(SQLException sqle) {
			error(sqle);
		}
	}

	/**
	 * Prints out the details of the SQL error that has occurred, and exits the programme
	 * @param sqle Exception representing the error that occurred
	 */
	private void error(SQLException sqle) {
		System.err.println("Problem Opening Database! " + sqle.getClass().getName());
		sqle.printStackTrace();
		System.exit(1);
	}
}
