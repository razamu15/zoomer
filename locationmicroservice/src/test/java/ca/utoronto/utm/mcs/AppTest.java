package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONException;
import org.junit.jupiter.api.*;
import org.json.JSONObject;

import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Date;
/*
Please Write Your Tests For CI/CD In This Class.
You will see these tests pass/fail on github under github actions.
*/
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTest {
   HttpClient client = HttpClient.newHttpClient();
   String time = null;

   private int getResponse(String body,String route, String method) throws Exception{
      HttpRequest httpReq = HttpRequest.newBuilder()
              .method(method,HttpRequest.BodyPublishers.ofString(body))
              .uri(URI.create("http://localhost:8000/location/" + route)).build();
      HttpResponse<String> res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
      // now return the response from micro back to caller
      return (res.statusCode());
   }

   @Test
   @Order(1)
   public void before() throws Exception {
      int res;
      //Set up nodes
      JSONObject user1 = new JSONObject();
      JSONObject user2 = new JSONObject();
      JSONObject user3 = new JSONObject();
      user1.put("uid", "0");
      user1.put("is_driver", false);
      user2.put("uid", "69");
      user2.put("is_driver", true);
      user3.put("uid", "123");
      user3.put("is_driver", true);
      res = getResponse(user1.toString(), "user", "PUT");
      res = getResponse(user2.toString(), "user", "PUT");
      res = getResponse(user3.toString(), "user", "PUT");

      //add locations to each nodes
      JSONObject user1loc = new JSONObject();
      JSONObject user2loc = new JSONObject();
      JSONObject user3loc = new JSONObject();

      user1loc.put("longitude", 2);
      user1loc.put("latitude", 3);
      user1loc.put("street", "Malvern");

      user2loc.put("longitude", 28.005);
      user2loc.put("latitude", 27.090);
      user2loc.put("street", "Ellsemere");

      user3loc.put("longitude", 15);
      user3loc.put("latitude", 15);
      user3loc.put("street", "Malvern");

      res = getResponse(user1loc.toString(), "0", "PATCH");
      res = getResponse(user2loc.toString(), "69", "PATCH");
      res = getResponse(user3loc.toString(), "123", "PATCH");

      //Road nodes

      JSONObject road1 = new JSONObject();
      JSONObject road2 = new JSONObject();
      JSONObject road3 = new JSONObject();

      road1.put("roadName", "Ellsemere");
      road1.put("hasTraffic", true);

      road2.put("roadName", "Malvern");
      road2.put("hasTraffic", true);

      road3.put("roadName", "Sheppard");
      road3.put("hasTraffic", false);


      res = getResponse(road1.toString(), "road", "PUT");
      res = getResponse(road2.toString(), "road", "PUT");
      res = getResponse(road3.toString(), "road", "PUT");


      JSONObject path1 = new JSONObject();
      JSONObject path2 = new JSONObject();
      JSONObject path3 = new JSONObject();

      path1.put("roadName1", "Ellsemere");
      path1.put("roadName2", "Sheppard");
      path1.put("hasTraffic", false);
      path1.put("time", 3);

      path2.put("roadName1", "Sheppard");
      path2.put("roadName2", "Malvern");
      path2.put("hasTraffic", false);
      path2.put("time", 2);

      path3.put("roadName1", "Ellsemere");
      path3.put("roadName2", "Malvern");
      path3.put("hasTraffic", true);
      path3.put("time", 66);


      res = getResponse(path1.toString(), "hasRoute", "POST");
      res = getResponse(path2.toString(), "hasRoute", "POST");
      res = getResponse(path3.toString(), "hasRoute", "POST");
      assertTrue(res == 200);
   }
   @Test
   @Order(2)
   public void nearbySuccess() throws Exception{
      int expected = 200;
      int received = getResponse("","nearbyDriver/0?radius=50","GET");
      assertTrue(expected == received);
   }

   @Test
   @Order(3)
   public void nearbyFailure() throws Exception{
      int expected = 400;
      int received = getResponse("","nearbyDriver/0?radius=1","GET");
      assertTrue(expected == received);
   }

   @Test
   @Order(4)
   public void navigationSuccess() throws Exception{
      int expected = 200;
      int received = getResponse("","navigation/69?passengerUid=0","GET");
      assertTrue(expected == received);
   }

   @Test
   @Order(5)
   public void navigationFailure() throws Exception{
      int expected = 400;
      int received = getResponse("","navigation/123?passengerUid=5","GET");
      assertTrue(expected == received);
   }
}
