package nassist.api.client.example;

import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import nassist.api.client.example.dto.Authenticate;
import nassist.api.client.example.dto.AuthenticateResponse;
import nassist.api.client.example.dto.AzureEvent;
import nassist.api.client.example.dto.DataPoint;
import nassist.api.client.example.dto.EventsBatch;
import nassist.api.client.example.dto.EventsBatchResponse;
import nassist.api.client.example.dto.InstallationSensors;
import nassist.api.client.example.dto.InstallationSensorsResponse;
import nassist.api.client.example.dto.Sensor;
import nassist.api.client.example.dto.SensorStatuses;
import nassist.api.client.example.dto.SensorStatusesResponse;
import nassist.api.client.example.dto.SensorValues;
import nassist.api.client.example.dto.SensorValuesResponse;
import nassist.api.client.example.dto.StatusPoint;
import nassist.api.client.impl.APIClient;
import net.servicestack.client.WebServiceException;

public class Program {
	private static APIClient client;

	public static void main (String[] args){
		Authenticate auth = new Authenticate();
		auth.setUserName("demo");
		auth.setPassword("demo");

		client = new APIClient("http://dev.nassist-test.com/api", auth);

		uploadSensorValues();

		uploadSensorStatuses();

		getSensorValues();

		getSensorStatuses();

		getSensorsForInstallation();

		getNotificationsByType();
	}

	public static void uploadSensorValues(){
		SensorValues uploadValuesRequest = new SensorValues();
		uploadValuesRequest.setId("127126ef-a96a-4177-9a7f-cd28f0e79326");

		ArrayList<DataPoint> dataPoints = new ArrayList<>();

		DataPoint data = new DataPoint();
		data.setDate(new Date());
		data.setValue(new Random().nextDouble());

		dataPoints.add(data);

		uploadValuesRequest.setDataPoints(dataPoints);

		try{
			client.post(uploadValuesRequest);
			System.out.println("Data upload success!");
		} catch (WebServiceException e){
			System.out.println("Error uploading data: " + e.getErrorCode() + " " + e.getErrorMessage());
		}
	}

	public static void uploadSensorStatuses(){
		SensorStatuses uploadStatusesRequest = new SensorStatuses();
		uploadStatusesRequest.setId("127126ef-a96a-4177-9a7f-cd28f0e79326");
		ArrayList<StatusPoint> statusPoints = new ArrayList<>();

		StatusPoint status = new StatusPoint();
		status.setDate(new Date());
		status.setStatus("ok");
		status.setTrigger(client.<AuthenticateResponse>getUserAuth().getUserId().toString());
		status.setTriggerName(client.<AuthenticateResponse>getUserAuth().getUserName());

		statusPoints.add(status);

		uploadStatusesRequest.setStatusPoints(statusPoints);

		try{
			client.post(uploadStatusesRequest);
			System.out.println("Status upload success!");
		} catch (WebServiceException e){
			System.out.println("Error uploading statuses: " + e.getErrorCode() + " " + e.getErrorMessage());
		}
	}

	public static void getSensorValues(){
		SensorValues valuesRequest = new SensorValues();
		valuesRequest.setId("127126ef-a96a-4177-9a7f-cd28f0e79326");

		SensorValuesResponse valuesResponse = client.get(valuesRequest);

		for(int i= 0; i < valuesResponse.Values.size(); i++){
			System.out.println(valuesResponse.Values.get(i).Value);
		}
	}

	public static void getSensorStatuses(){
		SensorStatuses statusesRequest = new SensorStatuses();
		statusesRequest.setId("127126ef-a96a-4177-9a7f-cd28f0e79326");

		SensorStatusesResponse statusesResponse = client.get(statusesRequest);

		for(int i= 0; i < statusesResponse.Statuses.size(); i++){
			System.out.println(statusesResponse.Statuses.get(i).Status);
		}
	}

	public static void getSensorsForInstallation(){
		InstallationSensors allSensorsRequest = new InstallationSensors();
		allSensorsRequest.Id = UUID.fromString("00000000-0000-0000-0000-b827eb9e544b");

		InstallationSensorsResponse installationSensors = client.get(allSensorsRequest);

		for(Sensor s : installationSensors.Sensors){
			System.out.println(s.getId() + " " + s.getName() + " " + s.getType());
		}
	}

	public static void getNotificationsByType(){
		EventsBatch eventsRequest = new EventsBatch();
		eventsRequest.setUserId(client.<AuthenticateResponse>getUserAuth().getUserId().toString());
		eventsRequest.setType("security");

		EventsBatchResponse eventsResponse = client.get(eventsRequest);

		for(AzureEvent event : eventsResponse.Events){
			System.out.println(event.Subtype + " " + event.TranslatedDescription);
		}
	}
}
