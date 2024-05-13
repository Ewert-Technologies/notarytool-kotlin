package ca.ewert.notarytoolkotlin.examples.java;

import ca.ewert.notarytoolkotlin.NotaryToolClient;
import ca.ewert.notarytoolkotlin.NotaryToolError;
import ca.ewert.notarytoolkotlin.NotaryToolError.UserInputError.JsonWebTokenError.PrivateKeyNotFoundError;
import ca.ewert.notarytoolkotlin.TestValuesReader;
import ca.ewert.notarytoolkotlin.response.SubmissionListResponse;
import com.github.michaelbull.result.Result;
import com.github.michaelbull.result.ResultKt;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.ECPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.Base64;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.ewert.notarytoolkotlin.TestUtilKt.resourceToPath;

/**
 * Lists the submission history
 */
public class SubmissionHistoryJ {
  public static void main(String[] args) {
    exampleUsingPrivateKeyFile();
    exampleUserPrivateKeyProvider();
  }

  /**
   * This example uses the private key file Path.
   */
  static void exampleUsingPrivateKeyFile() {
    TestValuesReader testValuesReader = new TestValuesReader();

    String keyId = testValuesReader.getKeyId$notarytool_kotlin_test();
    String issuerId = testValuesReader.getIssueId$notarytool_kotlin_test();
    Path privateKeyFile = resourceToPath("/private/AuthKey_Test.p8");

    if (privateKeyFile != null) {
      NotaryToolClient notaryToolClient = new NotaryToolClient(keyId, issuerId, privateKeyFile,
          Duration.ofMinutes(10), Duration.ofSeconds(10), "test");

      Result<SubmissionListResponse, NotaryToolError> result = notaryToolClient.getPreviousSubmissions();

      if (result.component1() != null) {
        SubmissionListResponse submissionListResponse = result.component1();
        System.out.println("Response Received on: " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
            .format(submissionListResponse.getReceivedTimestamp().atZone(ZoneId.systemDefault())));
        submissionListResponse.getSubmissionInfoList().forEach(submissionInfo -> {
          System.out.println(submissionInfo.getCreatedDate() + "\t" + submissionInfo.getName()
              + "\t" + submissionInfo.getId() + "\t" + submissionInfo.getStatus());
        });
      } else {
        NotaryToolError notaryToolError = result.component2();

        if (notaryToolError instanceof NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError) {
          System.out.println("Authentication Error: '" + notaryToolError.getMsg() + "' Please check your Apple API " +
              "Key " +
              "Credentials");
        } else if (notaryToolError instanceof NotaryToolError.HttpError) {
          System.out.println("Http error occurred: " + ((NotaryToolError.HttpError) notaryToolError).getResponseMetaData()
              .getHttpStatusMessage() + ", " + notaryToolError.getMsg());
        } else {
          System.out.println("Other Error: " + notaryToolError.getMsg());
        }
      }
    } else {
      System.out.printf("Private Key file is null: %s\n", privateKeyFile.toString());
    }
  }

  /**
   * This example uses the private key provider.
   */
  static void exampleUserPrivateKeyProvider() {
    TestValuesReader testValuesReader = new TestValuesReader();

    String keyId = testValuesReader.getKeyId$notarytool_kotlin_test();
    String issuerId = testValuesReader.getIssueId$notarytool_kotlin_test();

    NotaryToolClient notaryToolClient = new NotaryToolClient(keyId, issuerId, SubmissionHistoryJ::privateKeyProvider,
        Duration.ofMinutes(10), Duration.ofSeconds(10), "test");

    Result<SubmissionListResponse, NotaryToolError> result = notaryToolClient.getPreviousSubmissions();

    if (result.component1() != null) {
      SubmissionListResponse submissionListResponse = result.component1();
      System.out.println("Response Received on: " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.FULL)
          .format(submissionListResponse.getReceivedTimestamp().atZone(ZoneId.systemDefault())));
      submissionListResponse.getSubmissionInfoList().forEach(submissionInfo -> {
        System.out.println(submissionInfo.getCreatedDate() + "\t" + submissionInfo.getName()
            + "\t" + submissionInfo.getId() + "\t" + submissionInfo.getStatus());
      });
    } else {
      NotaryToolError notaryToolError = result.component2();

      if (notaryToolError instanceof NotaryToolError.UserInputError.JsonWebTokenError.AuthenticationError) {
        System.out.println("Authentication Error: '" + notaryToolError.getMsg() + "' Please check your Apple API " +
            "Key " +
            "Credentials");
      } else if (notaryToolError instanceof NotaryToolError.HttpError) {
        System.out.println("Http error occurred: " + ((NotaryToolError.HttpError) notaryToolError).getResponseMetaData()
            .getHttpStatusMessage() + ", " + notaryToolError.getMsg());
      } else {
        System.out.println("Other Error: " + notaryToolError.getMsg());
      }
    }
  }

  /**
   * Creates an ECPrivateKey using a private key file.
   *
   * @return an ECPrivateKey or a JsonWebTokenError
   */
  static Result<ECPrivateKey, NotaryToolError.UserInputError.JsonWebTokenError> privateKeyProvider() {
    Path privateKeyFile = resourceToPath("/private/AuthKey_Test.p8");
    if (privateKeyFile != null && Files.exists(privateKeyFile)) {
      if (privateKeyFile.toFile().exists()) {
        try (Stream<String> lines = Files.lines(privateKeyFile)) {
          byte[] keyBytes = lines
              .filter(line -> !line.matches("-*\\w+ PRIVATE KEY-*"))
              .collect(Collectors.joining())
              .getBytes();
          byte[] keyBytesBase64Decoded = Base64.getDecoder().decode(keyBytes);
          PKCS8EncodedKeySpec pkcS8EncodedKeySpec = new PKCS8EncodedKeySpec(keyBytesBase64Decoded);
          PrivateKey privateKey = KeyFactory.getInstance("EC").generatePrivate(pkcS8EncodedKeySpec);
          return ResultKt.Ok((ECPrivateKey) privateKey);
        } catch (Exception exception) {
          return ResultKt.Err(new NotaryToolError.UserInputError.JsonWebTokenError.InvalidPrivateKeyError(exception.getMessage() != null ? exception.getMessage() : "N/A"));
        }
      } else {
        return ResultKt.Err(new PrivateKeyNotFoundError(privateKeyFile));
      }
    } else {
      return ResultKt.Err(new PrivateKeyNotFoundError(privateKeyFile));
    }
  }
}
