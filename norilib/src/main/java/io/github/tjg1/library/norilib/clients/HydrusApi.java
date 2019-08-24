package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.koushikdutta.async.DataEmitter;
import com.koushikdutta.async.DataSink;
import com.koushikdutta.async.callback.CompletedCallback;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.async.future.TransformFuture;
import com.koushikdutta.async.parser.AsyncParser;
import com.koushikdutta.async.parser.StringParser;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.Response;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Type;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import io.github.tjg1.library.norilib.BuildConfig;
import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;

public class HydrusApi implements SearchClient {

    private static final int DEFAULT_LIMIT = 100;

    //region Service configuration instance fields
    /** Android context. */
    protected final Context context;
    /** Human-readable service name. */
    protected final String name;
    /** URL to the HTTP API Endpoint - the server implementing the API. */
    protected final String apiEndpoint;
    /** Username used for authentication. (optional) */
    protected final String username;
    /** Password used for authentication. (optional) */
    protected final String password;
    //endregion




    /**
     * Create a new Danbooru 1.x client without authentication.
     *
     * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
     */
    public HydrusApi(Context context, String name, String endpoint) {
        this.context = context;
        this.name = name;
        this.apiEndpoint = endpoint;
        this.username = null;
        this.password = null;
    }

    /**
     * Create a new Danbooru 1.x client with authentication.
     *
     * @param endpoint URL to the HTTP API Endpoint - the server implementing the API.
     * @param username Username used for authentication.
     * @param password Password used for authentication.
     */
    public HydrusApi(Context context, String name, String endpoint, String username, String password) {
        this.context = context;
        this.name = name;
        this.apiEndpoint = endpoint;
        this.username = username;
        this.password = password;
    }
    //endregion



    //region SearchClient methods
    @Override
    public SearchResult search(String tags) throws IOException {
        // Return results for page 0.
        return search(tags, 0);
    }

    @Override
    public SearchResult search(String tags, int pid) throws IOException {
        try {
            if (!TextUtils.isEmpty(this.password)) {

                System.out.println(createSearchURL(tags, pid, DEFAULT_LIMIT, this.password));

                return Ion.with(this.context)
                        .load(createSearchURL(tags, pid, DEFAULT_LIMIT, this.password))
                        .userAgent(SearchClient.USER_AGENT)
                        .as(new HydrusApi.SearchResultParser(tags, pid))
                        .get();
            } else {
                throw new IOException();
            }
        } catch (InterruptedException | ExecutionException e) {
            // Normalise exception to IOException, so method signatures are not tied to a single HTTP
            // library.
            throw new IOException(e);
        }
    }

    @Override
    public void search(String tags, SearchCallback callback) {
        // Return results for page 0.
        search(tags, 0, callback);
    }


    public void search(final String tags, final int pid, final SearchCallback callback) {
        // Define the ion callback. Not using FutureCallbacks as parameters, so the method signatures
        // are not tied to a single download library.
        FutureCallback<SearchResult> futureCallback = new FutureCallback<SearchResult>() {
            @Override
            public void onCompleted(Exception e, SearchResult result) {
                if (e != null) {
                    callback.onFailure(new IOException(e));
                } else {
                    callback.onSuccess(result);
                }
            }
        };

        // Handle authentication.
        if (!TextUtils.isEmpty(this.password)) {

            System.out.println(createSearchURL(tags, pid, DEFAULT_LIMIT, this.password));

            Ion.with(this.context)
                    .load(createSearchURL(tags, pid, DEFAULT_LIMIT, this.password))
                    .userAgent(SearchClient.USER_AGENT)
                    .as(new HydrusApi.SearchResultParser(tags, pid))
                    .setCallback(futureCallback);
        } else {
            Ion.with(this.context)
                    .load(createSearchURL(tags, pid, DEFAULT_LIMIT, this.password))
                    .userAgent(SearchClient.USER_AGENT)
                    .as(new HydrusApi.SearchResultParser(tags, pid))
                    .setCallback(futureCallback);
        }
    }


    @Override
    public Settings getSettings() {
        return new Settings(Settings.APIType.HYDRUS, name, apiEndpoint, username, password);
    }


