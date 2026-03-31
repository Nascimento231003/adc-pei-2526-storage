package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.cloud.datastore.Key;
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
import pt.unl.fct.di.adc.firstwebapp.results.MessageResult;
import pt.unl.fct.di.adc.firstwebapp.util.Atributes;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.ModifyData;

@Path("/modaccount")
public class ModifyAccountResource {
    private static final Logger LOG = Logger.getLogger(ModifyAccountResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response modifyAccount(ApiRequest<ModifyData> req) {
        ModifyData data = (req == null) ? null : req.input;
        AuthToken token = (req == null) ? null : req.token;
        Atributes attributes = (data == null) ? null : data.attributes;

        if (token == null || token.tokenId == null || token.tokenId.isBlank()) {
            return Response.ok(
                g.toJson(ApiResponse.error(ErrorCode.INVALID_TOKEN)), 
                MediaType.APPLICATION_JSON
            ).build();
        }

        if (data == null || data.username == null || data.username.isBlank()
            || attributes == null 
            || ((attributes.address == null || attributes.address.isBlank())
            && (attributes.phone == null || attributes.phone.isBlank()))) {
            return Response.ok(
                g.toJson(ApiResponse.error(ErrorCode.INVALID_INPUT)), 
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

            Key userKey = datastore.newKeyFactory().setKind("Account").newKey(data.username);
            Entity account = txn.get(userKey);

            if (account == null) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.USER_NOT_FOUND)),
                    MediaType.APPLICATION_JSON
                ).build();
            }

            String sessionRole = session.getString("role");
            String sessionUsername = session.getString("username");
            String targetRole = account.getString("role");
            
            if (!(sessionRole.equals("ADMIN")
                || (sessionRole.equals("USER") && sessionUsername.equals(data.username))
                || (sessionRole.equals("BOFFICER") 
                    && (sessionUsername.equals(data.username) || "USER".equals(targetRole))))) {
                return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.UNAUTHORIZED)),
                    MediaType.APPLICATION_JSON
                ).build();
            }

            Entity.Builder builder = Entity.newBuilder(account);

            if (attributes.address != null && !attributes.address.isBlank()) {
                builder.set("address", attributes.address);
            }
            
            if (attributes.phone != null && !attributes.phone.isBlank()) {
                builder.set("phone", attributes.phone);
            }

            txn.put(builder.build());
            txn.commit();

            return Response.ok(
                    g.toJson(ApiResponse.success(new MessageResult("Updated successfully"))),
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
		}finally {
            if (txn != null && txn.isActive()) {
                txn.rollback(); 
            }
        }
    }
}
