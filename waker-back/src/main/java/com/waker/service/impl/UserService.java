package com.waker.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.waker.dao.impl.UserDao;
import com.waker.model.User;
import com.waker.model.exception.BusinessErrorCodesAndMessages;
import com.waker.model.exception.BusinessException;
import com.waker.model.exception.TechnicalErrorCodesAndMessages;
import com.waker.model.exception.TechnicalException;
import com.waker.service.IUserService;
import com.waker.util.Tools;
import com.waker.util.security.Crypt;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class UserService extends BaseService<User, UserDao> implements IUserService {

    Gson gson = new Gson();
    private final String JWT_HEADER = "{\"alg\":\"" + Crypt.getSigningAlgorithm() + "\",\"typ\":\"JWT\"}";
    private final byte[] SECRET_KEY = new byte[] { -38, -31, 89, -25, 42, 34, 26, -32, -31, 53, -85, 106, -48, -66, -38,
            127, 77, 125, 90, 60, -40, -13, 27, 102, 52, 106, 55, 112, -37, -92, 71, -117, };
    private final String ENCODED_HEADER = Tools.encode(gson.toJson(JWT_HEADER));
    private final long TOKEN_AGE = 10; // in days

    private static IUserService instance = null;
    private UserService() {
        dao = UserDao.getInstance();
    }
    public static IUserService getInstance() {
        if (instance == null)
            instance = new UserService();
        return instance;
    }

    @Override
    public User getByEmail(String email) throws TechnicalException, BusinessException {
        List<User> users = dao.find("{email: #}", new Object[] {email}, null, null, 1, 0);
        if (users.size() == 1) {
            return users.get(0);
        }
        if (users.size() > 1) {
            throw new TechnicalException(TechnicalErrorCodesAndMessages.UNDEFINED_EXCEPTION, "What the...");
        }
        throw new BusinessException(BusinessErrorCodesAndMessages.ERROR_404, "User with given email not found.");
    }

    @Override
    public String createHash(String password) throws TechnicalException {
        return Crypt.createHash(password);
    }

    @Override
    public boolean validatePassword(String password, String goodHash) throws TechnicalException {
        return Crypt.validatePassword(password, goodHash);
    }

    @Override
    public String buildToken(User user) throws TechnicalException {
        String email = user.getEmail();
        String name = user.getFirstName() + " " + user.getLastName();
        JsonObject payload = new JsonObject();
        payload.addProperty("sub", email);
        payload.addProperty("name", name);
        LocalDateTime ldt = LocalDateTime.now();
        long issuedAt = ldt.toEpochSecond(ZoneOffset.UTC);
        long expirationDate = ldt.plusDays(TOKEN_AGE).toEpochSecond(ZoneOffset.UTC);
        payload.addProperty("iat", issuedAt);
        payload.addProperty("exp", expirationDate);

        String encodedPayload = Tools.encode(payload);
        String signature = Crypt.hmacSHA256(ENCODED_HEADER + "." + encodedPayload, SECRET_KEY);
        return ENCODED_HEADER + "." + encodedPayload + "." + signature;
    }

    @Override
    public boolean validateToken(String token) throws BusinessException, TechnicalException {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new BusinessException(BusinessErrorCodesAndMessages.LOGIN_ERROR, "Invalid token format");
        }
        String encodedHeader = parts[0];
        String payload = Tools.decode(parts[1]);
        String signature = parts[2];
        if (!encodedHeader.equals(ENCODED_HEADER)) {
            throw new BusinessException(BusinessErrorCodesAndMessages.LOGIN_ERROR, "Invalid token header");
        }
        JsonObject payloadObj = JsonParser.parseString(payload).getAsJsonObject();
        if (payloadObj == null || !payloadObj.has("exp") || !payloadObj.has("sub")) {
            throw new BusinessException(BusinessErrorCodesAndMessages.LOGIN_ERROR, "Invalid token format");
        }
        long now = LocalDateTime.now().toEpochSecond(ZoneOffset.UTC);
        long expirationDate = payloadObj.get("exp").getAsLong();
        String newSignature = Crypt.hmacSHA256(payload, SECRET_KEY);

        return signature.equals(newSignature) && now < expirationDate;
    }

    @Override
    public boolean emailExists(String email) throws BusinessException, TechnicalException {
        List<User> users = search("{email: #}", new Object[] {email}, null, null, 1, 0);
        return users.size() > 0;
    }
}
