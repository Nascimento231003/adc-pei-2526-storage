package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;
import java.util.logging.Level;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import com.google.gson.Gson;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import pt.unl.fct.di.adc.firstwebapp.model.ApiRequest;
import pt.unl.fct.di.adc.firstwebapp.model.ApiResponse;
import pt.unl.fct.di.adc.firstwebapp.model.ErrorCode;
import pt.unl.fct.di.adc.firstwebapp.results.LogoutResult;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LogoutData;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;

@Path("/logout")
public class LogoutResource {
    
    private static final Logger LOG = Logger.getLogger(LogoutResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON + ";charset=utf-8")
    @Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
    public Response logoutResource(ApiRequest<LogoutData> req){
        
		LogoutData data = (req == null) ? null : req.input;
        AuthToken token = (req == null) ? null : req.token;
        
        if (data == null || data.username == null || data.username.isBlank()
                || token == null || token.tokenId == null || token.tokenId.isBlank()) {
            return Response.ok(
                g.toJson(ApiResponse.error(ErrorCode.INVALID_TOKEN)), 
                MediaType.APPLICATION_JSON
            ).build();
        }

        Transaction txn = null;

        try{
            txn = datastore.newTransaction();
            Key tokenKey = datastore.newKeyFactory().setKind("AuthSession").newKey(token.tokenId);
            Entity session = txn.get(tokenKey);

            String targetUsername = data.username;

            if (session == null) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.INVALID_TOKEN)),
                    MediaType.APPLICATION_JSON
            ).build();
            }

            long expiresAt = session.getLong("expiresAt");
            if (expiresAt < System.currentTimeMillis()) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.TOKEN_EXPIRED)),
                    MediaType.APPLICATION_JSON
            ).build();
            }
            String sessionUsername = session.getString("username");
            String sessionRole = session.getString("role");
            
            if (!targetUsername.equals(sessionUsername) && !"ADMIN".equals(sessionRole)) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.UNAUTHORIZED)),
                    MediaType.APPLICATION_JSON
                ).build();
            }

            Query<Entity> q = Query.newEntityQueryBuilder()
                .setKind("AuthSession")
                .setFilter(PropertyFilter.eq("username", targetUsername))
                .build();

            QueryResults<Entity> sessions = datastore.run(q);
            while (sessions.hasNext()) {
                Entity s = sessions.next();
                datastore.delete(s.getKey());
            }

            return Response.ok(
                    g.toJson(ApiResponse.success(new LogoutResult("Logout successful"))),
                    MediaType.APPLICATION_JSON
                ).build();

        }catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "Datastore error on logout", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
        }catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error on logout", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
		
		}finally{
			if (txn != null && txn.isActive()) {
				txn.rollback();
			}
		}
    }
}
