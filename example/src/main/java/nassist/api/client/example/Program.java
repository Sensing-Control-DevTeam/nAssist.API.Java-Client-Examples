package nassist.api.client.example;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.joda.time.DateTime;

import nassist.api.client.example.dto.Authenticate;
import nassist.api.client.example.dto.AuthenticateResponse;
import nassist.api.client.example.dto.AzureEvent;
import nassist.api.client.example.dto.CameraPhoto;
import nassist.api.client.example.dto.CameraPhotoResponse;
import nassist.api.client.example.dto.DataPoint;
import nassist.api.client.example.dto.Events;
import nassist.api.client.example.dto.EventsBatch;
import nassist.api.client.example.dto.EventsBatchResponse;
import nassist.api.client.example.dto.Installation;
import nassist.api.client.example.dto.InstallationDetails;
import nassist.api.client.example.dto.InstallationDetailsResponse;
import nassist.api.client.example.dto.InstallationSensors;
import nassist.api.client.example.dto.InstallationSensorsResponse;
import nassist.api.client.example.dto.Photo;
import nassist.api.client.example.dto.Sensor;
import nassist.api.client.example.dto.SensorStatuses;
import nassist.api.client.example.dto.SensorStatusesResponse;
import nassist.api.client.example.dto.SensorValues;
import nassist.api.client.example.dto.SensorValuesResponse;
import nassist.api.client.example.dto.StatusPoint;
import net.servicestack.client.JsonServiceClient;
import net.servicestack.client.Utils;
import net.servicestack.client.WebServiceException;

public class Program {

	private static String USERNAME = "demo";
	private static String PASSWORD = "demo";

	private static String BASE_URL = "http://dev.nassist-test.com/api";

	private static String INSTALLATION_ID = "00000000-0000-0000-0000-b827eb3761f7";
	private static String SENSOR_ID = "127126ef-a96a-4177-9a7f-cd28f0e79326";
	private static String CAMERA_ID = "4c03631b-c62b-4ce4-ad0c-998cdbffbfc7";

	private static JsonServiceClient client;
	private static AuthenticateResponse authDetails;
	
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("yy-MM-dd");
	
	private static Installation installationDetails;
	
	public static void main (String[] args){
		Authenticate auth = new Authenticate();
		auth.setUserName(USERNAME);
		auth.setPassword(PASSWORD);

		client = new JsonServiceClient(BASE_URL);
		authDetails = client.post(auth);

		getInstallationDetails();
		
		uploadSensorValues();

		uploadSensorStatuses();

		getSensorValues();

		getSensorStatuses();

		getSensorsForInstallation();

		getNotificationsByType();

		uploadPicture();

		downloadPicture();
		
		createCustomNotification();
	}

	public static void getInstallationDetails(){
		InstallationDetails request = new InstallationDetails();
		request.setId(INSTALLATION_ID);
		
		InstallationDetailsResponse response = client.get(request);
		
		installationDetails = response.Installation;
		
		System.out.println("Installation Name: " + response.Installation.getName() + " Owner Id: " + response.Installation.getOwnerId());
	}
	
	public static void uploadSensorValues(){
		SensorValues uploadValuesRequest = new SensorValues();
		uploadValuesRequest.setId(SENSOR_ID);

		ArrayList<DataPoint> dataPoints = new ArrayList<DataPoint>();

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
		uploadStatusesRequest.setId(SENSOR_ID
				);
		ArrayList<StatusPoint> statusPoints = new ArrayList<StatusPoint>();

		StatusPoint status = new StatusPoint();
		status.setDate(new Date());
		status.setStatus("ok");
		status.setTrigger(authDetails.getUserId().toString());
		status.setTriggerName(authDetails.getUserName());

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
		valuesRequest.setId(SENSOR_ID);

		SensorValuesResponse valuesResponse = client.get(valuesRequest);

		for(int i = 0; i < valuesResponse.Values.size(); i++){
			System.out.println(valuesResponse.Values.get(i).Value);
		}
	}

