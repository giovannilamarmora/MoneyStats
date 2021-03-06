package com.moneystats.authentication;

import com.moneystats.authentication.AuthenticationException.Code;
import com.moneystats.authentication.DTO.*;
import com.moneystats.authentication.entity.AuthCredentialEntity;
import com.moneystats.generic.ResponseMapping;
import com.moneystats.generic.timeTracker.LogTimeTracker;
import com.moneystats.generic.timeTracker.LoggerMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Service
public class AuthCredentialService {

  @Autowired AuthCredentialDAO authCredentialDAO;
  @Autowired TokenService tokenService;
  @Autowired HttpServletRequest httpServletRequest;

  BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  private static final Logger LOG = LoggerFactory.getLogger(AuthCredentialService.class);

  /**
   * Process to signUp with a new user
   *
   * @param userCredential to be stored
   * @return A response of success
   * @throws AuthenticationException on invalid input
   */
  @LoggerMethod(type = LogTimeTracker.ActionType.APP_SERVICE_LOGIC)
  public AuthResponseDTO signUp(AuthCredentialDTO userCredential) throws AuthenticationException {
    userCredential.setRole(SecurityRoles.MONEYSTATS_USER_ROLE);
    AuthenticationValidator.validateAuthCredentialDTO(userCredential);
    AuthCredentialInputDTO authCredentialInputDTO =
        new AuthCredentialInputDTO(userCredential.getUsername(), userCredential.getPassword());

    // Check if the username is present5
    AuthCredentialEntity authCredentialEntity =
        authCredentialDAO.getCredential(authCredentialInputDTO);
    if (authCredentialEntity != null) {
      LOG.error("Username Present, needs different");
      throw new AuthenticationException(AuthenticationException.Code.USER_PRESENT);
    }

    // Check if Email is present
    AuthCredentialEntity checkEmail = authCredentialDAO.findByEmail(userCredential.getEmail());
    if (checkEmail != null) {
      LOG.error("Email Present, need another one");
      throw new AuthenticationException(AuthenticationException.Code.EMAIL_PRESENT);
    }

    userCredential.setPassword(bCryptPasswordEncoder.encode(userCredential.getPassword()));
    authCredentialDAO.insertUserCredential(userCredential);
    return new AuthResponseDTO(AuthResponseDTO.String.USER_ADDED);
  }

  /**
   * Process to login via input
   *
   * @param userCredential input
   * @return TokenDTO
   * @throws AuthenticationException on Token
   */
  @LoggerMethod(type = LogTimeTracker.ActionType.APP_SERVICE_LOGIC)
  public TokenDTO login(AuthCredentialInputDTO userCredential) throws AuthenticationException {
    AuthenticationValidator.validateAuthCredentialInputDTO(userCredential);
    AuthCredentialEntity userEntity = authCredentialDAO.getCredential(userCredential);
    if (userEntity == null) {
      LOG.error("User Not Found, username");
      throw new AuthenticationException(AuthenticationException.Code.WRONG_CREDENTIAL);
    }
    boolean matches =
        bCryptPasswordEncoder.matches(userCredential.getPassword(), userEntity.getPassword());
    if (!matches) {
      LOG.error("User Not Found, password");
      throw new AuthenticationException(AuthenticationException.Code.WRONG_CREDENTIAL);
    }

    String remoteAddress = httpServletRequest.getRemoteAddr();
    String userAgent =
        httpServletRequest.getHeader("Host") + " - " + httpServletRequest.getHeader("User-Agent");

    LOG.warn(
        "IP Address Connected: {} and Hostname: {} and Remote Address {}",
        httpServletRequest.getRemoteHost(),
        httpServletRequest.getServerName(),
        remoteAddress);
    LOG.warn("IP Address Connected to User Agent: {}", userAgent);
    AuthCredentialDTO userDto =
        new AuthCredentialDTO(
            userEntity.getFirstName(),
            userEntity.getLastName(),
            userEntity.getDateOfBirth(),
            userEntity.getEmail(),
            userEntity.getUsername(),
            userEntity.getRole());
    return tokenService.generateToken(userDto);
  }