    @Override
    public String getDefaultQuery() {
        // Show all safe-for-work images by default.
        return "";
    }

    @Override
    public AuthenticationType requiresAuthentication() {
        return AuthenticationType.REQUIRED;
    }


    //region Creating Search URLs
    /**
     * Generate request URL to the search API endpoint.
     *
     * @param tags  Space-separated tags.
     * @param pid   Page number (0-indexed).
     * @param limit Images to fetch per page.
     * @return URL to search results API.
     */
    protected String createSearchURL(String tags, int pid, int limit, String apiKey) {

        String result;
        if (tags.equals("")) {
            result = String.format(Locale.US, apiEndpoint + "/get_files/search_files?system_inbox=true&system_archive=true&Hydrus-Client-API-Access-Key=%s", apiKey);
        }
        else {

            result = String.format(Locale.US, apiEndpoint + "/get_files/search_files?system_inbox=true&system_archive=true&tags=%s&Hydrus-Client-API-Access-Key=%s", Uri.encode(new Gson().toJson(tags.split("\\s+"))), apiKey);
        }
        System.out.println("Created search URL: " + result);
        return result;
    }
    //endregion




    //region Service detection
    /**
     * Checks if the given URL exposes a supported API endpoint.
     *
     * @param context Android {@link Context}.
     * @param uri     URL to test.
     * @param timeout Timeout in milliseconds.
     * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
     */
    @Nullable
    public static String detectService(@NonNull Context context, @NonNull Uri uri, int timeout) {
        final String endpointUrl = Uri.withAppendedPath(uri, "api_version").toString();

        try {
            final Response<DataEmitter> response = Ion.with(context)
                    .load(endpointUrl)
                    .setTimeout(timeout)
                    .userAgent(SearchClient.USER_AGENT)
                    .followRedirect(false)
                    .noCache()
                    .asDataEmitter()
                    .withResponse()
                    .get();

            // Close the connection.
            final DataEmitter dataEmitter = response.getResult();
            if (dataEmitter != null) dataEmitter.close();

            if (response.getHeaders().code() == 200) {
                return uri.toString();
            }
        } catch (InterruptedException | ExecutionException ignored) {
        }
        return null;
    }
    //endregion
















    //region Parsing responses
    /**
     * Parse an XML response returned by the API.
     *
     * @param body   HTTP Response body.
     * @param tags   Tags used to retrieve the response.
     * @param offset Current paging offset.
     * @return A {@link io.github.tjg1.library.norilib.SearchResult} parsed from given XML.
     */
    protected SearchResult parseXMLResponse(String body, String tags, int offset) throws IOException {
        // Create variables to hold the values as XML is being parsed.
        final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);
        int position = 0;

