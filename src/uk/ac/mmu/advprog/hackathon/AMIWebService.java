package uk.ac.mmu.advprog.hackathon;
import static spark.Spark.get;
import static spark.Spark.port;

import spark.Request;
import spark.Response;
import spark.Route;

/**
 * Handles the setting up and starting of the web service
 * You will be adding additional routes to this class, and it might get quite large
 * Feel free to distribute some of the work to additional child classes, like I did with DB
 * @author Arslan Tabusam!
 */
public class AMIWebService {

	/**
	 * Main program entry point, starts the web service
	 * @param args not used
	 */
	public static void main(String[] args) {		
		port(8088);
		
		//Simple route so you can check things are working...
		//Accessible via http://localhost:8088/test in your browser
		get("/test", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				try (DB db = new DB()) {
					return "Number of Entries: " + db.getNumberOfEntries();
				}
			}			
		});
		
		//route to get the last signal displayed 
		get("/lastsignal", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				
				String signal_id = request.queryParams("signal_id");
				
				//checks if there are errors, and displays appropriate message
				if(signal_id == null || signal_id.equals("")) {
					response.status(400);
					return "no results";
				}
				
				//checks if the second part after /, of the signal is 6 character long as it should be
				else if(signal_id.contains("/")) {
					String[] strings = signal_id.split("/");
					
					if(strings[1].substring(0, strings[1].length()).length() < 6) {
						return "Please set a valid signal code after \"/\".";
					}
					//displays the results is everything is correct
						try(DB db = new DB()){
							if(db.getLastSignal(signal_id) == null || db.getLastSignal(signal_id) == "" ) {
								return "no results";
							}
							return db.getLastSignal(signal_id);
						}
				}
				
				else {
					try(DB db = new DB()){
						return db.getLastSignal(signal_id);
					}
				}
			}
		});
		
		//route to retrieve the most frequesnt signal disaplyed on a specifi motorway
		get("/frequentlyused", new Route() {
			@Override
			
			public Object handle(Request request, Response response) throws Exception {
				
				String motorway = request.queryParams("motorway");
				
				//sends a  Content-Type HTTP header to inform client of the xml format
				response.header("Accept", "application/xml");
				response.header("Content-type", "application/xml");
				
				//checks if there are any error in the input and if there are returns an auto closing xml tag
				if(motorway == null || motorway.equals("")) {
					response.status(400);
					try(DB db = new DB()){
						return db.getMostFrequentSignalsOnMotorway(motorway);
					}
				}
				else {
					try(DB db = new DB()){
						return db.getMostFrequentSignalsOnMotorway(motorway);
					}
				}
			}
		});
		
		//this route get a array of signal groups in json format(jsonarray)
		get("/signalgroups", new Route() {
			@Override
			
			public Object handle(Request request, Response response) throws Exception {
				
				//sends a  Content-Type HTTP header to inform client of the json format
				response.header("Accept", "application/json");
				response.header("Content-type", "application/json");
				
				try(DB db = new DB()){
					return db.getSignalGroups().toString();
				}
			}
		});
		
		//this route gives the signals displayed on a strecth of motorway on a specific date and time
		get("/signalsbygrouptime", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				
				//sends a  Content-Type HTTP header to inform client of the json format
				response.header("Accept", "application/json");
				response.header("Content-type", "application/json");
				
				
				String group = request.queryParams("group");
				String time = request.queryParams("time");
				
				//does a check for irregularities on both group and time parameters
				if(group == null || group.equals("") || time == null || time.equals("")) {
					response.status(400);
					return "no results";
				}
				else {
					try(DB db = new DB()){
								return db.getsignalsByGroupTime(group, time);
					}
				}
			}
		});
		
		System.out.println("Server up! Don't forget to kill the program when done!");
	}

}
