package ca.utoronto.utm.mcs;

import java.io.IOException;
import java.io.OutputStream;

import org.json.*;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.Record;
import java.util.ArrayList;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.Relationship;

import javax.management.relation.Relation;
import java.util.List;

import static org.neo4j.driver.Values.parameters;

public class Todos implements HttpHandler {

    @Override
    public void handle(HttpExchange r) throws IOException {
        System.out.println("Handling");
        try {
            String action = r.getRequestURI().toString();
            if (action.contains("navigation")) {
                navigation(r);
            } else {
                nearby(r);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void nearby(HttpExchange r) throws IOException, JSONException {
        int statusCode = 400;
        String requestURI = r.getRequestURI().toString();
        String[] uriSplitter = requestURI.split("/");
        JSONObject res = new JSONObject();
        JSONObject data = new JSONObject();
        // if there are extra url params send 400 and return
        if (uriSplitter.length != 4) {
            data.put("status", "BAD REQUEST");
            String response = data.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        try (Session session = Utils.driver.session()) {

            int uidEnd = uriSplitter[3].indexOf("?");
            String uid = uriSplitter[3].substring(0, uidEnd);

            int radiusEnd = uriSplitter[3].indexOf("=");
            double radius = Double.parseDouble(uriSplitter[3].substring(radiusEnd + 1));

            System.out.println("UID " + uid + " RADIUS " + radius);

            String preparedStatement = "MATCH (m:user {is_driver: true}) return m";
            String getLocationQuery = "MATCH (n: user {uid :$x}) RETURN n.longitude,n.latitude,n.street_at";
            Result uidResult = session.run(getLocationQuery, parameters("x", uid));

            Record user = uidResult.next();
            Double longitude = user.get("n.longitude").asDouble();
            Double latitude = user.get("n.latitude").asDouble();
            String street = user.get("n.street_at").asString();
            System.out.println(longitude + " " +  latitude + " " +  street);
            Result result = session.run(preparedStatement);
            if (result.hasNext()) {
                //found drivers
                while(result.hasNext()){
                    Record curDriver = result.next();
                    System.out.println(longitude + " " + latitude + " " + street);
                    System.out.println(curDriver.get(0));
                    System.out.println(curDriver.get(0).get("longitude"));
                    double latDistance = latitude - Double.parseDouble(curDriver.get(0).get("latitude").toString());
                    double longDistance = longitude - Double.parseDouble(curDriver.get(0).get("longitude").toString());
                    double distance = Math.sqrt(Math.pow(latDistance,2) + Math.pow(longDistance,2));
                    System.out.println("DISTANCE FROM DRIVER " + curDriver.get(0).get("uid") + " " + distance);
                    if(distance <= radius){
                        JSONObject curDriverInfo = new JSONObject();
                        curDriverInfo.put("longitude", Double.parseDouble(curDriver.get(0).get("longitude").toString()));
                        curDriverInfo.put("latitude", Double.parseDouble(curDriver.get(0).get("latitude").toString()));
                        curDriverInfo.put("street", (curDriver.get(0).get("street_at").asString()));
                        System.out.println("Add driver to data json");
                        data.put(curDriver.get(0).get("uid").asString(), curDriverInfo);
                    }
                }
                if(data.length() == 0){
                    statusCode = 400;
                    res.put("status", "No nearby drivers");
                }
                else{
                    statusCode = 200;
                    res.put("status", "OK");
                }
                //check if data.size is 0 then send 400
                res.put("data", data);
            } else {
                statusCode = 500;
                res.put("status", "INTERNAL SERVER ERROR");
            }
        } catch (Exception e) {
            System.out.println(e);
            statusCode = 500;
            res.put("status", "INTERNAL SERVER ERROR");
        }

        String response = res.toString();
        r.sendResponseHeaders(statusCode,response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    private void navigation(HttpExchange r) throws IOException, JSONException {
        //Help of 2 online sources to come up with query
        //http://www.loani.fun/?questions/14814124/get-all-routes-between-two-nodes-neo4j
        //https://stackoverflow.com/questions/45408618/cant-make-reduce-work-in-cypher
        System.out.println("/navigation in location microservice");
        int statusCode = 400;
        String requestURI = r.getRequestURI().toString();
        String[] uriSplitter = requestURI.split("/");
        JSONObject res = new JSONObject();
        JSONObject data = new JSONObject();
        // if there are extra url params send 400 and return
        if (uriSplitter.length != 4) {
            data.put("status", "BAD REQUEST");
            String response = data.toString();
            r.sendResponseHeaders(statusCode, response.length());
            OutputStream os = r.getResponseBody();
            os.write(response.getBytes());
            os.close();
            return;
        }

        try (Session session = Utils.driver.session()) {

            int driverIdEnd = uriSplitter[3].indexOf("?");
            String driverUid = uriSplitter[3].substring(0, driverIdEnd);

            int passIdEnd = uriSplitter[3].indexOf("=");
            String passUid = (uriSplitter[3].substring(passIdEnd + 1));

            String getNavigationStatement = "MATCH p=(a:road {name :$x})-[r*1..]->(b:road {name :$y}) with p, relationships(p) as rcoll return p, " +
                    "reduce(totalTime=0, x in relationships(p) | totalTime + x.travel_time) as totalTime order by totalTime LIMIT 1";

            String getLocationQuery = "MATCH (n: user {uid :$x}) RETURN n.longitude,n.latitude,n.street_at";
            Result driverResult = session.run(getLocationQuery, parameters("x", driverUid));
            Result passResult = session.run(getLocationQuery, parameters("x", passUid));
            if (driverResult.hasNext() && passResult.hasNext()) {
                Record driverUser = driverResult.next();
                String driverStreet = driverUser.get("n.street_at").asString();
                Record passUser = passResult.next();
                String passStreet = passUser.get("n.street_at").asString();

                Result result = session.run(getNavigationStatement, parameters("x", driverStreet, "y", passStreet));
                if ( result.hasNext() ) {
                    statusCode = 200;
                    Record test = result.next();
                    List<Node> pathNodes = (List<Node>) test.get("p").asPath().nodes();
                    List<Relationship> rels = (List<Relationship>) test.get("p").asPath().relationships();
                    res.put("status", "OK");
                    int totalTime = test.get("totalTime").asInt();
                    data.put("total_time", totalTime);

                    JSONArray route = new JSONArray();
                    for(int i=0; i<pathNodes.size(); i++){
                        JSONObject curPath = new JSONObject();
                        curPath.put("street", pathNodes.get(i).get("name").asString());
                        curPath.put("has_traffic", pathNodes.get(i).get("is_traffic").asBoolean());
                        if(i==0){
                            curPath.put("time", 0);
                        }
                        else{
                            curPath.put("time", Integer.parseInt(rels.get(i-1).get("travel_time").toString()) );
                        }
                        route.put(curPath);
                    }
                    data.put("route", route);
                    res.put("data", data);
                }
                else{
                    //check if driver and nav are at the same location
                    if(driverStreet.equals(passStreet)){
                        statusCode = 200;
                        res.put("status", "OK");
                        JSONArray route = new JSONArray();
                        JSONObject onlyPath = new JSONObject();
                        data.put("total_time", 0);
                        onlyPath.put("street", driverStreet);
                        onlyPath.put("time", 0);
                        //MATCH m=(a:road {name: "Ellsemere"}) return m
                        String getStreetQuery = "MATCH (n: road {name :$x}) RETURN n.is_traffic";
                        Result streetResult = session.run(getStreetQuery, parameters("x", driverStreet));

                        Record streetNode = streetResult.next();
                        Boolean traffic = streetNode.get("n.is_traffic").asBoolean();
                        onlyPath.put("has_traffic", traffic);
                        route.put(onlyPath);
                        data.put("route", route);
                        res.put("data", data);
                    }
                    else{
                        statusCode = 400;
                        res.put("status", "SERVER ERROR");
                    }
                }
            }
            else{
                statusCode = 400;
                res.put("status", "SERVER ERROR");
            }

        } catch (Exception e) {
            statusCode = 500;
            res.put("status", "INTERNAL SERVER ERROR");
        }

        String response = res.toString();
        r.sendResponseHeaders(statusCode,response.length());
        OutputStream os = r.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

}