        try {
            JSONObject owo = new JSONObject(body);

            JSONArray uwu = owo.getJSONArray("file_ids");

            ArrayList<String> ids = new Gson().fromJson(uwu.toString(), new TypeToken<List<String>>(){}.getType());

            for (int i = offset * DEFAULT_LIMIT; i < (offset+1) * DEFAULT_LIMIT && i < ids.size(); i++) {

                final Image image = new Image();
                image.searchPage = offset;
                image.searchPagePosition = i;


                image.fileUrl = this.apiEndpoint + "/get_files/file?Hydrus-Client-API-Access-Key=" + this.password + "&file_id=" + ids.get(i);
                image.width = 500; //Integer.parseInt(element.getElementsByTagName("width").item(0).getTextContent());
                image.height = 500; //Integer.parseInt(element.getElementsByTagName("height").item(0).getTextContent());

                image.previewUrl = this.apiEndpoint + "/get_files/thumbnail?Hydrus-Client-API-Access-Key=" + this.password + "&file_id=" + ids.get(i);
                image.previewWidth = 500; //Integer.parseInt(element.getElementsByTagName("preview_width").item(0).getTextContent());
                image.previewHeight = 500; //Integer.parseInt(element.getElementsByTagName("preview_height").item(0).getTextContent());

                image.sampleUrl = this.apiEndpoint + "/get_files/file?Hydrus-Client-API-Access-Key=" + this.password + "&file_id=" + ids.get(i);
                image.sampleWidth = 500; //Integer.parseInt(element.getElementsByTagName("sample_width").item(0).getTextContent());
                image.sampleHeight = 500; //Integer.parseInt(element.getElementsByTagName("sample_height").item(0).getTextContent());

                //image.tags = Tag.arrayFromString(element.getElementsByTagName("tags").item(0).getTextContent(), Tag.Type.GENERAL);
                image.id = (String) ids.get(i);
                //image.webUrl = webUrlFromId(image.id);
                //image.parentId = element.getElementsByTagName("parent_id").item(0).getTextContent();
                //image.safeSearchRating = Image.SafeSearchRating.fromString(element.getElementsByTagName("rating").item(0).getTextContent());
                //image.score = Integer.parseInt(element.getElementsByTagName("score").item(0).getTextContent());
                //image.md5 = element.getElementsByTagName("md5").item(0).getTextContent();
                //image.createdAt = dateFromString(element.getElementsByTagName("created_at").item(0).getTextContent());


                image.tags = Tag.arrayFromString("this does not work");
                image.webUrl =  this.apiEndpoint + "/get_files/file?Hydrus-Client-API-Access-Key=" + this.password + "&file_id=" + ids.get(i);
                image.parentId = null;
                image.safeSearchRating = Image.SafeSearchRating.S;
                image.score = 0;
                image.md5 = "2d57d21f35e060a4c5e81c03aea3efa8"; // not implemented
                image.createdAt = new Date();


                imageList.add(image);
            }
        }
        catch (Exception e) {
            System.err.println(e.toString());
            throw new IOException(e);
        }
        // Create and return a SearchResult.
        return new SearchResult(imageList.toArray(new Image[0]), Tag.arrayFromString(tags), offset);
    }

    /**
     * Convert a relative image URL to an absolute URL.
     *
     * @param url URL to convert.
     * @return Absolute URL.
     */
    protected String normalizeUrl(String url) throws java.net.MalformedURLException {
        // Return empty string for empty URLs.
        if (url == null || url.isEmpty()) {
            return "";
        }
        return new URL(new URL(apiEndpoint), url).toString();
    }

    /**
     * Get a URL viewable in the system web browser for given Image ID.
     *
     * @param id {@link io.github.tjg1.library.norilib.Image} ID.
     * @return URL for viewing the image in the browser.
     */
    protected String webUrlFromId(String id) {
        return apiEndpoint + "/post/show/" + id;
    }

    /**
     * Create a {@link java.util.Date} object from String date representation used by this API.
     *
     * @param date Date string.
     * @return Date converted from given String.
     */
    protected Date dateFromString(String date) throws ParseException {
        // Parser for the date format used by upstream Danbooru 1.x.
        final DateFormat DATE_FORMAT_DEFAULT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);

        if (TextUtils.isDigitsOnly(date)) {
            // Moebooru-based boards (Danbooru 1.x fork) use Unix timestamps.
            return new Date(Integer.valueOf(date));
        } else {
            return DATE_FORMAT_DEFAULT.parse(date);
        }
    }
    //endregion

    //region Ion async SearchResult parser
    /** Asynchronous search parser to use with ion. */
    protected class SearchResultParser implements AsyncParser<SearchResult> {
        /** Tags searched for. */
        private final String tags;
        /** Current page offset. */
        private final int pageOffset;

        public SearchResultParser(String tags, int pageOffset) {
            this.tags = tags;
            this.pageOffset = pageOffset;
        }

        @Override
        public Future<SearchResult> parse(DataEmitter emitter) {
            return new StringParser().parse(emitter)
                    .then(new TransformFuture<SearchResult, String>() {
                        @Override
                        protected void transform(String result) throws Exception {
                            setComplete(parseXMLResponse(result, tags, pageOffset));
                        }
                    });
        }

        @Override
        public void write(DataSink sink, SearchResult value, CompletedCallback completed) {
            // Not implemented.
        }

        @Override
        public Type getType() {
            return null;
        }
    }
    //endregion

}
