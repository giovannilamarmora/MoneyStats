package com.moneystats.MoneyStats.commStats.statement;

import com.moneystats.MoneyStats.commStats.statement.DTO.StatementDTO;
import com.moneystats.MoneyStats.commStats.statement.DTO.StatementInputDTO;
import com.moneystats.MoneyStats.commStats.statement.DTO.StatementResponseDTO;
import com.moneystats.MoneyStats.commStats.statement.entity.StatementEntity;
import com.moneystats.MoneyStats.commStats.wallet.IWalletDAO;
import com.moneystats.MoneyStats.commStats.wallet.WalletException;
import com.moneystats.MoneyStats.commStats.wallet.entity.WalletEntity;
import com.moneystats.authentication.AuthCredentialDAO;
import com.moneystats.authentication.AuthenticationException;
import com.moneystats.authentication.DTO.AuthCredentialDTO;
import com.moneystats.authentication.DTO.AuthCredentialInputDTO;
import com.moneystats.authentication.DTO.TokenDTO;
import com.moneystats.authentication.TokenService;
import com.moneystats.authentication.TokenValidation;
import com.moneystats.authentication.entity.AuthCredentialEntity;
import com.moneystats.generic.SchemaDescription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StatementService {

  private final Logger LOG = LoggerFactory.getLogger(this.getClass());

  @Autowired private IWalletDAO walletDAO;
  @Autowired private IStatementDAO statementDAO;
  @Autowired private AuthCredentialDAO authCredentialDAO;
  @Autowired private TokenService tokenService;

  /**
   * Used to add a statement into database
   *
   * @param tokenDTO
   * @return
   * @throws StatementException
   * @throws AuthenticationException
   */
  public StatementResponseDTO addStatement(TokenDTO tokenDTO, StatementInputDTO statementInputDTO)
          throws StatementException, AuthenticationException, WalletException {
    StatementValidator.validateStatementInputDTO(statementInputDTO);
    StatementDTO statementDTO = new StatementDTO();
    statementDTO.setDate(statementInputDTO.getDate());
    statementDTO.setValue(statementInputDTO.getValue());
    AuthCredentialEntity utente = validateAndCreate(tokenDTO);
    statementDTO.setUser(utente);
    WalletEntity walletEntity =
        walletDAO.findById(statementInputDTO.getWalletId()).orElse(null);
    if (walletEntity == null) {
      LOG.error("Wallet Not Found, into StatementService, walletDAO.findById:37");
      throw new WalletException(WalletException.Code.WALLET_NOT_FOUND);
    }
    String[] date = statementDTO.getDate().split("-");
    String statementDate = date[2] + "-" + date[1] + "-" + date[0];
    statementDTO.setDate(statementDate);
    statementDTO.setWalletEntity(walletEntity);
    StatementEntity statementEntity =
        new StatementEntity(
            statementDTO.getDate(),
            statementDTO.getValue(),
            statementDTO.getUser(),
            statementDTO.getWalletEntity());
    statementDAO.save(statementEntity);
    return new StatementResponseDTO(SchemaDescription.STATEMENT_ADDED_CORRECT);
  }

  /**
   * @param tokenDTO
   * @return a list of unique date
   * @throws StatementException
   * @throws AuthenticationException
   */
  public List<String> listOfDate(TokenDTO tokenDTO)
      throws StatementException, AuthenticationException {
    AuthCredentialEntity utente = validateAndCreate(tokenDTO);

    List<String> listDate = statementDAO.selectdistinctstatement(utente.getId());
    if (listDate.size() == 0) {
      LOG.error(
          "Statement Date Not Found, into StatementService, statementDAO.selectdistinctstatement(utente.getId()):61");
      throw new StatementException(StatementException.Code.LIST_STATEMENT_DATE_NOT_FOUND);
    }
    return listDate;
  }

  /**
   * @param tokenDTO
   * @param date
   * @return a list of statement by that day
   * @throws StatementException
   * @throws AuthenticationException
   */
  public List<StatementEntity> listStatementByDate(TokenDTO tokenDTO, String date)
      throws StatementException, AuthenticationException {
    AuthCredentialEntity utente = validateAndCreate(tokenDTO);

    List<StatementEntity> statementList =
        statementDAO.findAllByUserIdAndDateOrderByWalletId(utente.getId(), date);
    if (statementList.size() == 0) {
      LOG.error(
          "Statement Not Found, into StatementService, statementDAO.findAllByUserIdAndDateOrderByWalletId(utente.getId(), date):71");
      throw new StatementException(StatementException.Code.STATEMENT_NOT_FOUND);
    }
    return statementList;
  }

  // public List<String> listByWalletAndValue(TokenDTO tokenDTO)
  //    throws StatementException, WalletException, AuthenticationException {
  //  AuthCredentialEntity utente = validateAndCreate(tokenDTO);

  //  List<String> statementsByWallet = statementDAO.findStatementByDateOrdered(utente.getId());
  //  if (statementsByWallet == null) {
  //    LOG.error(
  //        "Statement Not Found, into StatementService,
  // statementDAO.findStatementByDateOrdered(utente.getId()):83");
  //    throw new StatementException(StatementException.Code.STATEMENT_NOT_FOUND);
  //  }
  //  return statementsByWallet;
  // }

  private AuthCredentialEntity validateAndCreate(TokenDTO tokenDTO)
      throws AuthenticationException {
    TokenValidation.validateTokenDTO(tokenDTO);
    if (tokenDTO.getAccessToken().equalsIgnoreCase("")) {
      throw new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED);
    }
    AuthCredentialDTO authCredentialDTO = tokenService.parseToken(tokenDTO);
    AuthCredentialInputDTO authCredentialInputDTO =
        new AuthCredentialInputDTO(
            authCredentialDTO.getUsername(), authCredentialDTO.getPassword());
    AuthCredentialEntity utente = authCredentialDAO.getCredential(authCredentialInputDTO);
    if (utente == null) {
      LOG.error("User Not Found, into StatementService, validateAndCreate(TokenDTO):96");
      throw new AuthenticationException(AuthenticationException.Code.USER_NOT_FOUND);
    }
    return utente;
  }
}
