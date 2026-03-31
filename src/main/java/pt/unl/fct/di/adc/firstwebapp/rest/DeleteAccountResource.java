package pt.unl.fct.di.adc.firstwebapp.rest;

import java.util.logging.Level;
import java.util.logging.Logger;


import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;

import com.google.gson.Gson;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;

import pt.unl.fct.di.adc.firstwebapp.model.ApiRequest;
import pt.unl.fct.di.adc.firstwebapp.model.ApiResponse;
import pt.unl.fct.di.adc.firstwebapp.model.ErrorCode;
import pt.unl.fct.di.adc.firstwebapp.resources.LogoutResource;
import pt.unl.fct.di.adc.firstwebapp.results.MessageResult;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.DeleteData;

@Path("/deleteaccount")
public class DeleteAccountResource {
     private static final Logger LOG = Logger.getLogger(DeleteAccountResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteAccount(ApiRequest<DeleteData> req) {
        DeleteData data = (req == null) ? null : req.input;
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
            Key tokenKey = datastore.newKeyFactory().setKind("AuthSession").newKey(token.tokenId);
            Entity session = datastore.get(tokenKey);

            Key userKey = datastore.newKeyFactory().setKind("Account").newKey(data.username);
            Entity account = datastore.get(userKey);

            if (account == null) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.USER_NOT_FOUND)),
                    MediaType.APPLICATION_JSON
            ).build();
            }

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
            String sessionRole = session.getString("role");
            
            if (!"ADMIN".equals(sessionRole)) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.UNAUTHORIZED)),
                    MediaType.APPLICATION_JSON
                ).build();
            }

            Query<Entity> q = Query.newEntityQueryBuilder()
                .setKind("AuthSession")
                .setFilter(PropertyFilter.eq("username", data.username))
                .build();

            QueryResults<Entity> sessions = datastore.run(q);
            while (sessions.hasNext()) {
                Entity s = sessions.next();
                datastore.delete(s.getKey());
            }

            datastore.delete(userKey);

            return Response.ok(
                g.toJson(ApiResponse.success(new MessageResult("Account deleted successfully"))),
                MediaType.APPLICATION_JSON
            ).build();

        }catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "Datastore error on delete", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
        }catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error on delete", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
		}
    }

}
