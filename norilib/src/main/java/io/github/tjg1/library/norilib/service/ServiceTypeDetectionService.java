/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.service;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;

import io.github.tjg1.library.norilib.clients.Danbooru;
import io.github.tjg1.library.norilib.clients.DanbooruLegacy;
import io.github.tjg1.library.norilib.clients.E621;
import io.github.tjg1.library.norilib.clients.Flickr;
import io.github.tjg1.library.norilib.clients.FlickrUser;
import io.github.tjg1.library.norilib.clients.Gelbooru;
import io.github.tjg1.library.norilib.clients.HydrusApi;
import io.github.tjg1.library.norilib.clients.SearchClient;
import io.github.tjg1.library.norilib.clients.Shimmie;
import io.github.tjg1.library.norilib.clients.HydrusApi;

/** Service that detects the {@link io.github.tjg1.library.norilib.clients.SearchClient} API type for given URL. */
public class ServiceTypeDetectionService extends IntentService {
  //region Constants (Broadcast result IDs)
  /** Result code returned when the service type was detected successfully. */
  public static final int RESULT_OK = 0x00;
  /** Result code returned when there was a problem with network connectivity. */
  public static final int RESULT_FAIL_NETWORK = 0x01;
  /** Result code returned when the URL given was not a valid URL. */
  public static final int RESULT_FAIL_INVALID_URL = 0x02;
  /** Result code returned when no valid API was found at given URL. */
  public static final int RESULT_FAIL_NO_API = 0x03;
  //endregion

  //region Constants (Broadcast actions)
  /** Action ID of the broadcast sent when the detection has completed. */
  public static final String ACTION_DONE = "io.github.tjg1.library.norilib.service.ServiceTypeDetectionService.done";
  //endregion

  //region Constants (Broadcast intent extra keys)
  /** Parcel ID to package the API endpoint URL to test. */
  public static final String ENDPOINT_URL = "io.github.tjg1.library.norilib.clients.SearchClient.Settings.url";
  /** Parcel ID used to send the result code back to the listening activity. */
  public static final String RESULT_CODE = "io.github.tjg1.library.norilib.service.ServiceTypeDetectionService.resultCode";
  /** Parcel ID used to send the {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings.APIType#ordinal()} value back to the {@link android.content.BroadcastReceiver}. */
  public static final String API_TYPE = "io.github.tjg1.library.norilib.clients.SearchClient.Settings.APIType.ordinal";
  //endregion

  //region Constants (Service detection settings)
  /** Uri schemes to use when detecting services. */
  public static final String[] URI_SCHEMES = {"https", "http"};
  /** Time to wait for the HTTP requests to complete. (Gelbooru tends to be slow :() */
  private static final int REQUEST_TIMEOUT = 15000;
  /** Time to wait for HTTPS requests to complete. */
  private static final int REQUEST_TIMEOUT_TLS = 5000; // Compromise. Not all sites support TLS,
                                                       // so we wait longer for SSL checks to complete.
  //endregion

  //region Constructors
  /** Called by the framework to instantiate the {@link io.github.tjg1.library.norilib.service.ServiceTypeDetectionService}. */
  public ServiceTypeDetectionService() {
    super("io.github.tjg1.library.norilib.ServiceTypeDetectionService");
  }
  //endregion

  //region IntentService methods (onHandleIntent)
  @Override
  protected void onHandleIntent(Intent intent) {
    // Extract SearchClient.Settings from the received Intent.
    final Uri uri = Uri.parse(intent.getStringExtra(ENDPOINT_URL));

    if (uri.getHost() == null || uri.getScheme() == null) {
      // The URL supplied is invalid.
      sendBroadcast(RESULT_FAIL_INVALID_URL, null, null);
      return;
    }

    // Detected API endpoint.
    String apiEndpoint;

    // Check for flickr and E621 (services with hardcoded URLs).
    apiEndpoint = Flickr.detectService(uri);
    if (apiEndpoint != null) {
      sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.FLICKR);
      return;
    }
    apiEndpoint = FlickrUser.detectService(uri);
    if (apiEndpoint != null) {
      sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.FLICKR_USER);
      return;
    }
    apiEndpoint = E621.detectService(uri);
    if (apiEndpoint != null) {
      sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.E621);
      return;
    }

    // Iterate over supported URI schemes for given URL.
    for (String uriScheme : URI_SCHEMES) {
      final Uri baseUri = new Uri.Builder().scheme(uriScheme).encodedAuthority(uri.getHost() + ":" + uri.getPort())
          .path(uri.getPath()).build();
      final int timeout = "https".equals(uriScheme) ? REQUEST_TIMEOUT_TLS : REQUEST_TIMEOUT;

      apiEndpoint = HydrusApi.detectService(this, baseUri, timeout);
      if (apiEndpoint != null) {
        sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.HYDRUS);
        System.out.println(apiEndpoint);
        return;
      }

      apiEndpoint = Danbooru.detectService(this, baseUri, timeout);
      if (apiEndpoint != null) {
        sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.DANBOARD);
        return;
      }

      apiEndpoint = DanbooruLegacy.detectService(this, baseUri, timeout);
      if (apiEndpoint != null) {
        sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.DANBOARD_LEGACY);
        return;
      }

      apiEndpoint = Gelbooru.detectService(this, baseUri, timeout);
      if (apiEndpoint != null) {
        sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.GELBOARD);
        return;
      }

      apiEndpoint = Shimmie.detectService(this, baseUri, timeout);
      if (apiEndpoint != null) {
        sendBroadcast(RESULT_OK, apiEndpoint, SearchClient.Settings.APIType.SHIMMIE);
        return;
      }
    }

    // End of the loop was reached without finding an API endpoint. Send error code to the BroadcastReceiver.
    sendBroadcast(RESULT_FAIL_NO_API, null, null);
  }
  //endregion

  //region Sending result broadcasts
  /**
   * Send result broadcast back to the listening activity.
   *
   * @param resultCode  Result code.
   * @param endpointURL Detected endpoint URL. Can be null.
   * @param apiType     Detected {@link SearchClient.Settings.APIType}.
   **/
  private void sendBroadcast(int resultCode, @Nullable String endpointURL,
                             @Nullable SearchClient.Settings.APIType apiType) {
    final Intent broadcastIntent = new Intent(ACTION_DONE);
    broadcastIntent.putExtra(RESULT_CODE, resultCode);

    if (endpointURL != null) {
      broadcastIntent.putExtra(ENDPOINT_URL, endpointURL);
    }
    if (apiType != null) {
      broadcastIntent.putExtra(API_TYPE, apiType.ordinal());
    }

    sendBroadcast(broadcastIntent);
  }
  //endregion
}
