package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.Timestamp;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.model.ErrorCode;
import pt.unl.fct.di.adc.firstwebapp.model.ApiResponse;
import pt.unl.fct.di.adc.firstwebapp.model.ApiRequest;
import pt.unl.fct.di.adc.firstwebapp.model.Role;
import pt.unl.fct.di.adc.firstwebapp.results.CreateAccountResult;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;

import com.google.gson.Gson;

@Path("/createaccount")
public class CreateAccountResource {

    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createAccount(ApiRequest<RegisterData> req) {
        RegisterData data = (req == null) ? null : req.input;
        
        if (data == null || data.validRegistration()) {
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.INVALID_INPUT)),
                    MediaType.APPLICATION_JSON
            ).build();
        }

        Transaction txn = null;
        try {
            txn = datastore.newTransaction();
            Key userKey = datastore.newKeyFactory()
                    .setKind("Account")
                    .newKey(data.username);

            Entity existing = txn.get(userKey); 
            if (existing != null) {
                return Response.ok(
                        g.toJson(ApiResponse.error(ErrorCode.USER_ALREADY_EXISTS)),
                        MediaType.APPLICATION_JSON
                ).build();
            }

            if (data.role == null) data.role = Role.USER;

            Entity user = Entity.newBuilder(userKey)
                    .set("passwordHash", DigestUtils.sha512Hex(data.password))
                    .set("phone", data.phone)
                    .set("address", data.address)
                    .set("role", data.role.name())
                    .set("createdAt", Timestamp.now())
                    .build();

            txn.put(user);   
            txn.commit();    

            return Response.ok(
                    g.toJson(ApiResponse.success(new CreateAccountResult(data.username, data.role.name()))),
                    MediaType.APPLICATION_JSON
            ).build();

        } catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "Datastore error on createaccount", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error on createaccount", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
        } finally {
            if (txn != null && txn.isActive()) {
                txn.rollback(); 
            }
        }
    }
}
