package com.moneystats.MoneyStats.walletTest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moneystats.MoneyStats.commStats.wallet.DTO.WalletDTO;
import com.moneystats.MoneyStats.commStats.wallet.DTO.WalletResponseDTO;
import com.moneystats.MoneyStats.commStats.wallet.WalletController;
import com.moneystats.MoneyStats.commStats.wallet.WalletException;
import com.moneystats.MoneyStats.commStats.wallet.WalletService;
import com.moneystats.MoneyStats.source.DTOTestObjets;
import com.moneystats.authentication.AuthenticationException;
import com.moneystats.authentication.DTO.TokenDTO;
import com.moneystats.authentication.utils.TestSchema;
import com.moneystats.generic.SchemaDescription;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import javax.ws.rs.core.MediaType;
import java.util.List;

@WebMvcTest(controllers = WalletController.class)
public class WalletControllerTest {

  @MockBean private WalletService walletService;
  @Autowired private MockMvc mockMvc;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Test
  public void testGetAllWalletList_OK() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    List<WalletDTO> walletDTOS = DTOTestObjets.walletDTOS;
    String walletAsString = objectMapper.writeValueAsString(walletDTOS);

    Mockito.when(walletService.getAll(Mockito.any())).thenReturn(walletDTOS);

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/wallet/list")
                .header("Authorization", "Bearer " + tokenDTO.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().json(walletAsString));
  }

  @Test
  public void testGetAllWalletList_shouldBeMappedOnInvalidTokenDTO() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;

    Mockito.when(walletService.getAll(Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/wallet/list")
                .header("Authorization", "Bearer " + tokenDTO.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  public void testGetAllWalletList_shouldBeMappedOnTokenDTORequired() throws Exception {
    TokenDTO tokenDTO = new TokenDTO("");
    List<WalletDTO> walletDTOS = DTOTestObjets.walletDTOS;

    Mockito.when(walletService.getAll(Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/wallet/list")
                .header("Authorization", "Bearer " + tokenDTO.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  public void testGetAllWalletList_shouldBeMappedOnWalletNotFound() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    List<WalletDTO> walletDTOS = DTOTestObjets.walletDTOS;

    Mockito.when(walletService.getAll(Mockito.any()))
        .thenThrow(new WalletException(WalletException.Code.WALLET_NOT_FOUND));

    mockMvc
        .perform(
            MockMvcRequestBuilders.get("/wallet/list")
                .header("Authorization", "Bearer " + tokenDTO.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  public void testAddWallet_OK() throws Exception {
    Integer idCategory = 1;
    WalletDTO walletDTO = DTOTestObjets.walletDTO;
    String walletDTOasString = objectMapper.writeValueAsString(walletDTO);
    WalletResponseDTO response =
        new WalletResponseDTO(SchemaDescription.WALLET_ADDED_CORRECT.toString());
    String responseAsString = objectMapper.writeValueAsString(response);

    Mockito.when(walletService.addWalletEntity(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenReturn(response);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/wallet/addWallet/" + idCategory)
                .contentType(MediaType.APPLICATION_JSON)
                .content(walletDTOasString)
                .header(
                    "Authorization",
                    "Bearer " + TestSchema.TOKEN_JWT_DTO_ROLE_USER.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().json(responseAsString));
  }

  @Test
  public void testAddWallet_shouldBeMappedOnError() throws Exception {
    WalletDTO walletDTO = DTOTestObjets.walletDTO;
    walletDTO.setName(null);
    int idCategory = 1;

    Mockito.when(
            walletService.addWalletEntity(
                TestSchema.TOKEN_JWT_DTO_ROLE_USER, idCategory, walletDTO))
        .thenThrow(new WalletException(WalletException.Code.INVALID_WALLET_DTO));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/wallet/addWallet/" + idCategory)
                .header(
                    "Authorization",
                    "Bearer " + TestSchema.TOKEN_JWT_DTO_ROLE_USER.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  public void testAddWalletEntity_shouldBeMappedOnInvalidTokenDTO() throws Exception {
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    int idCategory = 1;

    Mockito.when(walletService.addWalletEntity(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.INVALID_TOKEN_DTO));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/wallet/addWallet/" + idCategory)
                .header("Authorization", "Bearer " + tokenDTO.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  public void testAddWalletEntity_shouldBeMappedOnTokenDTORequired() throws Exception {
    TokenDTO tokenDTO = new TokenDTO("");
    List<WalletDTO> walletDTOS = DTOTestObjets.walletDTOS;
    int idCategory = 1;

    Mockito.when(walletService.addWalletEntity(Mockito.any(), Mockito.any(), Mockito.any()))
        .thenThrow(new AuthenticationException(AuthenticationException.Code.TOKEN_REQUIRED));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/wallet/addWallet/" + idCategory)
                .header("Authorization", "Bearer " + tokenDTO.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  @Test
  public void testAddWallet_shouldBeMappedUserNotFound() throws Exception {
    WalletDTO walletDTO = DTOTestObjets.walletDTO;
    Integer idCategory = 1;

    Mockito.when(
            walletService.addWalletEntity(
                TestSchema.TOKEN_JWT_DTO_ROLE_USER, idCategory, walletDTO))
        .thenThrow(new WalletException(WalletException.Code.USER_NOT_FOUND));

    mockMvc
        .perform(MockMvcRequestBuilders.post("/wallet/addWallet"))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  public void testAddWalletList_shouldBeMappedCategoryNotFound() throws Exception {
    WalletDTO walletDTO = DTOTestObjets.walletDTO;
    TokenDTO tokenDTO = TestSchema.TOKEN_JWT_DTO_ROLE_USER;
    Integer idCategory = null;

    Mockito.when(
            walletService.addWalletEntity(
                TestSchema.TOKEN_JWT_DTO_ROLE_USER, idCategory, walletDTO))
        .thenThrow(new WalletException(WalletException.Code.CATEGORY_NOT_FOUND));

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/wallet/addWallet")
                .header("Authorization", "Bearer " + tokenDTO.getAccessToken()))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  public void testDeleteWallet_OK() throws Exception {
    Long idWallet = 1L;
    WalletResponseDTO response =
        new WalletResponseDTO(SchemaDescription.WALLET_DELETE_CORRECT.toString());
    String responseAsString = objectMapper.writeValueAsString(response);

    Mockito.when(walletService.deleteWalletEntity(idWallet)).thenReturn(response);

    mockMvc
        .perform(MockMvcRequestBuilders.delete("/wallet/delete/" + idWallet))
        .andExpect(MockMvcResultMatchers.status().isOk())
        .andExpect(MockMvcResultMatchers.content().json(responseAsString));
  }

  @Test
  public void testDeleteWallet_shouldReturnWalletNotFound() throws Exception {
    Long idWallet = 1L;

    Mockito.when(walletService.deleteWalletEntity(idWallet))
        .thenThrow(new WalletException(WalletException.Code.WALLET_NOT_FOUND));

    mockMvc
        .perform(MockMvcRequestBuilders.delete("/wallet/delete/" + idWallet))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  @Test
  public void testDeleteWallet_shouldReturnCategoryNotFound() throws Exception {
    Long idWallet = 1L;

    Mockito.when(walletService.deleteWalletEntity(idWallet))
        .thenThrow(new WalletException(WalletException.Code.STATEMENT_NOT_FOUND));

    mockMvc
        .perform(MockMvcRequestBuilders.delete("/wallet/delete/" + idWallet))
        .andExpect(MockMvcResultMatchers.status().isNotFound());
  }
}