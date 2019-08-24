/*
 * This file is part of nori.
 * Copyright (c) 2014-2016 Tomasz Jan Góralczyk <tomg@fastmail.uk>
 * License: ISC
 */

package io.github.tjg1.library.norilib.clients;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.IOException;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import io.github.tjg1.library.norilib.Image;
import io.github.tjg1.library.norilib.SearchResult;
import io.github.tjg1.library.norilib.Tag;
import io.github.tjg1.library.norilib.util.HashUtils;

/** {@link io.github.tjg1.library.norilib.clients.SearchClient} for the E621 imageboard. */
public class E621 extends DanbooruLegacy {

  //region Constants
  /** Number of images to fetch with each search. */
  private static final int DEFAULT_LIMIT = 100;
  //endregion

  //region Constructors
  public E621(Context context, String name, String endpoint) {
    super(context, name, endpoint);
  }

  public E621(Context context, String name, String endpoint, String username, String password) {
    super(context, name, endpoint, username, password);
  }
  //endregion

  //region Service detection
  /**
   * Checks if the given URL exposes a supported API endpoint.
   *
   * @param uri URL to test.
   * @return Detected endpoint URL. null, if no supported endpoint URL was detected.
   */
  @Nullable
  public static String detectService(@NonNull Uri uri) {
    final String host = uri.getHost() + ":" + uri.getPort();

    // Check hardcoded URLs.
    if ("c6ce2f20c50fbc7c67fd34489bfb95a8d2ac0de0d4a44c380f8e6a8eea336a6373e8d7c33ab1a23cd64aa62ee7b7a920d0e0245165b337924e26c65f3646641e"
        .equals(HashUtils.sha512(host, "nori")) ||
        "29f0eb150146b597205df6b320ce551762459663b1c2333e29b3d08a0a7fcbc98644bf8e558ceefe8ceb3101463f7a04e14ab990215dce6bdbfb941951bb00fe"
            .equals(HashUtils.sha512(host, "nori")))
      return "https://" + host;

    return null;
  }
  //endregion

  //region SearchClient methods
  @Override
  public Settings getSettings() {
    return new Settings(Settings.APIType.E621, name, apiEndpoint);
  }
  //endregion

  //region Parsing responses
  @Override
  protected String webUrlFromId(String id) {
    return apiEndpoint + "/post/show/" + id;
  }

  @Override
  protected SearchResult parseXMLResponse(String body, String tags, int offset) throws IOException {

    final List<Image> imageList = new ArrayList<>(DEFAULT_LIMIT);

    try {
      DocumentBuilderFactory Factory = DocumentBuilderFactory.newInstance();
      DocumentBuilder Builder = Factory.newDocumentBuilder();
      Document doc = Builder.parse(new InputSource(new StringReader(body)));

      NodeList nList = doc.getElementsByTagName("post");

      for(int i = 0; i < nList.getLength(); i++) {

        Node node = nList.item(i);
        if(node.getNodeType() == Node.ELEMENT_NODE) {

          Element element = (Element) node;

          final Image image = new Image();
          image.searchPage = offset;
          image.searchPagePosition = i;


          image.fileUrl = element.getElementsByTagName("file_url").item(0).getTextContent();
          image.width = Integer.parseInt(element.getElementsByTagName("width").item(0).getTextContent());
          image.height = Integer.parseInt(element.getElementsByTagName("height").item(0).getTextContent());

          image.previewUrl = element.getElementsByTagName("preview_url").item(0).getTextContent();
          image.previewWidth = Integer.parseInt(element.getElementsByTagName("preview_width").item(0).getTextContent());
          image.previewHeight = Integer.parseInt(element.getElementsByTagName("preview_height").item(0).getTextContent());

          image.sampleUrl = element.getElementsByTagName("sample_url").item(0).getTextContent();
          image.sampleWidth = Integer.parseInt(element.getElementsByTagName("sample_width").item(0).getTextContent());
          image.sampleHeight = Integer.parseInt(element.getElementsByTagName("sample_height").item(0).getTextContent());

          image.tags = Tag.arrayFromString(element.getElementsByTagName("tags").item(0).getTextContent(), Tag.Type.GENERAL);
          image.id = element.getElementsByTagName("id").item(0).getTextContent();
          image.webUrl = webUrlFromId(image.id);
          image.parentId = element.getElementsByTagName("parent_id").item(0).getTextContent();
          image.safeSearchRating = Image.SafeSearchRating.fromString(element.getElementsByTagName("rating").item(0).getTextContent());
          image.score = Integer.parseInt(element.getElementsByTagName("score").item(0).getTextContent());
          image.md5 = element.getElementsByTagName("md5").item(0).getTextContent();
          image.createdAt = dateFromString(element.getElementsByTagName("created_at").item(0).getTextContent());

          imageList.add(image);
        }
      }

    }
    catch(Exception e) {
      throw new IOException(e);
    }

    return new SearchResult(imageList.toArray(new Image[imageList.size()]), Tag.arrayFromString(tags), offset);
  }

  /**
   * Create a {@link java.util.Date} object from String date representation used by this API.
   *
   * @param date Date string.
   * @return Date converted from given String.
   */
  @Override
  protected Date dateFromString(String date) throws ParseException {
    final DateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US);

    // Normalise the ISO8601 time zone into a format parse-able by SimpleDateFormat.
    if (!TextUtils.isEmpty(date)) {
      String newDate = date.replace("Z", "+0000");
      if (newDate.length() == 25) {
        newDate = newDate.substring(0, 22) + newDate.substring(23); // Remove timezone colon.
      }
      return DATE_FORMAT.parse(newDate);
    }

    return null;
  }
  //endregion
}
