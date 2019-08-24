/*
 * This file is part of nori.
 * Copyright (c) 2014 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import java.io.IOException;

import io.github.tjg1.library.norilib.BuildConfig;
import io.github.tjg1.library.norilib.SearchResult;

/**
 * Interface for a client consuming a Danbooru style API.
 */
public interface SearchClient {
  //region Constants
  /** Default user agent to use in all HTTP requests. */
  public static final String USER_AGENT = "nori/" + BuildConfig.VERSION_NAME;
  //endregion

  //region Search methods
  /**
   * Fetch first page of results containing images with the given set of tags.
   *
   * @param tags Search query. A space-separated list of tags.
   * @return A {@link io.github.tjg1.library.norilib.SearchResult} containing a set of Images.
   * @throws IOException Network error.
   */
  public SearchResult search(String tags) throws IOException;

  /**
   * Search for images with the given set of tags.
   *
   * @param tags Search query. A space-separated list of tags.
   * @param pid  Page number. (zero-indexed)
   * @return A {@link io.github.tjg1.library.norilib.SearchResult} containing a set of Images.
   * @throws java.io.IOException Network error.
   */
  public SearchResult search(String tags, int pid) throws IOException;

  /**
   * Asynchronously fetch first page of results containing image with the given set of tags.
   *
   * @param tags     Search query. A space-separated list of tags.
   * @param callback Callback listening for the SearchResult returned in the background.
   */
  public void search(String tags, SearchCallback callback);

  /**
   * Asynchronously search for images with the given set of tags.
   *
   * @param tags     Search query. A space-separated list of tags.
   * @param pid      Page number. (zero-indexed)
   * @param callback Callback listening for the SearchResult returned in the background.
   */
  public void search(String tags, int pid, SearchCallback callback);
  //endregion

  //region Default query
  /**
   * Get a SafeSearch default query to search for when an app is launched.
   *
   * @return Safe-for-work query to search for when an app is launched.
   */
  public String getDefaultQuery();
  //endregion

  //region API Authentication Types
  /**
   * Check if the API server requires or supports optional authentication.
   * <p/>
   * This is used in the API server settings activity as follows:
   * If REQUIRED, the user will need to supply valid credentials.
   * If OPTIONAL, the credential form is shown, but can be left empty.
   * If NONE, the credential form will not be shown.
   *
   * @return {@link io.github.tjg1.library.norilib.clients.SearchClient.AuthenticationType} value for this API backend.
   */
  public abstract AuthenticationType requiresAuthentication();

  /** API authentication types */
  public enum AuthenticationType {
    REQUIRED,
    OPTIONAL,
    NONE
  }
  //endregion

  //region Search callback inner interface
  /** Callback listening for an {@link io.github.tjg1.library.norilib.SearchResult} from an asynchronous request fetched on a background thread. */
  public static interface SearchCallback {
    /**
     * Called when the request could not be executed due to cancellation, a connectivity problem or timeout.
     *
     * @param e Exception that caused the failure.
     */
    public void onFailure(IOException e);

    /**
     * Called when the SearchResult was successfully returned by the remote server.
     *
     * @param searchResult Search result.
     */
    public void onSuccess(SearchResult searchResult);
  }
  //endregion

  //region API Settings getter + inner class
  /**
   * Get a serializable {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings} object with this
   * {@link io.github.tjg1.library.norilib.clients.SearchClient}'s settings.
   *
   * @return A serializable {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings} object.
   */
  public Settings getSettings();

  /** Class used to handle storing, serializing and deserializing {@link io.github.tjg1.library.norilib.clients.SearchClient} settings. */
  public static class Settings implements Parcelable {

    //region Parcelable
    // Parcelable are the standard Android serialization API used to retain data between sessions.
    /** Class loader used when deserializing from a {@link android.os.Parcel}. */
    public static final Creator<Settings> CREATOR = new Creator<Settings>() {

      @Override
      public Settings createFromParcel(Parcel source) {
        // Use the parcel constructor.
        return new Settings(source);
      }

      @Override
      public Settings[] newArray(int size) {
        return new Settings[size];
      }
    };

    /**
     * Constructor used to deserialize a {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings} object from
     * a parcel.
     *
     * @param in Parcel to deserialize from.
     */
    protected Settings(Parcel in) {
      this.apiType = APIType.values()[in.readInt()];
      this.name = in.readString();
      this.endpoint = in.readString();
      // Read authentication credentials, if available.
      if (in.readByte() == 0x01) {
        username = in.readString();
        password = in.readString();
      } else {
        username = null;
        password = null;
      }
    }

    @Override
    public int describeContents() {
      // Describe API type.
      return apiType.ordinal();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
      dest.writeInt(apiType.ordinal());
      dest.writeString(name);
      dest.writeString(endpoint);
      if (username != null && password != null) {
        dest.writeByte((byte) 0x01);
        dest.writeString(username);
        dest.writeString(password);
      } else {
        dest.writeByte((byte) 0x00);
      }
    }
    //endregion

    //region Instance fields
    /** {@link SearchClient} type. */
    private final APIType apiType;
    /** Human-readable service name. */
    private final String name;
    /** API server endpoint URL. */
    private final String endpoint;
    /** API authentication URL. */
    private final String username;
    /** API authentication password/API key. */
    private final String password;
    //endregion

    //region Constructors
    public Settings(APIType apiType, String name, String endpoint) {
      this(apiType, name, endpoint, null, null);
    }

    public Settings(APIType apiType, String name, String endpoint, String username, String password) {
      this.apiType = apiType;
      this.name = name;
      this.endpoint = endpoint;
      this.username = username;
      this.password = password;
    }
    //endregion

    //region Getters
    /** Get the {@link io.github.tjg1.library.norilib.clients.SearchClient} type used by the API endpoint */
    public APIType getApiType() {
      return apiType;
    }

    /** Get human readable name of the API endpoint. */
    public String getName() {
      return name;
    }

    /** Get the server endpoint URL. */
    public String getEndpoint() {
      return endpoint;
    }

    /** Get the server endpoint username. */
    public String getUsername() {
      return username;
    }

    /** Get the server endpoint password. */
    public String getPassword() {
      return password;
    }
    //endregion

    //region SearchClient deserialization
    /**
     * Create a {@link io.github.tjg1.library.norilib.clients.SearchClient} from this {@link io.github.tjg1.library.norilib.clients.SearchClient.Settings} object.
     *
     * @return A {@link io.github.tjg1.library.norilib.clients.SearchClient} created using settings from this object.
     */
    public SearchClient createSearchClient(Context context) {
      switch (apiType) {
        case DANBOARD:
          return new Danbooru(context, name, endpoint, username, password);
        case DANBOARD_LEGACY:
          return new DanbooruLegacy(context, name, endpoint, username, password);
        case SHIMMIE:
          return new Shimmie(context, name, endpoint, username, password);
        case GELBOARD:
          return new Gelbooru(context, name, endpoint, username, password);
        case E621:
          return new E621(context, name, endpoint, username, password);
        case FLICKR:
          return new Flickr(context, name, endpoint);
        case FLICKR_USER:
          return new FlickrUser(context, name, endpoint);
        case HYDRUS:
          return new HydrusApi(context, name, endpoint, username, password);
        default:
          return null;
      }
    }
    //endregion

    //region API type enumeration
    /** API client types used to construct an appropriate {@link SearchClient} from this Settings object. */
    public enum APIType {
      DANBOARD,
      DANBOARD_LEGACY,
      GELBOARD,
      SHIMMIE,
      E621,
      FLICKR,
      FLICKR_USER,
      HYDRUS
    }
    //endregion
  }
  //endregion
}
