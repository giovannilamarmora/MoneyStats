package com.moneystats.authentication;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.moneystats.authentication.DTO.AuthCredentialDTO;
import com.moneystats.authentication.DTO.AuthCredentialInputDTO;
import com.moneystats.authentication.DTO.AuthResponseDTO;
import com.moneystats.authentication.DTO.TokenDTO;
import com.moneystats.authentication.entity.AuthCredentialEntity;
import com.moneystats.generic.SchemaDescription;

@Service
public class AuthCredentialService {

  @Autowired AuthCredentialDAO authCredentialDAO;
  @Autowired TokenService tokenService;

  BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

  private static final Logger LOG = LoggerFactory.getLogger(AuthCredentialService.class);

  public AuthResponseDTO signUp(AuthCredentialDTO userCredential) throws AuthenticationException {
    AuthenticationValidator.validateAuthCredentialDTO(userCredential);
    AuthCredentialInputDTO authCredentialInputDTO =
        new AuthCredentialInputDTO(userCredential.getUsername(), userCredential.getPassword());
    AuthCredentialEntity authCredentialEntity =
        authCredentialDAO.getCredential(authCredentialInputDTO);
    if (authCredentialEntity != null) {
      LOG.error("User Present");
      throw new AuthenticationException(AuthenticationException.Code.USER_PRESENT);
    }
    userCredential.setPassword(bCryptPasswordEncoder.encode(userCredential.getPassword()));
    authCredentialDAO.insertUserCredential(userCredential);
    return new AuthResponseDTO(SchemaDescription.USER_ADDED_CORRECT);
  }

  public TokenDTO login(AuthCredentialInputDTO userCredential) throws AuthenticationException {
    AuthenticationValidator.validateAuthCredentialInputDTO(userCredential);
    AuthCredentialEntity userEntity = authCredentialDAO.getCredential(userCredential);
    if (userEntity == null) {
      LOG.error("User Not Found");
      throw new AuthenticationException(AuthenticationException.Code.WRONG_CREDENTIAL);
    }
    boolean matches =
        bCryptPasswordEncoder.matches(userCredential.getPassword(), userEntity.getPassword());
    if (!matches) {
      LOG.error("User Not Found");
      throw new AuthenticationException(AuthenticationException.Code.WRONG_CREDENTIAL);
    }
    AuthCredentialDTO userDto =
        new AuthCredentialDTO(
            userEntity.getFirstName(),
            userEntity.getLastName(),
            userEntity.getEmail(),
            userEntity.getUsername(),
            userEntity.getRole());
    TokenDTO token = tokenService.generateToken(userDto);
    return token;
  }

  public AuthCredentialDTO getUser(TokenDTO token) throws AuthenticationException {
    TokenValidation.validateTokenDTO(token);
    AuthCredentialDTO user = tokenService.parseToken(token);
    if (user == null) {
      LOG.error("User Not Found");
      throw new AuthenticationException(AuthenticationException.Code.AUTH_CREDENTIAL_DTO_NOT_FOUND);
    }
    return user;
  }

  public List<AuthCredentialDTO> getUsers(TokenDTO token) throws AuthenticationException {
    TokenValidation.validateTokenDTO(token);
    AuthCredentialDTO user = tokenService.parseToken(token);
    if (user == null) {
      LOG.error("User Not Found");
      throw new AuthenticationException(AuthenticationException.Code.AUTH_CREDENTIAL_DTO_NOT_FOUND);
    }
    if (!user.getRole().equalsIgnoreCase(SecurityRoles.MONEYSTATS_ADMIN_ROLE)) {
      throw new AuthenticationException(AuthenticationException.Code.NOT_ALLOWED);
    }
    List<AuthCredentialDTO> listUsers = new ArrayList<>();
    List<AuthCredentialEntity> list = authCredentialDAO.getUsers();
    for (int i = 0; i < list.size(); i++) {
      listUsers.add(
          new AuthCredentialDTO(
              list.get(i).getFirstName(),
              list.get(i).getLastName(),
              list.get(i).getEmail(),
              list.get(i).getUsername(),
              list.get(i).getRole()));
    }
    return listUsers;
  }
}
