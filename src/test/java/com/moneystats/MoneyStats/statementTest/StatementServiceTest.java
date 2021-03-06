package com.moneystats.MoneyStats.statementTest;

import com.moneystats.MoneyStats.commStats.category.ICategoryDAO;
import com.moneystats.MoneyStats.commStats.category.entity.CategoryEntity;
import com.moneystats.MoneyStats.commStats.statement.DTO.StatementInputDTO;
import com.moneystats.MoneyStats.commStats.statement.DTO.StatementResponseDTO;
import com.moneystats.MoneyStats.commStats.statement.IStatementDAO;
import com.moneystats.MoneyStats.commStats.statement.StatementException;
import com.moneystats.MoneyStats.commStats.statement.StatementService;
import com.moneystats.MoneyStats.commStats.statement.entity.StatementEntity;
import com.moneystats.MoneyStats.commStats.wallet.IWalletDAO;
import com.moneystats.MoneyStats.commStats.wallet.WalletException;
import com.moneystats.MoneyStats.commStats.wallet.entity.WalletEntity;
import com.moneystats.MoneyStats.source.DTOTestObjets;
import com.moneystats.authentication.AuthCredentialDAO;
import com.moneystats.authentication.AuthenticationException;
import com.moneystats.authentication.DTO.AuthCredentialDTO;
import com.moneystats.authentication.DTO.AuthCredentialInputDTO;
import com.moneystats.authentication.DTO.TokenDTO;
import com.moneystats.authentication.SecurityRoles;
import com.moneystats.authentication.TokenService;
import com.moneystats.authentication.entity.AuthCredentialEntity;
import com.moneystats.authentication.utils.TestSchema;
import com.moneystats.generic.ResponseMapping;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SpringBootTest
public class StatementServiceTest {

  @Mock private IWalletDAO walletDAO;
  @Mock private IStatementDAO statementDAO;
  @Mock private ICategoryDAO categoryDAO;
  @Mock private AuthCredentialDAO authCredentialDAO;
  @InjectMocks private StatementService statementService;
  @Mock private TokenService tokenService;

  @Captor ArgumentCaptor<AuthCredentialInputDTO> authCredentialInputDTOArgumentCaptor;

  /**
   * Test addStatement
   *
   * @throws Exception
   */
  @Test
  void test_addStatement_shouldAdd() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    StatementEntity statementEntity = createValidStatementEntity();
    StatementResponseDTO expected =
        new StatementResponseDTO(ResponseMapping.STATEMENT_ADDED_CORRECT);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    Optional<WalletEntity> walletEntity =
        Optional.ofNullable(createValidStatementEntity().getWallet());
    StatementInputDTO statementDTO = createValidStatementInputDTO();

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(walletDAO.findById(statementEntity.getWallet().getId())).thenReturn(walletEntity);

