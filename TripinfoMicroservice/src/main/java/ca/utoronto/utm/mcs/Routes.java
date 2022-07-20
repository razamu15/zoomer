package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.DriverManager;
import java.sql.SQLException;

import com.mongodb.*;
import com.mongodb.util.JSON;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.json.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.*;

import java.util.ArrayList;
import java.util.Arrays;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.*;

public class Routes implements HttpHandler{
    public MongoClient mongoClient;
    HttpClient client = HttpClient.newHttpClient();

    public Routes() {
        char[] pass = {'1', '2', '3', '4', '5', '6'};
        MongoCredential credential = MongoCredential.createCredential("root", "admin", pass);
        this.mongoClient = MongoClients.create(
                MongoClientSettings.builder()
                        .applyToClusterSettings(builder ->
                                builder.hosts(Arrays.asList(new ServerAddress("mongodb", 27017))))
                        .credential(credential)
                        .build());
    }

    @Override
    public void handle(HttpExchange r) throws IOException{
        try {
            // before we figure out which req it is, remove the /api/v1 off the front of the URI path
            String action = r.getRequestURI().toString().replace("/trip", "");


            if (r.getRequestMethod().equals("GET")) {
                if (action.contains("driverTime")) {
                    driverTime(r);
                } else if (action.contains("driver")) {
                    driver(r);
                } else if (action.contains("passenger")) {
                    passenger(r);
                } else {
                    System.out.println("not valid GET req");
                    r.sendResponseHeaders(404, -1);
                    r.close();
                }
            } else if (r.getRequestMethod().equals("POST")) {
                if (action.contains("request")) {
                    request(r);
                } else if (action.contains("confirm")) {
                    confirm(r);
                } else {
                    System.out.println("not valid PUT req");
                    r.sendResponseHeaders(404, -1);
                    r.close();
                }
            } else if (r.getRequestMethod().equals("PATCH")) {
                patch(r);
            } else {
                System.out.println("not valid req at all");
                r.sendResponseHeaders(404, -1);
                r.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void driverTime(HttpExchange req) throws IOException, JSONException {
        System.out.println("/driverTime in trip microservice");
    }

    public void driver(HttpExchange req) throws IOException, JSONException {
        try {
            MongoDatabase database = mongoClient.getDatabase("admin");
            MongoCollection<Document> collection = database.getCollection("trips");
            String id = req.getRequestURI().toString().replace("/trip/driver/", "");

            JSONArray trips = new JSONArray();
            FindIterable<Document> iterable = collection.find(eq("driver", id))
                    .projection(new Document("id", 1)
                            .append("distance", 1)
                            .append("startTime",1)
                            .append("endTime", 1)
                            .append("timeElapsed", 1)
                            .append("passenger", 1)
                            .append("driverPayout", 1));
            MongoCursor<Document> cursor = iterable.iterator();
            if (!cursor.hasNext()) {
                JSONObject res = new JSONObject();
                res.put("status", "not driver with this uid");
                String response = res.toString();
                req.sendResponseHeaders(404, response.length());
                // Writing response body
                OutputStream os = req.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                while (cursor.hasNext()) {
                    JSONObject data = new JSONObject(cursor.next().toJson());
                    String i = (new JSONObject(data.getString("_id"))).getString("$oid");
                    data.put("_id", i);
                    trips.put(data);
                }

                JSONObject res = new JSONObject();
                res.put("data", (new JSONObject()).put("trips", trips));
                res.put("status", "OK");
                String response = res.toString();
                req.sendResponseHeaders(200, response.length());
                // Writing response body
                OutputStream os = req.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject res = new JSONObject();
            res.put("status", "server error");
            String response = res.toString();
            req.sendResponseHeaders(401, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void passenger(HttpExchange req) throws IOException, JSONException {
        try {
            MongoDatabase database = mongoClient.getDatabase("admin");
            MongoCollection<Document> collection = database.getCollection("trips");
            String id = req.getRequestURI().toString().replace("/trip/passenger/", "");

            JSONArray trips = new JSONArray();
            FindIterable<Document> iterable = collection.find(eq("passenger", id))
                    .projection(new Document("id", 1)
                            .append("distance", 1)
                            .append("startTime",1)
                            .append("endTime", 1)
                            .append("timeElapsed", 1)
                            .append("driver", 1)
                            .append("discount", 1)
                            .append("totalCost", 1));
            MongoCursor<Document> cursor = iterable.iterator();
            if (!cursor.hasNext()) {
                JSONObject res = new JSONObject();
                res.put("status", "not passenger with the uid");
                String response = res.toString();
                req.sendResponseHeaders(404, response.length());
                // Writing response body
                OutputStream os = req.getResponseBody();
                os.write(response.getBytes());
                os.close();
            } else {
                while (cursor.hasNext()) {
                    JSONObject data = new JSONObject(cursor.next().toJson());
                    String i = (new JSONObject(data.getString("_id"))).getString("$oid");
                    data.put("_id", i);
                    trips.put(data);
                }

                JSONObject res = new JSONObject();
                res.put("data", (new JSONObject()).put("trips", trips));
                res.put("status", "OK");
                String response = res.toString();
                req.sendResponseHeaders(200, response.length());
                // Writing response body
                OutputStream os = req.getResponseBody();
                os.write(response.getBytes());
                os.close();
            }

        } catch (Exception e) {
            e.printStackTrace();
            JSONObject res = new JSONObject();
            res.put("status", "server error");
            String response = res.toString();
            req.sendResponseHeaders(401, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void request(HttpExchange req) throws IOException, JSONException, InterruptedException {
        JSONObject reqBody = new JSONObject(Utils.convert(req.getRequestBody()));
        String pass_id = reqBody.getString("uid");
        Double radius = reqBody.getDouble("radius");
        HttpRequest httpReq = HttpRequest.newBuilder()
                .method("GET",HttpRequest.BodyPublishers.ofString(""))
                .uri(URI.create("http://localhost:8004/location/nearbyDriver/" + pass_id + "?radius=" + radius.toString() )).build();
        HttpResponse<String> res = client.send(httpReq, HttpResponse.BodyHandlers.ofString());
        JSONObject bd = new JSONObject(res.body());
        if (res.statusCode() == 200) {
            ArrayList<String> did = (ArrayList<String>) bd.getJSONObject("data").keys();
            JSONObject res1 = new JSONObject();
            res1.put("data", did);
            String response = res1.toString();
            req.sendResponseHeaders(400, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();

        } else {
            JSONObject res1 = new JSONObject();
            res1.put("status", "neo4j returned 400");
            String response = res1.toString();
            req.sendResponseHeaders(400, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
        System.out.println(bd);
        System.out.println("random shit");

    }

    public void confirm(HttpExchange req) throws IOException, JSONException {
        try {
            MongoDatabase database = mongoClient.getDatabase("admin");
            MongoCollection<Document> collection = database.getCollection("trips");
            JSONObject reqBody = new JSONObject(Utils.convert(req.getRequestBody()));

            Document document = new Document("driver", reqBody.getString("driver"))
                    .append("passenger", reqBody.getString("passenger"))
                    .append("startTime", Integer.parseInt(reqBody.getString("startTime")));
            collection.withWriteConcern(WriteConcern.SAFE);
            collection.insertOne(document);

            JSONObject res = new JSONObject();
            res.put("data", (new JSONObject()).put("_id", document.getObjectId("_id")));
            res.put("status", "OK");
            String response = res.toString();
            req.sendResponseHeaders(200, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (JSONException e) {
            e.printStackTrace();
            JSONObject res = new JSONObject();
            res.put("status", "missing req body params");
            String response = res.toString();
            req.sendResponseHeaders(400, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

    public void patch(HttpExchange req) throws IOException, JSONException {
        try {
            MongoDatabase database = mongoClient.getDatabase("admin");
            MongoCollection<Document> collection = database.getCollection("trips");
            JSONObject reqBody = new JSONObject(Utils.convert(req.getRequestBody()));
            String id = req.getRequestURI().toString().replace("/trip/", "");

            collection.withWriteConcern(WriteConcern.SAFE);
            collection.updateOne(eq("_id", new ObjectId(id)),
                    combine(set("distance", Double.parseDouble(reqBody.getString("distance"))), set("endTime", Integer.parseInt(reqBody.getString("endTime"))),
                            set("timeElapsed", reqBody.getString("timeElapsed")), set("totalCost", Double.parseDouble(reqBody.getString("totalCost"))),
                            set("discount", reqBody.has("discount") ? Double.parseDouble(reqBody.getString("discount")) : 0),
                            set("driverPayout", reqBody.has("driverPayout") ? Double.parseDouble(reqBody.getString("driverPayout")) : 0.65 * Double.parseDouble(reqBody.getString("totalCost"))),
                            currentDate("lastModified")));

            JSONObject res = new JSONObject();
            res.put("status", "OK");
            String response = res.toString();
            req.sendResponseHeaders(200, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        } catch (JSONException e) {
            e.printStackTrace();
            JSONObject res = new JSONObject();
            res.put("status", "missing req body params");
            String response = res.toString();
            req.sendResponseHeaders(400, response.length());
            // Writing response body
            OutputStream os = req.getResponseBody();
            os.write(response.getBytes());
            os.close();
        }
    }

}
