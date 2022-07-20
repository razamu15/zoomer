package ca.utoronto.utm.mcs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Order;

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

   private int getResponse(String body,String route, String method) throws Exception{
      HttpRequest httpReq = HttpRequest.newBuilder()
              .method(method,HttpRequest.BodyPublishers.ofString(body))
              .uri(URI.create("http://localhost:8004/user/" + route)).build();
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
   public void registerSuccess() throws Exception{
      JSONObject req = new JSONObject();
      req.put("email", "test_"+ time + "@email.com");
      req.put("password", "secret");
      req.put("name", "saad");
      String reqBody = req.toString();

      int expected = 200;
      int recieved = getResponse(reqBody,"register","POST");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(2)
   public void registerFailure() throws Exception{
      JSONObject req = new JSONObject();
      req.put("email", "test@email.com");
      req.put("password", "wrong");
      String reqBody = req.toString();
      // 400 for bad request, missing name param
      int expected = 400;
      int recieved = getResponse(reqBody,"register","POST");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(3)
   public void loginSuccess() throws Exception{
      JSONObject req = new JSONObject();
      req.put("email", "test_" + time + "@email.com");
      req.put("password", "secret");
      String reqBody = req.toString();

      int expected = 200;
      int recieved = getResponse(reqBody,"login","POST");
      assertTrue(expected == recieved);
   }

   @Test
   @Order(4)
   public void loginFailure() throws Exception{
      JSONObject req = new JSONObject();
      req.put("email", "test_" + time + "@email.com");
      req.put("password", "wrong");
      String reqBody = req.toString();

      int expected = 403;
      int recieved = getResponse(reqBody,"login","POST");
      assertTrue(expected == recieved);
   }

}
