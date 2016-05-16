import org.vertx.java.core.AsyncResult;
import org.vertx.java.core.Handler;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.json.JsonArray;
import org.vertx.java.core.json.JsonObject;
import org.vertx.java.core.logging.Logger;
import org.vertx.java.core.sockjs.EventBusBridgeHook;
import org.vertx.java.core.sockjs.SockJSServer;
import org.vertx.java.core.sockjs.SockJSSocket;
import org.vertx.java.platform.Verticle;

/*SockJS 서버*/

public class TCPServerVerticle extends Verticle{
	private Logger logger;
	private HttpServer httpServer;
	private int port;
	private String origin;
	
	@Override
	public void start(){
		JsonObject appConfig = container.config();
		
		logger = container.logger();
		httpServer = vertx.createHttpServer();
		port = appConfig.getInteger("port", 80);
		origin = appConfig.getString("origin", "http://localhost");
		
		httpServer.requestHandler(new Handler<HttpServerRequest>() {
			@Override 
			public void handle(HttpServerRequest request) {
				if(request.path().equals("/")) request.response().sendFile("index.html");
				else if (request.path().endsWith("vertxbus-2.1.js")) request.response().sendFile("vertxbus-2.1.js");				
				else request.response().setStatusCode(404).end("Not Found Page");
			}
		});
		
		SockJSServer sockJSServer = vertx.createSockJSServer(httpServer);	//소켓JS 생성
		
		sockJSServer.setHook(new EventBusBridgeHook(){
			@Override
			public boolean handleSocketCreated(SockJSSocket sock){
				if(origin!=null){
					String originHeader = sock.headers().get("origin");
					if(originHeader == null || !originHeader.equals(origin)){
						return false;
					}
				}
				return true;
			}
			
			@Override
			public void handleSocketClosed(SockJSSocket sock){
				logger.info("handleSocketClosed, sock = " + sock);
			}
			
			@Override
			public boolean handleSendOrPub(SockJSSocket sock, boolean send, JsonObject msg, String address){
				logger.info("handleSendOrPub, sock = " + sock + ", send = " + send + ", address = " + address);
				return true;
			}
			
			@Override
			public boolean handlePreRegister(SockJSSocket sock, String address){
				logger.info("handlePreRegister, sock = " + sock + ", address = "+ address);
				return true;
			}
			
			@Override
			public void handlePostRegister(SockJSSocket sock, String address){
				logger.info("handlePostRegister, sock = " + sock + ", address = " + address);
			}
			
			@Override
			public boolean handleUnregister(SockJSSocket sock, String address){
				logger.info("handleUnregister, sock = "+ sock + ", address = " + address);
				return true;
			}
			
			@Override
			public boolean handleAuthorise(JsonObject message, String sessionID, Handler<AsyncResult<Boolean>> handler){
				return false;
			}
		});
		
		JsonObject sockJSconfig = new JsonObject();
		sockJSconfig.putString("prefix", appConfig.getString("prefix", "/mySockJS"));
		
		JsonArray permitted = new JsonArray();
		permitted.add(new JsonObject());
		
		sockJSServer.bridge(sockJSconfig,  permitted,  permitted);
		
		httpServer.listen(port, new Handler<AsyncResult<HttpServer>>(){
			@Override
			public void handle(AsyncResult<HttpServer> asyncResult){
				logger.info("bind result : " + asyncResult.succeeded());
			}
		});
	}
	
	@Override
	public void stop(){
		if(httpServer != null)
			httpServer.close();
	}
		
}