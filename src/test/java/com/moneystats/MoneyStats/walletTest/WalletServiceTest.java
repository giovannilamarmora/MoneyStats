package com.moneystats.MoneyStats.walletTest;

import com.moneystats.MoneyStats.commStats.category.CategoryException;
import com.moneystats.MoneyStats.commStats.category.ICategoryDAO;
import com.moneystats.MoneyStats.commStats.category.entity.CategoryEntity;
import com.moneystats.MoneyStats.commStats.statement.DTO.StatementInputDTO;
import com.moneystats.MoneyStats.commStats.statement.IStatementDAO;
import com.moneystats.MoneyStats.commStats.statement.StatementException;
import com.moneystats.MoneyStats.commStats.statement.entity.StatementEntity;
import com.moneystats.MoneyStats.commStats.wallet.DTO.*;
import com.moneystats.MoneyStats.commStats.wallet.IWalletDAO;
import com.moneystats.MoneyStats.commStats.wallet.WalletException;
import com.moneystats.MoneyStats.commStats.wallet.WalletService;
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
public class WalletServiceTest {

  @Mock private IWalletDAO walletDAO;
  @Mock private IStatementDAO statementDAO;
  @Mock private ICategoryDAO categoryDAO;
  @Mock private AuthCredentialDAO authCredentialDAO;
  @InjectMocks private WalletService walletService;

  @Mock private TokenService tokenService;

  @Captor ArgumentCaptor<AuthCredentialInputDTO> authCredentialInputDTOArgumentCaptor;

  /**
   * Test walletList
   *
   * @throws Exception
   */
  @Test
  void test_walletDTOlist_shouldReturnTheList() throws Exception {
    List<WalletDTO> walletDTO = createValidWalletDTOS();
    List<WalletEntity> walletEntities = createValidWalletEntities();
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(walletDAO.findAllByUserId(authCredentialEntity.getId()))
        .thenReturn(walletEntities);

    List<WalletEntity> actual = walletService.getAll(tokenDTO);
    for (int i = 0; i < actual.size(); i++) {
      Assertions.assertEquals(walletDTO.get(i).getName(), actual.get(i).getName());
      Assertions.assertEquals(
          walletDTO.get(i).getUser().getUsername(), actual.get(i).getUser().getUsername());
      Assertions.assertEquals(walletDTO.get(i).getUser().getId(), actual.get(i).getUser().getId());
      Assertions.assertEquals(
          walletDTO.get(i).getUser().getFirstName(), actual.get(i).getUser().getFirstName());
      Assertions.assertEquals(
          walletDTO.get(i).getUser().getLastName(), actual.get(i).getUser().getLastName());
      Assertions.assertEquals(
          walletDTO.get(i).getCategoryEntity().getId(), actual.get(i).getCategory().getId());
      Assertions.assertEquals(
          walletDTO.get(i).getCategoryEntity().getName(), actual.get(i).getCategory().getName());
    }
  }