	public static void getSensorStatuses(){
		SensorStatuses statusesRequest = new SensorStatuses();
		statusesRequest.setId(SENSOR_ID);

		SensorStatusesResponse statusesResponse = client.get(statusesRequest);

		for(int i= 0; i < statusesResponse.Statuses.size(); i++){
			System.out.println(statusesResponse.Statuses.get(i).Status);
		}
	}

	public static void getSensorsForInstallation(){
		InstallationSensors allSensorsRequest = new InstallationSensors();
		allSensorsRequest.Id = UUID.fromString(INSTALLATION_ID);

		InstallationSensorsResponse installationSensors = client.get(allSensorsRequest);

		for(Sensor s : installationSensors.Sensors){
			System.out.println(s.getId() + " " + s.getName() + " " + s.getType());
		}
	}

	public static void getNotificationsByType(){
		EventsBatch eventsRequest = new EventsBatch();
		eventsRequest.setUserId(authDetails.getUserId().toString());
		eventsRequest.setType("security");

		EventsBatchResponse eventsResponse = client.get(eventsRequest);

		for(AzureEvent event : eventsResponse.Events){
			System.out.println(event.Subtype + " " + event.TranslatedDescription);
		}
	}

	public static void uploadPicture()
	{
		try{
			CameraPhoto request = new CameraPhoto();
			request.setId(CAMERA_ID);
			request.setDate(new Date());
			request.setTrigger(authDetails.getUserId());		
			request.setBase64(imageToBase64("logo.jpg"));

			client.post(request);
			
			System.out.println("Image uploaded successfully!");
		} catch (Exception e){
			e.printStackTrace();
			System.out.println("Error uploading image: " + e.getMessage());
		}
	}

	public static void downloadPicture()
	{
		CameraPhoto request = new CameraPhoto();
		request.setId(CAMERA_ID);
		request.setFromDate(new DateTime().minusDays(1).toDate());
		request.setToDate(new Date());

		CameraPhotoResponse photosResponse = client.get(request);

		System.out.println("Obtained pictures list for camera: " + photosResponse.CameraName);

		if (!photosResponse.Photos.isEmpty())
		{
			Photo p = photosResponse.Photos.get(0);
			System.out.println("Downloading picture: " + p.TriggerId + "_" + dateFormat.format(p.Date) + ".jpg");
			
			HttpURLConnection httpRes = client.get(p.Url);
			byte[] imgBytes = Utils.readBytesToEnd(httpRes);

			try {
				FileUtils.copyInputStreamToFile(new ByteArrayInputStream(imgBytes), new File(dateFormat.format(p.Date) + ".jpg"));
				System.out.println("Picture downloaded successfully!");
			} catch (IOException e) {
				System.out.println("Error downloading picture: " + e.getMessage());
			}
		}
	}

	private static void createCustomNotification() {
		AzureEvent event = new AzureEvent();
		event.setDate(new Date());
		event.setType("custom");
		event.setSubtype("ruleengine");
		event.setDescription("My custom event text");
		event.setInstallationId(INSTALLATION_ID);
		event.setInstallation(installationDetails.getName());
		event.setPending(true);

		Events request = new Events();
		request.setEvent(event);
		
		ArrayList<Integer> usersToNotify = new ArrayList<Integer>();
		usersToNotify.add(installationDetails.OwnerId);
		
		request.setUserIds(usersToNotify);
		
		try{
			client.post(request);
			System.out.println("Event created successfully");
		} catch (Exception e){
			System.out.println("Error creating an event");
			e.printStackTrace();
		}
	}
	
	// Helpers
	public static String imageToBase64(String fileName) throws IOException
	{
		return Base64.encodeBase64String(FileUtils.readFileToByteArray(new File(Program.class.getResource(fileName).getFile())));
	}
}
