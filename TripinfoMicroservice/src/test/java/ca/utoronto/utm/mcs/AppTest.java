package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;

/*
Please Write Your Tests For CI/CD In This Class. 
You will see these tests pass/fail on github under github actions.
*/
public class AppTest {

   HttpClient client = HttpClient.newHttpClient();
   public static String time = null;
   public static String tripID = null;

   private int getResponse(String body,String route, String method) throws Exception{
      HttpRequest httpReq = HttpRequest.newBuilder()
              .method(method,HttpRequest.BodyPublishers.ofString(body))
              .uri(URI.create("http://localhost:8004/trip/" + route)).build();
      HttpResponse<String> res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
      // now return the response from micro back to caller
      return (res.statusCode());
   }

   @BeforeAll
   static void beforeAll() {
      time = Long.toString((new Date(System.currentTimeMillis())).getTime());
   }

   @Test
   @Order(1)
   public void confirmSuccess() throws Exception{
      JSONObject req = new JSONObject();
      req.put("driver", time);
      req.put("passenger", time);
      req.put("startTime", 1234567);
      String reqBody = req.toString();

      int expected = 200;
      HttpRequest httpReq = HttpRequest.newBuilder()
              .method("POST",HttpRequest.BodyPublishers.ofString(reqBody))
              .uri(URI.create("http://localhost:8004/trip/confirm")).build();
      HttpResponse<String> res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
      JSONObject bd = new JSONObject(res.body());
      tripID = bd.getJSONObject("data").getString("_id");
      int recieved = res.statusCode();
      assertTrue(expected == recieved);
   }

   @Test
   @Order(2)
   public void confirmFailure() throws Exception{
      JSONObject req = new JSONObject();
      req.put("driver", time);
      req.put("passenger", time);
      String reqBody = req.toString();
      // 400 for bad request, missing startTime param
      int expected = 400;
      int recieved = getResponse(reqBody,"confirm","POST");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(3)
   public void patchSuccess() throws Exception{
      JSONObject req = new JSONObject();
      req.put("distance", 50.1);
      req.put("endTime", 1234568);
      req.put("timeElapsed", "99:99:99");
      req.put("discount", 74.62);
      req.put("totalCost", 198.36);
      req.put("driverPayout", 133.14);
      String reqBody = req.toString();

      int expected = 200;
      int recieved = getResponse(reqBody,tripID,"PATCH");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(4)
   public void patchFailure() throws Exception{
      JSONObject req = new JSONObject();
      req.put("endTime", 1234568);
      req.put("timeElapsed", "99:99:99");
      req.put("discount", 74.62);
      req.put("totalCost", 198.36);
      req.put("driverPayout", 133.14);
      String reqBody = req.toString();

      int expected = 400;
      int recieved = getResponse(reqBody,tripID,"PATCH");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(5)
   public void driverSuccess() throws Exception{
      int expected = 200;
      int recieved = getResponse("","driver/"+time,"GET");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(6)
   public void driverFailure() throws Exception{
      int expected = 404;
      int recieved = getResponse("","driver/A"+time,"GET");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(7)
   public void passengerSuccess() throws Exception{
      int expected = 200;
      int recieved = getResponse("","passenger/"+time,"GET");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(8)
   public void passengerFailure() throws Exception{
      int expected = 404;
      int recieved = getResponse("","passenger/A"+time,"GET");
      assertTrue(expected == recieved);
   }

}
