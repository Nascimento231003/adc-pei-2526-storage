package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Query;
import com.google.cloud.datastore.QueryResults;
import com.google.cloud.datastore.StructuredQuery.PropertyFilter;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import pt.unl.fct.di.adc.firstwebapp.model.ApiRequest;
import pt.unl.fct.di.adc.firstwebapp.model.ApiResponse;
import pt.unl.fct.di.adc.firstwebapp.model.ErrorCode;
import pt.unl.fct.di.adc.firstwebapp.model.Role;
import pt.unl.fct.di.adc.firstwebapp.results.MessageResult;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.ChangeRoleData;

@Path("/changeuserrole")
public class ChangeUserRoleResource {
    private static final Logger LOG = Logger.getLogger(ChangeUserRoleResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response changeRole(ApiRequest<ChangeRoleData> req) {
        ChangeRoleData data = (req == null) ? null : req.input;
        AuthToken token = (req == null) ? null : req.token;

        if (data == null || data.username == null || data.username.isBlank() || data.newRole == null) {
                return Response.ok(
                g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)), 
                MediaType.APPLICATION_JSON
            ).build();
        }
        
        if (token == null || token.tokenId == null || token.tokenId.isBlank()) {
            return Response.ok(
                g.toJson(ApiResponse.error(ErrorCode.INVALID_TOKEN)), 
                MediaType.APPLICATION_JSON
            ).build();
        }

        Transaction txn = null;

        try {
            txn = datastore.newTransaction();
            Key tokenKey = datastore.newKeyFactory().setKind("AuthSession").newKey(token.tokenId);
            Entity session = txn.get(tokenKey);
            
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

            if (!session.getString("role").equals("ADMIN")) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.UNAUTHORIZED)),
                    MediaType.APPLICATION_JSON
                ).build();
            }

            Key userKey = datastore.newKeyFactory().setKind("Account").newKey(data.username);
            Entity account = txn.get(userKey);

            if (account == null) {
                if (txn.isActive()) txn.rollback();
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.USER_NOT_FOUND)),
                    MediaType.APPLICATION_JSON
                ).build();
            }

            Role newRole = data.newRole;

            Entity updatedAccount = Entity.newBuilder(account)
                    .set("role", newRole.toString())
                    .build();
            
            txn.put(updatedAccount);
            txn.commit();

            Query<Entity> q = Query.newEntityQueryBuilder()
                    .setKind("AuthSession")
                    .setFilter(PropertyFilter.eq("username", data.username))
                    .build();

            QueryResults<Entity> sessions = datastore.run(q);
            while (sessions.hasNext()) {
                Entity s = sessions.next();
                Entity updatedSession = Entity.newBuilder(s)
                        .set("role", newRole.toString())
                        .build();
                datastore.put(updatedSession);
            }

            return Response.ok(
                    g.toJson(ApiResponse.success(new MessageResult("Role updated successfully"))),
                    MediaType.APPLICATION_JSON
            ).build();

        }catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "Datastore error on changeuserrole", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error on changeuserrole", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
		
		}finally {
            if (txn != null && txn.isActive()) {
                txn.rollback(); 
            }
        }
    }
}
