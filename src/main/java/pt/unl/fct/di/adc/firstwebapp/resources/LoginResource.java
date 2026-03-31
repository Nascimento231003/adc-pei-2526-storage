package pt.unl.fct.di.adc.firstwebapp.resources;

import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import pt.unl.fct.di.adc.firstwebapp.model.ApiRequest;
import pt.unl.fct.di.adc.firstwebapp.model.ApiResponse;
import pt.unl.fct.di.adc.firstwebapp.util.AuthToken;
import pt.unl.fct.di.adc.firstwebapp.util.LoginData;
import pt.unl.fct.di.adc.firstwebapp.model.ErrorCode;
import pt.unl.fct.di.adc.firstwebapp.model.Role;
import pt.unl.fct.di.adc.firstwebapp.results.LoginResult;

import com.google.appengine.repackaged.org.apache.commons.codec.digest.DigestUtils;
import com.google.cloud.datastore.Transaction;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreException;
import com.google.cloud.datastore.DatastoreOptions;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Entity;

import com.google.gson.Gson;


@Path("/login")
@Produces(MediaType.APPLICATION_JSON + ";charset=utf-8")
public class LoginResource {

	/** 
	 * Logger Object
	 */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	private final Gson g = new Gson();
    private final Datastore datastore = DatastoreOptions.getDefaultInstance().getService();
	
	public LoginResource() {} // Nothing to be done here
	
	@POST
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response doLogin(ApiRequest<LoginData> req) {
		LoginData data = (req == null) ? null : req.input;

		if (data == null || !data.validLogin()) {
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.INVALID_INPUT)),
                    MediaType.APPLICATION_JSON
            ).build();
        }

		Transaction txn = null;

		try {
			txn = datastore.newTransaction();
			Key userKey = datastore.newKeyFactory().setKind("Account").newKey(data.username);
			Entity account = txn.get(userKey);

			if (account == null) {
				return Response.ok(
					g.toJson(ApiResponse.error(ErrorCode.USER_NOT_FOUND)),
					MediaType.APPLICATION_JSON
				).build();
			}

			String storedhash = account.getString("passwordHash");
			String givenhash = DigestUtils.sha512Hex(data.password);

			if (!storedhash.equals(givenhash)){
				return Response.ok(
					g.toJson(ApiResponse.error(ErrorCode.INVALID_CREDENTIALS)),
					MediaType.APPLICATION_JSON
				).build();
			}

			Role role = Role.valueOf(account.getString("role"));

			AuthToken at = new AuthToken(data.username, role); 

			Key sessionKey = datastore.newKeyFactory()
                    .setKind("AuthSession")
                    .newKey(at.tokenId);
				
			Entity session  = Entity.newBuilder(sessionKey)
                    .set("username", at.username)
                    .set("role", at.role.name())
                    .set("issuedAt", at.issuedAt)
                    .set("expiresAt", at.expiresAt)
                    .build();

            txn.put(session);   
            txn.commit();

			return Response.ok(
                    g.toJson(ApiResponse.success(new LoginResult(at))),
                    MediaType.APPLICATION_JSON
            ).build();

		}catch (DatastoreException e) {
            LOG.log(Level.SEVERE, "Datastore error on login", e);
            return Response.ok(
                    g.toJson(ApiResponse.error(ErrorCode.FORBIDDEN)),
                    MediaType.APPLICATION_JSON
            ).build();
        } catch (RuntimeException e) {
            LOG.log(Level.SEVERE, "Unexpected error on login", e);
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
