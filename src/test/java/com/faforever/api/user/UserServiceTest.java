package com.faforever.api.user;

import com.faforever.api.config.FafApiProperties;
import com.faforever.api.data.domain.NameRecord;
import com.faforever.api.data.domain.Player;
import com.faforever.api.data.domain.User;
import com.faforever.api.email.EmailService;
import com.faforever.api.error.ApiExceptionWithCode;
import com.faforever.api.error.ErrorCode;
import com.faforever.api.mautic.MauticService;
import com.faforever.api.player.PlayerRepository;
import com.faforever.api.security.FafPasswordEncoder;
import com.faforever.api.security.FafTokenService;
import com.faforever.api.security.FafTokenType;
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.faforever.api.user.UserService.KEY_PASSWORD;
import static com.faforever.api.user.UserService.KEY_USER_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserServiceTest {
  private static final String TEST_SECRET = "banana";
  private static final int TEST_USERID = 5;
  private static final String TEST_USERNAME = "Junit";
  private static final String TEST_USERNAME_CHANGED = "newLogin";
  private static final String TEST_CURRENT_EMAIL = "junit@example.com";
  private static final String TEST_NEW_EMAIL = "junit@example.com";
  private static final String TEST_CURRENT_PASSWORD = "oldPassword";
  private static final String TEST_NEW_PASSWORD = "newPassword";
  private static final String TOKEN_VALUE = "someToken";
  private static final String PASSWORD_RESET_URL_FORMAT = "http://www.example.com/resetPassword/%s";
  private static final String ACTIVATION_URL_FORMAT = "http://www.example.com/%s";
  private static final String STEAM_ID = "someSteamId";
  private static FafPasswordEncoder fafPasswordEncoder = new FafPasswordEncoder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  private UserService instance;
  @Mock
  private EmailService emailService;
  @Mock
  private UserRepository userRepository;
  @Mock
  private PlayerRepository playerRepository;
  @Mock
  private NameRecordRepository nameRecordRepository;
  @Mock
  private AnopeUserRepository anopeUserRepository;
  @Mock
  private SteamService steamService;
  @Mock
  private FafTokenService fafTokenService;
  @Mock
  private MauticService mauticService;

  private FafApiProperties properties;

  private static User createUser(int id, String name, String password, String email) {
    return (User) new User()
      .setPassword(fafPasswordEncoder.encode(password))
      .setLogin(name)
      .setEmail(email)
      .setId(id);
  }

  @Before
  public void setUp() throws Exception {
    properties = new FafApiProperties();
    properties.getJwt().setSecret(TEST_SECRET);
    properties.getLinkToSteam().setSteamRedirectUrlFormat("someUrl");
    instance = new UserService(emailService, playerRepository, userRepository, nameRecordRepository, properties, anopeUserRepository, fafTokenService, steamService, Optional.of(mauticService));

    when(fafTokenService.createToken(any(), any(), any())).thenReturn(TOKEN_VALUE);
    when(userRepository.save(any(User.class))).then(invocation -> ((User) invocation.getArgument(0)).setId(TEST_USERID));

    when(mauticService.createOrUpdateContact(any(), any(), any(), any(), any()))
      .thenReturn(CompletableFuture.completedFuture(null));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void register() throws Exception {
    properties.getRegistration().setActivationUrlFormat(ACTIVATION_URL_FORMAT);

    instance.register(TEST_USERNAME, TEST_CURRENT_EMAIL, TEST_CURRENT_PASSWORD);

    verify(emailService).validateEmailAddress(TEST_CURRENT_EMAIL);
    verify(userRepository).existsByEmailIgnoreCase(TEST_CURRENT_EMAIL);

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendActivationMail(eq(TEST_USERNAME), eq(TEST_CURRENT_EMAIL), urlCaptor.capture());
    assertThat(urlCaptor.getValue(), is(String.format(ACTIVATION_URL_FORMAT, TOKEN_VALUE)));

    verifyZeroInteractions(mauticService);
  }

  @Test
  public void registerEmailAlreadyRegistered() throws Exception {
    when(userRepository.existsByEmailIgnoreCase(TEST_CURRENT_EMAIL)).thenReturn(true);
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.EMAIL_REGISTERED));

    instance.register("junit", TEST_CURRENT_EMAIL, TEST_CURRENT_PASSWORD);
  }

  @Test
  public void registerInvalidUsernameWithComma() throws Exception {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_INVALID));
    instance.register("junit,", TEST_CURRENT_EMAIL, TEST_CURRENT_PASSWORD);
  }

  @Test
  public void registerInvalidUsernameStartsUnderscore() throws Exception {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_INVALID));
    instance.register("_junit", TEST_CURRENT_EMAIL, TEST_CURRENT_PASSWORD);
  }

  @Test
  public void registerInvalidUsernameTooShort() throws Exception {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_INVALID));
    instance.register("ju", TEST_CURRENT_EMAIL, TEST_CURRENT_PASSWORD);
  }

  @Test
  public void registerUsernameTaken() throws Exception {
    when(userRepository.existsByLoginIgnoreCase("junit")).thenReturn(true);
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_TAKEN));
    instance.register("junit", TEST_CURRENT_EMAIL, TEST_CURRENT_PASSWORD);
  }

  @Test
  public void registerUsernameReserved() throws Exception {
    when(nameRecordRepository.getLastUsernameOwnerWithinMonths(any(), anyInt())).thenReturn(Optional.of(1));
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_RESERVED));
    instance.register("junit", TEST_CURRENT_EMAIL, TEST_CURRENT_PASSWORD);
  }

  @Test
  public void activate() throws Exception {
    when(fafTokenService.resolveToken(FafTokenType.REGISTRATION, TOKEN_VALUE)).thenReturn(ImmutableMap.of(
      UserService.KEY_USERNAME, TEST_USERNAME,
      UserService.KEY_EMAIL, TEST_CURRENT_EMAIL,
      UserService.KEY_PASSWORD, fafPasswordEncoder.encode(TEST_NEW_PASSWORD)
    ));

    instance.activate(TOKEN_VALUE, "127.0.0.1");

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());

    User user = captor.getValue();
    assertThat(user.getLogin(), is(TEST_USERNAME));
    assertThat(user.getEmail(), is(TEST_CURRENT_EMAIL));
    assertThat(user.getPassword(), is(fafPasswordEncoder.encode(TEST_NEW_PASSWORD)));

    verify(mauticService).createOrUpdateContact(eq(TEST_NEW_EMAIL), eq(String.valueOf(TEST_USERID)), eq(TEST_USERNAME), eq("127.0.0.1"), any(OffsetDateTime.class));
  }

  @Test
  public void changePassword() {
    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);

    instance.changePassword(TEST_CURRENT_PASSWORD, TEST_NEW_PASSWORD, user);
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals(captor.getValue().getPassword(), fafPasswordEncoder.encode(TEST_NEW_PASSWORD));
    verify(anopeUserRepository).updatePassword(TEST_USERNAME, Hashing.md5().hashString(TEST_NEW_PASSWORD, StandardCharsets.UTF_8).toString());

    verifyZeroInteractions(mauticService);
  }

  @Test
  public void changePasswordInvalidPassword() {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.PASSWORD_CHANGE_FAILED_WRONG_PASSWORD));

    User user = createUser(TEST_USERID, TEST_USERNAME, "invalid password", TEST_CURRENT_EMAIL);
    instance.changePassword(TEST_CURRENT_PASSWORD, TEST_NEW_PASSWORD, user);
  }

  @Test
  public void changeEmail() {
    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);

    instance.changeEmail(TEST_CURRENT_PASSWORD, TEST_NEW_EMAIL, user, "127.0.0.1");
    verify(emailService).validateEmailAddress(TEST_NEW_EMAIL);
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals(captor.getValue().getEmail(), TEST_NEW_EMAIL);

    verify(mauticService).createOrUpdateContact(eq(TEST_NEW_EMAIL), eq(String.valueOf(TEST_USERID)), eq(TEST_USERNAME), eq("127.0.0.1"), any(OffsetDateTime.class));
  }

  @Test
  public void changeEmailInvalidPassword() {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.EMAIL_CHANGE_FAILED_WRONG_PASSWORD));

    User user = createUser(TEST_USERID, TEST_USERNAME, "invalid password", TEST_CURRENT_EMAIL);
    instance.changeEmail(TEST_CURRENT_PASSWORD, TEST_NEW_PASSWORD, user, "127.0.0.1");
  }

  @Test
  public void changeLogin() {
    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    when(playerRepository.findOne(TEST_USERID)).thenReturn(mock(Player.class));
    when(nameRecordRepository.getDaysSinceLastNewRecord(anyInt(), anyInt())).thenReturn(Optional.empty());

    instance.changeLogin(TEST_USERNAME_CHANGED, user, "127.0.0.1");
    ArgumentCaptor<User> captorUser = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captorUser.capture());
    assertEquals(captorUser.getValue().getLogin(), TEST_USERNAME_CHANGED);
    ArgumentCaptor<NameRecord> captorNameRecord = ArgumentCaptor.forClass(NameRecord.class);
    verify(nameRecordRepository).save(captorNameRecord.capture());
    assertEquals(captorNameRecord.getValue().getName(), TEST_USERNAME);

    verify(mauticService).createOrUpdateContact(eq(TEST_NEW_EMAIL), eq(String.valueOf(TEST_USERID)), eq(TEST_USERNAME_CHANGED), eq("127.0.0.1"), any(OffsetDateTime.class));
  }

  @Test
  public void changeLoginWithUsernameInUse() {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_TAKEN));

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    when(userRepository.existsByLoginIgnoreCase(TEST_USERNAME_CHANGED)).thenReturn(true);
    instance.changeLogin(TEST_USERNAME_CHANGED, user, "127.0.0.1");
  }

  @Test
  public void changeLoginWithInvalidUsername() {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_INVALID));

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    instance.changeLogin("$%&", user, "127.0.0.1");
  }

  @Test
  public void changeLoginTooEarly() {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_CHANGE_TOO_EARLY));
    when(nameRecordRepository.getDaysSinceLastNewRecord(anyInt(), anyInt())).thenReturn(Optional.of(BigInteger.valueOf(5)));

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    instance.changeLogin(TEST_USERNAME_CHANGED, user, "127.0.0.1");
  }

  @Test
  public void changeLoginUsernameReserved() {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.USERNAME_RESERVED));
    when(nameRecordRepository.getLastUsernameOwnerWithinMonths(any(), anyInt())).thenReturn(Optional.of(TEST_USERID + 1));

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    instance.changeLogin(TEST_USERNAME_CHANGED, user, "127.0.0.1");
  }

  @Test
  public void changeLoginUsernameReservedBySelf() {
    when(nameRecordRepository.getLastUsernameOwnerWithinMonths(any(), anyInt())).thenReturn(Optional.of(TEST_USERID));

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    instance.changeLogin(TEST_USERNAME_CHANGED, user, "127.0.0.1");

    verify(mauticService).createOrUpdateContact(eq(TEST_NEW_EMAIL), eq(String.valueOf(TEST_USERID)), eq(TEST_USERNAME_CHANGED), eq("127.0.0.1"), any(OffsetDateTime.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void resetPasswordByLogin() throws Exception {
    properties.getPasswordReset().setPasswordResetUrlFormat(PASSWORD_RESET_URL_FORMAT);

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);

    when(userRepository.findOneByLoginIgnoreCase(TEST_USERNAME)).thenReturn(Optional.of(user));
    instance.resetPassword(TEST_USERNAME, TEST_NEW_PASSWORD);

    verify(userRepository).findOneByLoginIgnoreCase(TEST_USERNAME);

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendPasswordResetMail(eq(TEST_USERNAME), eq(TEST_CURRENT_EMAIL), urlCaptor.capture());
    assertThat(urlCaptor.getValue(), is(String.format(PASSWORD_RESET_URL_FORMAT, TOKEN_VALUE)));

    ArgumentCaptor<Map<String, String>> attributesMapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(fafTokenService).createToken(eq(FafTokenType.PASSWORD_RESET), any(), attributesMapCaptor.capture());
    Map<String, String> tokenAttributes = attributesMapCaptor.getValue();
    assertThat(tokenAttributes.size(), is(2));
    assertThat(tokenAttributes.get(KEY_USER_ID), is(String.valueOf(TEST_USERID)));
    assertThat(tokenAttributes.get(KEY_PASSWORD), is(String.valueOf(TEST_NEW_PASSWORD)));
  }

  @Test
  @SuppressWarnings("unchecked")
  public void resetPasswordByEmail() throws Exception {
    properties.getPasswordReset().setPasswordResetUrlFormat(PASSWORD_RESET_URL_FORMAT);

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);

    when(userRepository.findOneByEmailIgnoreCase(TEST_CURRENT_EMAIL)).thenReturn(Optional.of(user));
    instance.resetPassword(TEST_CURRENT_EMAIL, TEST_NEW_PASSWORD);

    verify(userRepository).findOneByEmailIgnoreCase(TEST_CURRENT_EMAIL);

    ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
    verify(emailService).sendPasswordResetMail(eq(TEST_USERNAME), eq(TEST_CURRENT_EMAIL), urlCaptor.capture());
    assertThat(urlCaptor.getValue(), is(String.format(PASSWORD_RESET_URL_FORMAT, TOKEN_VALUE)));

    ArgumentCaptor<Map<String, String>> attributesMapCaptor = ArgumentCaptor.forClass(Map.class);
    verify(fafTokenService).createToken(eq(FafTokenType.PASSWORD_RESET), any(), attributesMapCaptor.capture());
    Map<String, String> tokenAttributes = attributesMapCaptor.getValue();
    assertThat(tokenAttributes.size(), is(2));
    assertThat(tokenAttributes.get(KEY_USER_ID), is(String.valueOf(TEST_USERID)));
    assertThat(tokenAttributes.get(KEY_PASSWORD), is(String.valueOf(TEST_NEW_PASSWORD)));
  }

  @Test
  public void resetPasswordUnknownUsernameAndEmail() throws Exception {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.UNKNOWN_IDENTIFIER));

    when(userRepository.findOneByEmailIgnoreCase(TEST_CURRENT_EMAIL)).thenReturn(Optional.empty());
    when(userRepository.findOneByEmailIgnoreCase(TEST_CURRENT_EMAIL)).thenReturn(Optional.empty());
    instance.resetPassword(TEST_CURRENT_EMAIL, TEST_NEW_PASSWORD);
  }

  @Test
  public void claimPasswordResetToken() throws Exception {
    when(fafTokenService.resolveToken(FafTokenType.PASSWORD_RESET, TOKEN_VALUE))
      .thenReturn(ImmutableMap.of(KEY_USER_ID, "5",
        KEY_PASSWORD, TEST_NEW_PASSWORD));

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    when(userRepository.findOne(5)).thenReturn(user);

    instance.claimPasswordResetToken(TOKEN_VALUE);

    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(captor.capture());
    assertEquals(captor.getValue().getPassword(), fafPasswordEncoder.encode(TEST_NEW_PASSWORD));
    verify(anopeUserRepository).updatePassword(TEST_USERNAME, Hashing.md5().hashString(TEST_NEW_PASSWORD, StandardCharsets.UTF_8).toString());
    verifyZeroInteractions(mauticService);
  }

  @Test
  public void buildSteamLinkUrl() throws Exception {
    when(steamService.buildLoginUrl(any())).thenReturn("steamLoginUrl");

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    instance.buildSteamLinkUrl(user);
  }

  @Test
  public void buildSteamLinkUrlAlreadLinked() throws Exception {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.STEAM_ID_UNCHANGEABLE));

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    user.setSteamId(STEAM_ID);
    instance.buildSteamLinkUrl(user);
  }

  @Test
  public void linkToSteam() throws Exception {
    when(fafTokenService.resolveToken(FafTokenType.LINK_TO_STEAM, TOKEN_VALUE)).thenReturn(ImmutableMap.of(KEY_USER_ID, "5"));
    when(steamService.ownsForgedAlliance(any())).thenReturn(true);

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    when(userRepository.findOne(5)).thenReturn(user);

    instance.linkToSteam(TOKEN_VALUE, STEAM_ID);

    assertThat(user.getSteamId(), is(STEAM_ID));
    verifyZeroInteractions(mauticService);
  }

  @Test
  public void linkToSteamUnknownUser() throws Exception {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.TOKEN_INVALID));

    when(fafTokenService.resolveToken(FafTokenType.LINK_TO_STEAM, TOKEN_VALUE)).thenReturn(ImmutableMap.of(KEY_USER_ID, "5"));
    when(userRepository.findOne(5)).thenReturn(null);

    instance.linkToSteam(TOKEN_VALUE, STEAM_ID);
    verifyZeroInteractions(mauticService);
  }

  @Test
  public void linkToSteamNoGame() throws Exception {
    expectedException.expect(ApiExceptionWithCode.apiExceptionWithCode(ErrorCode.STEAM_LINK_NO_FA_GAME));

    when(fafTokenService.resolveToken(FafTokenType.LINK_TO_STEAM, TOKEN_VALUE)).thenReturn(ImmutableMap.of(KEY_USER_ID, "5"));
    when(steamService.ownsForgedAlliance(any())).thenReturn(false);

    User user = createUser(TEST_USERID, TEST_USERNAME, TEST_CURRENT_PASSWORD, TEST_CURRENT_EMAIL);
    when(userRepository.findOne(5)).thenReturn(user);

    instance.linkToSteam(TOKEN_VALUE, STEAM_ID);
    verifyZeroInteractions(mauticService);
  }

  @Test
  public void mauticUpdateFailureDoesNotThrowException() throws Exception {
    when(mauticService.createOrUpdateContact(any(), any(), any(), any(), any()))
      .thenAnswer(invocation -> {
        CompletableFuture<Object> future = new CompletableFuture<>();
        future.completeExceptionally(new IOException("This exception should be logged but cause no harm."));
        return future;
      });

    activate();

    verify(mauticService).createOrUpdateContact(eq(TEST_NEW_EMAIL), eq(String.valueOf(TEST_USERID)), eq(TEST_USERNAME), eq("127.0.0.1"), any(OffsetDateTime.class));
  }
}