    StatementResponseDTO actual = statementService.addStatement(tokenDTO, statementDTO);
    Assertions.assertEquals(expected.getMessage(), actual.getMessage());
  }

  @Test
  void test_addStatement_shouldThrowsOnUnauthorized() throws Exception {
    TokenDTO token = new TokenDTO(TestSchema.ROLE_USER_TOKEN_JWT_WRONG);

    Mockito.when(tokenService.parseToken(token))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.UNAUTHORIZED));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.UNAUTHORIZED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_addStatement_shouldThrowsOnInvalidTokenDTO() throws Exception {
    TokenDTO token = new TokenDTO(TestSchema.ROLE_USER_TOKEN_JWT_WRONG);

    Mockito.when(tokenService.parseToken(token))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_addStatement_shouldBeMappedOnUserNotFound() throws Exception {
    StatementInputDTO statementDTO = createValidStatementInputDTO();
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    StatementEntity statementEntity = DTOTestObjets.statementEntityList.get(0);
    AuthCredentialDTO authCredentialDTO = TestSchema.USER_CREDENTIAL_DTO;
    AuthCredentialInputDTO authCredentialInputDTO =
        new AuthCredentialInputDTO(authCredentialDTO.getUsername(), authCredentialDTO.getRole());
    AuthCredentialEntity authCredentialEntity = TestSchema.USER_CREDENTIAL_ENTITY_ROLE_USER;

    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTO))
        .thenReturn(authCredentialEntity);
    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(statementDAO.save(statementEntity)).thenReturn(statementEntity);

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.USER_NOT_FOUND);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class,
            () -> statementService.addStatement(tokenDTO, statementDTO));
    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_addStatement_shouldThrowsOnWalletNotFound() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    StatementInputDTO statementDTO = createValidStatementInputDTO();

    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);

    WalletException expectedException = new WalletException(WalletException.Code.WALLET_NOT_FOUND);
    WalletException actualException =
        Assertions.assertThrows(
            WalletException.class, () -> statementService.addStatement(tokenDTO, statementDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  /**
   * Test ListOfDate
   *
   * @throws Exception
   */
  @Test
  void test_listOfDate_shouldReturnList() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    AuthCredentialDTO authCredentialDTO = TestSchema.USER_CREDENTIAL_DTO;
    AuthCredentialEntity authCredentialEntity = TestSchema.USER_CREDENTIAL_ENTITY_ROLE_USER;
    List<String> listDateExpected = List.of("2021-06-09");

    Mockito.when(tokenService.parseToken(Mockito.any())).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(Mockito.any())).thenReturn(authCredentialEntity);
    Mockito.when(statementDAO.selectdistinctstatement(Mockito.any())).thenReturn(listDateExpected);

    List<String> actual = statementService.listOfDate(tokenDTO);
    Assertions.assertEquals(listDateExpected, actual);
  }

  @Test
  void test_listOfDate_shouldThrowsTokenDTORequired() throws Exception {
    TokenDTO token = new TokenDTO("");

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> statementService.listOfDate(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listOfDate_shouldThrowsOnInvalidTokenDTO() throws Exception {
    TokenDTO token = new TokenDTO(TestSchema.ROLE_USER_TOKEN_JWT_WRONG);

    Mockito.when(tokenService.parseToken(token))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.UNAUTHORIZED));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.UNAUTHORIZED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listOfDate_shouldThrowsTokenDTOInvalid() throws Exception {
    TokenDTO token = TestSchema.TOKEN_JWT_DTO_ROLE_USER;

    Mockito.when(tokenService.parseToken(token))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> statementService.listOfDate(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listOfDate_shouldBeMappedOnUserNotFound() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    AuthCredentialDTO authCredentialDTO = TestSchema.USER_CREDENTIAL_DTO;
    AuthCredentialInputDTO authCredentialInputDTO =
        new AuthCredentialInputDTO(authCredentialDTO.getUsername(), authCredentialDTO.getRole());
    AuthCredentialEntity authCredentialEntity = TestSchema.USER_CREDENTIAL_ENTITY_ROLE_USER;

    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTO))
        .thenReturn(authCredentialEntity);
    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.USER_NOT_FOUND);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> statementService.listOfDate(tokenDTO));
    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listOfDate_shouldThrowsOnListNotFound() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    AuthCredentialDTO authCredentialDTO = TestSchema.USER_CREDENTIAL_DTO;
    AuthCredentialEntity authCredentialEntity = TestSchema.USER_CREDENTIAL_ENTITY_ROLE_USER;
    List<String> listDate = new ArrayList<>();

    Mockito.when(authCredentialDAO.getCredential(Mockito.any())).thenReturn(authCredentialEntity);
    Mockito.when(tokenService.parseToken(Mockito.any())).thenReturn(authCredentialDTO);
    Mockito.when(statementDAO.selectdistinctstatement(Mockito.any())).thenReturn(listDate);

    StatementException expectedException =
        new StatementException(StatementException.Code.LIST_STATEMENT_DATE_NOT_FOUND);
    StatementException actualException =
        Assertions.assertThrows(
            StatementException.class, () -> statementService.listOfDate(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  /**
   * Test listStatementBYDate
   *
   * @throws Exception
   */
  @Test
  void test_listStatementByDate_shouldReturnList() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    AuthCredentialDTO authCredentialDTO = TestSchema.USER_CREDENTIAL_DTO;
    AuthCredentialEntity authCredentialEntity = TestSchema.USER_CREDENTIAL_ENTITY_ROLE_USER;
    List<StatementEntity> listStatementExpected = DTOTestObjets.statementEntityList;
    String date = "2021-06-09";

    Mockito.when(tokenService.parseToken(Mockito.any())).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(Mockito.any())).thenReturn(authCredentialEntity);
    Mockito.when(statementDAO.findAllByUserIdAndDateOrderByWalletId(Mockito.any(), Mockito.any()))
        .thenReturn(listStatementExpected);

    List<StatementEntity> actual = statementService.listStatementByDate(tokenDTO, date);
    Assertions.assertEquals(listStatementExpected, actual);
  }

  @Test
  void test_listStatementByDate_shouldThrowsTokenDTORequired() throws Exception {
    TokenDTO token = new TokenDTO("");
    String date = "2021-06-09";

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> statementService.listStatementByDate(token, date));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listStatementByDate_shouldThrowsTokenDTOInvalid() throws Exception {
    TokenDTO token = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    String date = "2021-06-09";

    Mockito.when(tokenService.parseToken(token))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> statementService.listStatementByDate(token, date));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listStatementByDate_shouldThrowsOnInvalidTokenDTO() throws Exception {
    TokenDTO token = new TokenDTO(TestSchema.ROLE_USER_TOKEN_JWT_WRONG);

    Mockito.when(tokenService.parseToken(token))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.UNAUTHORIZED));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.UNAUTHORIZED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listStatementByDate_shouldBeMappedOnUserNotFound() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    AuthCredentialDTO authCredentialDTO = TestSchema.USER_CREDENTIAL_DTO;
    AuthCredentialInputDTO authCredentialInputDTO =
        new AuthCredentialInputDTO(authCredentialDTO.getUsername(), authCredentialDTO.getRole());
    AuthCredentialEntity authCredentialEntity = TestSchema.USER_CREDENTIAL_ENTITY_ROLE_USER;
    String date = "2021-06-09";

    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTO))
        .thenReturn(authCredentialEntity);
    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.USER_NOT_FOUND);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class,
            () -> statementService.listStatementByDate(tokenDTO, date));
    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_listStatementByDate_shouldThrowsOnListNotFound() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    AuthCredentialDTO authCredentialDTO = TestSchema.USER_CREDENTIAL_DTO;
    AuthCredentialEntity authCredentialEntity = TestSchema.USER_CREDENTIAL_ENTITY_ROLE_USER;
    List<StatementEntity> listStatementDate = new ArrayList<>();
    String date = "2021-06-09";

    Mockito.when(authCredentialDAO.getCredential(Mockito.any())).thenReturn(authCredentialEntity);
    Mockito.when(tokenService.parseToken(Mockito.any())).thenReturn(authCredentialDTO);
    Mockito.when(statementDAO.findAllByUserIdAndDateOrderByWalletId(Mockito.any(), Mockito.any()))
        .thenReturn(listStatementDate);

    StatementException expectedException =
        new StatementException(StatementException.Code.STATEMENT_NOT_FOUND);
    StatementException actualException =
        Assertions.assertThrows(
            StatementException.class, () -> statementService.listStatementByDate(tokenDTO, date));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  private StatementEntity createValidStatementEntity() {
    AuthCredentialEntity authCredentialEntity =
        new AuthCredentialEntity(
            1L,
            TestSchema.FIRSTNAME,
            TestSchema.LASTNAME,
            TestSchema.DATE_OF_BIRTH,
            TestSchema.EMAIL,
            TestSchema.STRING_USERNAME_ROLE_USER,
            TestSchema.STRING_TOKEN_JWT_ROLE_USER,
            SecurityRoles.MONEYSTATS_USER_ROLE);
    CategoryEntity categoryEntity = new CategoryEntity(1, "Category-name");
    WalletEntity walletEntity =
        new WalletEntity(1L, "my-Wallet-name", categoryEntity, authCredentialEntity, null);
    return new StatementEntity("01-01-2021", 10.00D, authCredentialEntity, walletEntity);
  }

  private AuthCredentialDTO createValidAuthCredentialDTO() {
    return new AuthCredentialDTO(
        TestSchema.FIRSTNAME,
        TestSchema.LASTNAME,
        TestSchema.DATE_OF_BIRTH,
        TestSchema.EMAIL,
        TestSchema.STRING_USERNAME_ROLE_USER,
        TestSchema.STRING_TOKEN_JWT_ROLE_USER,
        SecurityRoles.MONEYSTATS_USER_ROLE);
  }

  private StatementInputDTO createValidStatementInputDTO() {
    return new StatementInputDTO(10.00D, "01-01-2021", 1L);
  }
}
