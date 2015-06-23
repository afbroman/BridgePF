package org.sagebionetworks.bridge.play.controllers;

import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.sagebionetworks.bridge.BridgeConstants.STUDY_PROPERTY;

import org.apache.shiro.crypto.hash.Sha256Hash;
import org.sagebionetworks.bridge.BridgeConstants;
import org.sagebionetworks.bridge.exceptions.ConcurrentModificationException;
import org.sagebionetworks.bridge.exceptions.ConsentRequiredException;
import org.sagebionetworks.bridge.exceptions.EntityNotFoundException;
import org.sagebionetworks.bridge.json.JsonUtils;
import org.sagebionetworks.bridge.models.accounts.Email;
import org.sagebionetworks.bridge.models.accounts.EmailVerification;
import org.sagebionetworks.bridge.models.accounts.PasswordReset;
import org.sagebionetworks.bridge.models.accounts.SignIn;
import org.sagebionetworks.bridge.models.accounts.SignUp;
import org.sagebionetworks.bridge.models.accounts.UserSession;
import org.sagebionetworks.bridge.models.accounts.UserSessionInfo;
import org.sagebionetworks.bridge.models.studies.Study;
import org.sagebionetworks.bridge.models.studies.StudyIdentifier;
import org.sagebionetworks.bridge.models.studies.StudyIdentifierImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;

import play.mvc.Result;

import com.fasterxml.jackson.databind.JsonNode;

@Controller
public class AuthenticationController extends BaseController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);

    public Result signIn() throws Exception {
        return signInWithRetry(3);
    }

    public Result signOut() throws Exception {
        final UserSession session = getSessionIfItExists();
        if (session != null) {
            authenticationService.signOut(session);
        }
        response().discardCookie(BridgeConstants.SESSION_TOKEN_HEADER);
        return okResult("Signed out.");
    }

    public Result signUp() throws Exception {
        JsonNode json = requestToJSON(request());
        SignUp signUp = SignUp.fromJson(json, false);
        signUp.getRoles().clear();
        
        Study study = getStudyOrThrowException(json);

        authenticationService.signUp(study, signUp, true);
        return createdResult("Signed up.");
    }

    public Result verifyEmail() throws Exception {
        JsonNode json = requestToJSON(request());
        EmailVerification emailVerification = parseJson(request(), EmailVerification.class);
        Study study = getStudyOrThrowException(json);
        
        // In normal course of events (verify email, consent to research),
        // an exception is thrown. Code after this line will rarely execute
        UserSession session = authenticationService.verifyEmail(study, emailVerification);
        setSessionToken(session.getSessionToken());
        return okResult(new UserSessionInfo(session));
    }
    
    public Result resendEmailVerification() throws Exception {
        JsonNode json = requestToJSON(request());
        Email email = parseJson(request(), Email.class);
        StudyIdentifier studyIdentifier = getStudyIdentifierOrThrowException(json);
        
        authenticationService.resendEmailVerification(studyIdentifier, email);
        return okResult("A request to verify an email address was re-sent.");
    }

    public Result requestResetPassword() throws Exception {
        JsonNode json = requestToJSON(request());
        Email email = parseJson(request(), Email.class);
        Study study = getStudyOrThrowException(json);
        
        authenticationService.requestResetPassword(study, email);
        return okResult("An email has been sent allowing you to set a new password.");
    }

    public Result resetPassword() throws Exception {
        PasswordReset passwordReset = parseJson(request(), PasswordReset.class);
        authenticationService.resetPassword(passwordReset);
        return okResult("Password has been changed.");
    }

    /**
     * Retries sign-in on lock.
     *
     * @param retryCounter the number of retries, excluding the initial try
     */
    private Result signInWithRetry(final int retryCounter) throws Exception {

        UserSession session = getSessionIfItExists();
        if (session != null) {
            setSessionToken(session.getSessionToken());
            return okResult(new UserSessionInfo(session));
        }

        final JsonNode json = requestToJSON(request());
        final SignIn signIn = parseJson(request(), SignIn.class);
        final Study study = getStudyOrThrowException(json);
        // TODO: Remove the logging once the investigation is done
        final String userNameHash = new Sha256Hash(signIn.getUsername(), signIn.getUsername()).toBase64();
        try {
            logger.info("User " + userNameHash + " signing in for study " + study.getIdentifier() + ".");
            session = authenticationService.signIn(study, signIn);
        } catch(ConsentRequiredException e) {
            setSessionToken(e.getUserSession().getSessionToken());
            throw e;
        } catch(ConcurrentModificationException e) {
            if (retryCounter > 0) {
                logger.info("User " + userNameHash +
                        " is having a race condition with signing in for study " + study.getIdentifier() + "." +
                        " Will retry after 250 millisecond.");
                // controller.signIn() 95% is < 1000 ms 
                Thread.sleep(250);
                return signInWithRetry(retryCounter - 1);
            }
            throw e;
        }

        setSessionToken(session.getSessionToken());
        return okResult(new UserSessionInfo(session));
    }

    /**
     * Unauthenticated calls that require a study (most of the calls not requiring authentication, including this one),
     * should include the study identifier as part of the JSON payload. This call handles such JSON and converts it to a
     * study. As a fallback for existing clients, it also looks for the study information in the query string or
     * headers. If the study cannot be found in any of these places, it throws an exception, because the API will not
     * work correctly without it.
     * 
     * @param email
     * @return
     */
    private Study getStudyOrThrowException(JsonNode node) {
        String studyId = getStudyStringOrThrowException(node);
        return studyService.getStudy(studyId);
    }

    private StudyIdentifier getStudyIdentifierOrThrowException(JsonNode node) {
        String studyId = getStudyStringOrThrowException(node);
        return new StudyIdentifierImpl(studyId);
    }

    @SuppressWarnings("deprecation")
    private String getStudyStringOrThrowException(JsonNode node) {
        String studyId = JsonUtils.asText(node, STUDY_PROPERTY);
        if (isNotBlank(studyId)) {
            return studyId;
        }
        studyId = getStudyIdentifier();
        if (studyId != null) {
            return studyId;
        }
        throw new EntityNotFoundException(Study.class);
    }
}
