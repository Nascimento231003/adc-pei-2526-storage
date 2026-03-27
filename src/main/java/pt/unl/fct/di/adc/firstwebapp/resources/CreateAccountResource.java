package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Logger;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;
import com.google.cloud.Timestamp;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.model.ErrorCode;
import pt.unl.fct.di.adc.firstwebapp.model.ApiResponse;
import pt.unl.fct.di.adc.firstwebapp.results.CreateAccountResult;
import pt.unl.fct.di.adc.firstwebapp.util.RegisterData;

import com.google.gson.Gson;

public class CreateAccountResource {

    private static final Logger LOG = Logger.getLogger(CreateAccountResource.class.getName());
    private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();

    @POST
    @Path("createaccount")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createAccount(RegisterData data) {
        LOG.fine("Attempt to register user: " + data.username);

        if(!data.validRegistration()){
            String json = g.toJson(ApiResponse.error (ErrorCode.INVALID_INPUT));
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }

        Key userKey = datastore.newKeyFactory().setKind("Account").newKey(data.username);
        Entity user = datastore.get(userKey);

        if (user != null){
            String json = g.toJson(ApiResponse.error (ErrorCode.USER_ALREADY_EXISTS));
            return Response.ok(json, MediaType.APPLICATION_JSON).build();
        }

        user = Entity.newBuilder(userKey)
        .set("passwordHash", DigestUtils.sha512Hex(data.password))
        .set("phone", data.phone)
        .set("address", data.address)
        .set("role", data.role.name())
        .set("createdAt", Timestamp.now()).build();

        datastore.put(user);
        String json = g.toJson(ApiResponse.success(new CreateAccountResult(data.username, data.role.name())));
        return Response.ok(json, MediaType.APPLICATION_JSON).build();
    }
}