  /**
   * Process to return the user logged
   *
   * @param token to be check
   * @return An user
   * @throws AuthenticationException for token
   */
  @LoggerMethod(type = LogTimeTracker.ActionType.APP_SERVICE_LOGIC)
  public AuthCredentialDTO getUser(TokenDTO token) throws AuthenticationException {
    TokenValidation.validateTokenDTO(token);
    return tokenService.parseToken(token);
  }

  /**
   * Methods to get all the user
   *
   * @param token ADMIN param
   * @return list of user into db
   * @throws AuthenticationException parsing Token
   */
  @LoggerMethod(type = LogTimeTracker.ActionType.APP_SERVICE_LOGIC)
  public List<AuthCredentialDTO> getUsers(TokenDTO token) throws AuthenticationException {
    TokenValidation.validateTokenDTO(token);
    AuthCredentialDTO user = tokenService.parseToken(token);

    if (!user.getRole().equalsIgnoreCase(SecurityRoles.MONEYSTATS_ADMIN_ROLE)) {
      LOG.error("Not Allowed, SecurityRoles");
      throw new AuthenticationException(AuthenticationException.Code.NOT_ALLOWED);
    }
    List<AuthCredentialDTO> listUsers = new ArrayList<>();
    List<AuthCredentialEntity> list = authCredentialDAO.getUsers();
    for (AuthCredentialEntity authCredentialEntity : list) {
      listUsers.add(
          new AuthCredentialDTO(
              authCredentialEntity.getFirstName(),
              authCredentialEntity.getLastName(),
              authCredentialEntity.getDateOfBirth(),
              authCredentialEntity.getEmail(),
              authCredentialEntity.getUsername(),
              authCredentialEntity.getRole()));
    }
    return listUsers;
  }

  /**
   * Method used to get the user updated after the update process
   *
   * @param tokenDTO param required to get the current user logged
   * @return the user logged updated
   * @throws AuthenticationException
   */
  @LoggerMethod(type = LogTimeTracker.ActionType.APP_SERVICE_LOGIC)
  public AuthCredentialDTO getUpdateUser(TokenDTO tokenDTO) throws AuthenticationException {
    TokenValidation.validateTokenDTO(tokenDTO);
    AuthCredentialDTO parseToken = tokenService.parseToken(tokenDTO);

    AuthCredentialInputDTO authCredentialInputDTO =
        new AuthCredentialInputDTO(parseToken.getUsername(), null);
    AuthCredentialEntity authCredentialEntity =
        authCredentialDAO.getCredential(authCredentialInputDTO);
    if (authCredentialEntity == null) {
      LOG.error(
          "An error occured during AuthCredentialService getCredential:154 authCredentialEntity is {}",
          authCredentialEntity);
      throw new AuthenticationException(Code.USER_NOT_MATCH);
    }

    return new AuthCredentialDTO(
        authCredentialEntity.getFirstName(),
        authCredentialEntity.getLastName(),
        authCredentialEntity.getDateOfBirth(),
        authCredentialEntity.getEmail(),
        authCredentialEntity.getUsername(),
        authCredentialEntity.getRole());
  }