  @Test
  void test_walletDTOList_shouldThrowsTokenDTORequired() throws Exception {
    TokenDTO token = new TokenDTO("");

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED);
    AuthenticationException actualException =
        Assertions.assertThrows(AuthenticationException.class, () -> walletService.getAll(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_walletDTOList_shouldThrowsOnInvalidTokenDTO() throws Exception {
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
  void test_walletDTOList_shouldThrowsOnWalletNotFound() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    List<WalletEntity> list = new ArrayList<>();

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(walletDAO.findAllByUserId(authCredentialEntity.getId())).thenReturn(list);

    WalletException expectedException = new WalletException(WalletException.Code.WALLET_NOT_FOUND);
    WalletException actualException =
        Assertions.assertThrows(WalletException.class, () -> walletService.getAll(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  /**
   * Test addWallet
   *
   * @throws Exception
   */
  @Test
  void test_addWallet_shouldAddCorrectly() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    WalletResponseDTO expected = new WalletResponseDTO(ResponseMapping.WALLET_ADDED_CORRECT);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    Optional<CategoryEntity> categoryEntity =
        Optional.ofNullable(createValidStatementEntity().getWallet().getCategory());
    WalletInputDTO walletDTO = createWalletInputDTO();
    WalletEntity walletEntity = createValidWalletEntities().get(0);

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(categoryDAO.findById(categoryEntity.get().getId())).thenReturn(categoryEntity);
    Mockito.when(walletDAO.save(walletEntity)).thenReturn(walletEntity);

    WalletResponseDTO actual = walletService.addWalletEntity(tokenDTO, walletDTO);
    Assertions.assertEquals(expected.getMessage(), actual.getMessage());
  }

  @Test
  void test_addWallet_shouldThrowsOnInvalidWallet() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    WalletInputDTO walletDTO = DTOTestObjets.walletInputDTO;
    walletDTO.setName(null);

    WalletException expectedException =
        new WalletException(WalletException.Code.INVALID_WALLET_INPUT_DTO);
    WalletException actualException =
        Assertions.assertThrows(
            WalletException.class, () -> walletService.addWalletEntity(tokenDTO, walletDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_addWallet_shouldThrowsOnEmptyTokenDTO() throws Exception {
    TokenDTO token = new TokenDTO("");

    Mockito.when(tokenService.parseToken(token))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(token));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_addWallet_shouldThrowsOnInvalidTokenDTO() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;

    Mockito.when(tokenService.parseToken(tokenDTO))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_addWallet_shouldThrowsOnCategoryNotFound() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    WalletInputDTO walletDTO = createWalletInputDTO();
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);

    CategoryException expectedException =
        new CategoryException(CategoryException.Code.CATEGORY_NOT_FOUND);
    CategoryException actualException =
        Assertions.assertThrows(
            CategoryException.class, () -> walletService.addWalletEntity(tokenDTO, walletDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  /**
   * Test deleteWallet
   *
   * @throws Exception
   */
  @Test
  void test_deleteWallet_shouldDeleteWallet() throws Exception {
    WalletResponseDTO expected = new WalletResponseDTO(ResponseMapping.WALLET_DELETE_CORRECT);
    Optional<WalletEntity> walletEntity = Optional.ofNullable(DTOTestObjets.walletEntities.get(0));
    List<StatementEntity> statementEntity = DTOTestObjets.statementEntityList;
    Long idWallet = 1L;

    Mockito.when(walletDAO.findById(idWallet)).thenReturn(walletEntity);

    Mockito.when(statementDAO.findStatementByWalletId(idWallet)).thenReturn(statementEntity);

    Mockito.doNothing().when(walletDAO).deleteById(idWallet);

    WalletResponseDTO actual = walletService.deleteWalletEntity(idWallet);
    Assertions.assertEquals(expected.getMessage(), actual.getMessage());
  }

  @Test
  void test_deleteWallet_shouldThrowsOnWalletNotFound() throws Exception {
    Long idWallet = 1L;

    WalletException expectedException = new WalletException(WalletException.Code.WALLET_NOT_FOUND);
    WalletException actualException =
        Assertions.assertThrows(
            WalletException.class, () -> walletService.deleteWalletEntity(idWallet));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  private WalletInputDTO createWalletInputDTO() {
    return new WalletInputDTO("my-wallet-name", 1);
  }

  /**
   * Test myWalletMobile
   *
   * @throws Exception
   */
  @Test
  void test_myWalletMobile_shouldReturnCorrectValues() throws Exception {
    WalletStatementDTO walletStatementDTOExpected = createValidWalletStatementDTO();
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    List<WalletEntity> walletEntity = createValidWalletEntities();
    List<String> listDate = List.of("01-01-2021", "02-01-2021", "03-01-2021");
    List<StatementEntity> statementEntityList = createValidStatementEntities();

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(statementDAO.selectdistinctstatement(authCredentialEntity.getId()))
        .thenReturn(listDate);
    Mockito.when(walletDAO.findAllByUserId(authCredentialEntity.getId())).thenReturn(walletEntity);
    Mockito.when(statementDAO.findAllByUserIdAndDateOrderByWalletId(Mockito.any(), Mockito.any()))
        .thenReturn(statementEntityList);

    WalletStatementDTO actual = walletService.myWalletMobile(tokenDTO);
    for (int i = 0; i < actual.getWalletEntities().size(); i++) {
      Assertions.assertEquals(
          walletStatementDTOExpected.getStatementEntities().get(i).getId(),
          actual.getStatementEntities().get(i).getId());
      Assertions.assertEquals(
          walletStatementDTOExpected.getStatementEntities().get(i).getValue(),
          actual.getStatementEntities().get(i).getValue());
      Assertions.assertEquals(
          walletStatementDTOExpected.getStatementEntities().get(i).getDate(),
          actual.getStatementEntities().get(i).getDate());
      Assertions.assertEquals(
          walletStatementDTOExpected.getWalletEntities().get(i).getId(),
          actual.getWalletEntities().get(i).getId());
      Assertions.assertEquals(
          walletStatementDTOExpected.getWalletEntities().get(i).getName(),
          actual.getWalletEntities().get(i).getName());
      Assertions.assertEquals(
          walletStatementDTOExpected.getWalletEntities().get(i).getCategory().getName(),
          actual.getWalletEntities().get(i).getCategory().getName());
    }
  }

  @Test
  void test_myWalletMobile_shouldThrowsOnInvalidToken() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;

    Mockito.when(tokenService.parseToken(Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));
    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_myWalletMobile_shouldThrowsOnTokenRequired() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;

    Mockito.when(tokenService.parseToken(Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED));
    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_myWalletMobile_shouldThrowsOnListStatementNotFound() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);

    StatementException expectedException =
        new StatementException(StatementException.Code.LIST_STATEMENT_DATE_NOT_FOUND);
    StatementException actualException =
        Assertions.assertThrows(
            StatementException.class, () -> walletService.myWalletMobile(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_myWalletMobile_shouldThrowOnWalletNotFound() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    List<String> listDate = List.of("01-01-2021", "02-01-2021", "03-01-2021");

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(statementDAO.selectdistinctstatement(authCredentialEntity.getId()))
        .thenReturn(listDate);

    WalletException expectedException = new WalletException(WalletException.Code.WALLET_NOT_FOUND);
    WalletException actualException =
        Assertions.assertThrows(
            WalletException.class, () -> walletService.myWalletMobile(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_myWalletMobile_shouldThrowsOnStatementNotFound() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    List<WalletEntity> walletEntity = createValidWalletEntities();
    List<String> listDate = List.of("01-01-2021", "02-01-2021", "03-01-2021");

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(statementDAO.selectdistinctstatement(authCredentialEntity.getId()))
        .thenReturn(listDate);
    Mockito.when(walletDAO.findAllByUserId(authCredentialEntity.getId())).thenReturn(walletEntity);

    StatementException expectedException =
        new StatementException(StatementException.Code.STATEMENT_NOT_FOUND);
    StatementException actualException =
        Assertions.assertThrows(
            StatementException.class, () -> walletService.myWalletMobile(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  /**
   * Test EditWallet
   *
   * @throws Exception
   */
  @Test
  void test_editWallet_shouldReturnCorrectResponse() throws Exception {
    WalletResponseDTO expected = new WalletResponseDTO(ResponseMapping.WALLET_EDIT_CORRECT);
    WalletInputIdDTO walletInputIdDTO = createValidWalleInputDTO();
    CategoryEntity categoryEntity = createValidStatementEntity().getWallet().getCategory();
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);
    Mockito.when(categoryDAO.findById(walletInputIdDTO.getIdCategory()))
        .thenReturn(Optional.ofNullable(categoryEntity));

    WalletResponseDTO actual = walletService.editWallet(walletInputIdDTO, tokenDTO);
    Assertions.assertEquals(expected.getMessage(), actual.getMessage());
  }

  @Test
  void test_editWallet_shouldThrowsOnInvalidToken() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;

    Mockito.when(tokenService.parseToken(Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_editWallet_shouldThrowsOnRequiredToken() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;

    Mockito.when(tokenService.parseToken(Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED));

    AuthenticationException expectedException =
        new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED);
    AuthenticationException actualException =
        Assertions.assertThrows(
            AuthenticationException.class, () -> tokenService.parseToken(tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  @Test
  void test_editWallet_shouldThrowsOnCategoryNotFound() throws Exception {
    TokenDTO tokenDTO = new TokenDTO(TestSchema.STRING_TOKEN_JWT_ROLE_USER);
    AuthCredentialEntity authCredentialEntity = createValidStatementEntity().getUser();
    AuthCredentialDTO authCredentialDTO = createValidAuthCredentialDTO();
    WalletInputIdDTO walletInputIdDTO = createValidWalleInputDTO();

    Mockito.when(tokenService.parseToken(tokenDTO)).thenReturn(authCredentialDTO);
    Mockito.when(authCredentialDAO.getCredential(authCredentialInputDTOArgumentCaptor.capture()))
        .thenReturn(authCredentialEntity);

    CategoryException expectedException =
        new CategoryException(CategoryException.Code.CATEGORY_NOT_FOUND);
    CategoryException actualException =
        Assertions.assertThrows(
            CategoryException.class, () -> walletService.editWallet(walletInputIdDTO, tokenDTO));

    Assertions.assertEquals(expectedException.getCode(), actualException.getCode());
  }

  /**
   * Test WalletById
   *
   * @throws Exception
   */
  @Test
  void test_WalletById_shouldReturnWallet() throws Exception {
    Long idWallet = 1L;
    WalletEntity expected = DTOTestObjets.walletEntities.get(0);

    Mockito.when(walletDAO.findById(Mockito.any())).thenReturn(Optional.ofNullable(expected));

    WalletDTO actual = walletService.walletById(idWallet);

    Assertions.assertEquals(expected.getName(), actual.getName());
    Assertions.assertEquals(expected.getCategory(), actual.getCategoryEntity());
    Assertions.assertEquals(expected.getUser(), actual.getUser());
  }

  @Test
  void test_WalletById_shouldThrowsOnWalletNotFound() throws Exception {
    Long idWallet = 1L;

    WalletException expectedException = new WalletException(WalletException.Code.WALLET_NOT_FOUND);
    WalletException actualException =
        Assertions.assertThrows(WalletException.class, () -> walletService.walletById(idWallet));

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
        new WalletEntity(1L, "my-wallet-1", categoryEntity, authCredentialEntity, null);
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

  private List<WalletEntity> createValidWalletEntities() {
    List<WalletEntity> walletEntities =
        List.of(
            new WalletEntity(
                1L,
                "my-wallet-1",
                new CategoryEntity(1, "Credit Card"),
                createValidStatementEntity().getUser(),
                null),
            new WalletEntity(
                2L,
                "my-wallet-2",
                new CategoryEntity(1, "Credit Card"),
                createValidStatementEntity().getUser(),
                null),
            new WalletEntity(
                3L,
                "my-wallet-3",
                new CategoryEntity(1, "Credit Card"),
                createValidStatementEntity().getUser(),
                null),
            new WalletEntity(
                4L,
                "my-wallet-4",
                new CategoryEntity(1, "Credit Card"),
                createValidStatementEntity().getUser(),
                null));
    return walletEntities;
  }

  private WalletInputIdDTO createValidWalleInputDTO() {
    return new WalletInputIdDTO(1L, "my-wallet-1", 1);
  }

  private List<StatementEntity> createValidStatementEntities() {
    List<String> listDate = List.of("01-01-2021", "02-01-2021", "03-01-2021");
    List<StatementEntity> statementEntityList =
        List.of(
            new StatementEntity(
                listDate.get(0),
                250.00,
                createValidStatementEntity().getUser(),
                createValidWalletEntities().get(0)),
            new StatementEntity(
                listDate.get(0),
                250.00,
                createValidStatementEntity().getUser(),
                createValidWalletEntities().get(1)),
            new StatementEntity(
                listDate.get(0),
                250.00,
                createValidStatementEntity().getUser(),
                createValidWalletEntities().get(2)),
            new StatementEntity(
                listDate.get(0),
                250.00,
                createValidStatementEntity().getUser(),
                createValidWalletEntities().get(3)));
    return statementEntityList;
  }

  private WalletStatementDTO createValidWalletStatementDTO() {
    WalletStatementDTO walletStatementDTO =
        new WalletStatementDTO(createValidWalletEntities(), createValidStatementEntities());
    return walletStatementDTO;
  }

  private List<WalletDTO> createValidWalletDTOS() {
    return List.of(
        new WalletDTO(
            createValidWalletEntities().get(0).getName(),
            createValidWalletEntities().get(0).getCategory(),
            createValidWalletEntities().get(0).getUser()),
        new WalletDTO(
            createValidWalletEntities().get(1).getName(),
            createValidWalletEntities().get(1).getCategory(),
            createValidWalletEntities().get(1).getUser()),
        new WalletDTO(
            createValidWalletEntities().get(2).getName(),
            createValidWalletEntities().get(2).getCategory(),
            createValidWalletEntities().get(2).getUser()),
        new WalletDTO(
            createValidWalletEntities().get(3).getName(),
            createValidWalletEntities().get(3).getCategory(),
            createValidWalletEntities().get(3).getUser()));
  }
}
