package ca.ewert.notarytoolkotlin.examples.java;

import java.nio.file.Path;
import java.time.*;
import java.time.format.*;

import ca.ewert.notarytoolkotlin.*;
import ca.ewert.notarytoolkotlin.response.SubmissionListResponse;

import com.github.michaelbull.result.Result;

import static ca.ewert.notarytoolkotlin.TestUtilKt.resourceToPath;

/**
 * Lists the submission history
 */
public class SubmissionHistoryJ {
  public static void main(String[] args) {
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
    }
  }
}