  /**
   * Method that allow to update the user (Not let you update the username, or password)
   *
   * @param authCredentialToUpdateDTO input to update
   * @param tokenDTO valid for validator
   * @return A response of success
   * @throws AuthenticationException
   */
  @LoggerMethod(type = LogTimeTracker.ActionType.APP_SERVICE_LOGIC)
  public AuthResponseDTO updateUser(
      AuthCredentialToUpdateDTO authCredentialToUpdateDTO, TokenDTO tokenDTO)
      throws AuthenticationException {
    TokenValidation.validateTokenDTO(tokenDTO);
    AuthenticationValidator.validateAuthCredentiaToUpdateDTO(authCredentialToUpdateDTO);
    AuthCredentialDTO user = tokenService.parseToken(tokenDTO);

    if (!authCredentialToUpdateDTO.getUsername().equalsIgnoreCase(user.getUsername())) {
      LOG.error(
          "Username does not match AuthCredentialService updateUser:158 {} Exception {}",
          authCredentialToUpdateDTO.getUsername(),
          Code.USER_NOT_MATCH.toString());
      throw new AuthenticationException(Code.USER_NOT_MATCH);
    }

    authCredentialDAO.updateUserById(authCredentialToUpdateDTO);
    return new AuthResponseDTO(AuthResponseDTO.String.USER_UPDATED);
  }

  /**
   * Method to update the password of the current user, it will check first if the new password is
   * equals with the second password insert. Then it will get the authCredentialEntity from the db
   * to match with the old password insert. Then, if the password matches, it would proceed to
   * encode and update the password into the db.
   *
   * @param authChangePasswordInputDTO params in insert
   * @param tokenDTO for authentications
   * @return
   * @throws AuthenticationException
   */
  @LoggerMethod(type = LogTimeTracker.ActionType.APP_SERVICE_LOGIC)
  public AuthResponseDTO updatePassword(
      AuthChangePasswordInputDTO authChangePasswordInputDTO, TokenDTO tokenDTO)
      throws AuthenticationException {
    TokenValidation.validateTokenDTO(tokenDTO);
    AuthenticationValidator.validateAuthChangePasswordInputDTO(authChangePasswordInputDTO);

    if (!authChangePasswordInputDTO
        .getNewPassword()
        .equalsIgnoreCase(authChangePasswordInputDTO.getConfirmNewPassword())) {
      LOG.error(
          "Password don't match, at updatePassword:204, AuthCredentialService, password 1: {}, 2: {}, Exception {}",
          authChangePasswordInputDTO.getNewPassword(),
          authChangePasswordInputDTO.getConfirmNewPassword(),
          Code.PASSWORD_NOT_MATCH.toString());
      throw new AuthenticationException(Code.PASSWORD_NOT_MATCH);
    }

    AuthCredentialDTO user = tokenService.parseToken(tokenDTO);
    if (user == null) {
      LOG.error(
          "Username does not match chagePassword:210 {}, Exception {}",
          user.getUsername(),
          Code.USER_NOT_MATCH.toString());
      throw new AuthenticationException(Code.USER_NOT_MATCH);
    }

    AuthCredentialInputDTO inputGetCredential =
        new AuthCredentialInputDTO(authChangePasswordInputDTO.getUsername(), null);
    AuthCredentialEntity authCredentialEntity = authCredentialDAO.getCredential(inputGetCredential);
    if (authCredentialEntity == null) {
      LOG.error(
          "An error occured during getCredential():226, at AuthCredentialService is {}, Exception {}",
          authCredentialEntity,
          Code.USER_NOT_FOUND.toString());
      throw new AuthenticationException(Code.USER_NOT_FOUND);
    }

    boolean matches =
        bCryptPasswordEncoder.matches(
            authChangePasswordInputDTO.getOldPassword(), authCredentialEntity.getPassword());
    if (!matches) {
      LOG.error(
          "Old password don't match, needs to try again, updatePassword():234, at AuthCredentialService, Exception {}",
          Code.WRONG_CREDENTIAL.toString());
      throw new AuthenticationException(Code.WRONG_CREDENTIAL);
    }

    String newPassword = bCryptPasswordEncoder.encode(authChangePasswordInputDTO.getNewPassword());
    authCredentialDAO.updatePasswordUserById(authChangePasswordInputDTO.getUsername(), newPassword);
    return new AuthResponseDTO(AuthResponseDTO.String.PASSWORD_UPDATED);
  }
}
