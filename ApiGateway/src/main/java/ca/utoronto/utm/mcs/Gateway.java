package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import org.json.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class Gateway implements HttpHandler{

    HttpClient client = HttpClient.newHttpClient();

    @Override
    public void handle(HttpExchange r) throws IOException{
        try {
			// before we figure out which req it is, remove the /api/v1 off the front of the URI path
			// String action = r.getRequestURI().toString().replace("/api/v1/", "");
            // now get the first part of the action before the first / to find the microservice to send req to
            String url = r.getRequestURI().toString();
            String method = r.getRequestMethod();
            String newURL = "";

			if (url.contains("/user/")) {
                newURL = "http://usermicroservice:8000" + url;
//                newURL = "http://localhost:8002" + url;
			} else if (url.contains("/location/")) {
                newURL = "http://locationmicroservice:8000" + url;
//                newURL = "http://locationmicroservice:8002" + url;
            } else if (url.contains("/trip/")) {
                newURL = "http://tripinfomicroservice:8000" + url;
//                newURL = "http://localhost:8002" + url;
			} else {
				System.out.println("not valid req");
				r.sendResponseHeaders(404, -1);
				r.close();
			}
            
            System.out.println("forward to " + newURL);

            HttpRequest httpReq = HttpRequest.newBuilder()
                                             .method(method,HttpRequest.BodyPublishers.ofString(Utils.convert(r.getRequestBody())))
					                         .uri(URI.create(newURL)).build();
//            HttpResponse<String> res = client.sendAsync(httpReq, HttpResponse.BodyHandlers.ofString())
            client.sendAsync(httpReq, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(res -> {
                        try {
                            // now return the response from micro back to caller
                            r.sendResponseHeaders(res.statusCode(), res.body().length());
                            OutputStream os = r.getResponseBody();
                            os.write(res.body().getBytes());
                            os.close();
                        } catch (Exception e) {
                            System.out.println("Async request failed");
                        }
                    });
		} catch (Exception e) {
			e.printStackTrace();
		}
    }
}
